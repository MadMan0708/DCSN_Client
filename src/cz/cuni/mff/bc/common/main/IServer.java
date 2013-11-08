/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.common.main;

/**
 *
 * @author Aku
 */
import cz.cuni.mff.bc.common.enums.InformMessage;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import org.cojen.dirmi.Asynchronous;
import org.cojen.dirmi.Pipe;

public interface IServer extends Remote {

    //  public EUserAddingState addClient(String clientID) throws RemoteException;
    public Task getTask(String clientID, TaskID taskID) throws RemoteException;

    public void saveCompletedTask(String clientID, Task task) throws RemoteException;

    public boolean isProjectExists(String clientID, String projectID) throws RemoteException;

    @Asynchronous
    public Pipe uploadProject(String clientID, String projectID, int priority, String extension, Pipe pipe) throws RemoteException;

    public byte[] getClassData(TaskID id) throws RemoteException;

    public boolean isProjectReadyForDownload(String clientID, String projectID) throws RemoteException;

    public long getProjectFileSize(String clientID, String projectID) throws RemoteException;

    @Asynchronous
    public Pipe downloadProject(String clientID, String projectID, Pipe pipe) throws RemoteException;

    public void setSessionClassLoaderDetails(String clientSessionID, ProjectUID projectUID) throws RemoteException;

    public TaskID getTaskIdBeforeCalculation(String clientID) throws RemoteException;

    public ArrayList<ProjectInfo> getProjectList(String clientID) throws RemoteException;

    public void sendInformMessage(String clientID, InformMessage message) throws RemoteException;

    public boolean pauseProject(String clientID, String projectID) throws RemoteException;

    public boolean cancelProject(String clientID, String projectID) throws RemoteException;

    public boolean unpauseProject(String clientID, String projectID) throws RemoteException;

    public ArrayList<TaskID> calculatedTasks(String clientID, ArrayList<TaskID> tasks) throws RemoteException;

    public void cancelTaskFromClient(String clientID, TaskID taskToCancel) throws RemoteException;
}
