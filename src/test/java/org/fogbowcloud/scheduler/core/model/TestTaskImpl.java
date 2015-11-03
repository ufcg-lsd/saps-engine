package org.fogbowcloud.scheduler.core.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Test;

public class TestTaskImpl {
	
	private static final String TIME_OUT_VALUE_SMALL = "1";
	private static final String TIME_OUT_VALUE_BIG = "50000000000";
	String taskId;
	Specification spec;
	Task task;
	
	
	@Before
	public void setUp(){
		spec = mock(Specification.class);
		taskId = "taskId";
		task = spy(new TaskImpl(taskId, spec));
	}
	
	@Test
	public void testCheckTimeOutedNotTimeOuted(){
		task.startedRunning();
		
		doReturn(TIME_OUT_VALUE_BIG).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedTimeOuted() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn(TIME_OUT_VALUE_SMALL).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(true, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedBadlyFormated() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn("fbajsmnfsakl").when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedNullTimeOut() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn(null).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedEmptyTimeOut() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn("").when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}

}
