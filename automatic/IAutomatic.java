/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client.automatic;

import java.util.concurrent.Callable;

/**
 *
 * @author Jakub
 */
public interface IAutomatic extends Callable<Object> {
    @Override
    public Object call() throws Exception;
    public void endAfterActualCycle();
}
