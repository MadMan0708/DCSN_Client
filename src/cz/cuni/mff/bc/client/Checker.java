/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import cz.cuni.mff.bc.common.main.IServer;
import cz.cuni.mff.bc.common.main.Task;
import cz.cuni.mff.bc.common.main.TaskID;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class checks regularly server tasks pool. Tasks are collected by client for
 * the calculation.
 *
 * @author Aku
 */
public class Checker extends Thread {

    private final long sleepThreadTime = 10000;
    private IServer remoteService;
    private int limit = 3;
    private ExecutorService executor;
    private List<Future<Task>> futures;
    private Map<Future<Task>, IWorker> mapping;
    private String clientID;
    private boolean calculationInProgress;
    private ClientCustomClassLoader clientCustomClassLoader;

    public Checker(IServer remoteService, String clientID, ClientCustomClassLoader clientCustomClassLoader) {
        this.executor = Executors.newFixedThreadPool(limit);
        this.futures = new ArrayList<>();
        this.remoteService = remoteService;
        this.mapping = new HashMap<>();
        this.clientID = clientID;
        this.clientCustomClassLoader = clientCustomClassLoader;
    }

    /**
     *
     * @return TaskIDs of Task which are being currently calculated
     */
    public ArrayList<TaskID> getTasksInCalculation() {
        ArrayList<TaskID> taskIDs = new ArrayList<>();
        for (Future<Task> future : futures) {
            taskIDs.add(mapping.get(future).getCurrentTaskID());
        }
        return taskIDs;
    }

    /**
     *
     * @param tsk TaskID of Task which should be cancel
     * @return boolean if task was successfully cancel
     */
    public boolean cancelTaskCalculation(TaskID tsk) {
        Future<Task> del = null;
        for (Future<Task> future : futures) {
            if (mapping.get(future).getCurrentTaskID().equals(tsk)) {
                if (future.cancel(true)) {
                    del = future;
                    mapping.remove(future);
                    break;
                }
            }
        }
        return futures.remove(del);
    }

    public void setCalculationState(boolean calculationInProgress) {
        this.calculationInProgress = calculationInProgress;
    }

    public boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    public void endCalculation() {
        executor.shutdown();
        for (Future<Task> future : futures) {
            Client.logger.log("Calculation of " + mapping.get(future).getCurrentTaskID().toString() + " has been canceled");
            try {
                remoteService.cancelTaskFromClient(clientID, mapping.get(future).getCurrentTaskID());
            } catch (RemoteException e) {
                Client.logger.log("Canceling taks from client problem: " + e.getMessage(), ELoggerMessages.ERROR);
            }
        }
        executor.shutdownNow();
    }

    @Override
    public void run() {
        Task tsk;
        while (calculationInProgress) {
            if (checkStates() < limit) {
                try {
                    if ((tsk = getTask()) != null) { // Checking if there are tasks to calculate
                        IWorker wrk = new Worker(tsk);
                        Future<Task> submit = executor.submit(wrk);
                        mapping.put(submit, wrk);
                        futures.add(submit);
                    } else { // no more tasks, sleep
                        if (checkStates() == 0) { // check if all tasks has been sent to the server
                            try {
                                Client.logger.log("Checker thread is going to sleep, no tasks.");
                                Checker.sleep(sleepThreadTime);
                            } catch (InterruptedException e) {
                                Client.logger.log("Checker thread has been interrupted");
                            }
                        }
                    }
                } catch (RemoteException e) {
                    Client.logger.log("Loading tasks from server: Task could not be obtained : " + e.getMessage(), ELoggerMessages.ERROR);
                    // Handle this exception
                }
            }
        }
    }

    private int checkStates() {
        List<Future<Task>> del = new ArrayList<>();
        for (Future<Task> future : futures) {
            if (future.isDone()) {
                del.add(future);
                try {
                    Task tsk = (Task) future.get();
                    remoteService.setSessionClassLoaderDetails(clientID, tsk.getProjectUID());
                    remoteService.saveCompletedTask(clientID, tsk);
                } catch (ExecutionException e) {
                    // doslo k chybe, na server je treba odeslat info o odasociovani tohoto klienta a tohoto tasku
                    Client.logger.log("Problem during execution of task " + ((Exception) e.getCause()).toString(), ELoggerMessages.ERROR);
                } catch (InterruptedException e) {
                    // Interuption handling
                } catch (RemoteException e) {
                    Client.logger.log("Problem during sending task to server: " + e.getMessage(), ELoggerMessages.ERROR);
                }
            }
        }
        futures.removeAll(del);
        return futures.size();
    }

    private Task getTask() throws RemoteException {
        TaskID id = remoteService.getTaskIdBeforeCalculation(clientID);
        if (id != null) {
            clientCustomClassLoader.setTaskID(id);
            return remoteService.getTask(clientID, id);
        } else {
            return null; // no more tasks to calculate on server
        }
    }
}
