/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import java.util.concurrent.Callable;

/**
 * Interface used by process holders
 *
 * @author Jakub Hava
 */
public interface IProcessHolder extends Callable<Task> {

    /**
     * Returns id of task which is holden by process holder
     *
     * @return TaskID of Task which is holden by process holder
     */
    public TaskID getCurrentTaskID();

    @Override
    public Task call() throws Exception;
}
