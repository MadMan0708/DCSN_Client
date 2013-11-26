/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;

/**
 *
 * @author Aku
 */
public class ClientMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RemoteException {
        final Client client = new Client();
        switch (args.length) {
            case 0:
                client.startGUIConsole();
                client.startClassicConsole();
                client.initialize();
                break;
            case 1:
                if (args[0].equals("nogui")) {
                    client.startClassicConsole();
                    client.initialize();
                } else if (args[0].startsWith("task=")) {
                    client.startClassicConsole();
                    client.initialize();
                    String pathToTask = args[0].substring(args[0].indexOf("=") + 1);
                    client.autoMode(Paths.get(pathToTask));
                }else{
                    System.err.println("Incorrect parameter");
                }
                break;
            default:
                System.err.println("Wrong number of parameters");
                break;
        }
    }
}
