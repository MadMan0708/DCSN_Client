/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.api.enums.InformMessage;
import cz.cuni.mff.bc.api.main.IServer;
import cz.cuni.mff.bc.api.main.TaskID;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cojen.dirmi.Environment;
import org.cojen.dirmi.Session;

/**
 *
 * @author Jakub
 */
public class InternalAPI {

    private Session remoteSession;
    private IServer remoteService;
    private Environment env;
    private ClientCustomCL cl;
    private String clientName;
    private Checker checker;
    private Timer timer;
    private long informPeriod = 1000;
    private Handler logHandler;
    private static final Logger LOG = Logger.getLogger(InternalAPI.class.getName());

    public InternalAPI(Handler logHandler) {
        this.logHandler = logHandler;
        LOG.addHandler(logHandler);
    }

    public void startRecievingTasks() throws RemoteException {
        checker = new Checker(remoteService, clientName, cl, logHandler);
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

    private void sendInformMessage(InformMessage message) throws RemoteException {
        remoteService.sendInformMessage(clientName, message);
    }

    private boolean autorizeClient(String clientName) throws RemoteException {
        remoteSession.send(clientName);
        if (((Boolean) remoteSession.receive()).equals(Boolean.TRUE)) {
            remoteService = (IServer) remoteSession.receive();
            // cl.setRemoteService(remoteService);
            return true;
        } else {
            return false;
        }
    }

    public boolean connect(String IPAddress, int port, String clientName) throws RemoteException, IOException {
        this.clientName = clientName;
        cl = new ClientCustomCL();
        env = new Environment();

        remoteSession = env.newSessionConnector(IPAddress, port).connect();
        remoteSession.setClassLoader(cl);
        return autorizeClient(clientName);
    }

    public void disconnect() throws IOException {
        remoteSession.close();
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
                    LOG.log(Level.INFO, "Task {0} is canceled by server purposes", tsk);
                } else {
                    LOG.log(Level.INFO, "Task {0} coldn't be canceled", tsk);
                }
            }
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Server couldn't be informed due to network error");
        }
    }

    public IServer getRemoteService() {
        return remoteService;
    }
}