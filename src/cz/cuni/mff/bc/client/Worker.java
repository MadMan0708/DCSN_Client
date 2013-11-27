/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.api.enums.TaskState;
import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calculates and returns computed tasks
 *
 * @author Aku
 */
public class Worker implements IWorker {

    private static final Logger LOG = Logger.getLogger(Client.class.getName());
    private Task tsk;

    /**
     * Initialise Worker class with Task to be calculated
     *
     * @param tsk Task to be calculated
     */
    public Worker(Task tsk) {
        this.tsk = tsk;
    }

    @Override
    public TaskID getCurrentTaskID() {
        return tsk.getUnicateID();
    }

    @Override
    public Task call() throws Exception {
        LOG.log(Level.INFO, "Task : {0} >> calculation started", tsk.getUnicateID());
        tsk.calculate();
        tsk.setState(TaskState.COMPLETE);
        LOG.log(Level.INFO, "Task : {0} >> calculation completed", tsk.getUnicateID());
        return tsk;
    }
}
