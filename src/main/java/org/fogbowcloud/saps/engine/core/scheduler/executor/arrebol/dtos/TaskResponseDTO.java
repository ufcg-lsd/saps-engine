package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class TaskResponseDTO implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_FINISHED = "FINISHED";
    
    private String id;
    private String state;
    @SerializedName("tasks_specs")
    private TaskSpecResponseDTO tasksSpecs;

    public TaskResponseDTO(String id, String state, TaskSpecResponseDTO taskSpec) {
        this.id = id;
        this.state = state;
        this.tasksSpecs = taskSpec;
    }

    public String getId() {
        return id;
    }

    public String getState(){ return state; }

    public TaskSpecResponseDTO getTaskSpec() {
        return this.tasksSpecs;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setState(String state){
        this.state = state;
    }

    public void setTaskSpec(TaskSpecResponseDTO taskSpec){
        this.tasksSpecs = taskSpec;
    }

    @Override
    public String toString() {
        return "TaskResponseDTO [id=" + id + ", state=" + state + ", taskSpec="
                + tasksSpecs + "]";
    }

}