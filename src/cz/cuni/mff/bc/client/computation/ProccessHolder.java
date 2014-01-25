/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import cz.cuni.mff.bc.api.enums.TaskState;
import cz.cuni.mff.bc.api.main.CustomIO;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.client.Client;
import cz.cuni.mff.bc.misc.CustomClassLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class hold the external process with task computation
 *
 * @author Jakub Hava
 */
public class ProccessHolder implements IProcessHolder {

    private static final Logger LOG = Logger.getLogger(Client.class.getName());
    private Task tsk;
    private File classPath;
    private File tempDir;
    private CustomClassLoader customCL;
    private Process process = null;

    /**
     * Initialise ProcessHolder class with Task to be calculated
     *
     * @param tsk Task to be calculated
     * @param classPath class path
     * @param tempDir temporary directory
     */
    public ProccessHolder(Task tsk, File classPath, File tempDir) {
        this.tsk = tsk;
        this.classPath = classPath;
        this.tempDir = tempDir;
        customCL = new CustomClassLoader();
        try {
            customCL.addNewUrl(classPath.toURI().toURL());
        } catch (MalformedURLException e) {
            LOG.log(Level.FINE, "Incorrect classpath for deserialising task after computation for task {0}", tsk.getUnicateID().getTaskName());
        }
    }

    @Override
    public TaskID getCurrentTaskID() {
        return tsk.getUnicateID();
    }

    @Override
    public void killProcess() {
        if (process != null) { //if process started
            process.destroy();
        }
    }

    /**
     * Starts the new java virtual machine with given parameters
     *
     * @param classPath class path
     * @param jarLocation path to the worker jar
     * @param taskDir directory where the task is located
     * @param taskName task name
     * @param memory memory limit
     * @param redirectStream true if streams will be redirected, false otherwise
     * @return process created by the new JVM
     * @throws Exception
     */
    public Process startJVM(String classPath, String jarLocation, String taskDir, String taskName, int memory, boolean redirectStream) throws Exception {
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("java.home")
                + separator + "bin" + separator + "java";
        ProcessBuilder processBuilder =
                new ProcessBuilder(path,
                "-Xmx" + memory + "m",
                "-jar", jarLocation,
                classPath,
                taskDir,
                taskName);
        processBuilder.redirectErrorStream(redirectStream);
        Process p = processBuilder.start();
        LOG.log(Level.FINE, "Virtual machine for task {0} launched", tsk.getUnicateID());
        startProccessInputReadingThread(p, p.getErrorStream());
        processBuilder.start();
        LOG.log(Level.FINE, "Task : {0} >> calculation started", tsk.getUnicateID());
        return p;
    }

    /**
     * Reads and prints messages from input stream of task computation process
     *
     * @param process process with the task computation
     * @param inputStrem stream from which messages will be read
     */
    public void startProccessInputReadingThread(final Process process, final InputStream inputStrem) {
        new Thread() {
            @Override
            public void run() {
                try {
                    boolean finished = false;
                    while (!finished) {
                        try {
                            process.exitValue();
                            finished = true;
                        } catch (IllegalThreadStateException e) {
                            finished = false;
                        }
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(inputStrem))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                LOG.log(Level.FINE, "Process: {0} >> {1}", new Object[]{process.toString(), line});
                            }
                        }
                    }
                } catch (final Exception e) {
                    LOG.log(Level.FINE, "Coudln't read from process {0} input stream.", process.toString());
                }
            }
        }.start();
    }

    @Override
    public Task call() throws Exception {
        File tmp = Files.createTempDirectory(tempDir.toPath(), "task_" + tsk.getUnicateID().getTaskName() + "_").toFile();
        CustomIO.recursiveDeleteOnShutdownHook(tmp.toPath()); // if the file is not deleted immediatelly for example because of error in the task
        CompUtils.createWorkerJar(new File(tmp, "worker.jar"));
        CompUtils.serialiseToFile(tsk, tmp);
        this.process = startJVM(
                classPath.getAbsolutePath(),
                new File(tmp, "worker.jar").getAbsolutePath(),
                tmp.getAbsolutePath(),
                tsk.getUnicateID().getTaskName(),
                tsk.getUnicateID().getMemory(),
                true);

        if (process.waitFor() == 0) {
            tsk = CompUtils.deserialiseFromFile(new File(tmp, tsk.getUnicateID().getTaskName()), customCL);
            CustomIO.deleteDirectory(tmp.toPath());
            tsk.setState(TaskState.COMPLETE);
            LOG.log(Level.FINE, "Task : {0} >> calculation completed", tsk.getUnicateID());
            return tsk;
        } else if (process.waitFor() == 1) {
            throw new ExecutionException("Task : " + tsk.getUnicateID() + " >> Project of this task is corrupted: ", null);
        } else {
            throw new IOException("Task : " + tsk.getUnicateID() + " >> Local problem with task calculation");
        }
    }
}
