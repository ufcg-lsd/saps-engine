package org.fogbowcloud.sebal.engine.scheduler.monitor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.infrastructure.model.ResourceState;
import org.fogbowcloud.blowout.pool.AbstractResource;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class TestSebalTaskMonitor {
	
	private static final String FAKE_ID = "fakeId";
	
	public static long timeout;
	public ImageDataStore imageStore;
	public SebalTaskMonitor sebalTaskMonitor;
	public BlowoutPool pool;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		timeout = 10000;
		pool = mock(BlowoutPool.class);
		imageStore = mock(ImageDataStore.class);
		sebalTaskMonitor = spy(new SebalTaskMonitor(pool, imageStore));
	}
	
	@Test
	public void testProcMonImageToRunning() {
		TaskProcess fakeProcess = mock(TaskProcess.class);
		
		doReturn(TaskState.RUNNING).when(fakeProcess).getStatus();
		
		AbstractResource fakeResource = mock(AbstractResource.class);
		List<TaskProcess> runningProcesses = new ArrayList<TaskProcess>();
		runningProcesses.add(fakeProcess);
		
		doReturn(runningProcesses).when(this.sebalTaskMonitor).getRunningProcesses();
		doNothing().when(sebalTaskMonitor).imageToRunning(fakeProcess);
		
		this.sebalTaskMonitor.procMon();
		verify(pool, never()).updateResource(fakeResource, ResourceState.FAILED);
		verify(pool, never()).updateResource(fakeResource, ResourceState.IDLE);
	}
	
	@Test
	public void testProcMonImageToFailed() throws SQLException {				
		Task fakeTask = mock(Task.class);
		TaskProcess fakeProcess = mock(TaskProcess.class);
		
		doReturn(TaskState.FAILED).when(fakeProcess).getStatus();
		
		AbstractResource fakeResource = mock(AbstractResource.class);
		doReturn(FAKE_ID).when(fakeTask).getId();
		doReturn(FAKE_ID).when(fakeProcess).getTaskId();
		doReturn(fakeTask).when(this.sebalTaskMonitor).getTaskById(FAKE_ID);
		doReturn(fakeResource).when(fakeProcess).getResource();
		
		List<TaskProcess> runningProcesses = new ArrayList<TaskProcess>();
		runningProcesses.add(fakeProcess);
		doReturn(runningProcesses).when(this.sebalTaskMonitor).getRunningProcesses();
		
		Map<Task, TaskProcess> runningTasks = new HashMap<Task, TaskProcess>();
		runningTasks.put(fakeTask, fakeProcess);
		doReturn(runningTasks).when(this.sebalTaskMonitor).getRunningTasks();
		
		doNothing().when(sebalTaskMonitor).updateImageToQueued(fakeProcess);

		this.sebalTaskMonitor.procMon();
		
		verify(pool).updateResource(fakeResource, ResourceState.FAILED);
		verify(pool, never()).updateResource(fakeResource, ResourceState.IDLE);
	}
	
	@Test
	public void testProcMonProcFinnished() throws SQLException {
		Task fakeTask = mock(Task.class);
		TaskProcess fakeProcess = mock(TaskProcess.class);
		
		doReturn(TaskState.FINNISHED).when(fakeProcess).getStatus();
		
		AbstractResource fakeResource = mock(AbstractResource.class);
		doReturn(FAKE_ID).when(fakeTask).getId();
		doReturn(FAKE_ID).when(fakeProcess).getTaskId();
		doReturn(fakeTask).when(this.sebalTaskMonitor).getTaskById(FAKE_ID);
		doReturn(fakeResource).when(fakeProcess).getResource();
		
		List<TaskProcess> runningProcesses = new ArrayList<TaskProcess>();
		runningProcesses.add(fakeProcess);
		doReturn(runningProcesses).when(this.sebalTaskMonitor).getRunningProcesses();
		
		Map<Task, TaskProcess> runningTasks = new HashMap<Task, TaskProcess>();
		runningTasks.put(fakeTask, fakeProcess);
		doReturn(runningTasks).when(this.sebalTaskMonitor).getRunningTasks();
		
		doNothing().when(sebalTaskMonitor).updateImageToFinished(fakeProcess);
		
		this.sebalTaskMonitor.procMon();
		
		verify(pool, never()).updateResource(fakeResource, ResourceState.FAILED);
		verify(pool ).updateResource(fakeResource, ResourceState.IDLE);
	}
	
	@Test
	public void testRunTask() {
		Task fakeTask = mock(Task.class);
		TaskProcess fakeProcess = mock(TaskProcess.class);
		AbstractResource fakeResource = mock(AbstractResource.class);
		doReturn(FAKE_ID).when(fakeTask).getId();
		doReturn(FAKE_ID).when(fakeProcess).getTaskId();
		
		List<TaskProcess> runningPrc = new ArrayList<TaskProcess>();
		runningPrc.add(fakeProcess);
		doReturn(runningPrc).when(this.sebalTaskMonitor).getRunningProcesses();
		doNothing().when(pool).updateResource(fakeResource, ResourceState.BUSY);
		
		ExecutorService execServ = mock(ExecutorService.class);
		doReturn(execServ).when(this.sebalTaskMonitor).getExecutorService();
		doReturn(mock(Future.class)).when(execServ).submit(any(Runnable.class));
		
		Map<Task, TaskProcess> runningTasks = new HashMap<Task, TaskProcess>();
		runningTasks.put(fakeTask, fakeProcess);
		doReturn(runningTasks).when(this.sebalTaskMonitor).getRunningTasks();
		
		this.sebalTaskMonitor.runTask(fakeTask, fakeResource);
	}
}
