package org.fogbowcloud.scheduler.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class TestScheduler {

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	private Scheduler scheduler;
	private Job jobMock;
	private InfrastructureManager infraManagerMock;
	private CurrentThreadExecutorService executorService;
	
	@Before
	public void setUp() throws Exception {
		
		executorService = new CurrentThreadExecutorService();
		jobMock = mock(Job.class);
		infraManagerMock = mock(InfrastructureManager.class);
		scheduler = spy(new Scheduler(jobMock, infraManagerMock, executorService));
		
	}    

	@After
	public void setDown() throws Exception {
		
		jobMock = null;
		infraManagerMock = null;
		scheduler = null;
		
	}
	
	@Test
	public void runTest(){
		
		int qty = 5;
		
		Specification spec = new Specification("image", "username", "publicKey", "privateKeyFilePath");
		List<Task> tasks = this.generateMockTasks(qty,spec);
		
		doReturn(tasks).when(jobMock).getByState(TaskState.READY);
		scheduler.run();
		verify(infraManagerMock, times(qty)).orderResource(spec, scheduler);
	}
	
	@Test
	public void resourceReadyWithMatchTask(){
		
		int qty = 3;
		
		Resource resourceMock = mock(Resource.class);
		
		Specification specA = new Specification("image", "username", "publicKey", "privateKeyFilePath");
		Specification specB = new Specification("image", "username", "publicKey", "privateKeyFilePath");
		List<Task> tasks = this.generateMockTasks(qty,specA);
		
		Task tMatch = tasks.get(1);
		
		doReturn(tasks).when(jobMock).getByState(TaskState.READY);
		doReturn(specB).when(tMatch).getSpecification();
		doReturn("resource01").when(resourceMock).getId();
		doReturn(true).when(resourceMock).match(specB);
		
		scheduler.resourceReady(resourceMock);
		
		verify(resourceMock, times(1)).executeTask(tMatch);
		verify(jobMock, times(1)).run(tMatch);
		
		assertEquals(resourceMock.getId(), scheduler.getAssociateResource(tMatch).getId());
		
	}
	
	@Test
	public void resourceReadyWithoutMatchTask(){
		
		int qty = 3;
		
		Resource resourceMock = mock(Resource.class);
		
		Specification specA = new Specification("image", "username", "publicKey", "privateKeyFilePath");
		Specification specB = new Specification("image", "username", "publicKey", "privateKeyFilePath");
		List<Task> tasks = this.generateMockTasks(qty,specA);
		
		doReturn(tasks).when(jobMock).getByState(TaskState.READY);
		doReturn("resource01").when(resourceMock).getId();
		doReturn(true).when(resourceMock).match(specB);
		
		scheduler.resourceReady(resourceMock);
		
		verify(resourceMock, times(0)).executeTask(Mockito.any(Task.class));
		verify(jobMock, times(0)).run(Mockito.any(Task.class));
		verify(infraManagerMock, times(1)).releaseResource(resourceMock);
		
	}
	
	@Test
	public void taskFailed() {
		
		Task t = mock(Task.class);
		doReturn(String.valueOf("task1")).when(t).getId();
		
		Task tClone = mock(Task.class);
		doReturn(String.valueOf("task1_c")).when(t).getId();
		
		doReturn(tClone).when(t).clone();
		
		Resource resourceMock = mock(Resource.class);
		scheduler.getRunningTasks().put(t.getId(), resourceMock);
		
		scheduler.taskFailed(t);
		
		verify(jobMock, times(1)).addTask(tClone);
		verify(infraManagerMock, times(1)).releaseResource(resourceMock);
		
		assertNull(scheduler.getRunningTasks().get(t.getId()));
	}

	@Test
	public void taskCompleted() {
		
		Task t = mock(Task.class);
		doReturn(String.valueOf("task1")).when(t).getId();
		
		Resource resourceMock = mock(Resource.class);
		scheduler.getRunningTasks().put(t.getId(), resourceMock);
		
		scheduler.taskCompleted(t);
		
		verify(infraManagerMock, times(1)).releaseResource(resourceMock);
		
		assertNull(scheduler.getRunningTasks().get(t.getId()));
	}
	
	private List<Task> generateMockTasks(int qty, Specification spec){
		
		List<Task> tasks = new ArrayList<Task>();
		for(int count = 1; count <= qty; count++ ){
			Task t = mock(Task.class);
			doReturn("Task_0"+String.valueOf(count)).when(t).getId();
			doReturn(spec).when(t).getSpecification();
			tasks.add(t);
		}
		
		return tasks;
		
	}
}
