package org.fogbowcloud.scheduler.core.model;

import java.util.List;
import java.util.Map;

import org.fogbowcloud.scheduler.core.model.Command.Type;

public interface Task {

	public Specification getSpecification();

	public Task clone();

	public String getId();

	public void finish();

	public boolean isFinished();

	public void addCommand(Command command);
	
	public List<Command> getCommandsByType(Type commandType);
	
	public List<Command> getAllCommands();

	public void putMetadata(String attributeName, String value);

	public String getMetadata(String attributeName);
	
	public Map<String, String> getAllMetadata();
}
