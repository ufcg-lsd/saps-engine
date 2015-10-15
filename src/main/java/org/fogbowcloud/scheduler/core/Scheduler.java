package org.fogbowcloud.scheduler.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;

public class Scheduler implements Runnable{
	
	private final String id;
	private Job job;
	private InfrastructureManager infraManager;
	private Map<String, Resource> runningTasks = new HashMap<String, Resource>();
	private ExecutorService taskExecutor =  Executors.newCachedThreadPool();
	
	private static final Logger LOGGER = Logger.getLogger(Scheduler.class);
	
	public Scheduler(Job job, InfrastructureManager infraManager) {
		this.job = job;
		this.infraManager = infraManager;
		this.id = UUID.randomUUID().toString();
	}
	
	protected Scheduler(Job job, InfrastructureManager infraManager, ExecutorService taskExecutor) {
		this(job, infraManager);
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void run() {
		LOGGER.info("Running scheduler...");
		Map<Specification, Integer> specDemand = new HashMap<Specification, Integer>();		

		List<Task> readyTasks = job.getByState(TaskState.READY);
		
		LOGGER.debug("There are " + readyTasks.size() + " ready tasks.");
		LOGGER.debug("Scheduler running tasks is " + runningTasks.size()
				+ ", and job running tasks is " + job.getByState(TaskState.RUNNING));
		
		for (Task task : readyTasks) {
			Specification taskSpec = task.getSpecification();
			if (!specDemand.containsKey(taskSpec)) {
				specDemand.put(taskSpec, 0);
			}
			int currentDemand = specDemand.get(taskSpec); 
			specDemand.put(taskSpec, ++currentDemand);
		}
		
		LOGGER.debug("Current job demand is " + specDemand);
		for (Specification spec : specDemand.keySet()) {			
			infraManager.orderResource(spec, this, specDemand.get(spec));
		}
	}
	
	public void resourceReady(final Resource resource) {
		LOGGER.debug("Receiving resource ready [ID:"+resource.getId()+"]");
		
		for (final Task task : job.getByState(TaskState.READY)) {
			if(resource.match(task.getSpecification())){
				
				LOGGER.debug("Relating resource [ID:"+resource.getId()+"] with task [ID:"+task.getId()+"]");
				job.run(task);
				runningTasks.put(task.getId(), resource);

				taskExecutor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							resource.executeTask(task);
						} catch (Throwable e) {
							LOGGER.error("Error while executing task.", e);
						}
					}
				});
				return;
			}
		}
		
		infraManager.releaseResource(resource);
	}

	public void taskFailed(Task task) {
		LOGGER.debug("============================================================");
		LOGGER.debug("==  Task " + task.getId() + " failed and will be cloned.  ==");
		LOGGER.debug("============================================================");
		Task newTask = task.clone();
		job.addTask(newTask);
		infraManager.releaseResource(runningTasks.get(task.getId()));
		runningTasks.remove(task.getId());
		
	}

	public void taskCompleted(Task task) {
		LOGGER.info("Task " + task.getId() + " was completed.");
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

	protected String getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Scheduler other = (Scheduler) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
}
