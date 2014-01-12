/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.computation;

import cz.cuni.mff.bc.api.main.Task;
import cz.cuni.mff.bc.api.main.TaskID;
import java.util.concurrent.Callable;

/**
 *
 * @author UP711643
 */
public interface IProcessHolder extends Callable<Task> {

    /**
     *
     * @return TaskID of Task which is currently located in ProccessHolder
     */
    public TaskID getCurrentTaskID();

    @Override
    public Task call() throws Exception;
}
