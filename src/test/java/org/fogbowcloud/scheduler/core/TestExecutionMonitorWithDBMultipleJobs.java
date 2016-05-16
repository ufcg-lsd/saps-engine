package org.fogbowcloud.scheduler.core;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.fogbowcloud.scheduler.core.model.JDFJob;
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

public class TestExecutionMonitorWithDBMultipleJobs {

	public Task task;
	public Task task2;
	public Scheduler scheduler;
	public JDFJob job;
	public JDFJob job2;
	public InfrastructureManager IM;
	public Resource resource;
	public Resource resource2;
	public String FAKE_TASK_ID = "FAKE_TASK_ID";
	public String FAKE_TASK_ID2 = "FAKE_TASK_ID2";
	public ImageDataStore imageStore;
	private CurrentThreadExecutorService executorService;
	
	private ConcurrentMap<String, JDFJob> db;	

	@Before
	public void setUp(){
		task = spy(new TaskImpl(FAKE_TASK_ID, null));
		task2 = spy(new TaskImpl(FAKE_TASK_ID2, null));
		IM = mock(InfrastructureManager.class);
		resource = mock(Resource.class);
		resource2 = mock(Resource.class);
		imageStore = mock(ImageDataStore.class);
		job = mock(JDFJob.class);
		job2 = mock(JDFJob.class);
		db = mock(ConcurrentMap.class);
		executorService = new CurrentThreadExecutorService();
		scheduler = spy(new Scheduler(IM, job, job2));
	}

	@Test
	public void testExecutionMonitorWithDB() throws InfrastructureException, InterruptedException{
		ExecutionMonitorWithDB ExecutionMonitorWithDB = new ExecutionMonitorWithDB(scheduler, executorService, db);
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
		List<Task> runningTasks2 = new ArrayList<Task>();
		runningTasks2.add(task2);
		doReturn(runningTasks2).when(job2).getByState(TaskState.RUNNING);
		doReturn(resource2).when(scheduler).getAssociateResource(task2);
		doReturn(true).when(resource2).checkConnectivity();
		doReturn(true).when(task2).isFinished();
		doReturn(false).when(task2).isFailed();
		doReturn(false).when(task2).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task2);
		doNothing().when(job2).finish(task2);
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		doReturn("FAKE_JOB_ID1").when(job).getId();
		doReturn("FAKE_JOB_ID2").when(job2).getId();
		jobList.add(job);
		jobList.add(job2);
		
		doReturn(jobList).when(scheduler).getJobs();
		doReturn(job).when(db).put(eq("FAKE_JOB_ID1"), eq(job));
		doReturn(job2).when(db).put(eq("FAKE_JOB_ID2"), eq(job2));
		
