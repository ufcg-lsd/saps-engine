package org.fogbowcloud.scheduler.core;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;

public class ExecutionMonitor implements Runnable {

	private Job job;
	private Scheduler scheduler;
	
	public ExecutionMonitor(Job job, Scheduler scheduler) {
		// TODO Auto-generated constructor stub
		this.job = job;
		this.scheduler = scheduler;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		for (Task task : job.getByState(TaskState.RUNNING)) {
			
			/*
			 * monitor execution
			 * 
			 * 
			 * if (task fails)
			 *    job.move(TaskState.RUNNING, TaskState.FAILED, task.getId());
			 *    scheduler.taskFailed(task)
			 */
			
			scheduler.taskFailed(task);
			 /*    
			 * if (is task completed)
			 * 	   job.move(TaskState.RUNNING, TaskState.COMPLETED, task.getId());
			 *     scheduler.taskCompleted(task)
			 */
			
			scheduler.taskCompleted(task);
		}
	}

}
