/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.misc;

/**
 *
 * @author Jakub
 */
public interface IConsole {
    public void proceedCommand(String cmd);
    public void startGUIConsole();
    public void startClassicConsole();
}