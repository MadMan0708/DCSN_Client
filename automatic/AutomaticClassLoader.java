/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.automatic;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author Jakub
 */
public class AutomaticClassLoader extends ClassLoader {

    private String downloadDir;
    private String projectName;

    public AutomaticClassLoader(String downloadDir, String projectName) {
        this.downloadDir = downloadDir;
        this.projectName = projectName;
    }

    private byte[] loadClassData(String name) throws IOException {
        String[] parts = name.split("\\.");
        String last = parts[parts.length - 1];
        ByteArrayOutputStream out;

        try (BufferedInputStream in = new BufferedInputStream(
                new FileInputStream(downloadDir + File.separator + projectName + File.separator + last + ".class"))) {

            out = new ByteArrayOutputStream();
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
        }
        byte[] classData = out.toByteArray();
        out.close();

        return classData;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        byte[] classData;
        try {
            classData = loadClassData(name);
        } catch (IOException e) {
            throw new ClassNotFoundException("Class [" + name + "] could not be found", e);
        }
        try {
            Class<?> c = defineClass(name, classData, 0, classData.length);
            resolveClass(c);
            return c;
        } catch (ClassFormatError e) {
//            Client.logger.log(e.toString(), ELoggerMessages.ERROR);
        }
        return null;

    }
}
