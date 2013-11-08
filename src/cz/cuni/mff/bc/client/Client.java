/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.common.main.ProjectUID;
import cz.cuni.mff.bc.common.main.PropertiesManager;
import cz.cuni.mff.bc.common.main.IConsole;
import cz.cuni.mff.bc.common.main.GConsole;
import cz.cuni.mff.bc.common.main.Logger;
import cz.cuni.mff.bc.common.enums.ProjectState;
import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import cz.cuni.mff.bc.client.networkIO.ProgressChecker;
import cz.cuni.mff.bc.common.main.ProjectInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 *
 * @author Aku
 */
public class Client implements IConsole {

    private PropertiesManager propMan;
    public static Logger logger;
    private String clientID;
    private int[] serverAddress;
    private int serverPort;
    private String downloadDir;
    private Client_API api;

    public Client() {
        logger = new Logger("client.log");
        propMan = new PropertiesManager(logger, "client.config.properties");
        api = new Client_API();
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

    public void printAllProjects() {
        try {
            ArrayList<ProjectInfo> pAll = api.getProjectList();
            for (ProjectInfo projectInfo : pAll) {
                logger.log(projectInfo.toString());
            }
        } catch (RemoteException e) {
            logger.log("Project list couldn't be obtained due to network error: " + e.getMessage(), ELoggerMessages.ERROR);
        }
    }

    public void printProjects(ProjectState state) {
        try {
            ArrayList<ProjectInfo> pAll = api.getProjectList(state);
            for (ProjectInfo projectInfo : pAll) {
                logger.log(projectInfo.toString());
            }
        } catch (RemoteException e) {
            logger.log("Project list couldn't be obtained due to network error: " + e.getMessage(), ELoggerMessages.ERROR);
        }
    }

    private void exitClient() {
        try {
            api.stopRecievingTasks();
            System.exit(0);
        } catch (RemoteException e) {
            logger.log("Recieving of tasks could't be stoped due to network error: " + e.getMessage(), ELoggerMessages.ERROR);
        }
    }

    public void connect() {
        try {
            if (api.connect(getServerAddressString(), serverPort, clientID)) {
                logger.log("Connected to the server " + getServerAddressString() + ":" + serverPort + " with client ID " + clientID);
            } else {
                logger.log("Client ID \"" + clientID + "\" is already in the system ");
            }
        } catch (RemoteException e) {
            logger.log("Problem during creating session: " + e.toString(), ELoggerMessages.ERROR);
        } catch (IOException e) {
            logger.log("Connection problem: " + e.toString(), ELoggerMessages.ERROR);
        }
    }

    public void startRecievingTasks() {
        try {
            logger.log("Client is now participating in task computation");
            api.startRecievingTasks();
        } catch (RemoteException e) {
            logger.log("Computation couldn't start due to network error: " + e.getMessage());
        }
    }

    public void stopRecievingTasks() {
        try {
            api.stopRecievingTasks();
            logger.log("Client has stopped computation of tasks");
        } catch (RemoteException e) {
            logger.log("Computation couldn't be stopped to network error: " + e.getMessage());
        }
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
            logger.log("Client name is not set yet!");
        } else {
            logger.log("Client name is set to: " + clientID);
        }
    }

    private void printServerAddress() {
        if (serverAddress == null) {
            logger.log("Server adress is not set yet!");
        } else {
            logger.log("Server address is set to : " + getServerAddressString());
        }
    }

    private void printServerPort() {
        if (serverPort == -1) {
            logger.log("Server adress is not set yet!");
        } else {
            logger.log("Server port is set to : " + serverPort);
        }
    }

    private void printDownloadDir() {
        if (downloadDir == null) {
            logger.log("Download dir is not set yet!");
        } else {
            logger.log("Download dir is set to : " + downloadDir);
        }
    }