		ExecutionMonitorWithDB.run();
		Thread.sleep(500);
		verify(task, times(2)).isFinished();
		verify(job).finish(task);
		verify(task2, times(2)).isFinished();
		verify(job2).finish(task2);
	}

	@Test
	public void testExecutionMonitorWithDBTaskFails() throws InterruptedException{
		ExecutionMonitorWithDB ExecutionMonitorWithDB = new ExecutionMonitorWithDB(scheduler, executorService,db);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(true).when(task).isFinished();
		doReturn(true).when(task).isFailed();
		doReturn(false).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task);
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		List<Task> runningTasks2 = new ArrayList<Task>();
		runningTasks2.add(task2);
		doReturn(resource2).when(scheduler).getAssociateResource(task2);
		doReturn(true).when(resource2).checkConnectivity();
		doReturn(true).when(task2).isFinished();
		doReturn(true).when(task2).isFailed();
		doReturn(false).when(task2).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task2);
		doReturn(runningTasks2).when(job2).getByState(TaskState.RUNNING);
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		doReturn("FAKE_JOB_ID1").when(job).getId();
		doReturn("FAKE_JOB_ID2").when(job2).getId();
		jobList.add(job);
		jobList.add(job2);
		
		doReturn(jobList).when(scheduler).getJobs();
		doReturn(job).when(db).put(eq("FAKE_JOB_ID1"), eq(job));
		doReturn(job2).when(db).put(eq("FAKE_JOB_ID2"), eq(job2));
		

		ExecutionMonitorWithDB.run();
		Thread.sleep(500);
		verify(task, times(2)).isFailed();
		verify(job).fail(task);
		verify(task2, times(2)).isFailed();
		verify(job2).fail(task2);
	}

	@Test
	public void testConnectionFails() throws InfrastructureException, InterruptedException {
		ExecutionMonitorWithDB ExecutionMonitorWithDB = new ExecutionMonitorWithDB(scheduler, executorService, db);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(false).when(resource).checkConnectivity();
		doNothing().when(scheduler).taskFailed(task);
		doNothing().when(job).fail(task);
		List<Task> runningTasks2 = new ArrayList<Task>();
		runningTasks2.add(task2);
		doReturn(FAKE_TASK_ID2).when(task2).getId();
		doReturn(runningTasks2).when(job2).getByState(TaskState.RUNNING);
		doReturn(resource2).when(scheduler).getAssociateResource(task2);
		doReturn(false).when(resource2).checkConnectivity();
		doNothing().when(scheduler).taskFailed(task2);
		doNothing().when(job2).fail(task2);
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		doReturn("FAKE_JOB_ID1").when(job).getId();
		doReturn("FAKE_JOB_ID2").when(job2).getId();
		jobList.add(job);
		jobList.add(job2);
		
		doReturn(jobList).when(scheduler).getJobs();
		doReturn(job).when(db).put(eq("FAKE_JOB_ID1"), eq(job));
		doReturn(job2).when(db).put(eq("FAKE_JOB_ID2"), eq(job2));
		
		ExecutionMonitorWithDB.run();
		verify(job).fail(task);
		verify(scheduler).taskFailed(task);
		verify(job2).fail(task2);
		verify(scheduler).taskFailed(task2);
	}

	@Test
	public void testExecutionIsNotOver() throws InfrastructureException, InterruptedException{
		ExecutionMonitorWithDB ExecutionMonitorWithDB = new ExecutionMonitorWithDB(scheduler, executorService, db);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(false).when(task).isFinished();
		doReturn(false).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task);
		List<Task> runningTasks2 = new ArrayList<Task>();
		runningTasks.add(task2);
		doReturn(FAKE_TASK_ID2).when(task2).getId();
		doReturn(runningTasks2).when(job2).getByState(TaskState.RUNNING);
		doReturn(resource2).when(scheduler).getAssociateResource(task2);
		doReturn(true).when(resource2).checkConnectivity();
		doReturn(false).when(task2).isFinished();
		doReturn(false).when(task2).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task2);
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		doReturn("FAKE_JOB_ID1").when(job).getId();
		doReturn("FAKE_JOB_ID2").when(job2).getId();
		jobList.add(job);
		jobList.add(job2);
		
		doReturn(jobList).when(scheduler).getJobs();
		doReturn(job).when(db).put(eq("FAKE_JOB_ID1"), eq(job));
		doReturn(job2).when(db).put(eq("FAKE_JOB_ID2"), eq(job2));
		
		ExecutionMonitorWithDB.run();
		verify(task, times(2)).isFinished();
		verify(job, never()).finish(task);;
		verify(scheduler).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(task2, times(2)).isFinished();
		verify(job2, never()).finish(task2);;
		verify(scheduler).getAssociateResource(task2);
		verify(scheduler, never()).taskCompleted(task2);
	}

	@Test
	public void testExecutionTimedOut() throws InfrastructureException, InterruptedException{
		ExecutionMonitorWithDB ExecutionMonitorWithDB = new ExecutionMonitorWithDB(scheduler, executorService, db);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(false).when(task).isFinished();
		doReturn(true).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskFailed(task);
		List<Task> runningTasks2 = new ArrayList<Task>();
		runningTasks2.add(task2);
		doReturn(FAKE_TASK_ID2).when(task2).getId();
		doReturn(runningTasks2).when(job2).getByState(TaskState.RUNNING);
		doReturn(true).when(resource2).checkConnectivity();
		doReturn(false).when(task2).isFinished();
		doReturn(true).when(task2).checkTimeOuted();
		doNothing().when(scheduler).taskFailed(task2);
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		doReturn("FAKE_JOB_ID1").when(job).getId();
		doReturn("FAKE_JOB_ID2").when(job2).getId();
		jobList.add(job);
		jobList.add(job2);
		
		doReturn(jobList).when(scheduler).getJobs();
		doReturn(job).when(db).put(eq("FAKE_JOB_ID1"), eq(job));
		doReturn(job2).when(db).put(eq("FAKE_JOB_ID2"), eq(job2));
		
		ExecutionMonitorWithDB.run();
		verify(task).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, never()).taskCompleted(task);
		verify(job).fail(task);
		verify(scheduler).taskFailed(task);
		verify(task2).checkTimeOuted();
		verify(job2, never()).finish(task2);;
		verify(scheduler, never()).taskCompleted(task2);
		verify(job2).fail(task2);
		verify(scheduler).taskFailed(task2);
	}

	@Test
	public void testTaskRetry() throws InfrastructureException, InterruptedException{
		ExecutionMonitorWithDB ExecutionMonitorWithDB = new ExecutionMonitorWithDB(scheduler, executorService, db);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(FAKE_TASK_ID).when(task).getId();
		task.putMetadata(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES, "5");
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(false).when(resource).checkConnectivity();
		doReturn(false).when(task).isFinished();
		doReturn(false).when(task).checkTimeOuted();
		List<Task> runningTasks2 = new ArrayList<Task>();
		runningTasks2.add(task2);
		doReturn(FAKE_TASK_ID2).when(task2).getId();
		task2.putMetadata(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES, "5");
		doReturn(runningTasks2).when(job2).getByState(TaskState.RUNNING);
		doReturn(resource2).when(scheduler).getAssociateResource(task2);
		doReturn(false).when(resource2).checkConnectivity();
		doReturn(false).when(task2).isFinished();
		doReturn(false).when(task2).checkTimeOuted();

		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		doReturn("FAKE_JOB_ID1").when(job).getId();
		doReturn("FAKE_JOB_ID2").when(job2).getId();
		jobList.add(job);
		jobList.add(job2);
		
		doReturn(jobList).when(scheduler).getJobs();
		doReturn(job).when(db).put(eq("FAKE_JOB_ID1"), eq(job));
		doReturn(job2).when(db).put(eq("FAKE_JOB_ID2"), eq(job2));
		//first retry
		ExecutionMonitorWithDB.run();
		verify(task).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource).checkConnectivity();
		Assert.assertEquals(1, task.getRetries());
		verify(task2).checkTimeOuted();
		verify(job2, never()).finish(task2);;
		verify(scheduler).getAssociateResource(task2);
		verify(scheduler, never()).taskCompleted(task2);
		verify(resource2).checkConnectivity();
		Assert.assertEquals(1, task2.getRetries());

		//second retry
		ExecutionMonitorWithDB.run();
		verify(task, times(2)).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, times(2)).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource, times(2)).checkConnectivity();
		Assert.assertEquals(2, task2.getRetries());
		verify(task2, times(2)).checkTimeOuted();
		verify(job2, never()).finish(task2);;
		verify(scheduler, times(2)).getAssociateResource(task2);
		verify(scheduler, never()).taskCompleted(task2);
		verify(resource2, times(2)).checkConnectivity();
		Assert.assertEquals(2, task2.getRetries());

		//third retry
		ExecutionMonitorWithDB.run();
		verify(task, times(3)).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, times(3)).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource, times(3)).checkConnectivity();
		Assert.assertEquals(3, task2.getRetries());
		verify(task2, times(3)).checkTimeOuted();
		verify(job2, never()).finish(task2);;
		verify(scheduler, times(3)).getAssociateResource(task2);
		verify(scheduler, never()).taskCompleted(task2);
		verify(resource2, times(3)).checkConnectivity();
		Assert.assertEquals(3, task2.getRetries());

		//setting retry to 0 again
		doReturn(true).when(resource).checkConnectivity();
		doReturn(true).when(resource2).checkConnectivity();
		ExecutionMonitorWithDB.run();
		verify(task, times(4)).checkTimeOuted();
		verify(job, never()).finish(task);;
		verify(scheduler, times(4)).getAssociateResource(task);
		verify(scheduler, never()).taskCompleted(task);
		verify(resource, times(4)).checkConnectivity();
		Assert.assertEquals(0, task.getRetries());
		verify(task2, times(4)).checkTimeOuted();
		verify(job2, never()).finish(task2);;
		verify(scheduler, times(4)).getAssociateResource(task2);
		verify(scheduler, never()).taskCompleted(task2);
		verify(resource2, times(4)).checkConnectivity();
		Assert.assertEquals(0, task2.getRetries());
	}
	
	@Test
	public void testOneFailsOtherCompletes() throws InterruptedException{
		ExecutionMonitorWithDB ExecutionMonitorWithDB = new ExecutionMonitorWithDB(scheduler, executorService, db);
		List<Task> runningTasks = new ArrayList<Task>();
		runningTasks.add(task);
		doReturn(resource).when(scheduler).getAssociateResource(task);
		doReturn(true).when(resource).checkConnectivity();
		doReturn(true).when(task).isFinished();
		doReturn(true).when(task).isFailed();
		doReturn(false).when(task).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task);
		doReturn(runningTasks).when(job).getByState(TaskState.RUNNING);
		
		List<Task> runningTasks2 = new ArrayList<Task>();
		runningTasks2.add(task2);
		doReturn(runningTasks2).when(job2).getByState(TaskState.RUNNING);
		doReturn(resource2).when(scheduler).getAssociateResource(task2);
		doReturn(true).when(resource2).checkConnectivity();
		doReturn(true).when(task2).isFinished();
		doReturn(false).when(task2).isFailed();
		doReturn(false).when(task2).checkTimeOuted();
		doNothing().when(scheduler).taskCompleted(task2);
		doNothing().when(job2).finish(task2);
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		doReturn("FAKE_JOB_ID1").when(job).getId();
		doReturn("FAKE_JOB_ID2").when(job2).getId();
		jobList.add(job);
		jobList.add(job2);
		
		doReturn(jobList).when(scheduler).getJobs();
		doReturn(job).when(db).put(eq("FAKE_JOB_ID1"), eq(job));
		doReturn(job2).when(db).put(eq("FAKE_JOB_ID2"), eq(job2));
		
		ExecutionMonitorWithDB.run();
		Thread.sleep(500);
		verify(task, times(2)).isFailed();
		verify(job).fail(task);
		verify(task2, times(2)).isFinished();
		verify(job2).finish(task2);
	}

}
