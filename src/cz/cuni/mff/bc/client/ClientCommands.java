/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.api.enums.ProjectState;
import cz.cuni.mff.bc.api.main.CustomIO;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains console commands
 *
 * @author Jakub Hava
 */
public class ClientCommands {

    private final Client client;
    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    /**
     * Constructor
     *
     * @param client client
     */
    public ClientCommands(Client client) {
        this.client = client;
    }

    /**
     * Splits the parameters into the array
     *
     * @param params string of parameters
     * @return parameters split in the array
     */
    public static String[] parseCommand(String params) {
        return params.split("\\s+");
    }

    /**
     * Checks if the number of parameters is correct
     *
     * @param expected expected number of parameters
     * @param params array of parameters
     * @return true if number of parameters is correct, false otherwise
     */
    public static boolean checkParamNum(int expected, String[] params) {
        return expected == params.length;
    }

    /**
     * Creates new project from old one with new project name
     *
     * @param params array of parameters
     */
    public void createProjectFrom(String[] params) {
        if (checkParamNum(3, params)) {
            try {
                Path projectJar = client.getUploadFileLocation(params[0]);
                Path destinationJar = client.getUploadFileLocation(params[1]);
                if (CustomIO.getExtension(destinationJar.toFile()).equals("jar")) {
                    client.createProjectFrom(projectJar, destinationJar, params[2]);
                } else {
                    LOG.log(Level.WARNING, "Created project file has to have jar extension");
                }
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Incorrect path");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 3");
            LOG.log(Level.INFO, "1: Path to the existing project jar file");
            LOG.log(Level.INFO, "2: Path where new project will be created");
            LOG.log(Level.INFO, "3: New project name");
        }
    }

