/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.misc;

/**
 * Interface used to define custom node
 *
 * @author Jakub Hava
 */
public interface IConsole {

    /**
     * Proceeds the command
     *
     * @param cmd string containing the command exactly how was written by user
     */
    public void processCommand(String cmd);

    /**
     * Start graphical console
     */
    public void startGUIConsole();

    /**
     * Start classical console
     */
    public void startClassicConsole();
}
