/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.api.main.IServer;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jakub
 */
public class InternalAPIWithLog {

    private InternalAPI internalAPI;
    private String IPAddress;
    private int port;
    private String clientName;
    private Handler logHandler;
    private static final Logger LOG = Logger.getLogger(InternalAPIWithLog.class.getName());

    public InternalAPIWithLog(String IPAddress, int port, String clientName, Handler logHandler) {
        this.IPAddress = IPAddress;
        this.port = port;
        this.clientName = clientName;
        this.logHandler = logHandler;
        this.internalAPI = new InternalAPI(logHandler);
        LOG.addHandler(logHandler);
    }

    public String getClientName() {
        return clientName;
    }

    public IServer getRemoteService() {
        return internalAPI.getRemoteService();
    }

    public void disconnect() {
        try {
            internalAPI.disconnect();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Disconnecting unsuccessful due to network erro:{0}", e.getMessage());
        }
    }

    public void connect() {
        try {
            if (internalAPI.connect(IPAddress, port, clientName)) {
                LOG.log(Level.INFO, "Connected to the server {0}:{1} with client ID {2}", new Object[]{IPAddress, port, clientName});
            } else {
                LOG.log(Level.INFO, "Client ID \"{0}\" is already in the system ", clientName);
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
            internalAPI.startRecievingTasks();
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Computation couldn''t start due to network error: {0}", e.getMessage());
        }
    }

    public void stopRecievingTasks() {
        try {
            internalAPI.stopRecievingTasks();
            LOG.log(Level.INFO, "Client has stopped computation of tasks");
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Computation couldn''t be stopped to network error: {0}", e.getMessage());
        }
    }
}
