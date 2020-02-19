package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TaskRequestDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String label;
    private Map<String, String> requirements;
    private List<String> commands;
    private Map<String, String> metadata;

    public TaskRequestDTO(String label, Map<String, String> requirements, List<String> commands,
                          Map<String, String> metadata) {
        this.label = label;
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

    @Override
    public String toString() {
        return "TaskRequestDTO [id=" + label + ", requirements=" + requirements+ ", commands=" + commands + ", metadata="
                + metadata + "]";
    }

}