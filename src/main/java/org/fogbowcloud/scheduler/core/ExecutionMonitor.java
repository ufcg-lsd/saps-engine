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

	private Job job;
	private Scheduler scheduler;
	private static final Logger LOGGER = Logger.getLogger(ExecutionMonitor.class);
	private final int TEST_SSH_TIMEOUT = 10000;

	public ExecutionMonitor(Job job, Scheduler scheduler) {
		// TODO Auto-generated constructor stub
		this.job = job;
		this.scheduler = scheduler;
	}

	@Override
	public void run() {
		
		ExecutorService service = Executors.newFixedThreadPool(3);
		for (Task task : job.getByState(TaskState.RUNNING)) {
			class TaskExecutionTest implements Runnable {

				protected Task task; 
				protected Scheduler scheduler;
				protected Job job;

				public TaskExecutionTest(Task task, Scheduler scheduler, Job job){
					this.task = task;
					this.scheduler = scheduler;
					this.job = job;
				}

				@Override
				public void run() {
					try {
						if (!testSshConnection(task)){
							job.fail(task);
							scheduler.taskFailed(task);
						}
					} catch (InfrastructureException e) {
						e.printStackTrace();
					}
					/*    
					 * if (is task completed)
					 * 	   job.move(TaskState.RUNNING, TaskState.COMPLETED, task.getId());
					 *     scheduler.taskCompleted(task)
					 */
					if (task.isFinished()){
						job.finish(task);
						scheduler.taskCompleted(task);
					}

				}

				public boolean testSshConnection(Task task) throws InfrastructureException{
					Resource resource = scheduler.getAssociateResource(task);
					return resource.testSSHConnection();
				}
			}
			service.submit(new TaskExecutionTest(task, this.scheduler, this.job));
		}
	}


}
