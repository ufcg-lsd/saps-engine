package org.fogbowcloud.saps.engine.core.scheduler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.exception.SapsException;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetCountsSlotsException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.core.scheduler.selector.DefaultRoundRobin;
import org.fogbowcloud.saps.engine.core.scheduler.selector.Selector;
import org.fogbowcloud.saps.engine.util.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class SchedulerTest {

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	private Scheduler createDefaultScheduler(Selector selector, Arrebol arrebol, ImageDataStore imageStore) {
		Properties properties = new Properties();
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_IP, "");
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_PORT, "");
		properties.put(SapsPropertiesConstants.IMAGE_WORKER, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER, "");
		properties.put(SapsPropertiesConstants.ARREBOL_BASE_URL, "");

		try {
			return new Scheduler(properties, imageStore, null, arrebol, selector);
		} catch (SapsException | SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Test
	public void testSelectZeroTasksWithZeroCapacitySubmitionWhenThereIsNoAvailableTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		List<ImageTask> createdTasks = new LinkedList<ImageTask>();

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(0);
		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectZeroTasksWithZeroCapacitySubmitionWhenThereIsAvailableTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> createdTasks = new LinkedList<ImageTask>();
		createdTasks.add(new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.CREATED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", ""));

		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		downloadedTasks.add(new ImageTask("2", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 3, "user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", ""));

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		readyTasks.add(new ImageTask("3", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user3", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", ""));

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(0);
		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectZeroTasksWithFiveCapacitySubmitionWhenThereIsNoAvailableTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		List<ImageTask> createdTasks = new LinkedList<ImageTask>();

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(5);
		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneTaskWithFiveCapacitySubmitionWhenOneAvailableCreatedTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		List<ImageTask> createdTasks = new LinkedList<ImageTask>();

		ImageTask task01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.CREATED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		createdTasks.add(task01);

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(5);

		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();
		expectedSelectedTasks.add(task01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneTaskWithFiveCapacitySubmitionWhenOneAvailableDownloadedTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		List<ImageTask> createdTasks = new LinkedList<ImageTask>();

		ImageTask task01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		downloadedTasks.add(task01);

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(5);

		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();
		expectedSelectedTasks.add(task01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneTaskWithFiveCapacitySubmitionWhenOneAvailableReadyTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		List<ImageTask> createdTasks = new LinkedList<ImageTask>();

		ImageTask task01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		readyTasks.add(task01);

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(5);

		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();
		expectedSelectedTasks.add(task01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneReadyTaskWithOneCapacitySubmitionWhenAvailableTasksOneInEachStateReadyDownloadedCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		ImageTask readyTask01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		readyTasks.add(readyTask01);

		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		ImageTask downloadedTask01 = new ImageTask("2", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		downloadedTasks.add(downloadedTask01);

		List<ImageTask> createdTasks = new LinkedList<ImageTask>();
		ImageTask createdTask01 = new ImageTask("3", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		createdTasks.add(createdTask01);

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(1);

		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();
		expectedSelectedTasks.add(readyTask01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneDownloadedTaskWithOneCapacitySubmitionWhenAvailableTasksOneInEachStateDownloadedCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();

		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		ImageTask downloadedTask01 = new ImageTask("1", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		downloadedTasks.add(downloadedTask01);

		List<ImageTask> createdTasks = new LinkedList<ImageTask>();
		ImageTask createdTask01 = new ImageTask("2", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		createdTasks.add(createdTask01);

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(1);

		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();
		expectedSelectedTasks.add(downloadedTask01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectTwoDownloadedTaskOfUsersDiffirentsWithOneCapacitySubmitionWhenAvailableTasksTwoInEachStateReadyDownloadedCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		ImageDataStore imageStore = mock(ImageDataStore.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), null, imageStore);

		List<ImageTask> readyTasks = new LinkedList<ImageTask>();
		ImageTask readyTask01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		ImageTask readyTask02 = new ImageTask("2", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		readyTasks.add(readyTask01);
		readyTasks.add(readyTask02);

		List<ImageTask> downloadedTasks = new LinkedList<ImageTask>();
		ImageTask downloadedTask01 = new ImageTask("3", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		ImageTask downloadedTask02 = new ImageTask("4", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		downloadedTasks.add(downloadedTask01);
		downloadedTasks.add(downloadedTask02);

		List<ImageTask> createdTasks = new LinkedList<ImageTask>();
		ImageTask createdTask01 = new ImageTask("5", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		ImageTask createdTask02 = new ImageTask("6", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				ImageTask.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1),
				"", "");
		createdTasks.add(createdTask01);
		createdTasks.add(createdTask02);

		when(imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getIn(ImageTaskState.DOWNLOADED, ImageDataStore.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getIn(ImageTaskState.CREATED, ImageDataStore.UNLIMITED)).thenReturn(createdTasks);

		List<ImageTask> selectedTasks = scheduler.selectTasks(2);

		List<ImageTask> expectedSelectedTasks = new LinkedList<ImageTask>();
		expectedSelectedTasks.add(readyTask01);
		expectedSelectedTasks.add(readyTask02);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

}
