package org.fogbowcloud.scheduler.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

public abstract class Job {

	public static enum TaskState{
		READY,RUNNING,COMPLETED,FAILED
	}
	
	public static final Logger LOGGER = Logger.getLogger(Job.class);
	
	protected List<Task> tasksReady = new ArrayList<Task>();
	protected List<Task> tasksRunning = new ArrayList<Task>();
	protected List<Task> tasksCompleted = new ArrayList<Task>();
	protected List<Task> tasksFailed = new ArrayList<Task>();
	
	protected ReentrantReadWriteLock taskReadyLock = new ReentrantReadWriteLock();
	
	public void addTask(Task task) {
		LOGGER.debug("Adding task " + task.getId());
		taskReadyLock.writeLock().lock();
		try {
			tasksReady.add(task);
		} finally {
			taskReadyLock.writeLock().unlock();
		}
	}

	public List<Task> getByState(TaskState state) {
		if (state.equals(TaskState.READY)) {
			return new ArrayList<Task>(tasksReady);
		} else if (state.equals(TaskState.RUNNING)) {
			return new ArrayList<Task>(tasksRunning);
		} else if (state.equals(TaskState.COMPLETED)) {
			return new ArrayList<Task>(tasksCompleted);
		} else if (state.equals(TaskState.FAILED)) {
			return new ArrayList<Task>(tasksFailed);
		} 
		return null;
	}
	
	public abstract void run(Task task);
	
	public abstract void finish(Task task);

	public abstract void fail(Task task);
}