    public void uploadProject(Path pathToProject, String projectName, int projectPriority) {
        try {
            final String projectNameLocal = projectName;
            final ProgressChecker pc = api.uploadProject(pathToProject, projectName, projectPriority);
            if (pc != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logger.log("Project: " + projectNameLocal + ", Uploaded: 0 %...");
                        while (pc.isInProgress()) {
                            logger.log("Project: " + projectNameLocal + ", Uploaded: " + pc.getProgress() + " %...");
                            try {
                                Thread.sleep(800);
                            } catch (InterruptedException e) {
                                logger.log("Progress checking during uploading has been interupted: " + e.getMessage());
                            }
                        }
                        try {
                            pc.wasSuccesfull();
                            logger.log("Project: " + projectNameLocal + ", Uploaded: 100 %...");
                            logger.log("Project " + projectNameLocal + " has been uploaded");
                        } catch (IOException e) {
                            logger.log(e.getMessage());
                        }
                    }
                }).start();
            } else {
                logger.log("Project with name " + projectName + " already exists");
            }
        } catch (RemoteException e) {
            logger.log("Project couldn't be uploded due to network error:" + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            api.disconnect();
        } catch (IOException e) {
            logger.log("Disconnecting unsuccessful due to network erro:" + e.getMessage());
        }
    }

    public void pauseProject(String projectName) {
        try {
            if (api.pauseProject(projectName)) {
                logger.log("Project " + projectName + " was successfuly paused");
            } else {
                logger.log("No such project: " + projectName);
            }
        } catch (RemoteException e) {
            logger.log("Prolem during pausing project due to network erorr: " + e.getMessage());
        }
    }

    public void unpauseProject(String projectName) {
        try {
            if (api.unpauseProject(projectName)) {
                logger.log("Project " + projectName + " was successfuly unpaused");
            } else {
                logger.log("No such project: " + projectName);
            }
        } catch (RemoteException e) {
            logger.log("Prolem during unpausing project due to network erorr: " + e.getMessage());
        }
    }

    public void cancelProject(String projectName) {
        try {
            if (api.cancelProject(projectName)) {
                logger.log("Project " + projectName + " was successfuly canceled");
            } else {
                logger.log("No such project: " + projectName);
            }
        } catch (RemoteException e) {
            logger.log("Prolem during canceling project due to network erorr: " + e.getMessage());
        }
    }

    public void isProjectReadyForDownload(String projectName) {
        try {
            if (api.isProjectReadyForDownload(projectName)) {
                logger.log("Project " + projectName + " is ready for download");
            } else {
                logger.log("Project " + projectName + " is not ready for download");
            }
        } catch (RemoteException e) {
            logger.log("Problem during determining project state due to network error", ELoggerMessages.ERROR);
        }
    }

    public void download(String projectName) {
        try {
            final String projectNameLocal = projectName;
            final ProgressChecker pc = api.downloadProject(projectName, Paths.get(downloadDir + File.separator + projectName + ".zip"));
            if (pc != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logger.log("Project: " + projectNameLocal + ", Downloaded: 0 %...");
                        while (pc.isInProgress()) {
                            logger.log("Project: " + projectNameLocal + ", Downloaded: " + pc.getProgress() + " %...");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                logger.log("Progress checking during downloading has been interupted: " + e.getMessage());
                            }
                        }
                        try {
                            pc.wasSuccesfull();
                            logger.log("Project: " + projectNameLocal + ", Downloaded: 100 %...");
                            logger.log("Project " + projectNameLocal + " has been downloaded");
                        } catch (RemoteException e) {
                            logger.log(e.getMessage());
                        } catch (IOException e) {
                            logger.log(e.getMessage());
                        }
                    }
                }).start();
            } else {
                logger.log("Project " + projectName + " is not ready for download");
            }
        } catch (RemoteException e) {
            logger.log("Project couldn't be downloaded due to network error:" + e.getMessage());
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
             logger.log("Expected parameters: 4, 5", ELoggerMessages.ALERT);
             logger.log("1: Path to file with project", ELoggerMessages.ALERT);
             logger.log("2: Project name", ELoggerMessages.ALERT);
             logger.log("3: Project prority", ELoggerMessages.ALERT);
             logger.log("4: Path to class file implementing IAlert interface", ELoggerMessages.ALERT);
             logger.log("5(OPTIONAL): Number of cycles, without this parameter it will repeat cycles until user cancel the automatic calculation", ELoggerMessages.ALERT);

             }
             break;
             }*/
            case "getName": {
                if (checkParamNum(0, cmd)) {
                    printClientName();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "getServerAddress": {
                if (checkParamNum(0, cmd)) {
                    printServerAddress();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "getServerPort": {
                if (checkParamNum(0, cmd)) {
                    printServerPort();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "setName": {
                if (checkParamNum(1, cmd)) {
                    clientID = cmd[1];
                    logger.log("Client name is now set to: " + clientID);
                    propMan.setProperty("name", clientID);
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Client new id", ELoggerMessages.ALERT);
                }
                break;
            }
            case "setServerAddress": {
                if (checkParamNum(1, cmd)) {
                    setServerAddress(cmd[1]);
                    logger.log("Server adress is now set to: " + getServerAddressString());
                    propMan.setProperty("address", getServerAddressString());
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Server new IP address", ELoggerMessages.ALERT);
                }
                break;
            }
            case "setServerPort": {
                if (checkParamNum(1, cmd)) {
                    setServerPort(Integer.parseInt(cmd[1]));
                    logger.log("Server port is now set to: " + serverPort);
                    propMan.setProperty("port", serverPort + "");
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Server new port", ELoggerMessages.ALERT);
                }
                break;
            }
            case "getDownloadDir": {
                if (checkParamNum(0, cmd)) {
                    printDownloadDir();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
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
                        logger.log("Download dir is set to: " + getDownloadDir());
                        propMan.setProperty("downloadDir", getDownloadDir());
                    } catch (IOException e) {
                        logger.log("Path " + cmd[1] + " is not correct path");
                    }
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: new download dir", ELoggerMessages.ALERT);
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
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "startCalculation": {
                if (checkParamNum(0, cmd)) {
                    startRecievingTasks();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "endCalculation": {
                if (checkParamNum(0, cmd)) {
                    stopRecievingTasks();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "connect": {
                if (checkParamNum(0, cmd)) {
                    boolean ready = true;
                    if (clientID == null) {
                        logger.log("Client name has to be set to perform connection!", ELoggerMessages.ALERT);
                        ready = false;
                    }
                    if (serverAddress == null) {
                        logger.log("Server address has to be set to perform connection!", ELoggerMessages.ALERT);
                        ready = false;
                    }
                    if (serverPort == -1) {
                        logger.log("Server port has to be set to perform connection!", ELoggerMessages.ALERT);
                        ready = false;
                    }
                    if (ready) {
                        connect();
                    }
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "disconnect": {
                if (checkParamNum(0, cmd)) {
                    disconnect();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }
            case "upload": {
                if (checkParamNum(3, cmd)) {
                    Path path = Paths.get(cmd[1]);
                    path = path.toAbsolutePath();
                    uploadProject(path, cmd[2], Integer.parseInt(cmd[3]));
                } else {
                    logger.log("Expected parameters: 3", ELoggerMessages.ALERT);
                    logger.log("1: Path to project file", ELoggerMessages.ALERT);
                    logger.log("2: Project name", ELoggerMessages.ALERT);
                    logger.log("3: Project priority", ELoggerMessages.ALERT);
                }
                break;
            }


            case "download": {
                if (checkParamNum(1, cmd)) {
                    if (downloadDir == null) {
                        logger.log("Download dir has to be set before downloading project", ELoggerMessages.ALERT);
                    } else {
                        download(cmd[1]);
                    }
                } else {
                    logger.log("Expected parameters: 1");
                    logger.log("1: Project name", ELoggerMessages.ALERT);
                }
                break;
            }


            case "list": {
                if (checkParamNum(1, cmd)) {
                    switch (cmd[1]) {
                        case "all":
                            printAllProjects();
                            break;
                        case "active":
                            printProjects(ProjectState.ACTIVE);
                            break;
                        case "paused":
                            printProjects(ProjectState.PAUSED);
                            break;
                        case "completed":
                            printProjects(ProjectState.COMPLETED);
                            break;
                        default:
                            logger.log("states which can listed are: all, completed, paused, active", ELoggerMessages.ALERT);
                    }
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Type of projects - all, completed, paused, active", ELoggerMessages.ALERT);
                }
                break;
            }


            case "pause": {
                if (checkParamNum(1, cmd)) {
                    pauseProject(cmd[1]);
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Name of project which should be paused", ELoggerMessages.ALERT);
                }
                break;
            }

            case "unpause": {
                if (checkParamNum(1, cmd)) {
                    unpauseProject(cmd[1]);
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Name of project which should be unpaused", ELoggerMessages.ALERT);
                }
                break;
            }


            case "cancel": {
                if (checkParamNum(1, cmd)) {
                    cancelProject(cmd[1]);
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Name of project which should be canceled", ELoggerMessages.ALERT);
                }
                break;
            }


            case "downloadReady": {

                if (checkParamNum(1, cmd)) {
                    isProjectReadyForDownload(cmd[1]);
                } else {
                    logger.log("Expected parameters: 1", ELoggerMessages.ALERT);
                    logger.log("1: Name of project which should be checkes if is ready for download", ELoggerMessages.ALERT);
                }
                break;
            }


            case "exit": {
                if (checkParamNum(0, cmd)) {
                    exitClient();
                } else {
                    logger.log("Command has no parameters", ELoggerMessages.ALERT);
                }
                break;
            }


            case "": {
                logger.log("No command written", ELoggerMessages.ALERT);
                break;
            }


            default: {
                logger.log("Command doesn't exist", ELoggerMessages.ALERT);
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
            logger.log("Problem with reading command from console", ELoggerMessages.ERROR);
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

    private Object Thread(Runnable runnable) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
