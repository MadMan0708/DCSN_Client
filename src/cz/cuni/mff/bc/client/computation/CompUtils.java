/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import cz.cuni.mff.bc.api.enums.TaskState;
import cz.cuni.mff.bc.api.main.ITask;
import cz.cuni.mff.bc.api.main.ProjectUID;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import java.io.ByteArrayOutputStream;
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
 *
 * @author UP711643
 */
public class CompUtils {

    public static void serialiseToFile(Task task, File folder) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(folder, task.getUnicateID().getTaskName())))) {
            oos.writeObject(task);
            oos.flush();
        }
    }

    public static Task deserialiseFromFile(File file, CustomCL customCL) throws IOException, ClassNotFoundException {
        try (CustObjectInputStream ois = new CustObjectInputStream(new FileInputStream(file), customCL)) {
            Object o = ois.readObject();
            if (o instanceof Task) {
                return (Task) o;
            } else {
                throw new ClassNotFoundException();
            }
        }

    }

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
            addClassToJar(jarOutputStream, CustomCL.class);
            addClassToJar(jarOutputStream, CustObjectInputStream.class);
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
