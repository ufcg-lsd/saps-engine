package org.fogbowcloud.saps.engine.core.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.task.Task;

public abstract class Job implements Serializable {

	private static final long serialVersionUID = -6111900503095749695L;
	private static final Logger LOGGER = Logger.getLogger(Job.class);

	private Map<String, Task> taskList;
	private final ReentrantReadWriteLock taskReadyLock;
	private boolean isCreated;

	public Job(List<Task> tasks) {
		this.isCreated = true;
		this.taskList = new HashMap<>();
		this.taskReadyLock = new ReentrantReadWriteLock();
		addTasks(tasks);
	}

	public void addTask(Task task) {
		taskReadyLock.writeLock().lock();
		try {
			getTaskList().put(task.getId(), task);
		} finally {
			taskReadyLock.writeLock().unlock();
		}
	}

	private void addTasks(List<Task> tasks) {
		for(Task task : tasks){
			addTask(task);
		}
	}
	
	public List<Task> getTasks(){
		return new ArrayList<>(taskList.values());
	}
	
	public abstract void setState(JobState state);

	public abstract String getId();

	public boolean isCreated() {
		return this.isCreated;
	}

	public void setCreated() { this.isCreated = true; }

	public void restart() {
		this.isCreated = false;
	}

	public Map<String, Task> getTaskList() {
		return taskList;
	}

	public void setTaskList(Map<String, Task> taskList) {
		this.taskList = taskList;
	}
}