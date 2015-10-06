package org.fogbowcloud.scheduler.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class Job {

	public static enum TaskState{
		READY,RUNNING,COMPLETED,FAILED
	}
	
	protected List<Task> tasksReady = new ArrayList<Task>();
	protected List<Task> tasksRunning = new ArrayList<Task>();
	protected List<Task> tasksCompleted = new ArrayList<Task>();
	protected List<Task> tasksFailed = new ArrayList<Task>();
	
	protected ReentrantReadWriteLock taskReadyLock = new ReentrantReadWriteLock();
	
	public void addTask(Task task) {
		taskReadyLock.writeLock().lock();
		try {
			tasksReady.add(task);
		} finally {
			taskReadyLock.writeLock().unlock();
		}
	}

	public List<Task> getByState(TaskState state) {
		if (state.equals(TaskState.READY)) {
			return tasksReady;
		} else if (state.equals(TaskState.RUNNING)) {
			return tasksRunning;
		} else if (state.equals(TaskState.COMPLETED)) {
			return tasksCompleted;
		} else if (state.equals(TaskState.FAILED)) {
			return tasksFailed;
		} 
		return null;
	}
	
	public abstract void run(Task task);
	
	public abstract void finish(Task task);

	public abstract void fail(Task task);
}
