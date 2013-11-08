/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.common.main;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Aku
 */
public class TaskID implements Serializable {

    private String projectID;
    private String clientID;
    private String className;
    private String taskID;
    private int priority;

    public TaskID(String projectID, String clientID, String taskID, int priority, String className) {
        this.projectID = projectID;
        this.clientID = clientID;
        this.className = className;
        this.taskID = taskID;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getClassName() {
        return className;
    }

    public String getProjectID() {
        return projectID;
    }

    public ProjectUID getProjectUID() {
        return new ProjectUID(clientID, projectID);
    }

    public String getClientID() {
        return clientID;
    }

    public String getTaskID() {
        return taskID;
    }

    @Override
    public String toString() {
        return clientID + "_" + projectID + "_" + taskID;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TaskID)) {
            return false;
        } else {
            TaskID ID = (TaskID) other;
            if (this.clientID.equals(ID.getClientID()) && this.projectID.equals(ID.getProjectID()) && this.taskID.equals(ID.getTaskID())) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.projectID);
        hash = 53 * hash + Objects.hashCode(this.clientID);
        hash = 53 * hash + Objects.hashCode(this.taskID);
        return hash;
    }
}
