package org.fogbowcloud.saps.engine.core.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dto.CommandRequestDTO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TaskImpl implements Task {

	private static final long serialVersionUID = -8068456932331499162L;

	private static final Logger LOGGER = Logger.getLogger(TaskImpl.class);

	public static final String ENV_LOCAL_OUT_DIR = "";

	public static final String METADATA_REMOTE_OUTPUT_FOLDER = "remote_output_folder";
	public static final String METADATA_LOCAL_OUTPUT_FOLDER = "local_output_folder";
	public static final String METADATA_SANDBOX = "sandbox";
	public static final String METADATA_REMOTE_COMMAND_EXIT_PATH = "remote_command_exit_path";
	public static final String METADATA_RESOURCE_ID = "resource_id";
	public static final String METADATA_TASK_TIMEOUT = "task_timeout";
	public static final String METADATA_MAX_RESOURCE_CONN_RETRIES = "max_conn_retries";

	private String id;
	private String uuid;
	private Map<String, String> requirements;
	private TaskState state;
	private List<CommandRequestDTO> commands;
	private List<String> commandsStr;
	private List<String> processes;
	private Map<String, String> metadata;
	private boolean isFailed;
	private boolean isFinished;
	private int retries;
	private long startedRunningAt;

	public TaskImpl(String id, Map<String, String> requirements, String uuid) {
		this.commands = new ArrayList<>();
		this.processes = new ArrayList<>();
		this.metadata = new HashMap<>();
		this.isFailed = false;
		this.isFinished = false;
		this.id = id;
		this.retries = -1;
		this.requirements = requirements;
		this.state = TaskState.READY;
		this.uuid = uuid;
		this.startedRunningAt = Long.MAX_VALUE;
		this.commandsStr = new ArrayList<>();
	}

	public TaskImpl(String id, String uuid){
		this(id, null, uuid);
	}

	public TaskImpl(String id, Map<String, String> requirements) {
		this(id, requirements, UUID.randomUUID().toString());
	}

	private void populateCommandStrList() {
		for (int i = 0; i < this.commands.size(); i++) {
			this.commandsStr.add(this.commands.get(i).getCommand());
		}
	}

	@Override
	public void putRequirement(String attributeName, String value) {
		requirements.put(attributeName, value);
	}
	
	@Override
	public void putMetadata(String attributeName, String value) {
		metadata.put(attributeName, value);
	}

	@Override
	public String getMetadata(String attributeName) {
		return metadata.get(attributeName);
	}

	@Override
	public Map<String, String> getRequirements() {
		return this.requirements;
	}

	@Override
	public Task clone() {
		TaskImpl taskClone = new TaskImpl(UUID.randomUUID() + "_clonedFrom_" + getId(), getRequirements(), getUUID());
		Map<String, String> allMetadata = getAllMetadata();
		for (String attribute : allMetadata.keySet()) {
			taskClone.putMetadata(attribute, allMetadata.get(attribute));
		}

		List<CommandRequestDTO> commands = getAllCommands();
		for (CommandRequestDTO command : commands) {
			taskClone.addCommand(command);
		}
		return taskClone;
	}

	@Override
	public List<CommandRequestDTO> getAllCommands() {
		return commands;
	}

	public List<String> getAllCommandsInStr() {
		return this.commandsStr;
	}

	@Override
	public Map<String, String> getAllMetadata() {
		return metadata;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void finish() {
		this.isFinished = true;
		setState(TaskState.COMPLETED);
	}

	@Override
	public boolean isFinished() {
		return this.isFinished;
	}

	@Override
	public void addCommand(CommandRequestDTO command) {
		commands.add(command);
	}

	@Override
	public void fail() {
		isFailed = true;
	}

	@Override
	public boolean isFailed() {
		return isFailed;
	}

	@Override
	public boolean checkTimeOuted() {
		String timeOutRaw = getMetadata(METADATA_TASK_TIMEOUT);

		if (timeOutRaw == null || timeOutRaw.trim().isEmpty()) {
			return false;
		}
		long timeOut;
		try {
			timeOut = Long.parseLong(timeOutRaw);
		} catch (NumberFormatException e) {
			LOGGER.error("Timeout badly formated, ignoring it: ", e);
			return false;
		}
		return System.currentTimeMillis() - this.startedRunningAt > timeOut;
	}

	@Override
	public void startedRunning() {
		this.startedRunningAt = System.currentTimeMillis();
		this.retries++;
	}

	@Override
	public boolean mayRetry() {
		if (getMetadata(METADATA_MAX_RESOURCE_CONN_RETRIES) != null) {
			return getRetries() <= Integer.parseInt(getMetadata(METADATA_MAX_RESOURCE_CONN_RETRIES));
		}
		return false;
	}

	@Override
	public int getRetries() {
		return retries;
	}

	@Override
	public void setRetries(int retries) {
		this.retries = retries;
	}

	@Override
	public void addProcessId(String procId) {
		this.processes.add(procId);
	}

	@Override
	public int getNumberOfCommands() {
		return commands.size();
	}

	@Override
	public List<String> getProcessId() {

		return this.processes;
	}

	@Override
	public TaskState getState() {
		return this.state;
	}

	@Override
	public void setState(TaskState state) {
		this.state = state;
	}

	@Override
	public String getUUID() {
		return this.uuid;
	}

	public JSONObject toJSON() {
		try {
			JSONObject task = new JSONObject();
			task.put("isFinished", this.isFinished());
			task.put("isFailed", this.isFailed());
			task.put("id", this.getId());
			
			JSONObject requirements = new JSONObject();
			for (Map.Entry<String, String> entry : this.getRequirements().entrySet()) {
				metadata.put(entry.getKey(), entry.getValue());
			}
			task.put("requirements", requirements);
			
			task.put("retries", this.getRetries());
			task.put("uuid", this.getUUID());
			task.put("state", this.state.getDesc());
			JSONArray commands = new JSONArray();
			for (CommandRequestDTO command : this.getAllCommands()) {
				commands.put(command.toJSON());
			}
			task.put("commands", commands);
			JSONObject metadata = new JSONObject();
			for (Map.Entry<String, String> entry : this.getAllMetadata().entrySet()) {
				metadata.put(entry.getKey(), entry.getValue());
			}
			task.put("metadata", metadata);
			return task;
		} catch (JSONException e) {
			LOGGER.debug("Error while trying to create a JSON from task", e);
			return null;
		}
	}

	public static Task fromJSON(JSONObject taskJSON) {
		Task task = new TaskImpl(taskJSON.optString("id"), taskJSON.optString("uuid"));
		task.setRetries(taskJSON.optInt("retries"));
		if (taskJSON.optBoolean("isFinished")) {
			task.finish();
		}
		if (taskJSON.optBoolean("isFailed")) {
			task.fail();
		}
		
		JSONObject requirements = taskJSON.optJSONObject("requiriments");
		Iterator<?> requirementsKeys = requirements.keys();
		while (requirementsKeys.hasNext()) {
			String key = (String) requirementsKeys.next();
			task.putRequirement(key, requirements.optString(key));
		}

		JSONArray commands = taskJSON.optJSONArray("commands");
		for (int i = 0; i < commands.length(); i++) {
			task.addCommand(CommandRequestDTO.fromJSON(commands.optJSONObject(i)));
		}

		JSONObject metadata = taskJSON.optJSONObject("metadata");
		Iterator<?> metadataKeys = metadata.keys();
		while (metadataKeys.hasNext()) {
			String key = (String) metadataKeys.next();
			task.putMetadata(key, metadata.optString(key));
		}
		return task;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((requirements == null) ? 0 : requirements.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskImpl other = (TaskImpl) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (requirements == null) {
			return other.requirements == null;
		} else
			return requirements.equals(other.requirements);
	}
}