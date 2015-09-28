package org.fogbowcloud.scheduler.core.model;

import java.util.ArrayList;
import java.util.List;

public class Job {

	public static enum TaskState{
		READY,RUNNING,COMPLETED,FAILED
	}
	
	List<Task> tasksReady = new ArrayList<Task>();
	List<Task> tasksRunning = new ArrayList<Task>();
	List<Task> tasksCompleted = new ArrayList<Task>();
	List<Task> tasksFailed = new ArrayList<Task>();
	
	public void addTask(Task task){
		tasksReady.add(task);
	}
	
	public void alterTaskState(TaskState currentState, TaskState targetState, String taskId){
		//TODO move task across lists
	}

	public List<Task> getByState(TaskState ready) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void finish(Task task) {
		this.tasksRunning.remove(task);
		this.tasksCompleted.add(task);
	}

	public void fail(Task task) {
		this.tasksRunning.remove(task);
		this.tasksFailed.add(task);
		
	}
}
