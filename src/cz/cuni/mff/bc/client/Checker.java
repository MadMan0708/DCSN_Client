/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.misc.CustomClassLoader;
import cz.cuni.mff.bc.client.computation.ProccessHolder;
import cz.cuni.mff.bc.client.computation.IProcessHolder;
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
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
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
 * @author Jakub Hava
 */
public class Checker extends Thread {

    private HashMap<ProjectUID, File> projectJars;
    private final long sleepThreadTime = 10000;
    private IServer remoteService;
    private ExecutorService executor;
    private ConcurrentHashMap<Future<Task>, IProcessHolder> mapping;
    private boolean calculationInProgress;
    private boolean receivingTasks;
    private CustomClassLoader clientCustClassLoader;
    private File baseJarDir;
    private ClientParams clientParams;
    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    /**
     * Constructor
     *
     * @param remoteService remote interface implementation
     * @param clientParams client parameters
     * @param clientCustClassLoader client class loader
     */
    public Checker(IServer remoteService, ClientParams clientParams, CustomClassLoader clientCustClassLoader) {
        this.projectJars = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(clientParams.getCores());
        this.remoteService = remoteService;
        this.mapping = new ConcurrentHashMap<>();
        this.clientParams = clientParams;
        this.clientCustClassLoader = clientCustClassLoader;
        baseJarDir = new File(clientParams.getTemporaryDir().toFile(), "project_jars");
        baseJarDir.mkdirs();
        CustomIO.recursiveDeleteOnShutdownHook(baseJarDir.toPath());
    }

    /**
     * Return the lists of tasks which are currently calculated
     *
     * @return List of task IDs of tasks which are being currently calculated
     */
    public ArrayList<TaskID> getTasksInCalculation() {
        ArrayList<TaskID> taskIDs = new ArrayList<>();
        Set<Future<Task>> futures = mapping.keySet();
        for (Future<Task> future : futures) {
            taskIDs.add(mapping.get(future).getCurrentTaskID());
        }
        return taskIDs;
    }

    /**
     * Cancels calculation of the task
     *
     * @param tsk TaskID of Task which should be cancel
     * @return true if task was mapping contained the future with this task,
     * false otherwise
     */
    public synchronized boolean cancelTaskCalculation(TaskID tsk) {
        Future<Task> del = null;
        Set<Future<Task>> futures = new LinkedHashSet<>(mapping.keySet());
        for (Future<Task> future : futures) {
            if (mapping.get(future).getCurrentTaskID().equals(tsk)) {
                if (future.cancel(true)) {
                    del = future;
                    mapping.get(future).killProcess();
                    mapping.remove(future);
                    break;
                }
            }
        }
        return futures.remove(del);
    }

    /**
     * Stop calculation of the tasks
     */
    public void stopCalculation() {
        calculationInProgress = false;
    }

    /**
     * Checks the status of the calculation
     *
     * @return true if calculation is in progress, false otherwise
     */
    public boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    /**
     * Stops receiving of new tasks
     */
    public void stopReceivingTasks() {
        receivingTasks = false;
    }

    /**
     * Terminates immediately current tasks
     */
    public void terminateCurrentTasks() {
        executor.shutdown();
        Set<Future<Task>> futures = new LinkedHashSet<>(mapping.keySet());
        for (Future<Task> future : futures) {
            mapping.get(future).killProcess();
            LOG.log(Level.INFO, "Calculation of {0} has been canceled", mapping.get(future).getCurrentTaskID().toString());
            try {
                remoteService.cancelTaskOnClient(clientParams.getClientName(), mapping.get(future).getCurrentTaskID());
            } catch (RemoteException e) {
                LOG.log(Level.INFO, "Canceling taks from client problem: {0}", e.getMessage());
            }
        }
        executor.shutdownNow();
    }

    /**
     * Starts tasks receiving and calculation
     */
    public void startCalculation() {
        this.projectJars = new HashMap<>();
        this.mapping = new ConcurrentHashMap<>();
        calculationInProgress = true;
        receivingTasks = true;
        start();
    }

    @Override
    public void run() {
        try {
            getAndCalculateTasks();
        } catch (RemoteException e) {
            LOG.log(Level.WARNING, "Stopping the calculation because of the connection problem: {0}", e.getMessage());
            stopCalculation();
        } catch (InterruptedException e) {
            LOG.log(Level.CONFIG, "Checker thread has been interrupted");
        }
    }

