/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.logging;

/**
 * Class used to define custom logging targets
 *
 * @author Jakub Hava
 */
public interface ILogTarget {

    /**
     * Logs the message
     *
     * @param message message to log
     */
    public void log(String message);
}
