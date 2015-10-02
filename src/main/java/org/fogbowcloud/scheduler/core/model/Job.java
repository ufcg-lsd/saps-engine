package org.fogbowcloud.scheduler.core.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Job {

	public static enum TaskState{
		READY,RUNNING,COMPLETED,FAILED
	}
	
	protected List<Task> tasksReady = new ArrayList<Task>();
	protected List<Task> tasksRunning = new ArrayList<Task>();
	protected List<Task> tasksCompleted = new ArrayList<Task>();
	protected List<Task> tasksFailed = new ArrayList<Task>();
	
	public void addTask(Task task){
		tasksReady.add(task);
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
