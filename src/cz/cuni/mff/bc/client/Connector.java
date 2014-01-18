/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.misc.IClient;
import cz.cuni.mff.bc.misc.CustomClassLoader;
import cz.cuni.mff.bc.api.enums.InformMessage;
import cz.cuni.mff.bc.api.main.IServer;
import cz.cuni.mff.bc.api.main.TaskID;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cojen.dirmi.Environment;
import org.cojen.dirmi.Session;

/**
 * Handles connection session
 *
 * @author Jakub Hava
 */
public class Connector {

    private Session remoteSession;
    private IServer remoteService;
    private Environment env;
    private CustomClassLoader cl;
    private Checker checker;
    private Timer timer;
    private long informPeriod = 1000;
    private ClientParams clientParams;
    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    /**
     *Constructor
     * @param clientParams client parameters
     */
    public Connector(ClientParams clientParams) {
        this.clientParams = clientParams;
    }

    /**
     * Starts the calculation
     *
     * @throws RemoteException
     */
    public void startCalculation() throws RemoteException {
        if (remoteSession != null) {
            checker.startCalculation();
            sendInformMessage(InformMessage.CALCULATION_STARTED);
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (checker.isCalculationInProgress()) {
                        informServer();
                    }
                }
            }, 0, informPeriod);
        } else {
            throw new RemoteException();
        }
    }

    /**
     * Checks if checker receiving tasks
     *
     * @return true if checker is receiving tasks, false otherwise
     */
    public boolean isReceivingTasks() {
        if (checker == null || !checker.isCalculationInProgress()) {
            return false;
        } else {
            return true;
        }
    }

    private void sendInformMessage(InformMessage message) throws RemoteException {
        remoteService.sendInformMessage(clientParams.getClientName(), message);
    }

    private boolean autorizeClient(String clientName) throws RemoteException {
        remoteSession.send(clientName);
        if (((Boolean) remoteSession.receive()).equals(Boolean.TRUE)) {
            remoteService = (IServer) remoteSession.receive();
            remoteSession.send(new IClient() {
                // prepared for future, if server needs to manipulate with client
            });
            // set checker if the connection was successfull
            this.checker = new Checker(remoteService, clientParams, cl);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Connects the the server, takes parameters from actual client parameters
     * object
     *
     * @return true if connection was successful, false otherwise
     * @throws RemoteException
     * @throws IOException
     */
    public boolean connect() throws RemoteException, IOException {
        cl = new CustomClassLoader();
        env = new Environment();

        remoteSession = env.newSessionConnector(clientParams.getServerAddress(), clientParams.getServerPort()).connect();
        remoteSession.setClassLoader(cl);
        return autorizeClient(clientParams.getClientName());
    }

    /**
     * Disconnects from the server
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
        remoteSession.close();
        env.close();
    }

    /**
     * Stops the calculation
     *
     * @param force true if the task should be cancelled immedeatelly, false if
     * the method should wait for current task to finish computation
     * @throws RemoteException
     */
    public void stopCalculation(boolean force) throws RemoteException {
        if (force) {
            checker.stopCalculation();
            checker.terminateCurrentTasks();
        } else {
            checker.stopReceivingTasks();
        }
    }

    private void informServer() {
        try {
            if (!checker.isCalculationInProgress()) {
                sendInformMessage(InformMessage.CALCULATION_ENDED);
            }
            ArrayList<TaskID> taskToCancel = remoteService.sendTasksInCalculation(clientParams.getClientName(), checker.getTasksInCalculation());
            for (TaskID tsk : taskToCancel) {
                checker.cancelTaskCalculation(tsk);
                LOG.log(Level.INFO, "Task {0} is canceled by server purposes", tsk);
            }
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Server couldn't be informed due to network error");
        }
    }

    /**
     * Gets remote interface implementation
     *
     * @return remote interface implementation
     */
    public IServer getRemoteService() {
        return remoteService;
    }
}