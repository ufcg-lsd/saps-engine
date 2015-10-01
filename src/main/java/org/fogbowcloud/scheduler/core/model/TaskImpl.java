package org.fogbowcloud.scheduler.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.scheduler.core.model.Command.Type;

public class TaskImpl implements Task {

	//Environment variables related to task
	public static final String ENV_LOCAL_OUT_DIR = "";
	
	public static final String METADATA_REMOTE_OUTPUT_FOLDER = "remote_output_folder";
	public static final String METADATA_LOCAL_OUTPUT_FOLDER = "local_output_folder";
	public static final String METADATA_SANDBOX = "sandbox";
	
	private boolean isFinished = false;
	private String id;
	private Specification spec;
	private List<Command> commands = new ArrayList<Command>();
	private Map<String, String> metadata = new HashMap<String, String>();
	
	public TaskImpl(String id, Specification spec) {
		this.id = id;
		this.spec = spec;
	}
	
	public void putMetadata(String attributeName, String value) {
		metadata.put(attributeName, value);
	}
	
	public String getMetadata(String attributeName) {
		return metadata.get(attributeName);
	}
	
	@Override
	public Specification getSpecification() {
		return this.spec;
	}

	@Override
	public Task clone() {
		// TODO Auto-generated method stub
		// Add commands unstarted??
		// Same id?
		return new TaskImpl(getId(), getSpecification());
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
		for (Command command : commands) {
			if (command.getType().equals(commandType)) {
				commandsToReturn.add(command);
			}
		}
		return commandsToReturn;
	}
}
