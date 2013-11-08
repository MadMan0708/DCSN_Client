/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.networkIO;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Jakub
 */
public class ProgressChecker {

    private Future<?> future;
    private IUpDown upDown;

    public ProgressChecker(Future<?> future, IUpDown upDown) {
        this.future = future;
        this.upDown = upDown;
    }

    public boolean isInProgress() {
        if (!future.isDone()) {
            return true;
        } else {
            return false;
        }
    }

    public int getProgress() {
        return upDown.getProgress();
    }

    public boolean wasSuccesfull() throws RemoteException, IOException {
        if (upDown.isCompleted()) {
            return true;
        } else {
            try {
                future.get();
                return true;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    throw new IOException(e.getCause().getMessage());
                }
                if (e.getCause() instanceof RemoteException) {
                    throw new RemoteException(e.getCause().getMessage());
                }
            } catch (InterruptedException e) {
                // TODO
            }
            return false;
        }
    }
}
