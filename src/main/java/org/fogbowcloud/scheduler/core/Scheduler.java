package org.fogbowcloud.scheduler.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;

public class Scheduler implements Runnable{
	
	private Job job;
	private InfrastructureManager infraManager;
	private Map<String, Resource> runningTasks = new HashMap<String, Resource>();
	private ExecutorService taskExecutor =  Executors.newCachedThreadPool();
	
	private static final Logger LOGGER = Logger.getLogger(Scheduler.class);
	
	public Scheduler(Job job, InfrastructureManager infraManager) {
		this.job = job;
		this.infraManager = infraManager;
	}
	
	protected Scheduler(Job job, InfrastructureManager infraManager, ExecutorService taskExecutor) {
		this(job, infraManager);
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void run() {
		for (Task task : job.getByState(TaskState.READY)) {
			infraManager.orderResource(task.getSpecification(), this);
		}
	}
	
	public void resourceReady(final Resource resource) {
		
		LOGGER.debug("Receiving resource ready [ID:"+resource.getId()+"]");
		
		for (final Task task : job.getByState(TaskState.READY)) {
			if(resource.match(task.getSpecification())){
				
				LOGGER.debug("Relating resource [ID:"+resource.getId()+"] with task [ID:"+task.getId()+"]");
				taskExecutor.submit(new Runnable() {

					@Override
					public void run() {
						job.run(task);
						resource.executeTask(task);
						runningTasks.put(task.getId(), resource);
					}
				});
				return;
			}
		}
		
		infraManager.releaseResource(resource);
	}

	public void taskFailed(Task task) {
		Task newTask = task.clone();
		job.addTask(newTask);
		infraManager.releaseResource(runningTasks.get(task.getId()));
		runningTasks.remove(task.getId());
		
	}

	public void taskCompleted(Task task) {
		infraManager.releaseResource(runningTasks.get(task.getId()));
		runningTasks.remove(task.getId());
	}

	public Resource getAssociateResource(
			Task task) {
		return runningTasks.get(task.getId());
	}
	
	protected Map<String, Resource> getRunningTasks(){
		return runningTasks;
	}
}
