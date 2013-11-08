/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.networkIO;

import cz.cuni.mff.bc.common.main.IServer;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.cojen.dirmi.Pipe;

/**
 *
 * @author Aku
 */
public class Uploader implements IUpDown {

    private IServer remoteService;
    private File fileToUpload;
    private int uploadProgress;
    private long bytesReaded;
    private String projectID;
    private String clientID;
    private int priority;

    public Uploader(IServer remoteService, File toUpload, String clientID, String projectID, int priority) {
        this.remoteService = remoteService;
        this.fileToUpload = toUpload;
        this.projectID = projectID;
        this.clientID = clientID;
        this.priority = priority;
        this.uploadProgress = 0;
        this.bytesReaded = 0;
    }

    @Override
    public Object call() throws Exception {
        long size = fileToUpload.length();
        // int uploadProgressTemp = -1;
        int dotIndex = fileToUpload.getName().lastIndexOf(".") + 1;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileToUpload));
                Pipe pipe = remoteService.uploadProject(clientID, projectID, priority, fileToUpload.getName().substring(dotIndex), null)) {
            int n;
            byte[] buffer = new byte[8192];
            while ((n = in.read(buffer)) > 0) {
                pipe.write(buffer, 0, n);
                bytesReaded = bytesReaded + n;
                uploadProgress = (int) Math.ceil(100 / (float) size * bytesReaded);
                //   if (uploadProgress % 5 == 0 && uploadProgress != uploadProgressTemp) {
                //       uploadProgressTemp = uploadProgress;
                //       logger.log("Project: "+projectID+", Uploaded: " + uploadProgress + " %...");
                //   }   
            }
            pipe.close();
            return null;
        } catch (IOException e) {
            throw new IOException("Problem during accessing project file: " + projectID);
        }
    }

    @Override
    public int getProgress() {
        return this.uploadProgress;
    }

    @Override
    public boolean isCompleted() {
        return bytesReaded == fileToUpload.length();
    }
}
