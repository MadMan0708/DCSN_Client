/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.networkIO;

import java.util.concurrent.Callable;

/**
 * Interface for download and upload classes
 *
 * @author Jakub
 */
public interface IUpDown extends Callable<Object> {

    /**
     *
     * @return percentage progress of uploading, downloading
     */
    public int getProgress();

    /**
     * Tests if uploading, downloading work is completed
     *
     * @return boolean if uploading or downloading is completed
     */
    public boolean isCompleted();

    @Override
    public Object call() throws Exception;
}
