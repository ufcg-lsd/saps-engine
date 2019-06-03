package org.fogbowcloud.saps.engine.core.dto;

import org.fogbowcloud.saps.engine.core.task.Specification;

import java.io.Serializable;

public class TaskResponseDTO implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String id;
    private String state;
    private TaskSpecResponseDTO taskSpec;

    public TaskResponseDTO(String id, String state, TaskSpecResponseDTO taskSpec) {
        this.id = id;
        this.state = state;
        this.taskSpec = taskSpec;
    }

    public String getId() {
        return id;
    }

    public String getState(){ return state; }

    public TaskSpecResponseDTO getTaskSpec() {
        return this.taskSpec;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setState(String state){
        this.state = state;
    }

    public void setTaskSpec(TaskSpecResponseDTO taskSpec){
        this.taskSpec = taskSpec;
    }

    @Override
    public String toString() {
        return "TaskResponseDTO [id=" + id + ", state=" + state + ", taskSpec="
                + taskSpec + "]";
    }

}