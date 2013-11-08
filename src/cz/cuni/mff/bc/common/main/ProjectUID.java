/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.common.main;

import cz.cuni.mff.bc.common.enums.ProjectState;
import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Jakub
 */
public class ProjectUID implements Serializable {

    private String clientID;
    private String projectID;
    private ProjectState state;

    public ProjectUID(String clientID, String projectID) {
        this.clientID = clientID;
        this.projectID = projectID;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.clientID);
        hash = 41 * hash + Objects.hashCode(this.projectID);
        return hash;
    }

    public String getProjectID() {
        return projectID;
    }

    public String getClientID() {
        return clientID;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProjectUID other = (ProjectUID) obj;
        if (!Objects.equals(this.clientID, other.clientID)) {
            return false;
        }
        if (!Objects.equals(this.projectID, other.projectID)) {
            return false;
        }
        return true;
    }
}
