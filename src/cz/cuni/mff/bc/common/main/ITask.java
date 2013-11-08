package cz.cuni.mff.bc.common.main;

import java.io.Serializable;
import java.nio.file.Path;
import java.rmi.Remote;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Aku
 */
public interface ITask extends Remote, Serializable {
    public void loadData(Path from);

    public void saveData(Path to);

    public void calculate();
    
    // public void calculationState();
}