    private void getAndCalculateTasks() throws RemoteException, InterruptedException {
        Task tsk;
        while (calculationInProgress) {
            if (receivingTasks) {
                if (getCoresUsed() < clientParams.getCores()) {
                    if ((tsk = getTaskToCalculate()) != null) { // Check if there are tasks to calculate
                        IProcessHolder holder = new ProccessHolder(tsk, projectJars.get(tsk.getProjectUID()), clientParams.getTemporaryDir().toFile());
                        Future<Task> submit = executor.submit(holder);
                        mapping.put(submit, holder);
                    } else { // no more tasks, sleep
                        if (getCoresUsed() == 0) { // check if all tasks has been sent to the server
                            LOG.log(Level.INFO, "No tasks to calculate, waiting...");
                            Checker.sleep(sleepThreadTime);
                        }
                    }
                } else {
                    LOG.log(Level.INFO, "Waiting for computation capacity for next tasks.");
                    Checker.sleep(sleepThreadTime);
                }
            } else {
                if (getCoresUsed() == 0) {
                    calculationInProgress = false;
                } else {
                    LOG.log(Level.INFO, "Waiting for the rest of tasks to be finished");
                    Checker.sleep(sleepThreadTime);
                }
            }
            sendCompletedIfAny();
        }
    }

    private List<Future<Task>> getCompletedFutures() {
        List<Future<Task>> completed = new ArrayList<>();
        Set<Future<Task>> futures = new LinkedHashSet<>(mapping.keySet());
        for (Future<Task> future : futures) {
            if (future.isDone()) {
                completed.add(future);
            }
        }
        return completed;
    }

    private void sendCompletedIfAny() throws RemoteException {
        List<Future<Task>> completedFutures = getCompletedFutures();
        for (Future<Task> future : completedFutures) {
            TaskID inTheHolder = mapping.get(future).getCurrentTaskID();
            mapping.remove(future);
            try {
                Task tsk = (Task) future.get();
                remoteService.saveCompletedTask(clientParams.getClientName(), tsk);
                LOG.log(Level.INFO, "Task : {0} >> sent to the server", tsk.getUnicateID());
            } catch (ExecutionException e) {
                Throwable ee = e.getCause();
                if (ee instanceof ExecutionException) {
                    LOG.log(Level.WARNING, "Problem during task execution: {0}", ee.getMessage());
                    // unassociate the task
                    remoteService.cancelTaskOnClient(clientParams.getClientName(), inTheHolder);
                    // marks project as corrupted
                    remoteService.markProjectAsCorrupted(inTheHolder.getClientName(), inTheHolder.getProjectName());
                }
                if (ee instanceof IOException) {
                    LOG.log(Level.WARNING, "{0}", ee.getMessage());
                    remoteService.cancelTaskOnClient(clientParams.getClientName(), inTheHolder);
                }
            } catch (InterruptedException e) {
                LOG.log(Level.INFO, "Checker has been interupted");
            } catch (CancellationException e) {
                LOG.log(Level.INFO, "Problem with the task cancelation: {0}", e.getMessage());
            }
        }
    }

    private int getCoresUsed() {
        int coresUsed = 0;
        for (IProcessHolder holder : mapping.values()) {
            coresUsed += holder.getCurrentTaskID().getCores();
        }
        return coresUsed;
    }

    private File downloadProjectJar(ProjectUID uid) throws IOException {
        File tmp = Files.createTempFile(clientParams.getTemporaryDir(), uid.getClientName(), uid.getProjectName()).toFile();
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

    private Task getTaskToCalculate() throws RemoteException {
        ProjectUID projectUID = remoteService.getProjectIdBeforeCalculation(clientParams.getClientName());
        if (projectUID != null) {
            try {
                if (!projectJars.containsKey(projectUID)) {
                    File tmp = downloadProjectJar(projectUID);
                    projectJars.put(projectUID, tmp);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Temp file couldn't be created: {0}", e.getMessage());
            }
            try {
                clientCustClassLoader.addNewUrl(projectJars.get(projectUID).toURI().toURL());
            } catch (MalformedURLException e) {
                LOG.log(Level.WARNING, "Path to temp file is incorrect: {0}", e.getMessage());
            }
            return remoteService.getTask(clientParams.getClientName(), projectUID);
        } else {
            return null; // no more tasks to calculate on server
        }
    }
}
