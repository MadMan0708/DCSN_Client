/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.computation.ProccessHolder;
import cz.cuni.mff.bc.computation.IProcessHolder;
import cz.cuni.mff.bc.api.main.IServer;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cojen.dirmi.Pipe;

/**
 * Class checks regularly server tasks pool. Tasks are collected by client for
 * the calculation.
 *
 * @author Aku
 */
public class Checker extends Thread {

    private HashMap<ProjectUID, File> tempJars;
    private final long sleepThreadTime = 10000;
    private IServer remoteService;
    private int coreLimit = 3;
    private ExecutorService executor;
    private List<Future<Task>> futures;
    private Map<Future<Task>, IProcessHolder> mapping;
    private boolean calculationInProgress;
    private boolean recievingTasks;
    private ClientCustomCL clientCustClassLoader;
    private static final Logger LOG = Logger.getLogger(Client.class.getName());
    private File tmpDir;
    private String clientName;

    public Checker(IServer remoteService, String clientName, ClientCustomCL clientCustClassLoader) {
        this.tempJars = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(coreLimit);
        this.futures = new ArrayList<>();
        this.remoteService = remoteService;
        this.mapping = new HashMap<>();
        this.clientName = clientName;
        this.clientCustClassLoader = clientCustClassLoader;
        try {
            tmpDir = Files.createTempDirectory("tasksJars").toFile();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Can't create temp directory: {0}", e.getMessage());
        }
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

    public void stopCalculation() {
        calculationInProgress = false;
    }

    public boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    public void stopRecievingTasks() {
        recievingTasks = false;
    }

    public void terminateCurrentTasks() {
        executor.shutdown();
        for (Future<Task> future : futures) {
            LOG.log(Level.INFO, "Calculation of {0} has been canceled", mapping.get(future).getCurrentTaskID().toString());
            try {
                remoteService.cancelTaskOnClient(clientName, mapping.get(future).getCurrentTaskID());
            } catch (RemoteException e) {
                LOG.log(Level.INFO, "Canceling taks from client problem: {0}", e.getMessage());
            }
        }
        executor.shutdownNow();
    }

    public void startCalculation() {
        calculationInProgress = true;
        recievingTasks = true;
        start();
    }

    @Override
    public void run() {
        Task tsk;
        while (calculationInProgress) {
            if (recievingTasks) {
                if (checkStates() < coreLimit) {
                    try {
                        if ((tsk = getTask()) != null) { // Checking if there are tasks to calculate

                            IProcessHolder holder = new ProccessHolder(tsk, tempJars.get(tsk.getProjectUID()));
                            Future<Task> submit = executor.submit(holder);
                            mapping.put(submit, holder);
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
            } else {
                if (checkStates() == 0) {
                    calculationInProgress = false;
                } else {
                    try {
                        Checker.sleep(sleepThreadTime);
                    } catch (InterruptedException e) {
                        LOG.log(Level.CONFIG, "Waiting for computation of rest of the tasks");
                    }
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
                    remoteService.saveCompletedTask(clientName, tsk);
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

    private File downloadProjectJar(ProjectUID uid) throws IOException {
        File tmp = File.createTempFile(uid.getClientName(), uid.getProjectName(), tmpDir);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp));
                Pipe pipe = remoteService.downloadProjectJar(uid, null)) {

            int n;
            byte[] buffer = new byte[8192];
            while ((n = pipe.read(buffer)) > -1) {
                out.write(buffer, 0, n);
            }
            pipe.close();
            return tmp;
        }

    }

    private Task getTask() throws RemoteException {
        TaskID id = remoteService.getTaskIdBeforeCalculation(clientName);
        if (id != null) {
            try {
                if (!tempJars.containsKey(id.getProjectUID())) {
                    File tmp = downloadProjectJar(id.getProjectUID());
                    tempJars.put(id.getProjectUID(), tmp);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Tmp file couldn't be created: {0}", e.getMessage());
            }
            try {
                clientCustClassLoader.addNewUrl(tempJars.get(id.getProjectUID()).toURI().toURL());
            } catch (MalformedURLException e) {
                LOG.log(Level.WARNING, "Path to temp file is incorrect: {0}", e.getMessage());
            }
            return remoteService.getTask(clientName, id);
        } else {
            return null; // no more tasks to calculate on server
        }
    }
}
