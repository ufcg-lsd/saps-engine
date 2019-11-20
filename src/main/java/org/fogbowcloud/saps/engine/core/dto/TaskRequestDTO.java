package org.fogbowcloud.saps.engine.core.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.task.Specification;

public class TaskRequestDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String id;
    private Map<String, String> requirements;
    private List<String> commands;
    private Map<String, String> metadata;

    public TaskRequestDTO(String id, Map<String, String> requirements, List<String> commands,
                          Map<String, String> metadata) {
        this.id = id;
        this.requirements = requirements;
        this.commands = commands;
        this.metadata = metadata;
    }

    // empty constructor required for Gson.
    public TaskRequestDTO() {}

    public List<String> getCommands() {
        return this.commands;
    }

    public Map<String, String> getRequirements() {
		return requirements;
	}

	public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "TaskRequestDTO [id=" + id + ", requirements=" + requirements+ ", commands=" + commands + ", metadata="
                + metadata + "]";
    }

}