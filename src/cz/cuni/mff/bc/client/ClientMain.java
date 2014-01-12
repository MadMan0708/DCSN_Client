/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import java.rmi.RemoteException;

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
}
