package org.fogbowcloud.saps.engine.core.dispatcher;

import java.util.ArrayList;
import java.util.List;

public class Submission {

	private String id;
	private List<Task> tasks;

	public Submission(String id) {
		this.id = id;
		this.tasks = new ArrayList<Task>();
	}

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

	public void addTask(Task task) {
		this.tasks.add(task);
	}

	public void removeTask(Task task) {
		this.tasks.remove(task);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Submission) {
			Submission other = (Submission) o;
			return getId().equals(other.getId()) && getTasks().equals(other.getTasks());
		}
		return false;
	}
}
