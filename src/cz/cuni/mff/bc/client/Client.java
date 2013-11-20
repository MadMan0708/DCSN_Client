/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.client.misc.PropertiesManager;
import cz.cuni.mff.bc.client.misc.IConsole;
import cz.cuni.mff.bc.client.misc.GConsole;
import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.Commander;
import cz.cuni.mff.bc.api.main.StandartRemoteProvider;
import cz.cuni.mff.bc.client.logging.CustomFormater;
import cz.cuni.mff.bc.client.logging.CustomHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Handler;
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
    private StandartRemoteProvider standartRemoteProvider;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());
    private Handler logHandler;
    private Connector connector;

    public Client() {
        logHandler = new CustomHandler(new File("client.log"));
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);
        LOG.addHandler(logHandler);
        this.connector = new Connector(logHandler);
        propMan = new PropertiesManager("client.config.properties", logHandler);
    }

    private boolean remoteProviderAvailable() {
        if (standartRemoteProvider == null) {
            return false;
        } else {
            return true;
        }
    }

    public void initialize() {
        if (propMan.getProperty("name") == null) {
            setClientName(System.getProperty("user.name"));
        } else {
            clientID = propMan.getProperty("name");
        }

        if (propMan.getProperty("address") == null) {
            try {
                setServerAddress("localhost");
            } catch (UnknownHostException e) {
                // tested, can not entry to this part
            }
        } else {
            try {
                setServerAddress(propMan.getProperty("address"));
            } catch (UnknownHostException e) {
                LOG.log(Level.WARNING, "INITIALIZING: Not correct host or IP address: {0}", e.getMessage());
            }
        }

        if (propMan.getProperty("port") == null) {
            setServerPort(1099);
        } else {
            int tmpPort = Integer.parseInt(propMan.getProperty("port"));
            if (tmpPort != -1) {
                try {
                    setServerPort(tmpPort);
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "INITIALIZING: Port number has to be between 1 - 65535");
                }
            }
        }

        if (propMan.getProperty("downloadDir") == null) {
            setDownloadDir(System.getProperty("user.home") + File.separator + "DCSN_downloaded");
        } else {
            setDownloadDir(propMan.getProperty("downloadDir"));
        }
    }

    public void loadJar(Path jar) {
        final Path currentJar = jar;
        try (JarInputStream jarStream = new JarInputStream(new FileInputStream(jar.toFile()))) {
            Manifest mf = jarStream.getManifest();
            Attributes attr = mf.getMainAttributes();

            String commanderName = attr.getValue("Main-Commander-Class");
            try {
                ClassLoader cl = new URLClassLoader(new URL[]{jar.toUri().toURL()});

                final Class<?> clazz = cl.loadClass(commanderName);
                final Commander commander = (Commander) clazz.newInstance();
                new Thread() {
                    @Override
                    public void run() {
                        commander.start(new StandartRemoteProvider(connector.getRemoteService(), clientID, currentJar, logHandler));
                    }
                }.start();

            } catch (InstantiationException | MalformedURLException | ClassNotFoundException | IllegalAccessException | SecurityException e) {
                LOG.log(Level.WARNING, e.toString());
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, e.toString());
        }
    }

    private void exitClient() {
        if (connector != null) {
            if (connector.isRecievingTasks()) {
                stopRecievingTasks();
            }
        }
        System.exit(0);
    }

    private void setServerIPPort(String connDetails) throws UnknownHostException, IllegalArgumentException {
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

    private void setServerAddress(String serverIPAddress) throws UnknownHostException {
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

    private boolean setDownloadDir(String dir) {
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

    public void setClientName(String newClientID) {
        clientID = newClientID;
        LOG.log(Level.INFO, "Client ID is now set to: {0}", clientID);
        propMan.setProperty("name", clientID);
    }

    public String getClientName() {
        return clientID;
    }

    private boolean checkParamNum(int expected, String[] args) {
        if (expected == args.length - 1) {
            // It's ok, correct number of parameters
            return true;
        } else {
            return false;
        }
    }

    private void printClientName() {
        if (clientID == null) {
            LOG.log(Level.INFO, "Client name is not set yet!");
        } else {
            LOG.log(Level.INFO, "Client name is set to: {0}", clientID);
        }
    }

    private void printServerAddress() {
        if (serverAddress == null) {
            LOG.log(Level.INFO, "Server adress is not set yet!");
        } else {
            LOG.log(Level.INFO, "Server address is set to : {0}", serverAddress);
        }
    }

    private void printServerPort() {
        if (serverPort == -1) {
            LOG.log(Level.INFO, "Server adress is not set yet!");
        } else {
            LOG.log(Level.INFO, "Server port is set to : {0}", serverPort);
        }
    }

    private void printDownloadDir() {
        if (downloadDir == null) {
            LOG.log(Level.INFO, "Download dir is not set yet!");
        } else {
            LOG.log(Level.INFO, "Download dir is set to : {0}", downloadDir);
        }
    }

    public void disconnect() {
        try {
            connector.disconnect();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Disconnecting unsuccessful due to network erro:{0}", e.getMessage());
        }
    }

    public void connect() {
        try {
            if (connector.connect(serverAddress, serverPort, clientID)) {
                LOG.log(Level.INFO, "Connected to the server {0}:{1} with client ID {2}", new Object[]{serverAddress, serverPort, clientID});
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
            LOG.log(Level.INFO, "Client is now participating in task computation");
            connector.startRecievingTasks();
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Computation couldn''t start due to network error: {0}", e.getMessage());
        }
    }

    private void stopRecievingTasks() {
        try {
            connector.stopRecievingTasks();
            LOG.log(Level.INFO, "Client has stopped computation of tasks");
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Computation couldn''t be stopped to network error: {0}", e.getMessage());
        }
    }

    @Override
    public void proceedCommand(String command) {
        String[] cmd = parseCommand(command);

        switch (cmd[0]) {
            /*    case "automatic": {
             if (checkParamNum(4, cmd)) {
             File alter = new File(cmd[1]);
             File alterClass = new File(cmd[4]);
             if (alter.exists() && !alter.isDirectory() && alter.getName().endsWith(".class")) {
             uploadProject(Paths.get(cmd[1]), cmd[2], Integer.parseInt(cmd[3]));
             automatic(alter, cmd[2], Integer.parseInt(cmd[3]), alterClass, 0);
             }

             } else if (checkParamNum(5, cmd)) {
             File alter = new File(cmd[4]);
             if (alter.exists() && !alter.isDirectory() && alter.getName().endsWith(".class")) {
             uploadProject(Paths.get(cmd[1]), cmd[2], Integer.parseInt(cmd[3]));
             automatic(new File(cmd[1]), cmd[2], Integer.parseInt(cmd[3]), alter, Integer.parseInt(cmd[5]));
             }

             } else {
             logger.log("Expected parameters: 4, 5");
             logger.log("1: Path to file with project");
             logger.log("2: Project name");
             logger.log("3: Project prority");
             logger.log("4: Path to class file implementing IAlert interface");
             logger.log("5(OPTIONAL): Number of cycles, without this parameter it will repeat cycles until user cancel the automatic calculation");

             }
             break;
             }*/
            case "auto": {
                if (checkParamNum(1, cmd)) {
                    String jar = cmd[1];
                    LOG.log(Level.INFO, "Startig automatic proccessing");

                    loadJar(Paths.get(jar));
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Path to project jar file");
                }
            }
            break;
            case "getName": {
                if (checkParamNum(0, cmd)) {
                    printClientName();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "getServerAddress": {
                if (checkParamNum(0, cmd)) {
                    printServerAddress();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "getServerPort": {
                if (checkParamNum(0, cmd)) {
                    printServerPort();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "setName": {
                if (checkParamNum(1, cmd)) {
                    clientID = cmd[1];
                    LOG.log(Level.INFO, "Client name is now set to: {0}", clientID);
                    propMan.setProperty("name", clientID);
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Client new id");
                }
                break;
            }
            case "setServerAddress": {
                if (checkParamNum(1, cmd)) {
                    try {
                        setServerIPPort(cmd[1]);
                    } catch (UnknownHostException e) {
                        LOG.log(Level.WARNING, "Not correct host or IP address: {0}", e.getMessage());
                    } catch (IllegalArgumentException e) {
                        LOG.log(Level.WARNING, "Port number has to be between 1 - 65535");
                    }
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Server new IP address");
                }
                break;
            }
            case "setServerPort": {
                if (checkParamNum(1, cmd)) {
                    try {
                        setServerPort(Integer.parseInt(cmd[1]));
                    } catch (IllegalArgumentException e) {
                        LOG.log(Level.WARNING, "Port number has to be between 1 - 65535");
                    }
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Server new port");
                }
                break;
            }
            case "getDownloadDir": {
                if (checkParamNum(0, cmd)) {
                    printDownloadDir();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;

            }
            case "setDownloadDir": {
                if (checkParamNum(1, cmd)) {
                    setDownloadDir(cmd[1]);
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: new download dir");
                }
                break;

            }
            case "getInfo": {
                if (checkParamNum(0, cmd)) {
                    printClientName();
                    printServerAddress();
                    printServerPort();
                    printDownloadDir();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "startCalculation": {
                if (checkParamNum(0, cmd)) {
                    startRecievingTasks();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "endCalculation": {
                if (checkParamNum(0, cmd)) {
                    stopRecievingTasks();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "connect": {
                if (checkParamNum(0, cmd)) {
                    boolean ready = true;
                    if (clientID == null) {
                        LOG.log(Level.INFO, "Client name has to be set to perform connection!");
                        ready = false;
                    }
                    if (serverAddress == null) {
                        LOG.log(Level.INFO, "Server address has to be set to perform connection!");
                        ready = false;
                    }
                    if (serverPort == -1) {
                        LOG.log(Level.INFO, "Server port has to be set to perform connection!");
                        ready = false;
                    }
                    if (ready) {
                        connect();
                        standartRemoteProvider = new StandartRemoteProvider(connector.getRemoteService(), clientID, logHandler);
                    }
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "disconnect": {
                if (checkParamNum(0, cmd)) {
                    disconnect();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "upload": {
                if (remoteProviderAvailable()) {
                    if (checkParamNum(2, cmd)) {
                        Path projectJar = Paths.get(cmd[1]).toAbsolutePath();
                        Path projectData = Paths.get(cmd[2]).toAbsolutePath();
                        standartRemoteProvider.uploadProject(projectJar, projectData);
                    } else {
                        LOG.log(Level.INFO, "Expected parameters: 2");
                        LOG.log(Level.INFO, "1: Path to project jar");
                        LOG.log(Level.INFO, "2: Path to project data");
                    }
                } else {
                    LOG.log(Level.WARNING, "Not connected");
                }
                break;
            }
            case "download": {
                if (remoteProviderAvailable()) {
                    if (checkParamNum(1, cmd)) {
                        if (downloadDir == null) {
                            LOG.log(Level.WARNING, "Download dir has to be set before downloading project");
                        } else {
                            standartRemoteProvider.download(cmd[1], new File(downloadDir, cmd[1] + ".zip"));
                        }
                    } else {
                        LOG.log(Level.INFO, "Expected parameters: 1");
                        LOG.log(Level.INFO, "1: Project name");
                    }
                } else {
                    LOG.log(Level.WARNING, "Not connected");
                }
                break;
            }


            case "list": {
                if (remoteProviderAvailable()) {
                    if (checkParamNum(1, cmd)) {

                        switch (cmd[1]) {
                            case "all":
                                standartRemoteProvider.printAllProjects();
                                break;
                            case "active":
                                standartRemoteProvider.printProjects(ProjectState.ACTIVE);
                                break;
                            case "paused":
                                standartRemoteProvider.printProjects(ProjectState.PAUSED);
                                break;
                            case "completed":
                                standartRemoteProvider.printProjects(ProjectState.COMPLETED);
                                break;
                            default:
                                LOG.log(Level.INFO, "states which can listed are: all, completed, paused, active");
                        }
                    } else {
                        LOG.log(Level.INFO, "Expected parameters: 1");
                        LOG.log(Level.INFO, "1: Type of projects - all, completed, paused, active");
                    }
                } else {
                    LOG.log(Level.WARNING, "Not connected");
                }
                break;
            }


            case "pause": {
                if (remoteProviderAvailable()) {
                    if (checkParamNum(1, cmd)) {
                        standartRemoteProvider.pauseProject(cmd[1]);
                    } else {
                        LOG.log(Level.INFO, "Expected parameters: 1");
                        LOG.log(Level.INFO, "1: Name of project which should be paused");
                    }
                } else {
                    LOG.log(Level.WARNING, "Not connected");
                }
                break;
            }

            case "unpause": {
                if (remoteProviderAvailable()) {
                    if (checkParamNum(1, cmd)) {
                        standartRemoteProvider.unpauseProject(cmd[1]);
                    } else {
                        LOG.log(Level.INFO, "Expected parameters: 1");
                        LOG.log(Level.INFO, "1: Name of project which should be unpaused");
                    }
                } else {
                    LOG.log(Level.WARNING, "Not connected");
                }
                break;
            }


            case "cancel": {
                if (remoteProviderAvailable()) {
                    if (checkParamNum(1, cmd)) {
                        standartRemoteProvider.cancelProject(cmd[1]);
                    } else {
                        LOG.log(Level.INFO, "Expected parameters: 1");
                        LOG.log(Level.INFO, "1: Name of project which should be canceled");
                    }
                } else {
                    LOG.log(Level.WARNING, "Not connected");
                }
                break;
            }


            case "downloadReady": {
                if (remoteProviderAvailable()) {
                    if (checkParamNum(1, cmd)) {
                        standartRemoteProvider.isProjectReadyForDownload(cmd[1]);
                    } else {
                        LOG.log(Level.INFO, "Expected parameters: 1");
                        LOG.log(Level.INFO, "1: Name of project which should be checked if is ready for download");
                    }
                } else {
                    LOG.log(Level.WARNING, "Not connected");
                }
                break;
            }


            case "exit": {
                if (checkParamNum(0, cmd)) {
                    exitClient();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }


            case "": {
                LOG.log(Level.WARNING, "No command written");
                break;
            }


            default: {
                LOG.log(Level.WARNING, "Command doesn't exist");
                break;
            }
        }
    }

    @Override
    public void startClassicConsole() {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("client>");
                proceedCommand(br.readLine());
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Problem with reading command from console");
        }
    }

    @Override
    public void startGUIConsole() {
        GConsole con = new GConsole(this, "client");
        con.startConsole();
    }

    private String[] parseCommand(String cmd) {
        return cmd.split("\\s+");
    }
}
