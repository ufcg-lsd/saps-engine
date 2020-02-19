package org.fogbowcloud.saps.engine.core.model;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SapsTask {

	private static final Logger LOGGER = Logger.getLogger(SapsTask.class);
	
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static final String JSON_HEADER_ID = "id";
	private static final String JSON_HEADER_REQUIREMENTS = "requirements";
	private static final String JSON_HEADER_COMMANDS = "commands";
	private static final String JSON_HEADER_METADATA = "metadata";

	private String id;
	private Map<String, String> requirements;
	private List<String> commands;
	private Map<String, String> metadata;

	public SapsTask(String id, Map<String, String> requirements, List<String> commands, Map<String, String> metadata) {
		this.id = id;
		this.requirements = requirements;
		this.commands = commands;
		this.metadata = metadata;
	}

	public SapsTask(String id) {
		this(id, new HashMap<String, String>(), new LinkedList<String>(), new HashMap<String, String>());
	}

	public static List<String> buildCommandList(SapsImage task, String phase) {
		// info shared dir between host (with NFS) and container
		// ...

		DateFormat dateFormater = new SimpleDateFormat(DATE_FORMAT);
		String taskDir = task.getTaskId();
		String rootPath = "/nfs/" + taskDir;
		String phaseDirPath = "/nfs/" + taskDir + File.separator + phase;
		List<String> commands = new LinkedList<String>();

		// Remove dirs
		String removeThings = String.format("rm -rf %s", phaseDirPath);
		commands.add(removeThings);

		// Create dirs
		String createDirectory = String.format("mkdir -p %s", phaseDirPath);
		commands.add(createDirectory);

		// Run command
		String runCommand = String.format("bash /home/saps/run.sh %s %s %s %s", rootPath, task.getDataset(),
				task.getRegion(), dateFormater.format(task.getImageDate()));
		commands.add(runCommand);
		
		return commands;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, String> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<String, String> requirements) {
		this.requirements = requirements;
	}
	
	public void addRequirement(String key, String value) {
		this.requirements.put(key, value);
	}

	public List<String> getCommands() {
		return commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}
	
	public void addCommand(String command) {
		this.commands.add(command);
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	
	public void addMetadata(String key, String value) {
		this.metadata.put(key, value);
	}

	public JSONObject toJSON() {
		try {
			JSONObject sapsTask = new JSONObject();
			sapsTask.put(JSON_HEADER_ID, this.getId());

			JSONObject requirements = new JSONObject();
			for (Map.Entry<String, String> entry : this.getRequirements().entrySet())
				requirements.put(entry.getKey(), entry.getValue());
			sapsTask.put(JSON_HEADER_REQUIREMENTS, requirements);

			JSONArray commands = new JSONArray();
			for (String command : this.getCommands())
				commands.put(command);
			sapsTask.put(JSON_HEADER_COMMANDS, commands);

			JSONObject metadata = new JSONObject();
			for (Map.Entry<String, String> entry : this.getMetadata().entrySet())
				metadata.put(entry.getKey(), entry.getValue());
			sapsTask.put(JSON_HEADER_METADATA, metadata);

			return sapsTask;
		} catch (JSONException e) {
			LOGGER.debug("Error while trying to create a JSON from task", e);
			return null;
		}
	}

	public static SapsTask fromJSON(JSONObject taskJSON) {
		SapsTask sapsTask = new SapsTask(taskJSON.optString(JSON_HEADER_ID));

		JSONObject requirements = taskJSON.optJSONObject(JSON_HEADER_REQUIREMENTS);
		Iterator<?> requirementsKeys = requirements.keys();
		while (requirementsKeys.hasNext()) {
			String key = (String) requirementsKeys.next();
			sapsTask.addRequirement(key, requirements.optString(key));
		}

		JSONArray commands = taskJSON.optJSONArray("commands");
		for (int i = 0; i < commands.length(); i++)
			try {
				sapsTask.addCommand((String) commands.toString(i));
			} catch (JSONException e) {
				e.printStackTrace();
			}

		JSONObject metadata = taskJSON.optJSONObject("metadata");
		Iterator<?> metadataKeys = metadata.keys();
		while (metadataKeys.hasNext()) {
			String key = (String) metadataKeys.next();
			sapsTask.addMetadata(key, metadata.optString(key));
		}
		return sapsTask;
	}
}
