/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.logging;


import cz.cuni.mff.bc.client.misc.GConsole;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author Jakub
 */
public class CustomHandler extends Handler {

    private File log;

    public CustomHandler(File log) {
        this.log = log;
    }

    private void logToFile(String msg) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(log, true)))) {
            pw.println(msg);
        } catch (IOException e) {
            GConsole.printToLog("Could't write to log file: " + msg + ": " + e.getMessage());
        }
    }

    @Override
    public void publish(LogRecord record) {
        logToFile(getFormatter().format(record));
        GConsole.printToLog(getFormatter().format(record));
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() throws SecurityException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
