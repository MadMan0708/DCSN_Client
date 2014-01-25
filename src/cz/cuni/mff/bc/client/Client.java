/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.misc.IConsole;
import cz.cuni.mff.bc.misc.GConsole;
import cz.cuni.mff.bc.api.main.Commander;
import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.api.main.JarAPI;
import cz.cuni.mff.bc.api.main.StandardRemoteProvider;
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

/**
 * Main client class, handles basic client behaviour
 *
 * @author Jakub Hava
 */
public class Client implements IConsole {

    private StandardRemoteProvider standartRemoteProvider;
    private CustomHandler logHandler;
    private Connector connector;
    private ClientCommands commands;
    private GConsole con;
    private ClientParams clientParams;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());
    private ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Constructor
     */
    public Client() {
        logHandler = new CustomHandler();
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);
        logHandler.addLogTarget(new FileLogger(new File("client.log")));
        LOG.addHandler(logHandler);
        clientParams = new ClientParams(logHandler);
        connector = new Connector(clientParams);
        commands = new ClientCommands(this);
    }

    /**
     * Gets client parameters
     *
     * @return client parameters
     */
    public ClientParams getClientParams() {
        return clientParams;
    }

    /**
     * Checks if the client is connected
     *
     * @return true if remote provider exists and client is connected to server,
     * false otherwise
     */
    public boolean isConnected() {
        if (standartRemoteProvider == null) {
            return false;
        } else {
            return standartRemoteProvider.isConnected();
        }
    }

    /**
     *
     * @return standard remote provider
     */
    public StandardRemoteProvider getStandardRemoteProvider() {
        return standartRemoteProvider;
    }

    /**
     * Initialises client
     */
    public void initialise() {
        clientParams.initialisesParameters();
        deleteContentOfTempDirectory();
        CustomIO.recursiveDeleteOnShutdownHook(clientParams.getTemporaryDir()); // set folder to be deleted at the end
        new Thread() {
            @Override
            public void run() {
                searchForServer();

            }
        }.start();
    }

    /*
     * Deletes content of temporary directory
     */
    private void deleteContentOfTempDirectory() {
        File[] files = clientParams.getTemporaryDir().toFile().listFiles();
        for (File file : files) {
            CustomIO.deleteDirectory(file.toPath());
        }
    }

    /**
     * Creates the new jar file from existing one with new project name
     *
     * @param source path to the source jar
     * @param destination path to the destination
     * @param projectName new project name
     */
    public void createProjectFrom(Path source, Path destination, String projectName) {
        try {
            JarAPI.createJarWithChangedAttributeValue(source, destination, "Project-Name", projectName);
            LOG.log(Level.INFO, "New project from file \"{0}\" has been created, with projec name: {1}", new Object[]{source.toString(), projectName});
        } catch (FileNotFoundException e) {
            LOG.log(Level.WARNING, "Project file not exists on written path: {0}", e.toString());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Project file could not be load: {0}", e.toString());
        }

    }

    /**
     * Starts manual processing. Executes start method in client's
     * implementation of Commander class
     *
     * @param jar destination to the project jar
     * @return future representing processing
     */
    public Future<?> startManual(Path jar) {
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
                        commander.start(new StandardRemoteProvider(connector.getRemoteService(), clientParams.getClientName(),
                                clientParams.getDownloadDir(), clientParams.getUploadDir(), currentJar, LOG));
                    }
                });
            } catch (InstantiationException | MalformedURLException | ClassNotFoundException | IllegalAccessException | SecurityException e) {
                LOG.log(Level.WARNING, e.toString());
            }
        } catch (FileNotFoundException e) {
            LOG.log(Level.WARNING, "Project file doesn't exist on written path: {0}", e.toString());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Project file couldn't be load: {0}", e.toString());
        }
        return f;
    }

    /**
     * Returns path to to the file
     *
     * @param pathToFile path to file in string
     * @return if string contains file separator, return it's path, otherwise
     * return path to the default upload folder
     * @throws IllegalArgumentException
     */
    public Path getUploadFileLocation(String pathToFile) throws IllegalArgumentException {
        Path path = Paths.get(pathToFile);
        if (!pathToFile.contains(File.separator)) {
            return Paths.get(clientParams.getUploadDir().toString(), path.toFile().getName());

        } else {
            return path;
        }
    }

    /**
     * This methods connects to the server, executes the start method in
     * client's Commander class, disconnects and closes the client.
     *
     * @param pathTojar path to the project jar
     */
    public void autoMode(String pathTojar) {
        try {
            Path projectJar = getUploadFileLocation(pathTojar);
            if (!isConnected()) {
                connect();
            }
            Future<?> f = startManual(projectJar);
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

    /**
     * Exits the client
     */
    public void exitClient() {
        if (connector != null) {
            if (connector.isReceivingTasks()) {
                stopRecievingTasks(true);
            }
        }
        System.exit(0);
    }

    /**
     * Disconnects from the server
     */
    public void disconnect() {
        try {
            connector.disconnect();
            standartRemoteProvider = null;
            LOG.log(Level.INFO, "Client {0} was successfully disconnected", clientParams.getClientName());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Disconnecting unsuccessful due to network error: {0}", e.getMessage());
        }
    }

    /**
     * Connects to the server
     */
    public void connect() {
        try {
            if (connector.connect()) {
                standartRemoteProvider = new StandardRemoteProvider(connector.getRemoteService(), clientParams.getClientName(),
                        clientParams.getDownloadDir(), clientParams.getUploadDir(), clientParams.getTemporaryDir(), LOG);
                LOG.log(Level.INFO, "Connected to the server {0}:{1} with client ID {2}", new Object[]{clientParams.getServerAddress(), clientParams.getServerPort(), clientParams.getClientName()});
                standartRemoteProvider.hasClientTasksInProgress();
                standartRemoteProvider.setCoresLimit(clientParams.getCores());
                standartRemoteProvider.setMemoryLimit(clientParams.getMemory());
            } else {
                LOG.log(Level.INFO, "Client ID \"{0}\" is already in the system ", clientParams.getClientName());
            }
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Problem during creating session: {0}", e.getMessage());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Connection problem: {0}", e.getMessage());
        }
    }

    /**
     * Starts receiving the tasks
     */
    public void startRecievingTasks() {
        try {
            connector.startCalculation();
            LOG.log(Level.INFO, "Client is now participating in task computation");
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Not Connected to the server.");
        }
    }

    /**
     * Stops receiving the tasks
     *
     * @param force true if the task should be cancelled immedeatelly, false if
     * the method should wait for current task to finish computation
     */
    public void stopRecievingTasks(boolean force) {
        try {
            if (connector.isReceivingTasks()) {
                connector.stopCalculation(force);
                if (force) {
                    LOG.log(Level.INFO, "Client has stopped computation of tasks");
                } else {
                    LOG.log(Level.INFO, "Client is not recieving new tasks");
                }
            } else {
                LOG.log(Level.INFO, "Client is not participating in task computation");
            }
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Problem during disconnection from server.");
        }
    }

    // search for server using broadcast packets
    private void searchForServer() {
        try {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                byte[] sendData = "DISCOVER_SERVER_REQUEST".getBytes();
                try {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            InetAddress.getByName("255.255.255.255"), clientParams.getServerPort());
                    socket.send(sendPacket);
                    LOG.log(Level.INFO, "Request packet sent to: 255.255.255.255 (DEFAULT)");
                } catch (IOException e) {
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
                        // Send the broadcast packet
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, clientParams.getServerPort());
                            socket.send(sendPacket);
                            LOG.log(Level.INFO, "Request packet sent to: {0}; Interface: {1}", new Object[]{broadcast.getHostAddress(), networkInterface.getDisplayName()});
                        } catch (IOException e) {
                        }
                    }
                }
                LOG.log(Level.INFO, "Looping over all network interfaces done. Now waiting for a reply!");

                //Wait for a response
                byte[] recvBuf = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(receivePacket);
                //We have a response
                LOG.log(Level.INFO, "Broadcast response from server: {0}", receivePacket.getAddress().getHostAddress());
                //Check if the message is correct
                String message = new String(receivePacket.getData()).trim();
                if (message.equals("DISCOVER_SERVER_RESPONSE")) {
                    // now we have server IP address
                    String address = receivePacket.getAddress().toString();
                    if (address.startsWith("/")) {
                        address = address.substring(1);
                    }
                    commands.setServerAddress(new String[]{address});
                    connect();
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Problem during sending broadcast packet: {0}", ex);
        }
    }

    @Override
    public void proceedCommand(String command) {
        String[] cmd = ClientCommands.parseCommand(command);
        String[] params = Arrays.copyOfRange(cmd, 1, cmd.length);
        try {
            // don't want to execute functions which aren't real console commands
            switch (command) {
                case "ClientCommands":
                    ;
                case "parseCommand":
                    ;
                case "checkParamNum":
                    throw new NoSuchMethodException();
            }
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
