package org.fogbowcloud.scheduler.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;

public class ExecutionMonitor implements Runnable {

	private Job[] job;
	private Scheduler scheduler;
	private static final Logger LOGGER = Logger.getLogger(ExecutionMonitor.class);
	private ExecutorService service;

	public ExecutionMonitor(Scheduler scheduler, Job... job) {
		this(scheduler, Executors.newFixedThreadPool(3), job);
	}

	public ExecutionMonitor(Scheduler scheduler, ExecutorService service, Job... job) {
		this.job = job;
		this.scheduler = scheduler;
		if(service == null){
			this.service = Executors.newFixedThreadPool(3);
		}else{
			this.service = service;
		}
	}

	@Override
	public void run() {		
		for (Job aJob : job){
			for (Task task : aJob.getByState(TaskState.RUNNING)) {
				service.submit(new TaskExecutionChecker(task, this.scheduler, aJob));
			}
		}
	}

	class TaskExecutionChecker implements Runnable {

		protected Task task; 
		protected Scheduler scheduler;
		protected Job job;

		public TaskExecutionChecker(Task task, Scheduler scheduler, Job job){
			this.task = task;
			this.scheduler = scheduler;
			this.job = job;
		}

		@Override
		public void run() {
			LOGGER.info("Monitoring task " + task.getId() + ", failed=" + task.isFailed()
			+ ", completed=" + task.isFinished());

			if (task.checkTimeOuted()){
				job.fail(task);
				scheduler.taskFailed(task);
				LOGGER.error("Task "+ task.getId() + " timed out");
				return;
			}

			if (task.isFailed()) {
				LOGGER.info("Failing task " + task.getId());
				job.fail(task);
				scheduler.taskFailed(task);
				return;
			}

			if (task.isFinished()){
				LOGGER.info("Completing task " + task.getId());
				job.finish(task);
				scheduler.taskCompleted(task);
				return;
			}

			try {
				if (!checkResourceConnectivity(task)){
					if (!task.mayRetry()) {
						job.fail(task);
						scheduler.taskFailed(task);
					} else {
						task.setRetries(task.getRetries() + 1);
					}
				} else {
					task.setRetries(0);
				}
			} catch (InfrastructureException e) {
				LOGGER.error("Error while checking connectivity.", e);
			}
		}

		private boolean checkResourceConnectivity(Task task) throws InfrastructureException{
			Resource resource = scheduler.getAssociateResource(task);
			return resource.checkConnectivity();
		}
	}
	
}
