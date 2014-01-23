/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import cz.cuni.mff.bc.misc.CustomObjectInputStream;
import cz.cuni.mff.bc.api.enums.TaskState;
import cz.cuni.mff.bc.api.main.ITask;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import cz.cuni.mff.bc.misc.CustomClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Methods used by classes which provides computation of tasks
 *
 * @author Jakub Hava
 */
public class CompUtils {

    /**
     * Serialise the task to the file
     *
     * @param task task to serialise
     * @param folder destination folder
     * @throws IOException
     */
    public static void serialiseToFile(Task task, File folder) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(folder, task.getUnicateID().getTaskName() + ".tmp")))) {
            oos.writeObject(task);
        }
        new File(folder, task.getUnicateID().getTaskName() + ".tmp").renameTo(new File(folder, task.getUnicateID().getTaskName()));
    }

    /**
     * Deserialise the task from file
     *
     * @param file file with the serialised task
     * @param customCL custom class loader
     * @return deserialised task
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Task deserialiseFromFile(File file, CustomClassLoader customCL) throws IOException, ClassNotFoundException {
        try (CustomObjectInputStream ois = new CustomObjectInputStream(new FileInputStream(file), customCL)) {
            return (Task) ois.readObject();
        }
    }

    /**
     * Creates the worker jar which provides the computation on task, in
     * different process
     *
     * @param file file where to create the worker
     * @throws IOException
     */
    public static void createWorkerJar(File file) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Worker.class.getName());
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file), manifest);) {
            addClassToJar(jarOutputStream, Worker.class);
            addClassToJar(jarOutputStream, Task.class);
            addClassToJar(jarOutputStream, TaskState.class);
            addClassToJar(jarOutputStream, TaskID.class);
            addClassToJar(jarOutputStream, ITask.class);
            addClassToJar(jarOutputStream, ProjectUID.class);
            addClassToJar(jarOutputStream, CustomClassLoader.class);
            addClassToJar(jarOutputStream, CustomObjectInputStream.class);
            addClassToJar(jarOutputStream, CompUtils.class);
        }
    }

    private static void addClassToJar(JarOutputStream jarOutputStream, Class clazz) throws IOException {
        String path = clazz.getName().replace('.', '/') + ".class";
        jarOutputStream.putNextEntry(new JarEntry(path));
        jarOutputStream.write(classToByteArray(clazz.getClassLoader().getResourceAsStream(path)));
        jarOutputStream.closeEntry();
    }

    private static byte[] classToByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4000];
        while (true) {
            int r = in.read(buf);
            if (r == -1) {
                break;
            }
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }
}
