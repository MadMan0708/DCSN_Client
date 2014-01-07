/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.client.misc.PropertiesManager;
import cz.cuni.mff.bc.client.misc.IConsole;
import cz.cuni.mff.bc.client.misc.GConsole;
import cz.cuni.mff.bc.api.main.Commander;
import cz.cuni.mff.bc.api.main.JarAPI;
import cz.cuni.mff.bc.api.main.StandartRemoteProvider;
import cz.cuni.mff.bc.client.logging.CustomFormater;
import cz.cuni.mff.bc.client.logging.CustomHandler;
import cz.cuni.mff.bc.client.logging.FileLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Aku
 */
public class Client implements IConsole {

    private PropertiesManager propMan;
    private StandartRemoteProvider standartRemoteProvider;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());
    private CustomHandler logHandler;
    private Connector connector;
    private ClientCommands commands;
    private GConsole con;
    private ClientParameters params;
    private ExecutorService executor = Executors.newCachedThreadPool();

    public Client() {
        this.params = new ClientParameters();
        logHandler = new CustomHandler();
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);
        logHandler.addLogTarget(new FileLogger(new File("client.log")));
        LOG.addHandler(logHandler);
        connector = new Connector();
        propMan = new PropertiesManager("client.config.properties", logHandler);
        commands = new ClientCommands(this);
    }

    public StandartRemoteProvider getRemoteProvider() {
        return standartRemoteProvider;
    }

    public boolean remoteProviderAvailable() {
        if (standartRemoteProvider == null) {
            return false;
        } else {
            return true;
        }
    }

    public StandartRemoteProvider getStandartRemoteProvider() {
        return standartRemoteProvider;
    }

    private void setDefaultAddress() {
        try {
            setServerAddress("localhost");
        } catch (UnknownHostException e) {
            // tested, can not entry to this part
        }
    }

    private void setDefaultPort() {
        setServerPort(1099);
    }

    private void setDefaulClientName() {
        setClientName(System.getProperty("user.name"));
    }

    private void setDefaultDownloadDir() {
        setDownloadDir(System.getProperty("user.home") + File.separator + "DCSN_downloaded");
    }

    private void setDefaultUploadDir() {
        setUploadDir(System.getProperty("user.home") + File.separator + "DCSN_uploaded");
    }

    private void setDefaultCores() {
        setCores(4);
    }

    private void setDefaultMemory() {
        setMemory(125);
    }

    public void initialize() {
        if (propMan.getProperty("cores") == null) {
            setDefaultCores();
        } else {
            try {
                setCores(Integer.parseInt(propMan.getProperty("cores")));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Number of cores has to be positive integer");
                setDefaultCores();
            }
        }

        if (propMan.getProperty("memory") == null) {
            setDefaultMemory();
        } else {
            try {
                setMemory(Integer.parseInt(propMan.getProperty("memory")));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Amount of memory has to be positive integer");
                setDefaultMemory();
            }
        }

        if (propMan.getProperty("name") == null) {
            setDefaulClientName();
        } else {
            setClientName(propMan.getProperty("name"));
        }

        if (propMan.getProperty("address") == null) {
            setDefaultAddress();
        } else {
            try {
                setServerAddress(propMan.getProperty("address"));
            } catch (UnknownHostException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Not correct host or IP address: {0}", e.getMessage());
                setDefaultAddress();
            }
        }

        if (propMan.getProperty("port") == null) {
            setDefaultPort();
        } else {
            int tmpPort = Integer.parseInt(propMan.getProperty("port"));
            try {
                setServerPort(tmpPort);
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Port number has to be between 1 - 65535");
                setDefaultPort();
            }
        }

        if (propMan.getProperty("downloadDir") == null) {
            setDefaultDownloadDir();
        } else {
            if (!setDownloadDir(propMan.getProperty("downloadDir"))) {
                setDefaultDownloadDir();
            }
        }

        if (propMan.getProperty("uploadDir") == null) {
            setDefaultUploadDir();
        } else {
            if (!setUploadDir(propMan.getProperty("uploadDir"))) {
                setDefaultUploadDir();
            }
        }

        searchForServer();
    }

    private void searchForServer() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = "DISCOVER_SERVER_REQUEST".getBytes();
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), getServerPort());
                socket.send(sendPacket);
                LOG.log(Level.INFO, "Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
            }
            // Broadcast the message over all the network interfaces
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, getServerPort());
                        socket.send(sendPacket);
                        LOG.log(Level.INFO, "Request packet sent to: {0}; Interface: {1}", new Object[]{broadcast.getHostAddress(), networkInterface.getDisplayName()});
                    } catch (Exception e) {
                    }


                }
            }

            LOG.log(Level.INFO, "Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(receivePacket);

            //We have a response
            LOG.log(Level.INFO, "Broadcast response from server: {0}", receivePacket.getAddress().getHostAddress());
            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals("DISCOVER_SERVER_RESPONSE")) {
                // now we have server ip address
                commands.setServerAddress(new String[]{receivePacket.getAddress().toString()});
                connect();
            }
            //Close the port!
            socket.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public void createProjectFrom(Path jar, Path destination, String value) {
        try {
            JarAPI.createJarWithChangedAttributeValue(jar, destination, "Project-Name", value);
            LOG.log(Level.INFO, "New project from file \"{0}\" has been created, with projec name: {1}", new Object[]{jar.toString(), value});
        } catch (FileNotFoundException e) {
            LOG.log(Level.WARNING, "Project file not exists on written path: {0}", e.toString());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Project file could not be load: {0}", e.toString());
        }

    }

    public Future<?> startAutoProccess(Path jar) {
        Future<?> f = null;
        final Path currentJar = jar;
        try (JarInputStream jarStream = new JarInputStream(new FileInputStream(jar.toFile()))) {
            Manifest mf = jarStream.getManifest();
            Attributes attr = mf.getMainAttributes();
            String commanderName = attr.getValue("Main-Commander-Class");
            try {
                ClassLoader cl = new URLClassLoader(new URL[]{jar.toUri().toURL()});

                final Class<?> clazz = cl.loadClass(commanderName);
                final Commander commander = (Commander) clazz.newInstance();
                f = executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        commander.start(new StandartRemoteProvider(connector.getRemoteService(), params.getClientName(),
                                Paths.get(params.getDownloadDir()), Paths.get(params.getUploadDir()), currentJar, LOG));
                    }
                });
            } catch (InstantiationException | MalformedURLException | ClassNotFoundException | IllegalAccessException | SecurityException e) {
                LOG.log(Level.WARNING, e.toString());
            }
        } catch (FileNotFoundException e) {
            LOG.log(Level.WARNING, "Project file not exists on written path: {0}", e.toString());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Project file couldn't be load: {0}", e.toString());
        }
        return f;
    }

    /**
     *
     * @param pathToFile path to file in string
     * @return if string contained file separator, return it's path, otherwise
     * return path in default upload folder
     * @throws IllegalArgumentException
     */
    public Path getUploadFileLocation(String pathToFile) throws IllegalArgumentException {
        Path path = Paths.get(pathToFile);
        if (!pathToFile.contains(File.separator)) {
            return new File(getUploadDir(), path.toFile().getName()).toPath();
        } else {
            return path;
        }
    }

    public void autoMode(String pathTojar) {
        try {
            Path projectJar = getUploadFileLocation(pathTojar);
            connect();
            Future<?> f = startAutoProccess(projectJar);
            while (!f.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
            disconnect();
            exitClient();
        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Incorrect path");
        }
    }

    public void exitClient() {
        if (connector != null) {
            if (connector.isRecievingTasks()) {
                stopRecievingTasks(true);
            }
        }
        System.exit(0);
    }

    public void setServerIPPort(String connDetails) throws UnknownHostException, IllegalArgumentException {
        String pattern = "^(?<IP>.*):(?<port>\\d+)$";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(connDetails);
        if (m.find()) {
            setServerAddress(m.group("IP"));
            setServerPort(Integer.parseInt(m.group("port")));
        } else {
            setServerAddress(connDetails);
        }
    }

    private boolean validatePort(int port) {
        if (port >= 1 && port <= 65535) {
            return true;
        } else {
            return false;
        }
    }

    public void setServerPort(int port) throws IllegalArgumentException {
        if (validatePort(port)) {
            params.setServerPort(port);
            LOG.log(Level.INFO, "Server port is now set to: {0}", params.getServerPort());
            propMan.setProperty("port", params.getServerPort() + "");
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getServerPort() {
        return params.getServerPort();
    }

    public void setMemory(int memory) {
        params.setMemory(memory);
        LOG.log(Level.INFO, "Amount of memory allowed is now set to: {0}", memory);
        propMan.setProperty("memory", memory + "");
    }

    public int getMemory() {
        return params.getMemory();
    }

    public void setCores(int cores) {
        params.setCores(cores);
        LOG.log(Level.INFO, "Number of cores allowed is now set to: {0}", cores);
        propMan.setProperty("memory", cores + "");
    }

    public int getCores() {
        return params.getCores();
    }

    public void setServerAddress(String serverIPAddress) throws UnknownHostException {
        params.setServerAddress(InetAddress.getByName(serverIPAddress).getHostAddress());
        LOG.log(Level.INFO, "Server address is now set to: {0}", params.getServerAddress());
        propMan.setProperty("address", params.getServerAddress());
    }

    public String getServerAddress() {
        return params.getServerAddress();
    }

    public String getDownloadDir() {
        return params.getDownloadDir();
    }

    public boolean setDownloadDir(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            params.setDownloadDir(f.getAbsolutePath());
            LOG.log(Level.INFO, "Download dir is set to: {0}", params.getDownloadDir());
            propMan.setProperty("downloadDir", params.getDownloadDir());
            return true;
        } else {
            if (f.mkdirs()) {
                params.setDownloadDir(f.getAbsolutePath());
                LOG.log(Level.INFO, "Download dir is set to: {0}", params.getDownloadDir());
                propMan.setProperty("downloadDir", params.getDownloadDir());
                return true;
            } else {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }
        }
    }

    public boolean setUploadDir(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            params.setUploadDir(f.getAbsolutePath());
            LOG.log(Level.INFO, "Upload dir is set to: {0}", params.getUploadDir());
            propMan.setProperty("uploadDir", params.getUploadDir());
            return true;
        } else {
            if (f.mkdirs()) {
                params.setUploadDir(f.getAbsolutePath());
                LOG.log(Level.INFO, "Upload dir is set to: {0}", params.getUploadDir());
                propMan.setProperty("uploadDir", params.getUploadDir());
                return true;
            } else {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }
        }
    }

    public void setClientName(String clientName) {
        params.setClientName(clientName);
        LOG.log(Level.INFO, "Client name is now set to: {0}", clientName);
        propMan.setProperty("name", clientName);
    }

    public String getClientName() {
        return params.getClientName();
    }

    public String getUploadDir() {
        return params.getUploadDir();
    }

    public void disconnect() {
        try {
            connector.disconnect();
            LOG.log(Level.INFO, "Client {0} was successfully disconnected", getClientName());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Disconnecting unsuccessful due to network erro:{0}", e.getMessage());
        }
    }

    public void connect() {
        try {
            if (connector.connect(params.getServerAddress(), params.getServerPort(), params.getClientName())) {
                standartRemoteProvider = new StandartRemoteProvider(connector.getRemoteService(), params.getClientName(),
                        Paths.get(params.getDownloadDir()), Paths.get(params.getUploadDir()), LOG);
                LOG.log(Level.INFO, "Connected to the server {0}:{1} with client ID {2}", new Object[]{params.getServerAddress(), params.getServerPort(), params.getClientName()});
                standartRemoteProvider.hasClientTasksInProgress();
            } else {
                LOG.log(Level.INFO, "Client ID \"{0}\" is already in the system ", params.getClientName());
            }
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Problem during creating session: {0}", e.getMessage());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Connection problem: {0}", e.getMessage());
        }
    }

    public void startRecievingTasks() {
        try {
            connector.startCalculation();
            LOG.log(Level.INFO, "Client is now participating in task computation");
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Not Connected to the server.");
        }
    }

    public void stopRecievingTasks(boolean force) {
        try {
            if (connector.isRecievingTasks()) {
                connector.stopCalculation(force);
                if (force) {
                    LOG.log(Level.INFO, "Client has stopped computation of tasks");
                } else {
                    LOG.log(Level.INFO, "Client is finishing currently calculated tasks");
                }

            } else {
                LOG.log(Level.INFO, "Client is not participating in task computation");
            }
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Problem during disconnection from server.");
        }
    }

    @Override
    public void proceedCommand(String command) {
        String[] cmd = ClientCommands.parseCommand(command);
        String[] params = Arrays.copyOfRange(cmd, 1, cmd.length);
        try {
            Class<?> c = Class.forName("cz.cuni.mff.bc.client.ClientCommands");
            Method method = c.getMethod(cmd[0], new Class[]{String[].class
            });
            method.invoke(commands, new Object[]{params});
        } catch (ClassNotFoundException e) {
            // will be never thrown
        } catch (IllegalAccessException e) {
        } catch (IllegalArgumentException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
            LOG.log(Level.WARNING, "No such command");
        } catch (SecurityException e) {
        }
    }

    @Override
    public void startClassicConsole() {
        new Thread() {
            @Override
            public void run() {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                    while (true) {
                        System.out.print("client>");
                        proceedCommand(br.readLine());
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Problem with reading command from console");
                }
            }
        }.start();
    }

    @Override
    public void startGUIConsole() {
        con = new GConsole(this, "client", new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitClient();
            }
        });
        con.startConsole();
        logHandler.addLogTarget(con);
    }
}
