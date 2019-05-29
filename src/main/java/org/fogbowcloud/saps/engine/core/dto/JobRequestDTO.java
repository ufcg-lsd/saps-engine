package org.fogbowcloud.saps.engine.core.dto;

import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.core.task.Task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JobRequestDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String label;
    private List<TaskRequestDTO> tasksSpecs;

    public JobRequestDTO(SapsJob job) {
        this.tasksSpecs = new ArrayList<>();
        this.label = job.getName();
        populateTaskSpec(job);
    }

    private void populateTaskSpec(SapsJob job) {
        List<Task> taskList = job.getTasks();
        for (Task task : taskList) {
            this.tasksSpecs.add(new TaskRequestDTO(task.getId(), task.getSpecification(),
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
