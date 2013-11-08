/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.common.main;

import cz.cuni.mff.bc.common.enums.ProjectState;
import java.io.Serializable;

/**
 *
 * @author Jakub
 */
public class ProjectInfo implements Serializable {

    private String projectName;
    private String clientName;
    private int numOfCompletedTasks;
    private int numOfAllTasks;
    private ProjectState state;
    private int priority;

    public ProjectInfo(String projectName, String clientName, int numOfCompletedTasks, int numOfAllTasks, ProjectState state, int priority) {
        this.projectName = projectName;
        this.clientName = clientName;
        this.numOfCompletedTasks = numOfCompletedTasks;
        this.numOfAllTasks = numOfAllTasks;
        this.state = state;
        this.priority = priority;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public int getNumOfCompletedTasks() {
        return numOfCompletedTasks;
    }

    public void setNumOfCompletedTasks(int numOfCompletedTasks) {
        this.numOfCompletedTasks = numOfCompletedTasks;
    }

    public int getNumOfAllTasks() {
        return numOfAllTasks;
    }

    public void setNumOfAllTasks(int numOfAllTasks) {
        this.numOfAllTasks = numOfAllTasks;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "ProjectID: " + projectName + "; State: " + state + "; Priority " + priority + "; Num of completed tasks: " + numOfCompletedTasks + "/" + numOfAllTasks;
    }
}