    /**
     * Gets the temporary directory
     *
     * @param params array of parameters
     */
    public void getTemporaryDir(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Temporary dir is set to : {0}", client.getClientParams().getTemporaryDir());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the temporary directory
     *
     * @param params array of parameters
     */
    public void setTemporaryDir(String[] params) {
        if (checkParamNum(1, params)) {
            client.getClientParams().setTemporaryDir(params[0]);
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Path to the temporary directory");
        }
    }

    /**
     * Gets the download directory
     *
     * @param params array of parameters
     */
    public void getDownloadDir(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Download dir is set to : {0}", client.getClientParams().getDownloadDir());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the download directory
     *
     * @param params array of parameters
     */
    public void setDownloadDir(String[] params) {
        if (checkParamNum(1, params)) {
            client.getClientParams().setDownloadDir(params[0]);
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Path to the download directory");
        }
    }

    /**
     * Gets the upload directory
     *
     * @param params array of parameters
     */
    public void getUploadDir(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Upload dir is set to : {0}", client.getClientParams().getUploadDir());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the upload directory
     *
     * @param params array of parameters
     */
    public void setUploadDir(String[] params) {
        if (checkParamNum(1, params)) {
            client.getClientParams().setUploadDir(params[0]);
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Path to the upload directory");
        }
    }

    /**
     * Sets the server address
     *
     * @param params array of parameters
     */
    public void setServerAddress(String[] params) {
        if (checkParamNum(1, params)) {
            try {
                client.getClientParams().setServerIPPort(params[0]);
            } catch (UnknownHostException e) {
                LOG.log(Level.WARNING, "Not correct host or IP address: {0}", e.getMessage());
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Port number has to be between 1 - 65535");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Server IP address[:port]");
        }
    }

    /**
     * Gets the server address
     *
     * @param params array of parameters
     */
    public void getServerAddress(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Server address is set to : {0}", client.getClientParams().getServerAddress());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the server port
     *
     * @param params array of parameters
     */
    public void setServerPort(String[] params) {
        if (checkParamNum(1, params)) {
            try {
                client.getClientParams().setServerPort(Integer.parseInt(params[0]));
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Port number has to be integer between 1 - 65535");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Server port");
        }
    }

    /**
     * Gets the server port
     *
     * @param params array of parameters
     */
    public void getServerPort(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Server port is set to : {0}", client.getClientParams().getServerPort());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the client name
     *
     * @param params array of parameters
     */
    public void setName(String[] params) {
        if (checkParamNum(1, params)) {
            client.getClientParams().setClientName(params[0]);
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Client name");
        }
    }

    /**
     * Gets the client name
     *
     * @param params array of parameters
     */
    public void getName(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Client name is set to: {0}", client.getClientParams().getClientName());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the memory limit for task computation
     *
     * @param params array of parameters
     */
    public void setMemory(String[] params) {
        if (checkParamNum(1, params)) {
            try {
                client.getClientParams().setMemory(Integer.parseInt(params[0]));
                if (client.isConnected()) {
                    client.getStandardRemoteProvider().setMemoryLimit(Integer.parseInt(params[0]));
                }
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Memory limit for task computation has to be positive integer");
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Memory limit for task computation has to be positive integer");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Memory limit for task computation");
        }
    }

    /**
     * Gets the memory limit for task computation
     *
     * @param params array of parameters
     */
    public void getMemory(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Memory limit for task computation is set to: {0}mb", client.getClientParams().getMemory());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Sets the cores limit for task computation
     *
     * @param params array of parameters
     */
    public void setCores(String[] params) {
        if (checkParamNum(1, params)) {
            try {
                client.getClientParams().setCores(Integer.parseInt(params[0]));
                if (client.isConnected()) {
                    client.getStandardRemoteProvider().setCoresLimit(Integer.parseInt(params[0]));
                }
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Cores limit for task computation has to be positive integer");
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Cores limit for task computation has to be positive integer");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Cores limit for task computation");
        }
    }

    /**
     * Gets the cores limit for task computation
     *
     * @param params array of parameters
     */
    public void getCores(String[] params) {
        if (checkParamNum(0, params)) {
            LOG.log(Level.INFO, "Number of cores allowed to use by tasks is set to: {0}", client.getClientParams().getCores());
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Prints information about client
     *
     * @param params array of parameters
     */
    public void getInfo(String[] params) {
        if (checkParamNum(0, params)) {
            getName(params);
            getServerAddress(params);
            getServerPort(params);
            getTemporaryDir(params);
            getDownloadDir(params);
            getUploadDir(params);
            getMemory(params);
            getCores(params);
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Prints information about the project
     *
     * @param params array of parameters
     */
    public void printProjectInfo(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                client.getStandardRemoteProvider().printProjectInfo(params[0]);
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Project name");
        }
    }

    /**
     * Starts the calculation on task obtained from server
     *
     * @param params array of parameters
     */
    public void startCalculation(String[] params) {
        if (checkParamNum(0, params)) {
            client.startRecievingTasks();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Stops the calculation
     *
     * @param params array of parameters
     */
    public void stopCalculation(String[] params) {
        if (checkParamNum(0, params)) {
            client.stopRecievingTasks(false);
        } else if (checkParamNum(1, params) && params[0].equals("force")) {
            client.stopRecievingTasks(true);
        }else{
            LOG.log(Level.INFO, "Command has 1 optional parameter");
            LOG.log(Level.INFO, "1: \"force\" to stop the tasks computation immediately, otherwise nothing");
        }
    
    }

    /**
     * Connects to the server
     *
     * @param params array of parameters
     */
    public void connect(String[] params) {
        if (params.length == 0) {
            client.connectInNewThread();
        } else {
            try {
                if (params.length == 1) {
                    client.getClientParams().setServerIPPort(params[0]);
                    client.connectInNewThread();
                } else if (params.length == 2) {
                    client.getClientParams().setServerAddress(params[0]);
                    client.getClientParams().setServerPort(Integer.parseInt(params[1]));
                    client.connectInNewThread();
                } else {
                    LOG.log(Level.INFO, "Incorect number of parameter, can be 0, 1 or 2");
                    LOG.log(Level.INFO, "See user documentation");
                }
            } catch (UnknownHostException e) {
                LOG.log(Level.WARNING, "Host or IP address is not valid");
                LOG.log(Level.INFO, "Trying to connect with original address {0}", client.getClientParams().getServerAddress());
                client.connectInNewThread();
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Port number is not valid");
                LOG.log(Level.INFO, "Trying to connect with original port {0}", client.getClientParams().getServerPort());
                client.connectInNewThread();
            }
        }
    }

    /**
     * Disconnects from the server
     *
     * @param params array of parameters
     */
    public void disconnect(String[] params) {
        if (checkParamNum(0, params)) {
            if (client.isConnected()) {
                client.disconnect();
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }

    /**
     * Starts manual processing. Executes start method in client's
     * implementation of Commander class
     *
     * @param params array of parameters
     */
    public void manual(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                LOG.log(Level.INFO, "Starting manual proccessing");
                try {
                    Path projectJar = client.getUploadFileLocation(params[0]);
                    client.startManual(projectJar);
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "Incorrect path");
                }
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Path to project jar file");
        }
    }

    /**
     * Prints the list of client's projects
     *
     * @param params array of parameters
     */
    public void list(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                switch (params[0]) {
                    case "all":
                        client.getStandardRemoteProvider().printAllProjects();
                        break;
                    case "active":
                        client.getStandardRemoteProvider().printProjects(ProjectState.ACTIVE);
                        break;
                    case "paused":
                        client.getStandardRemoteProvider().printProjects(ProjectState.PAUSED);
                        break;
                    case "completed":
                        client.getStandardRemoteProvider().printProjects(ProjectState.READY_FOR_DOWNLOAD);
                        break;
                    case "corrupted":
                        client.getStandardRemoteProvider().printProjects(ProjectState.CORRUPTED);
                        break;
                    default:
                        LOG.log(Level.INFO, "states which can listed are: all, completed, paused, active, corrupted");
                }
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Type of projects - all, completed, paused, active, corrupted");
        }
    }

    /**
     * Pauses the project
     *
     * @param params array of parameters
     */
    public void pause(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                client.getStandardRemoteProvider().pauseProject(params[0]);
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Name of project which should be paused");
        }
    }

    /**
     * Cancels the project
     *
     * @param params array of parameters
     */
    public void cancel(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                client.getStandardRemoteProvider().cancelProject(params[0]);
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Name of project which should be canceled");
        }
    }

    /**
     * Resumes the project
     *
     * @param params array of parameters
     */
    public void resume(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                client.getStandardRemoteProvider().resumeProject(params[0]);
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Name of project which should be unpaused");
        }
    }

    /**
     * Checks if the project is ready for download
     *
     * @param params array of parameters
     */
    public void downloadReady(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                client.getStandardRemoteProvider().isProjectReadyForDownload(params[0]);
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Name of project which should be checked if is ready for download");
        }
    }

    /**
     * Uploads the project
     *
     * @param params array of parameters
     */
    public void upload(String[] params) {
        if (checkParamNum(2, params)) {
            if (client.isConnected()) {
                try {
                    Path projectJar = client.getUploadFileLocation(params[0]);
                    Path projectData = client.getUploadFileLocation(params[1]);
                    client.getStandardRemoteProvider().uploadProject(projectJar, projectData);
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "Incorrect path: {0}", e.getMessage());
                }
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 2");
            LOG.log(Level.INFO, "1: Path to project jar");
            LOG.log(Level.INFO, "2: Path to project data");
        }
    }

    /**
     * Downloads the project
     *
     * @param params array of parameters
     */
    public void download(String[] params) {
        if (checkParamNum(1, params)) {
            if (client.isConnected()) {
                client.getStandardRemoteProvider().download(params[0], Paths.get(client.getClientParams().getDownloadDir().toString(), params[0] + ".zip"));
            } else {
                LOG.log(Level.WARNING, "Not connected");
            }
        } else {
            LOG.log(Level.INFO, "Expected parameters: 1");
            LOG.log(Level.INFO, "1: Project name");
        }
    }

    /**
     * Exits the client
     *
     * @param params array of parameters
     */
    public void exit(String[] params) {
        if (checkParamNum(0, params)) {
            client.exitClient();
        } else {
            LOG.log(Level.INFO, "Command has no parameters");
        }
    }
}
