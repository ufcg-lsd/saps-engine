package org.fogbowcloud.scheduler.core.model;

import java.util.UUID;

public class JDFJob extends Job{

	public String jobId = "0";
	
	public String name;
	
	public String schedPath;
	
	public JDFJob(){
		super();
		setJobId(UUID.randomUUID().toString());
	}
	
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	public void setSchedPath(String schedPath) {
		this.schedPath = schedPath;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	
	public String getSchedPath() {
		return this.schedPath;
	}
	
	@Override
	public void run(Task task) {
		tasksReady.remove(task);
		tasksRunning.add(task);
		
	}

	@Override
	public void finish(Task task) {
		tasksRunning.remove(task);
		tasksCompleted.add(task);
	}

	@Override
	public void fail(Task task) {
		tasksRunning.remove(task);
		tasksFailed.add(task);		
	}


	public String getId() {
		return jobId;
	}
	
	public Task getCompletedTask(String taskId){
		for (Task task : this.tasksCompleted){
			if (task.getId().equals(taskId)){
				return task;
			}
		}
		return null;
	}

	public Task getTaskById(String taskId) {
		for (Task task : this.tasksCompleted){
			if (task.getId().equals(taskId)){
				return task;
			}
		}
		for (Task task : this.tasksFailed){
			if (task.getId().equals(taskId)){
				return task;
			}
		}
		for (Task task : this.tasksRunning){
			if (task.getId().equals(taskId)){
				return task;
			}
		}
		for (Task task : this.tasksReady){
			if (task.getId().equals(taskId)){
				return task;
			}
		}
		return null;
	}
	
	public TaskState getTaskState(String taskId) {
		for (Task task : this.tasksCompleted){
			if (task.getId().equals(taskId)){
				return TaskState.COMPLETED;
			}
		}
		for (Task task : this.tasksFailed){
			if (task.getId().equals(taskId)){
				return TaskState.FAILED;
			}
		}
		for (Task task : this.tasksRunning){
			if (task.getId().equals(taskId)){
				return TaskState.RUNNING;
			}
		}
		for (Task task : this.tasksReady){
			if (task.getId().equals(taskId)){
				return TaskState.READY;
			}
		}
		return null;
	}
	
	
	
	
}
