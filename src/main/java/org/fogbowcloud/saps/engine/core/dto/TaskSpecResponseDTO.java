package org.fogbowcloud.saps.engine.core.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TaskSpecResponseDTO implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String id;
	private List<CommandResponseDTO> commands;
	private Map<String, String> metadata;
	private Map<String, String> requirements;

	public TaskSpecResponseDTO(String id, List<CommandResponseDTO> commands, Map<String, String> metadata,
			Map<String, String> requirements) {
		this.id = id;
		this.commands = commands;
		this.metadata = metadata;
		this.requirements = requirements;
	}

	public String getId() {
		return id;
	}

	public List<CommandResponseDTO> getCommands() {
		return commands;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public Map<String, String> getRequirements() {
		return requirements;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setCommands(List<CommandResponseDTO> commands) {
		this.commands = commands;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public void setRequirements(Map<String, String> requirements) {
		this.requirements = requirements;
	}

	@Override
	public String toString() {
		return "TaskSpecResponseDTO [id=" + id + ", requirements=" + requirements + ", commands=" + commands
				+ ", metadata=" + metadata + "]";
	}

}