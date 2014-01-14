/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.misc.CustomClassLoader;
import java.io.File;
import java.io.IOException;

/**
 * Calculates and returns computed tasks
 *
 * @author Jakub Hava
 */
public class Worker {

    private static CustomClassLoader customCL;

    /**
     * Main entry fro the task calculation itself
     *
     * @param args arguments to specify task which will be calculated
     */
    public static void main(String[] args) {
        try {
            customCL = new CustomClassLoader();
            customCL.addNewUrl(new File(args[0]).toURI().toURL());
            Task tsk = CompUtils.deserialiseFromFile(new File(args[1], args[2]), customCL);
            tsk.calculate();
            CompUtils.serialiseToFile(tsk, new File(args[1]));

        } catch (ClassNotFoundException e) {
            System.err.println("Corrupted file with task file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Problem with loadind or storing serialized file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Problem in the client code: " + e.getMessage());
        }
    }
}
