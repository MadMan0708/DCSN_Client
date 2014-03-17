/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents logging target logging to the file
 *
 * @author Jakub Hava
 */
public class FileLogger implements ILogTarget {

    private final File logFile;

    /**
     * Constructor
     *
     * @param logFile file where to log
     */
    public FileLogger(File logFile) {
        this.logFile = logFile;
    }

    @Override
    public void log(String msg) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
            pw.println(msg);
        } catch (IOException e) {
            System.err.println("Can not write message to log file:" + logFile.getAbsolutePath());
        }
    }
}
