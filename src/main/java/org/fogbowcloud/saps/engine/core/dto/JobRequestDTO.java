package org.fogbowcloud.saps.engine.core.dto;

import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.core.task.Task;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JobRequestDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String label;
    
    @SerializedName("task_specs")
    private List<TaskRequestDTO> tasksSpecs;

    public JobRequestDTO(SapsJob job) {
        this.tasksSpecs = new ArrayList<TaskRequestDTO>();
        this.label = job.getName();
        populateTaskSpec(job);
    }

    private void populateTaskSpec(SapsJob job) {
        List<Task> taskList = job.getTasks();
        for (Task task : taskList) {
            this.tasksSpecs.add(new TaskRequestDTO(task.getId(), task.getRequirements(),
                    task.getAllCommandsInStr(), task.getAllMetadata()));
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<TaskRequestDTO> getTasksSpecs() {
        return tasksSpecs;
    }

    public void setTasksSpecs(List<TaskRequestDTO> tasksSpecs) {
        this.tasksSpecs = tasksSpecs;
    }

    @Override
    public String toString() {
        return "JobRequestDTO [label=" + label + ", tasksSpec=" + tasksSpecs + "]";
    }
}
