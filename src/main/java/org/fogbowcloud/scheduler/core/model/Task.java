package org.fogbowcloud.scheduler.core.model;

import java.util.List;

import org.fogbowcloud.scheduler.core.model.Command.Type;

public interface Task {

	public Specification getSpecification();

	public Task clone();

	public String getId();

	public void finish();

	public boolean isFinished();

	public List<Command> getCommandsByType(Type commandType);
}
