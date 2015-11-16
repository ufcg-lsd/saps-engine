package org.fogbowcloud.scheduler.core;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.sebal.ImageDataStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.verification.VerificationMode;

public class TestExecutionMonitor {

	public Task task;
	public Scheduler scheduler;
	public Job job;
	public InfrastructureManager IM;
	public Resource resource;
	public String FAKE_TASK_ID = "FAKE_TASK_ID";
	public ImageDataStore imageStore;
	private CurrentThreadExecutorService executorService;
	
	@Before
	public void setUp(){
		task = spy(new TaskImpl(FAKE_TASK_ID, null));
		IM = mock(InfrastructureManager.class);
		resource = mock(Resource.class);
		imageStore = mock(ImageDataStore.class);
		job = mock(Job.class);
		executorService = new CurrentThreadExecutorService();
		scheduler = spy(new Scheduler(IM, job));
	}
	
	@Test
	public void testExecutionMonitor() throws InfrastructureException, InterruptedException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler,executorService);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(true).when(task).isFinished();
		doReturn(false).when(task).isFailed();
		doReturn(false).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task);
		doNothing().when(job).finish(task);
		executionMonitor.run();
		Thread.sleep(500);
		verify(task, times(2)).isFinished();
		verify(job).finish(task);
	}
	
	@Test
	public void testExecutionMonitorTaskFails() throws InterruptedException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler,executorService);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(true).when(task).isFinished();
		doReturn(true).when(task).isFailed();
		doReturn(false).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task);
		doNothing().when(job).finish(task);
		executionMonitor.run();
		Thread.sleep(500);
		verify(task, times(2)).isFailed();
		verify(job).fail(task);
	}
	
	@Test
	public void testConnectionFails() throws InfrastructureException, InterruptedException {
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler,executorService);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(false).when(resource).checkConnectivity();
		doNothing().when(scheduler).taskFailed(task);
		doNothing().when(job).fail(task);
		executionMonitor.run();
		verify(job).fail(task);
		verify(scheduler).taskFailed(task);
	}
	
	@Test
	public void testExecutionIsNotOver() throws InfrastructureException, InterruptedException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler,executorService);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(false).when(task).isFinished();
		doReturn(false).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task);
		executionMonitor.run();
		verify(task, times(2)).isFinished();
		verify(job, never()).finish(task);;
		verify(scheduler).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
	}
	
	@Test
	public void testExecutionTimedOut() throws InfrastructureException, InterruptedException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler,executorService);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(false).when(task).isFinished();
		doReturn(true).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskFailed(task);
		executionMonitor.run();
		verify(task).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, never()).taskCompleted(task);
		verify(job).fail(task);
		verify(scheduler).taskFailed(task);
	}

	@Test
	public void testTaskRetry() throws InfrastructureException, InterruptedException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler,executorService);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		task.putMetadata(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES, "5");
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(false).when(resource).checkConnectivity();
		doReturn(false).when(task).isFinished();
		doReturn(false).when(task).checkTimeOuted();
		
		//first retry
		executionMonitor.run();
		verify(task).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource).checkConnectivity();
		Assert.assertEquals(1, task.getRetries());
		
		//second retry
		executionMonitor.run();
		verify(task, times(2)).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, times(2)).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource, times(2)).checkConnectivity();
		Assert.assertEquals(2, task.getRetries());
		
		//third retry
		executionMonitor.run();
		verify(task, times(3)).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, times(3)).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource, times(3)).checkConnectivity();
		Assert.assertEquals(3, task.getRetries());
		
		//setting retry to 0 again
		doReturn(true).when(resource).checkConnectivity();
		executionMonitor.run();
		verify(task, times(4)).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, times(4)).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource, times(4)).checkConnectivity();
		Assert.assertEquals(0, task.getRetries());
	}
	
}
