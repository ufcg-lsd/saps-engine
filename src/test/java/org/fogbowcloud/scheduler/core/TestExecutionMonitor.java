package org.fogbowcloud.scheduler.core;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestExecutionMonitor {

	public Task task;
	public Scheduler scheduler;
	public Job job;
	public InfrastructureManager IM;
	public Resource resource;
	public String FAKE_TASK_ID = "FAKE_TEST_ID";
	
	@Before
	public void setUp(){
		task = spy(new Task());
		IM = mock(InfrastructureManager.class);
		resource = mock(Resource.class);
		job = spy(new Job());
		
		scheduler = spy(new Scheduler(job, IM));
	}
	
	@Test
	public void testExecutionMonitor() throws InfrastructureException{
		ExecutionMonitor executionMonitor = new ExecutionMonitor(job, scheduler);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).testSSHConnection();
		doReturn(true).when(task).isFinished();
		doNothing().when(IM).releaseResource(Mockito.any(Resource.class));	
		executionMonitor.run();
		verify(task).isFinished();
		verify(scheduler.getAssociateResource(task));
	}
}
