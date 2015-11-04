package org.fogbowcloud.scheduler.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.fogbowcloud.scheduler.core.model.Command.Type;

import org.apache.log4j.Logger;

public class TaskImpl implements Task {

	private static final Logger LOGGER = Logger.getLogger(TaskImpl.class);
	
	//Environment variables related to task
	public static final String ENV_LOCAL_OUT_DIR = "";
	
	public static final String METADATA_REMOTE_OUTPUT_FOLDER = "remote_output_folder";
	public static final String METADATA_LOCAL_OUTPUT_FOLDER = "local_output_folder";
	public static final String METADATA_SANDBOX = "sandbox";
	public static final String METADATA_REMOTE_COMMAND_EXIT_PATH = "remote_command_exit_path";
	public static final String METADATA_RESOURCE_ID = "resource_id";
	public static final String METADATA_TASK_TIMEOUT = "task_timeout";
	
	
	private boolean isFinished = false;
	private String id;
	private Specification spec;
	private List<Command> commands = new ArrayList<Command>();
	private Map<String, String> metadata = new HashMap<String, String>();
	private boolean isFailed = false;
	
	private long startedRunningAt = Long.MAX_VALUE;
	
	public TaskImpl(String id, Specification spec) {
		this.id = id;
		this.spec = spec;
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
	public Specification getSpecification() {
		return this.spec;
	}

	@Override
	public Task clone() {
		TaskImpl taskClone = new TaskImpl(UUID.randomUUID().toString() + "_clonedFrom_" + getId(),
				getSpecification());
		Map<String, String> allMetadata = getAllMetadata();
		for (String attribute : allMetadata.keySet()) {
			taskClone.putMetadata(attribute, allMetadata.get(attribute));
		}

		List<Command> commands = getAllCommands();
		for (Command command : commands) {
			taskClone.addCommand(command);
		}
		return taskClone;
	}

	@Override
	public List<Command> getAllCommands() {
		return commands;
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
	public void finish(){
		this.isFinished = true;
	}

	@Override
	public boolean isFinished() {
		return this.isFinished;
	}

	@Override
	public List<Command> getCommandsByType(Type commandType) {
		List<Command> commandsToReturn = new ArrayList<Command>();
		for (Command command : getAllCommands()) {
			if (command.getType().equals(commandType)) {
				commandsToReturn.add(command);
			}
		}
		return commandsToReturn;
	}

	@Override
	public void addCommand(Command command) {
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
		if (timeOutRaw == null || timeOutRaw.isEmpty()){
			return false;
		}
		long timeOut;
		try {
		timeOut = Long.parseLong(timeOutRaw);
		} catch (NumberFormatException e){
			LOGGER.error("Timeout badly formated, ignoring it: ", e);
			return false;
		}
		if (System.currentTimeMillis() - this.startedRunningAt > timeOut){
			return true;
		} else {
			return false;
		}
		
	}

	@Override
	public void startedRunning() {
		this.startedRunningAt = System.currentTimeMillis();
		
	}
}
