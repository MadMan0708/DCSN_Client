/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.client.misc.PropertiesManager;
import cz.cuni.mff.bc.client.misc.IConsole;
import cz.cuni.mff.bc.client.misc.GConsole;
import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.ClientAPIWithLog;
import cz.cuni.mff.bc.api.main.Commander;
import cz.cuni.mff.bc.client.logging.CustomFormater;
import cz.cuni.mff.bc.client.logging.CustomHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 *
 * @author Aku
 */
public class Client implements IConsole {

    private PropertiesManager propMan;
    private String clientID;
    private int[] serverAddress;
    private int serverPort;
    private String downloadDir;
    private ClientAPIWithLog clientAPIWithLog;
    private InternalAPIWithLog internalAPIWithLog;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Client.class.getName());
    private Handler logHandler;

    public Client() {
        logHandler = new CustomHandler(new File("client.log"));
        logHandler.setFormatter(new CustomFormater());
        logHandler.setLevel(Level.ALL);

        propMan = new PropertiesManager("client.config.properties", logHandler);
        LOG.addHandler(logHandler);
    }

    public void initialize() {

        clientID = propMan.getProperty("name");
        if (propMan.getProperty("address") != null) {
            setServerAddress(propMan.getProperty("address"));
        }
        serverPort = Integer.parseInt(propMan.getProperty("port", "-1"));
        downloadDir = propMan.getProperty("downloadDir");

        if (downloadDir != null) {
            File down = new File(downloadDir);
            if (!down.exists()) {
                down.mkdir();
            }
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
                        commander.start(new ClientAPIWithLog(internalAPIWithLog.getRemoteService(), clientID, currentJar, logHandler));
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
        if (internalAPIWithLog != null) {
            if (internalAPIWithLog.isRecievingTasks()) {
                internalAPIWithLog.stopRecievingTasks();
            }
        }
        System.exit(0);
    }

    private void setServerAddress(String serverIPAddress) {
        String[] ipStr = serverIPAddress.split("\\.");
        int[] ipByte = new int[4];
        for (int i = 0; i < 4; i++) {
            ipByte[i] = Integer.parseInt(ipStr[i]);
        }
        this.serverAddress = ipByte;
    }

    public int[] getServerAddress() {
        return this.serverAddress;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    private boolean setDownloadDir(String newDownloadDir) {
        File f = new File(newDownloadDir);
        if (f.exists() && f.isDirectory()) {
            downloadDir = newDownloadDir;
            return true;
        } else {
            if (f.mkdirs()) {
                downloadDir = newDownloadDir;
                return true;
            } else {
                return false;
            }
        }
    }

    public String getServerAddressString() {
        int[] addr = getServerAddress();
        return addr[0] + "." + addr[1] + "." + addr[2] + "." + addr[3];
    }

    public void setServerPort(int port) {
        this.serverPort = port;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public void setClientID(String newClientID) {
        clientID = newClientID;
    }

    public String getClientID() {
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
            LOG.log(Level.INFO, "Server address is set to : {0}", getServerAddressString());
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
                    setServerAddress(cmd[1]);
                    LOG.log(Level.INFO, "Server adress is now set to: {0}", getServerAddressString());
                    propMan.setProperty("address", getServerAddressString());
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Server new IP address");
                }
                break;
            }
            case "setServerPort": {
                if (checkParamNum(1, cmd)) {
                    setServerPort(Integer.parseInt(cmd[1]));
                    LOG.log(Level.INFO, "Server port is now set to: {0}", serverPort);
                    propMan.setProperty("port", serverPort + "");
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
                    try {
                        File f = new File(cmd[1]);
                        if (!setDownloadDir(f.getCanonicalPath())) {
                            throw new IOException();
                        }
                        LOG.log(Level.INFO, "Download dir is set to: {0}", getDownloadDir());
                        propMan.setProperty("downloadDir", getDownloadDir());
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Path {0} is not correct path", cmd[1]);
                    }
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
                    internalAPIWithLog.startRecievingTasks();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "endCalculation": {
                if (checkParamNum(0, cmd)) {
                    internalAPIWithLog.stopRecievingTasks();
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
                        internalAPIWithLog = new InternalAPIWithLog(getServerAddressString(), serverPort, clientID, logHandler);
                        internalAPIWithLog.connect();
                        clientAPIWithLog = new ClientAPIWithLog(internalAPIWithLog.getRemoteService(), clientID, logHandler);
                    }
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "disconnect": {
                if (checkParamNum(0, cmd)) {
                    internalAPIWithLog.disconnect();
                } else {
                    LOG.log(Level.INFO, "Command has no parameters");
                }
                break;
            }
            case "upload": {
                if (checkParamNum(2, cmd)) {
                    Path projectJar = Paths.get(cmd[1]).toAbsolutePath();
                    Path projectData = Paths.get(cmd[2]).toAbsolutePath();
                    clientAPIWithLog.uploadProject(projectJar, projectData);
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 2");
                    LOG.log(Level.INFO, "1: Path to project jar");
                    LOG.log(Level.INFO, "2: Path to project data");
                }
                break;
            }


            case "download": {
                if (checkParamNum(1, cmd)) {
                    if (downloadDir == null) {
                        LOG.log(Level.WARNING, "Download dir has to be set before downloading project");
                    } else {
                        clientAPIWithLog.download(cmd[1], new File(downloadDir, cmd[1] + ".zip"));
                    }
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Project name");
                }
                break;
            }


            case "list": {
                if (checkParamNum(1, cmd)) {
                    switch (cmd[1]) {
                        case "all":
                            clientAPIWithLog.printAllProjects();
                            break;
                        case "active":
                            clientAPIWithLog.printProjects(ProjectState.ACTIVE);
                            break;
                        case "paused":
                            clientAPIWithLog.printProjects(ProjectState.PAUSED);
                            break;
                        case "completed":
                            clientAPIWithLog.printProjects(ProjectState.COMPLETED);
                            break;
                        default:
                            LOG.log(Level.INFO, "states which can listed are: all, completed, paused, active");
                    }
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Type of projects - all, completed, paused, active");
                }
                break;
            }


            case "pause": {
                if (checkParamNum(1, cmd)) {
                    clientAPIWithLog.pauseProject(cmd[1]);
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Name of project which should be paused");
                }
                break;
            }

            case "unpause": {
                if (checkParamNum(1, cmd)) {
                    clientAPIWithLog.unpauseProject(cmd[1]);
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Name of project which should be unpaused");
                }
                break;
            }


            case "cancel": {
                if (checkParamNum(1, cmd)) {
                    clientAPIWithLog.cancelProject(cmd[1]);
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Name of project which should be canceled");
                }
                break;
            }


            case "downloadReady": {

                if (checkParamNum(1, cmd)) {
                    clientAPIWithLog.isProjectReadyForDownload(cmd[1]);
                } else {
                    LOG.log(Level.INFO, "Expected parameters: 1");
                    LOG.log(Level.INFO, "1: Name of project which should be checked if is ready for download");
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
