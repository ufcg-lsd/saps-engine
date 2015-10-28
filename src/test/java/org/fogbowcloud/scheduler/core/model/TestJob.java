package org.fogbowcloud.scheduler.core.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.junit.Test;

public class TestJob {

	@Test
	public void testAddRecoveredTask(){
		Job job = new FakeJob();
		
		Task t = mock(Task.class);
		doReturn("task1").when(t).getId();
		
		Task t2 = mock(Task.class);
		doReturn("task2").when(t2).getId();
		
		Task trecovered = mock(Task.class);		
		doReturn("taskRecovered").when(trecovered).getId();
		doReturn(trecovered).when(trecovered).clone();
		
		job.addTask(t);
		job.addTask(t2);
		
		assertEquals("task1", job.getByState(TaskState.READY).get(0).getId());
		
		job.recoverTask(trecovered);
		
		assertEquals("taskRecovered", job.getByState(TaskState.READY).get(0).getId());
		assertEquals("task1", job.getByState(TaskState.READY).get(1).getId());
	}
	
	@Test
	public void testAddTwoRecoveredTask(){
		Job job = new FakeJob();
		
		Task t = mock(Task.class);
		doReturn("task1").when(t).getId();
		
		Task t2 = mock(Task.class);
		doReturn("task2").when(t2).getId();
		
		Task trecovered = mock(Task.class);		
		doReturn("taskRecovered").when(trecovered).getId();
		doReturn(trecovered).when(trecovered).clone();
		
		Task trecovered2 = mock(Task.class);		
		doReturn("taskRecovered2").when(trecovered2).getId();
		doReturn(trecovered2).when(trecovered2).clone();
		
		job.addTask(t);
		job.addTask(t2);
		
		assertEquals("task1", job.getByState(TaskState.READY).get(0).getId());
		assertEquals("task2", job.getByState(TaskState.READY).get(1).getId());
		
		job.recoverTask(trecovered);
		job.recoverTask(trecovered2);
		
		assertEquals("taskRecovered2", job.getByState(TaskState.READY).get(0).getId());
		assertEquals("taskRecovered", job.getByState(TaskState.READY).get(1).getId());
	}
}
