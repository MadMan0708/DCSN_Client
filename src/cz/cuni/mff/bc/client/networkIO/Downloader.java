/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.networkIO;

import cz.cuni.mff.bc.common.main.IServer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import org.cojen.dirmi.Pipe;

/**
 *
 * @author Aku
 */
public class Downloader implements IUpDown {

    private IServer remoteService;
    private int downloadProgress;
    private long bytesReaded;
    private String projectName;
    private String clientName;
    private File downloadFile;
    private long downloadFileLength;

    public Downloader(IServer remoteService, String clientName, String projectName, Path fileSave) {
        this.remoteService = remoteService;
        this.projectName = projectName;
        this.clientName = clientName;
        this.downloadProgress = 0;
        this.bytesReaded = 0;
        this.downloadFile = fileSave.toFile();
    }

    @Override
    public Object call() throws Exception {
        //int downloadProgressTemp = -1;
        try {
            downloadFileLength = remoteService.getProjectFileSize(clientName, projectName);
        } catch (RemoteException e) {
            throw new RemoteException("File length couldn't be find out due to network error");
        }
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(downloadFile));
                Pipe pipe = remoteService.downloadProject(clientName, projectName, null)) {

            int n;
            byte[] buffer = new byte[8192];
            while ((n = pipe.read(buffer)) > -1) {
                out.write(buffer, 0, n);
                bytesReaded = bytesReaded + n;
                downloadProgress = (int) Math.ceil(100 / (float) downloadFileLength * bytesReaded);
                //  if (downloadProgress % 5 == 0 && downloadProgress != downloadProgressTemp) {
                //      downloadProgressTemp = downloadProgress;
                //      logger.log("Project: " + projectID + ", Downloaded: " + downloadProgress + " %...");
                //  }
            }

            pipe.close();
            return null;
            // logger.log("Project " + projectID + " has been downloaded");
        } catch (IOException e) {
            throw new IOException("Problem durring accessing project file: " + projectName);
        }

    }

    @Override
    public int getProgress() {
        return this.downloadProgress;
    }

    @Override
    public boolean isCompleted() {
        return bytesReaded == downloadFile.length();
    }
}
