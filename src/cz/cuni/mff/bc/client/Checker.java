/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.api.main.IServer;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(Checker.class.getName());
    private Handler loggingHandler;

    public Checker(IServer remoteService, String clientID, ClientCustomClassLoader clientCustomClassLoader, Handler loggingHandler) {
        this.executor = Executors.newFixedThreadPool(limit);
        this.futures = new ArrayList<>();
        this.remoteService = remoteService;
        this.mapping = new HashMap<>();
        this.clientID = clientID;
        this.clientCustomClassLoader = clientCustomClassLoader;
        this.loggingHandler = loggingHandler;
        LOG.addHandler(loggingHandler);
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
            LOG.log(Level.INFO, "Calculation of {0} has been canceled", mapping.get(future).getCurrentTaskID().toString());
            try {
                remoteService.cancelTaskFromClient(clientID, mapping.get(future).getCurrentTaskID());
            } catch (RemoteException e) {
                LOG.log(Level.INFO, "Canceling taks from client problem: {0}", e.getMessage());
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
                        IWorker wrk = new Worker(tsk, loggingHandler);
                        Future<Task> submit = executor.submit(wrk);
                        mapping.put(submit, wrk);
                        futures.add(submit);
                    } else { // no more tasks, sleep
                        if (checkStates() == 0) { // check if all tasks has been sent to the server
                            try {
                                LOG.log(Level.INFO, "Checker thread is going to sleep, no tasks.");
                                Checker.sleep(sleepThreadTime);
                            } catch (InterruptedException e) {
                                LOG.log(Level.CONFIG, "Checker thread has been interrupted");
                            }
                        }
                    }
                } catch (RemoteException e) {
                    LOG.log(Level.WARNING, "Loading tasks from server: Task could not be obtained : {0}", e.getMessage());
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
                    LOG.log(Level.WARNING, "Problem during execution of task {0}", ((Exception) e.getCause()).toString());
                } catch (InterruptedException e) {
                    // Interuption handling
                } catch (RemoteException e) {
                    LOG.log(Level.WARNING, "Problem during sending task to server: {0}", e.getMessage());
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
