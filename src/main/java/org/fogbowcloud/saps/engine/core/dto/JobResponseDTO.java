package org.fogbowcloud.saps.engine.core.dto;

import java.io.Serializable;
import java.util.List;

public class JobResponseDTO implements Serializable {

    private String id;
    private String label;
    private String jobState;
    private List<TaskResponseDTO> tasks;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getJobState() {
        return jobState;
    }

    public void setJobState(String jobState) {
        this.jobState = jobState;
    }

    public List<TaskResponseDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskResponseDTO> tasks) {
        this.tasks = tasks;
    }

    @Override
    public String toString() {
        return "JobResponseDTO [id=" + id + ", label=" + label + ", jobState=" + jobState + ", tasks="
                + tasks + "]";
    }
}
