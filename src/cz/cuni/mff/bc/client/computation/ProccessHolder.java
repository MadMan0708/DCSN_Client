/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import cz.cuni.mff.bc.api.enums.TaskState;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.client.Client;
import cz.cuni.mff.bc.client.misc.CustomClassLoader;
import java.io.BufferedReader;
import java.io.File;
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
    private CustomClassLoader customCL;

    /**
     * Initialise ProcessHolder class with Task to be calculated
     *
     * @param tsk Task to be calculated
     */
    public ProccessHolder(Task tsk, File classPath) {
        this.tsk = tsk;
        this.classPath = classPath;
        customCL = new CustomClassLoader();
        try {
            customCL.addNewUrl(classPath.toURI().toURL());
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Incorrect classpath for deserialising task after computation for task {0}", tsk.getUnicateID().getTaskName());
        }
    }

    @Override
    public TaskID getCurrentTaskID() {
        return tsk.getUnicateID();
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
        Process process = processBuilder.start();
        LOG.log(Level.INFO, "Virtual machine for task {0} launched", tsk.getUnicateID());
        LOG.log(Level.INFO, "Task : {0} >> calculation started", tsk.getUnicateID());
        processBuilder.start();
        return process;
    }

    /**
     * Reads and prints messages from input stream of task computation process
     *
     * @param process process with the task computation
     */
    public void startProccessInputReadingThread(final Process process) {
        new Thread() {
            @Override
            public void run() {
                try {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            LOG.log(Level.INFO, "Process: {0} >> {1}", new Object[]{process.toString(), line});
                        }
                    }
                } catch (final Exception e) {
                    LOG.log(Level.WARNING, "Coudln''t read from process {0} input stream", process.toString());
                }
            }
        }.start();
    }

    @Override
    public Task call() throws Exception {
        File tmp = Files.createTempDirectory("DCSN_tasks_").toFile();
        CompUtils.createWorkerJar(new File(tmp, "worker.jar"));
        CompUtils.serialiseToFile(tsk, tmp);
        final Process p = startJVM(
                classPath.getAbsolutePath(),
                new File(tmp, "worker.jar").getAbsolutePath(),
                tmp.getAbsolutePath(),
                tsk.getUnicateID().getTaskName(),
                tsk.getUnicateID().getMemory(),
                true);

        startProccessInputReadingThread(p);
        if (p.waitFor() != 0) {
            throw new ExecutionException("Problem in the client code: ", null);
        }
        tsk = CompUtils.deserialiseFromFile(new File(tmp, tsk.getUnicateID().getTaskName()), customCL);
        tsk.setState(TaskState.COMPLETE);
        LOG.log(Level.INFO, "Task : {0} >> calculation completed", tsk.getUnicateID());
        return tsk;
    }
}
