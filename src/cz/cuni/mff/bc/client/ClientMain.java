/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

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
        client.startGUIConsole();
        client.initialize();
        if (System.console() != null) {
            client.startClassicConsole();
        }
    }
}
