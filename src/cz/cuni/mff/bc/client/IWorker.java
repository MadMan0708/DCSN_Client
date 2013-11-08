/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.common.main.Task;
import cz.cuni.mff.bc.common.main.TaskID;
import java.util.concurrent.Callable;

/**
 * Interface used by Worker class
 *
 * @author Jakub
 */
public interface IWorker extends Callable<Task> {

    /**
     *
     * @return TaskID of Task which is currently calculated by Worker
     */
    public TaskID getCurrentTaskID();

    @Override
    public Task call() throws Exception;
}
