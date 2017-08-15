package org.fogbowcloud.saps.engine.core.dispatcher;

import java.util.List;

public class Submission {
	
	private String id;
	private List<Task> tasks;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}
}
