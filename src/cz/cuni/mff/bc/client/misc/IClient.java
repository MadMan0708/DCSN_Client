/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.misc;

import cz.cuni.mff.bc.api.main.TaskID;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface on the client side
 *
 * @author Jakub Hava
 */
public interface IClient extends Remote {

    /**
     * Cancels calculation of the task
     *
     * @param taskID taskID to cancel
     * @throws RemoteException
     */
    public void cancelTaskCalculation(TaskID taskID) throws RemoteException;
}
