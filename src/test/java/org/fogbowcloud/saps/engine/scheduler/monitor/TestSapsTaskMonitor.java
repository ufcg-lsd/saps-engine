package org.fogbowcloud.saps.engine.scheduler.monitor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.fogbowcloud.blowout.core.model.Command;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskProcessImpl;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.infrastructure.model.AbstractResource;
import org.fogbowcloud.blowout.infrastructure.model.ResourceState;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.blowout.pool.DefaultBlowoutPool;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestSapsTaskMonitor {

	private static final String FAKE_ID = "fakeId";

	public long timeout;
	public ImageDataStore imageStore;
	public SapsTaskMonitor sebalTaskMonitor;
	public BlowoutPool pool;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() {
		timeout = 10000;
		pool = mock(BlowoutPool.class);
		imageStore = mock(ImageDataStore.class);
		sebalTaskMonitor = spy(new SapsTaskMonitor(pool, imageStore));
	}

	@Test
	public void testProcMonTaskRunning() {
		TaskProcess fakeProcess = mock(TaskProcess.class);

		doReturn(TaskState.RUNNING).when(fakeProcess).getStatus();

		AbstractResource fakeResource = mock(AbstractResource.class);
		List<TaskProcess> runningProcesses = new ArrayList<TaskProcess>();
		runningProcesses.add(fakeProcess);

		doReturn(runningProcesses).when(this.sebalTaskMonitor).getRunningProcesses();
		doNothing().when(sebalTaskMonitor).imageTaskToRunning(fakeProcess);

		this.sebalTaskMonitor.procMon();
		verify(pool, never()).updateResource(fakeResource, ResourceState.FAILED);
		verify(pool, never()).updateResource(fakeResource, ResourceState.IDLE);
	}

	@Test
	public void testProcMonTaskFailed() throws SQLException {
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

		doNothing().when(sebalTaskMonitor).updateImageTaskToFailed(fakeProcess);
		doNothing().when(sebalTaskMonitor).storeMetadata(fakeProcess);

		this.sebalTaskMonitor.procMon();

		verify(pool).updateResource(fakeResource, ResourceState.IDLE);
		verify(pool, never()).updateResource(fakeResource, ResourceState.FAILED);
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

	@Test
	public void testProcMonImageTaskRunning() throws SQLException {
		Date date = new Date();
		ImageTask imageTask = new ImageTask("task-id", "LT5", "region-53", date, "NE",
				ImageTaskState.READY, "NE", 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()), "NE",
				"NE");
		TaskProcess fakeTaskProcess = mock(TaskProcess.class);

		doReturn(imageTask.getTaskId()).when(sebalTaskMonitor)
				.getImageTaskFromTaskProcess(fakeTaskProcess);
		doReturn(imageTask).when(imageStore).getTask(imageTask.getTaskId());
		doNothing().when(imageStore).updateImageTask(imageTask);
		doNothing().when(imageStore).addStateStamp(imageTask.getTaskId(), imageTask.getState(),
				imageTask.getUpdateTime());

		this.sebalTaskMonitor.updateImageTaskToRunning(fakeTaskProcess);
		Assert.assertEquals(ImageTaskState.RUNNING, imageTask.getState());
	}

	@Test
	public void testProcMonImageTaskFinished() throws SQLException {
		Date date = new Date();
		ImageTask imageTask = new ImageTask("task-id", "LT5", "region-53", date, "NE",
				ImageTaskState.RUNNING, "NE", 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()), "NE",
				"NE");
		TaskProcess fakeTaskProcess = mock(TaskProcess.class);

		doReturn(imageTask.getTaskId()).when(sebalTaskMonitor)
				.getImageTaskFromTaskProcess(fakeTaskProcess);
		doReturn(imageTask).when(imageStore).getTask(imageTask.getTaskId());
		doNothing().when(imageStore).updateImageTask(imageTask);
		doNothing().when(imageStore).addStateStamp(imageTask.getTaskId(), imageTask.getState(),
				imageTask.getUpdateTime());

		this.sebalTaskMonitor.updateImageTaskToFinished(fakeTaskProcess);
		Assert.assertEquals(ImageTaskState.FINISHED, imageTask.getState());
	}

	@Test
	public void testProcMonImageTaskFailed() throws SQLException {
		Date date = new Date();
		ImageTask imageTask = new ImageTask("task-id", "LT5", "region-53", date, "NE",
				ImageTaskState.RUNNING, "NE", 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()), "NE",
				"NE");
		TaskProcess fakeTaskProcess = mock(TaskProcess.class);

		doReturn(imageTask.getTaskId()).when(sebalTaskMonitor)
				.getImageTaskFromTaskProcess(fakeTaskProcess);
		doReturn(imageTask).when(imageStore).getTask(imageTask.getTaskId());
		doNothing().when(imageStore).updateImageTask(imageTask);
		doNothing().when(imageStore).addStateStamp(imageTask.getTaskId(), imageTask.getState(),
				imageTask.getUpdateTime());

		this.sebalTaskMonitor.updateImageTaskToFailed(fakeTaskProcess);
		Assert.assertEquals(ImageTaskState.FAILED, imageTask.getState());
	}

	@Test
	public void testProcMonImageTaskReady() throws SQLException {
		Date date = new Date();
		ImageTask imageTask = new ImageTask("task-id", "LT5", "region-53", date, "NE",
				ImageTaskState.RUNNING, "NE", 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()), "NE",
				"NE");
		TaskProcess fakeTaskProcess = mock(TaskProcess.class);

		doReturn(imageTask.getTaskId()).when(sebalTaskMonitor)
				.getImageTaskFromTaskProcess(fakeTaskProcess);
		doReturn(imageTask).when(imageStore).getTask(imageTask.getTaskId());
		doNothing().when(imageStore).updateImageTask(imageTask);
		doNothing().when(imageStore).addStateStamp(imageTask.getTaskId(), imageTask.getState(),
				imageTask.getUpdateTime());

		this.sebalTaskMonitor.updateImageTaskToReady(fakeTaskProcess);
		Assert.assertEquals(ImageTaskState.READY, imageTask.getState());
	}

	@Test
	public void testProcMonTaskFailedWithMetadata() throws SQLException {
		String taskId = "fake-id";
		@SuppressWarnings("unchecked")
		List<Command> commandList = mock(List.class);
		Specification spec = mock(Specification.class);

		String metadataFilePath = "/fake/export/path/fake-id/metadata/outputDescription.txt";
		String operatingSystem = "operating-system";
		String kernelVersion = "kernel-version";

		TaskImpl taskImpl = new TaskImpl(taskId, spec, UUID.randomUUID().toString());
		taskImpl.putMetadata(SapsTask.METADATA_TASK_ID, taskId);
		taskImpl.putMetadata(SapsTask.METADATA_EXPORT_PATH, "/fake/export/path");
		taskImpl.putMetadata(SapsTask.METADATA_WORKER_OPERATING_SYSTEM, operatingSystem);
		taskImpl.putMetadata(SapsTask.METADATA_WORKER_KERNEL_VERSION, kernelVersion);

		List<Task> tasks = new ArrayList<>();
		tasks.add(taskImpl);

		TaskProcessImpl taskProcessImpl = new TaskProcessImpl(taskId, commandList, spec, UUID.randomUUID().toString());

		BlowoutPool blowoutPool = new DefaultBlowoutPool();
		blowoutPool.addTasks(tasks);

		doReturn(blowoutPool).when(sebalTaskMonitor).getBlowoutPool();

		Assert.assertEquals(metadataFilePath,
				this.sebalTaskMonitor.getMetadataFilePath(taskProcessImpl));
		Assert.assertEquals(operatingSystem,
				this.sebalTaskMonitor.getOperatingSystem(taskProcessImpl));
		Assert.assertEquals(kernelVersion, this.sebalTaskMonitor.getKernelVersion(taskProcessImpl));
	}

	@Test
	public void testMetadataStoreWhenTaskFinish() throws SQLException {
		// ImageTask set
		ImageTask imageTask = new ImageTask("task-id", "LT5", "region-53", new Date(), "link1",
				ImageTaskState.RUNNING, ImageTask.NON_EXISTENT_DATA, 0, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, new Timestamp(new java.util.Date().getTime()),
				new Timestamp(new java.util.Date().getTime()), "available", "");

		// Database set
		Properties properties = new Properties();
		properties.setProperty("datastore_ip", "");
		properties.setProperty("datastore_port", "");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_username", "testuser");
		properties.setProperty("datastore_password", "testuser");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_name", "testdb");

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);
		imageStore.addImageTask(imageTask);
		imageStore.dispatchMetadataInfo(imageTask.getTaskId());

		// Task set
		@SuppressWarnings("unchecked")
		List<Command> commandList = mock(List.class);
		Specification spec = mock(Specification.class);

		String metadataFilePath = "/fake/export/path/" + imageTask.getTaskId()
				+ "/metadata/outputDescription.txt";
		String operatingSystem = "operating-system";
		String kernelVersion = "kernel-version";

		TaskImpl taskImpl = new TaskImpl(imageTask.getTaskId(), spec, UUID.randomUUID().toString());
		taskImpl.putMetadata(SapsTask.METADATA_TASK_ID, imageTask.getTaskId());
		taskImpl.putMetadata(SapsTask.METADATA_EXPORT_PATH, "/fake/export/path");
		taskImpl.putMetadata(SapsTask.METADATA_WORKER_OPERATING_SYSTEM, operatingSystem);
		taskImpl.putMetadata(SapsTask.METADATA_WORKER_KERNEL_VERSION, kernelVersion);

		List<Task> tasks = new ArrayList<>();
		tasks.add(taskImpl);

		BlowoutPool blowoutPool = new DefaultBlowoutPool();
		blowoutPool.addTasks(tasks);

		TaskProcessImpl taskProcess = new TaskProcessImpl(imageTask.getTaskId(), commandList, spec, UUID.randomUUID().toString());
		taskProcess.setStatus(TaskState.FINISHED);

		Map<Task, TaskProcess> taskProcesses = new HashMap<>();
		taskProcesses.put(taskImpl, taskProcess);

		// Task monitor set
		SapsTaskMonitor taskMonitor = spy(new SapsTaskMonitor(blowoutPool, imageStore));
		taskMonitor.setRunningTasks(taskProcesses);

		// exercise
		taskMonitor.procMon();

		// expect
		Assert.assertEquals(metadataFilePath,
				imageStore.getMetadataInfo(imageTask.getTaskId(),
						SapsPropertiesConstants.WORKER_COMPONENT_TYPE,
						SapsPropertiesConstants.METADATA_TYPE));
		
		Assert.assertEquals(operatingSystem, imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.WORKER_COMPONENT_TYPE, SapsPropertiesConstants.OS_TYPE));
		
		Assert.assertEquals(kernelVersion,
				imageStore.getMetadataInfo(imageTask.getTaskId(),
						SapsPropertiesConstants.WORKER_COMPONENT_TYPE,
						SapsPropertiesConstants.KERNEL_TYPE));
	}
	
	@Test
	public void testTaskTimedout() throws SQLException {
		// ImageTask set
		ImageTask imageTask = new ImageTask("fake-task-id", "LT5", "region-53", new Date(), "link1",
				ImageTaskState.RUNNING, ImageTask.NON_EXISTENT_DATA, 0, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, new Timestamp(new java.util.Date().getTime()),
				new Timestamp(new java.util.Date().getTime()), "available", "");

		// Database set
		Properties properties = new Properties();
		properties.setProperty("datastore_ip", "");
		properties.setProperty("datastore_port", "");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_username", "testuser");
		properties.setProperty("datastore_password", "testuser");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_name", "testdb");

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);
		imageStore.addImageTask(imageTask);

		List<Task> tasks = new ArrayList<>();

		Specification spec = mock(Specification.class);		
		String operatingSystem = "operating-system";
		String kernelVersion = "kernel-version";
		
		TaskImpl taskImpl = new TaskImpl(imageTask.getTaskId(), spec, UUID.randomUUID().toString());
		taskImpl.putMetadata(SapsTask.METADATA_TASK_ID, imageTask.getTaskId());
		taskImpl.putMetadata(SapsTask.METADATA_EXPORT_PATH, "/fake/export/path");
		taskImpl.putMetadata(SapsTask.METADATA_WORKER_OPERATING_SYSTEM, operatingSystem);
		taskImpl.putMetadata(SapsTask.METADATA_WORKER_KERNEL_VERSION, kernelVersion);
		
		tasks.add(taskImpl);

		BlowoutPool blowoutPool = new DefaultBlowoutPool();
		blowoutPool.addTasks(tasks);

		TaskProcessImpl taskProcess = new TaskProcessImpl(imageTask.getTaskId(),
				new ArrayList<Command>(), spec, UUID.randomUUID().toString());
		taskProcess.setStatus(TaskState.TIMEDOUT);

		Map<Task, TaskProcess> taskProcesses = new HashMap<>();
		taskProcesses.put(taskImpl, taskProcess);

		// Task monitor set
		SapsTaskMonitor taskMonitor = spy(new SapsTaskMonitor(blowoutPool, imageStore));
		taskMonitor.setRunningTasks(taskProcesses);

		// exercise
		taskMonitor.procMon();

		// expect
		Assert.assertEquals(ImageTaskState.FAILED,
				imageStore.getTask(imageTask.getTaskId()).getState());
		Assert.assertFalse(taskMonitor.getBlowoutPool().getAllTasks().contains(taskImpl));
	}
}