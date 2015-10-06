package org.fogbowcloud.scheduler.core;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.SebalJob;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.sebal.ImageDataStore;
import org.junit.Before;
import org.junit.Test;

public class TestExecutionMonitor {

	public Task task;
	public Scheduler scheduler;
	public Job job;
	public InfrastructureManager IM;
	public Resource resource;
	public String FAKE_TASK_ID = "FAKE_TASK_ID";
	public ImageDataStore imageStore;
	
	@Before
	public void setUp(){
		task = spy(new TaskImpl(FAKE_TASK_ID, null));
		IM = mock(InfrastructureManager.class);
		resource = mock(Resource.class);
		imageStore = mock(ImageDataStore.class);
		job = spy(new SebalJob(imageStore));
		
		scheduler = spy(new Scheduler(job, IM));
	}
	
	@Test
	public void testExecutionMonitor() throws InfrastructureException, InterruptedException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(true).when(task).isFinished();
		doNothing().when(scheduler).taskCompleted(task);
		executionMonitor.run();
		Thread.sleep(500);
		verify(task).isFinished();
		verify(job).finish(task);
	}
	
	@Test
	public void testConnectionFails() throws InfrastructureException, InterruptedException {
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(false).when(resource).checkConnectivity();
		doNothing().when(scheduler).taskFailed(task);
		executionMonitor.run();
		Thread.sleep(500);
		verify(job).fail(task);
		verify(scheduler).taskFailed(task);
	}
	
	@Test
	public void testExecutionIsNotOver() throws InfrastructureException, InterruptedException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(false).when(task).isFinished();
		doNothing().when(scheduler).taskCompleted(task);
		executionMonitor.run();
		Thread.sleep(500);
		verify(task).isFinished();
		verify(job, never()).finish(task);;
		verify(scheduler).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
	}
}
