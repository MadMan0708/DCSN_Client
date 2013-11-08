/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.client.networkIO.ProgressChecker;
import cz.cuni.mff.bc.common.enums.InformMessage;
import cz.cuni.mff.bc.common.enums.ProjectState;
import cz.cuni.mff.bc.common.enums.EUserAddingState;
import cz.cuni.mff.bc.client.networkIO.Downloader;
import cz.cuni.mff.bc.client.networkIO.IUpDown;
import cz.cuni.mff.bc.client.networkIO.Uploader;
import cz.cuni.mff.bc.common.main.IServer;
import cz.cuni.mff.bc.common.main.ProjectInfo;
import cz.cuni.mff.bc.common.main.TaskID;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.cojen.dirmi.Environment;
import org.cojen.dirmi.Session;

/**
 *
 * @author Jakub
 */
public class Client_API {

    private Session remoteSession;
    private IServer remoteService;
    private Environment env;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private ClientCustomClassLoader cl;
    private Checker checker;
    private Timer timer;
    private final int informPeriod = 10;
    private String clientName;

    private boolean autorizeClient(String clientName) throws RemoteException {
        remoteSession.send(clientName);
        if ((EUserAddingState) remoteSession.receive() == EUserAddingState.OK) {
            remoteService = (IServer) remoteSession.receive();
            cl.setRemoteService(remoteService);
            return true;
        } else {
            return false;
        }
    }

    public boolean connect(String IPAddress, int port, String clientName) throws RemoteException, IOException {
        this.clientName = clientName;
        cl = new ClientCustomClassLoader();
        env = new Environment();

        remoteSession = env.newSessionConnector(IPAddress, port).connect();
        remoteSession.setClassLoader(cl);
        return autorizeClient(clientName);
    }

    public void disconnect() throws IOException {
        remoteSession.close();
    }

    public boolean isProjectReadyForDownload(String projectName) throws RemoteException {
        if (remoteService.isProjectReadyForDownload(clientName, projectName)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isProjectExists(String projectName) throws RemoteException {
        if (remoteService.isProjectExists(clientName, projectName)) {
            return true;
        } else {
            return false;
        }

    }

    public ProgressChecker downloadProject(String projectName, Path target) throws RemoteException {
        if (isProjectReadyForDownload(projectName)) {
            IUpDown downloader = new Downloader(remoteService, clientName, projectName, target);
            Future<?> f = executor.submit(downloader);
            return new ProgressChecker(f, downloader);
        } else {
            return null;
        }
    }

    public ProgressChecker uploadProject(Path pathToProject, String projectName, int priority) throws RemoteException {
        if (!isProjectExists(projectName)) {
            File fileToUpload = pathToProject.toFile();
            IUpDown uploader = new Uploader(remoteService, fileToUpload, clientName, projectName, priority);
            Future<?> f = executor.submit(uploader);
            return new ProgressChecker(f, uploader);
        } else {
            return null;
        }
    }

    public boolean cancelProject(String projectName) throws RemoteException {
        if (remoteService.cancelProject(clientName, projectName)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean pauseProject(String projectName) throws RemoteException {
        if (remoteService.pauseProject(clientName, projectName)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean unpauseProject(String projectName) throws RemoteException {
        if (remoteService.unpauseProject(clientName, projectName)) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<ProjectInfo> getProjectList() throws RemoteException {
        return remoteService.getProjectList(clientName);
    }

    public ArrayList<ProjectInfo> getProjectList(ProjectState state) throws RemoteException {
        ArrayList<ProjectInfo> projects = new ArrayList<>();
        ArrayList<ProjectInfo> allProjects = getProjectList();
        for (ProjectInfo projectInfo : allProjects) {
            if (projectInfo.getState() == state) {
                projects.add(projectInfo);
            }
        }
        return projects;
    }

    private void sendInformMessage(InformMessage message) throws RemoteException {
        remoteService.sendInformMessage(clientName, message);
    }

    /* private void automatic(File alter, String projectName, int projectPriority, File alterClass, int numberOfCycles) {
     if (numberOfCycles == 0) {
     executor.submit(new Automatic(this, alter, projectName, projectPriority, alterClass));
     } else {
     executor.submit(new Automatic(this, alter, projectName, projectPriority, alterClass, numberOfCycles));
     }
     }*/
    public void startRecievingTasks() throws RemoteException {
        checker = new Checker(remoteService, clientName, cl);
        checker.setCalculationState(true);
        checker.start();
        sendInformMessage(InformMessage.CALCULATION_STARTED);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (checker.isCalculationInProgress()) {
                    informServer();
                }
            }
        }, 0, informPeriod * 1000);

    }

    public void stopRecievingTasks() throws RemoteException {
        checker.setCalculationState(false);
        checker.endCalculation();
        informServer();
        sendInformMessage(InformMessage.CALCULATION_ENDED);
    }

    private void informServer() {
        try {
            ArrayList<TaskID> taskToCancel = remoteService.calculatedTasks(clientName, checker.getTasksInCalculation());

            for (TaskID tsk : taskToCancel) {
                if (checker.cancelTaskCalculation(tsk)) {
//                    logger.log("Task " + tsk + " is canceled by server purposes");
                } else {
                    // TODO 
                }
            }
        } catch (RemoteException e) {
            //logger.log("Server couldn't be informed due to network error", ELoggerMessages.ERROR);
        }
    }
}
