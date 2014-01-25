/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.client.logging.CustomFormater;
import java.rmi.RemoteException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main entry point of the client side
 *
 * @author Jakub Hava
 */
public class ClientMain {

    /**
     * Main method
     *
     * @param args the command line arguments
     * @throws RemoteException
     */
    public static void main(String[] args) throws RemoteException {
        final Client client = new Client();
        ConsoleHandler consoleHandler = getAndSetConsoleHandler();
        switch (args.length) {
            case 0:
                client.startGUIConsole();
                client.startClassicConsole();
                client.initialise();
                break;
            case 1:
                if (args[0].equals("nogui")) {
                    client.startClassicConsole();
                    client.initialise();
                } else if (args[0].startsWith("task=")) {
                    if (getAndSetConsoleHandler() != null) {
                        consoleHandler.setLevel(Level.ALL); // only in this case allow fully logging to the console
                    }
                    client.startClassicConsole();
                    client.initialise();
                    String pathToTask = args[0].substring(args[0].indexOf("=") + 1);
                    client.autoMode(pathToTask);
                } else {
                    System.err.println("Incorrect parameter");
                }
                break;
            default:
                System.err.println("Wrong number of parameters");
                break;
        }
    }

    /**
     * Gets console handler and sets the logging level
     *
     * @return console handler
     */
    public static ConsoleHandler getAndSetConsoleHandler() {
        ConsoleHandler consoleHandler = null;
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.INFO);
                handler.setFormatter(new CustomFormater());
                consoleHandler = (ConsoleHandler) handler;
                break;
            }
        }
        return consoleHandler;
    }
}
