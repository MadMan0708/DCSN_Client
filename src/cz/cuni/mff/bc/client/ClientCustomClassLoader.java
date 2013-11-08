/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.client;

import cz.cuni.mff.bc.common.main.IServer;
import cz.cuni.mff.bc.common.main.ProjectUID;
import cz.cuni.mff.bc.common.main.TaskID;
import java.io.IOException;
import java.util.HashMap;

/**
 * Loads general tasks from server Uses cache, therefor there is no need to
 * download class data from server after it has been done for first
 *
 * @author Jakub
 */
public class ClientCustomClassLoader extends ClassLoader {

    private IServer remoteService;
    private TaskID taskID;
    private HashMap<ProjectUID, byte[]> classCache;

    /**
     * Default class loader
     */
    public ClientCustomClassLoader() {
        classCache = new HashMap<>();
    }

    /**
     * Sets taskID, for which will be correct class loaded from server
     *
     * @param taskID taskID to be set
     */
    public void setTaskID(TaskID taskID) {
        this.taskID = taskID;
    }

    /**
     * Sets IServer remote service
     *
     * @param remoteService IServer remote service
     */
    public void setRemoteService(IServer remoteService) {
        this.remoteService = remoteService;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            if (classCache.containsKey(taskID.getProjectUID())) {
            } else {
                classCache.put(taskID.getProjectUID(), loadClassData());
            }

        } catch (IOException e) {
            throw new ClassNotFoundException("Class [" + name
                    + "] could not be found", e);
        }
        byte[] classData = classCache.get(taskID.getProjectUID());
        Class<?> c = defineClass(name, classData, 0, classData.length);

        resolveClass(c);
        return c;
    }

    private byte[] loadClassData() throws IOException {
        byte[] classData = remoteService.getClassData(taskID);

        return classData;
    }
}
