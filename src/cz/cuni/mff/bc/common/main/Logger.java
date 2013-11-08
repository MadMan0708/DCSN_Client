/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.common.main;

import cz.cuni.mff.bc.common.enums.ELoggerMessages;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *
 * @author Aku
 */
public class Logger {

    File file;
    Calendar cal;

    public Logger(String file) {
        this.file = new File(file);
    }

    private String actualTime() {
        cal = GregorianCalendar.getInstance();
        String sec;
        String min;
        String hour;

        int n;
        if ((n = cal.get(Calendar.SECOND)) < 10) {
            sec = "0" + n;
        } else {
            sec = "" + n;
        }
        if ((n = cal.get(Calendar.MINUTE)) < 10) {
            min = "0" + n;
        } else {
            min = "" + n;
        }
        if ((n = cal.get(Calendar.HOUR_OF_DAY)) < 10) {
            hour = "0" + n;
        } else {
            hour = "" + n;
        }
        return hour + ":" + min + ":" + sec;
    }

    public synchronized void log(String msg) {
        log(msg, ELoggerMessages.INFORMATIVE);
    }

    public synchronized void log(String msg, ELoggerMessages type) {
        //Commented to make classical console able to use
        //System.out.println(type.toString() + " : " + msg);
        cal = GregorianCalendar.getInstance();

        logToFile(actualTime() + "\t: " + type.toString() + "\t: " + msg);
        GConsole.printToLog(actualTime() + "\t: " + type.toString() + "\t: " + msg);
    }

    private void logToFile(String msg) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            pw.println(msg);
        } catch (IOException e) {
            log("Could't write msg to log file: " + msg, ELoggerMessages.ERROR);
        }
    }
}
