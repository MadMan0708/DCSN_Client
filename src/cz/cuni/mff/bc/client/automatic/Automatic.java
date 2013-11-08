/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.automatic;

import cz.cuni.mff.bc.client.Client;
import cz.cuni.mff.bc.common.main.CustomIO;
import cz.cuni.mff.bc.common.main.IAlter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Automatically downloads project, then can alter data and uploads project back
 * to the server // it uses interface IAutomatic
 *
 * @author Jakub
 */
public class Automatic implements IAutomatic {

    private final long timeout = 10000;
    private Client client;
    private String projectName;
    private int priority;
    private File project;
    private int loops;
    private int diff;
    private IAlter alterClass;
    private File alterClassFile;
    private AutomaticClassLoader automaticCl;
    private int counter = 1;

    public Automatic(Client client, File project, String projectName, int priority, File alterClassFile) {
        this(client, project, projectName, priority, alterClassFile, 1);
        diff = 0;
    }

    public Automatic(Client client, File project, String projectName, int priority, File alterClassFile, int loops) {
        this.client = client;
        this.project = project;
        this.projectName = projectName;
        this.priority = priority;
        this.loops = loops;
        this.alterClassFile = alterClassFile;
        diff = 1;
    }

    @Override
    public synchronized void endAfterActualCycle() {
        diff = 1;
    }

    @Override
    public Object call() throws Exception {
        File f = new File(client.getDownloadDir() + File.separator + projectName);
        if (!f.exists()) {
            f.mkdir();
        }
        CustomIO.copyFile(alterClassFile,
                new File(client.getDownloadDir() + File.separator + projectName + File.separator + alterClassFile.getName()));
        automaticCl = new AutomaticClassLoader(client.getDownloadDir(), projectName);
        String name = alterClassFile.getName().substring(0, alterClassFile.getName().length() - 6);
        Class<?> cl = automaticCl.loadClass("cz.cuni.mff.bc.common." + name);
        this.alterClass = (IAlter) cl.newInstance();

        do {
            Client.logger.log("Cycle " + counter + " of project " + projectName + " started ");
//            download();
            if (loops - diff > 0) {
                nextRoundPrepare();
            }

            Client.logger.log("Cycle " + counter + " of project " + projectName + " ended ");
            synchronized (this) {
                loops = loops - diff;
                counter++;
            }
        } while (loops > 0);
        client.logger.log("Automatic calculatin of project " + projectName + " successfully ended");
        return null;
    }

    private void nextRoundPrepare() {
        File project = new File(client.getDownloadDir() + File.separator + projectName + File.separator + "cycle_" + counter + "_completed.zip");
        String projectFolder = client.getDownloadDir() + File.separator + projectName + File.separator + "cycle_" + counter + "_completed";
        String output = client.getDownloadDir() + File.separator + projectName + File.separator + "cycle_" + counter + "_after_alternation.zip";
        unZip(project, projectFolder);
        Client.logger.log(output);
        File[] list = new File(projectFolder).listFiles();
        // alter and place to new zip archive
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
            for (File file : list) {
                if (file.getName().endsWith(".class")) {
                    addFileToZip(file, zos);
                } else {
                    alterClass.alterData(file);
                    addFileToZip(file, zos);
                }
            }

            zos.closeEntry();
            zos.close();
        } catch (IOException e) {
        }
        deleteDirectory(new File(projectFolder));

        client.uploadProject(Paths.get(output), projectName, priority);
    }

    private void deleteDirectory(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    /*   private void download() throws InterruptedException, RemoteException {
     while (!client.isProjectReadyForDownload(projectName)) {
     Thread.sleep(timeout);
     }
     Future<?> fDownload = client.downloadProject(projectName, "cycle_" + counter + "_completed.zip");
     while (!fDownload.isDone()) {
     Thread.sleep(1000);
     }
     }
     */
    private void addFileToZip(File sourceFile, ZipOutputStream zos) throws IOException {

        ZipEntry ze = new ZipEntry(sourceFile.getName());
        zos.putNextEntry(ze);
        try (FileInputStream in = new FileInputStream(sourceFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            // TODO throw new adding to archive exception
        }

    }

    public void unZip(File zipFile, String outputFolder) {
        byte[] buffer = new byte[1024];

        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                } catch (IOException e) {
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            // handle
        }

    }
}
