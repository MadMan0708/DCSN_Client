/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.logging;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author Jakub
 */
public class CustomHandler extends Handler {
    
    private ArrayList<ILogTarget> targets = new ArrayList<>();
    
    public void addLogTarget(ILogTarget target) {
        targets.add(target);
    }

    public void deleteLogTarger(ILogTarget target) {
        targets.remove(target);
    }

    @Override
    public void publish(LogRecord record) {
        for (ILogTarget iLogTarget : targets) {
            iLogTarget.log(getFormatter().format(record));
        }
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
