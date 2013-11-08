/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.common.enums.ECalculationState;
import cz.cuni.mff.bc.common.main.Task;
import cz.cuni.mff.bc.common.main.TaskID;

/**
 * Calculates and returns computed tasks
 *
 * @author Aku
 */
public class Worker implements IWorker {

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
        Client.logger.log("Task : " + tsk.getUnicateID() + " >> calculation started");
        tsk.calculate();
        tsk.setState(ECalculationState.COMPLETE);
        Client.logger.log("Task : " + tsk.getUnicateID() + " >> calculation completed");
        return tsk;
    }
}
