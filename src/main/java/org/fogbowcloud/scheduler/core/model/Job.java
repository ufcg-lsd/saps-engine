package org.fogbowcloud.scheduler.core.model;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.scheduler.core.model.Job.TaskState;

public class Job {

	public static enum TaskState{
		READY,RUNNING,COMPLETED,FAILED
	}
	
	List<Task> tasksReady = new ArrayList<Task>();
	List<Task> tasksRunning = new ArrayList<Task>();
	List<Task> tasksCompleted = new ArrayList<Task>();
	List<Task> tasksfailed = new ArrayList<Task>();
	
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
		
}
