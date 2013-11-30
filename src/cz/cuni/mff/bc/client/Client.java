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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;
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
    private String clientID;
    private String serverAddress;
    private int serverPort;
    private String downloadDir;
    private String uploadDir;
    private StandartRemoteProvider standartRemoteProvider;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());
    private CustomHandler logHandler;
    private Connector connector;
    private ClientCommands commands;
    private GConsole con;
    private ExecutorService executor = Executors.newCachedThreadPool();

    public Client() {
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

    public void initialize() {
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
                        commander.start(new StandartRemoteProvider(connector.getRemoteService(), clientID,
                                Paths.get(downloadDir), Paths.get(uploadDir), currentJar, LOG));
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
            this.serverPort = port;
            LOG.log(Level.INFO, "Server port is now set to: {0}", serverPort);
            propMan.setProperty("port", serverPort + "");
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public void setServerAddress(String serverIPAddress) throws UnknownHostException {
        serverAddress = InetAddress.getByName(serverIPAddress).getHostAddress();
        LOG.log(Level.INFO, "Server address is now set to: {0}", serverAddress);
        propMan.setProperty("address", serverAddress);
    }

    public String getServerAddress() {
        return serverAddress.toString();
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public boolean setDownloadDir(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            downloadDir = f.getAbsolutePath();
            LOG.log(Level.INFO, "Download dir is set to: {0}", downloadDir);
            propMan.setProperty("downloadDir", downloadDir);
            return true;
        } else {
            if (f.mkdirs()) {
                downloadDir = f.getAbsolutePath();
                LOG.log(Level.INFO, "Download dir is set to: {0}", downloadDir);
                propMan.setProperty("downloadDir", downloadDir);
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
            uploadDir = f.getAbsolutePath();
            LOG.log(Level.INFO, "Upload dir is set to: {0}", uploadDir);
            propMan.setProperty("uploadDir", uploadDir);
            return true;
        } else {
            if (f.mkdirs()) {
                uploadDir = f.getAbsolutePath();
                LOG.log(Level.INFO, "Upload dir is set to: {0}", uploadDir);
                propMan.setProperty("uploadDir", uploadDir);
                return true;
            } else {
                LOG.log(Level.WARNING, "Path {0} is not correct path", dir);
                return false;
            }
        }
    }

    public void setClientName(String newClientID) {
        clientID = newClientID;
        LOG.log(Level.INFO, "Client name is now set to: {0}", clientID);
        propMan.setProperty("name", clientID);
    }

    public String getClientName() {
        return clientID;
    }

    public String getUploadDir() {
        return uploadDir;
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
            if (connector.connect(serverAddress, serverPort, clientID)) {
                standartRemoteProvider = new StandartRemoteProvider(connector.getRemoteService(), clientID,
                        Paths.get(downloadDir), Paths.get(uploadDir), LOG);
                LOG.log(Level.INFO, "Connected to the server {0}:{1} with client ID {2}", new Object[]{serverAddress, serverPort, clientID});
                standartRemoteProvider.hasClientTasksInProgress();
            } else {
                LOG.log(Level.INFO, "Client ID \"{0}\" is already in the system ", clientID);
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
