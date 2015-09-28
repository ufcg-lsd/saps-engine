package org.fogbowcloud.scheduler.core;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;

public class Scheduler implements Runnable{
	
	private Job job;
	private InfrastructureManager infraManager;
	private Map<String, Resource> runningTasks = new HashMap<String, Resource>();
	
	public Scheduler(Job job, InfrastructureManager infraManager) {
		this.job = job;
		this.infraManager = infraManager;
	}

	@Override
	public void run() {
		for (Task task : job.getByState(TaskState.READY)) {
			infraManager.orderResource(task.getSpecification(), this);
		}
	}
	
	public void resourceReady(Resource resource) {
		/*
		 * Looking for task compliant with resource
		 * Relate task -> resource
		 * runningTask.put(taskId, resource)
		 */
		
	}

	public void taskFailed(Task task) {
		Task newTask = task.clone();
		job.addTask(newTask);
		
		// TODO Should we try reuse the same resource to new task? 
		infraManager.releaseResource(runningTasks.get(task.getId()));
		runningTasks.remove(task.getId());
		
		//TODO order new resource?!
	}

	public void taskCompleted(Task task) {
		infraManager.releaseResource(runningTasks.get(task.getId()));
		runningTasks.remove(task.getId());
	}

	public Resource getAssociateResource(
			Task task) {
		return runningTasks.get(task.getId());
	}
}
