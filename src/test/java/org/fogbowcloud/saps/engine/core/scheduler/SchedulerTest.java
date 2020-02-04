package org.fogbowcloud.saps.engine.core.scheduler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetCountsSlotsException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.core.scheduler.selector.DefaultRoundRobin;
import org.fogbowcloud.saps.engine.core.scheduler.selector.Selector;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
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

	private Scheduler createDefaultScheduler(Selector selector, Arrebol arrebol, Catalog imageStore)
			throws SapsException, SQLException {
		Properties properties = new Properties();
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_IP, "");
		properties.put(SapsPropertiesConstants.IMAGE_DATASTORE_PORT, "");
		properties.put(SapsPropertiesConstants.IMAGE_WORKER, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR, "");
		properties.put(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER, "");
		properties.put(SapsPropertiesConstants.ARREBOL_BASE_URL, "");

		return new Scheduler(properties, imageStore, null, arrebol, selector);
	}

	@Test
	public void testSelectZeroTasksWithZeroSubmissionCapacityWhenThereIsNoAvailableTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		List<SapsImage> createdTasks = new LinkedList<SapsImage>();

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(0);

		List<SapsImage> selectedTasks = scheduler.selectTasks();
		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectZeroTasksWithZeroSubmissionCapacityWhenThereIsAvailableTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> createdTasks = new LinkedList<SapsImage>();
		createdTasks.add(new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", ""));

		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		downloadedTasks.add(new SapsImage("2", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 3, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", ""));

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		readyTasks.add(new SapsImage("3", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user3", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", ""));

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(0);

		List<SapsImage> selectedTasks = scheduler.selectTasks();
		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectZeroTasksWithFiveSubmissionCapacityWhenThereIsNoAvailableTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		List<SapsImage> createdTasks = new LinkedList<SapsImage>();

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(5);

		List<SapsImage> selectedTasks = scheduler.selectTasks();
		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneTaskWithFiveSubmissionCapacityWhenOneAvailableCreatedTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		List<SapsImage> createdTasks = new LinkedList<SapsImage>();

		SapsImage task01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		createdTasks.add(task01);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(5);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(task01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneTaskWithFiveSubmissionCapacityWhenOneAvailableDownloadedTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		List<SapsImage> createdTasks = new LinkedList<SapsImage>();

		SapsImage task01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		downloadedTasks.add(task01);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(5);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(task01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneTaskWithFiveSubmissionCapacityWhenOneAvailableReadyTasks()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		List<SapsImage> createdTasks = new LinkedList<SapsImage>();

		SapsImage task01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		readyTasks.add(task01);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(5);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(task01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneReadyTaskWithOneSubmissionCapacityWhenAvailableTasksOneInEachStateReadyDownloadedCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		SapsImage readyTask01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		readyTasks.add(readyTask01);

		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		SapsImage downloadedTask01 = new SapsImage("2", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		downloadedTasks.add(downloadedTask01);

		List<SapsImage> createdTasks = new LinkedList<SapsImage>();
		SapsImage createdTask01 = new SapsImage("3", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		createdTasks.add(createdTask01);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(1);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(readyTask01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneDownloadedTaskWithOneSubmissionCapacityWhenAvailableTasksOneInEachStateDownloadedCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();

		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		SapsImage downloadedTask01 = new SapsImage("1", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		downloadedTasks.add(downloadedTask01);

		List<SapsImage> createdTasks = new LinkedList<SapsImage>();
		SapsImage createdTask01 = new SapsImage("2", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		createdTasks.add(createdTask01);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(1);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(downloadedTask01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectTwoDownloadedTaskOfUsersDiffirentsWithTwoSubmissionCapacityWhenAvailableTasksTwoInEachStateReadyDownloadedCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		SapsImage readyTask01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage readyTask02 = new SapsImage("2", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		readyTasks.add(readyTask01);
		readyTasks.add(readyTask02);

		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		SapsImage downloadedTask01 = new SapsImage("3", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage downloadedTask02 = new SapsImage("4", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		downloadedTasks.add(downloadedTask01);
		downloadedTasks.add(downloadedTask02);

		List<SapsImage> createdTasks = new LinkedList<SapsImage>();
		SapsImage createdTask01 = new SapsImage("5", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage createdTask02 = new SapsImage("6", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		createdTasks.add(createdTask01);
		createdTasks.add(createdTask02);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(2);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(readyTask01);
		expectedSelectedTasks.add(readyTask02);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectTwoReadyAndOneDownloadTasksWithThreeSubmissionCapacityWhenAvailableTasksInEachStateTwoReadyOneDownloadedThreeCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		SapsImage readyTask01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage readyTask02 = new SapsImage("2", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		readyTasks.add(readyTask01);
		readyTasks.add(readyTask02);

		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		SapsImage downloadedTask01 = new SapsImage("3", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		downloadedTasks.add(downloadedTask01);

		List<SapsImage> createdTasks = new LinkedList<SapsImage>();
		SapsImage createdTask01 = new SapsImage("4", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage createdTask02 = new SapsImage("5", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage createdTask03 = new SapsImage("6", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user3", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		createdTasks.add(createdTask01);
		createdTasks.add(createdTask02);
		createdTasks.add(createdTask03);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(3);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(readyTask01);
		expectedSelectedTasks.add(readyTask02);
		expectedSelectedTasks.add(downloadedTask01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectOneReadyAndTwoDownloadTasksWithThreeSubmissionCapacityWhenAvailableTasksInEachStateOneReadyTwoDownloadedThreeCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		SapsImage readyTask01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		readyTasks.add(readyTask01);

		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		SapsImage downloadedTask01 = new SapsImage("2", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage downloadedTask02 = new SapsImage("3", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		downloadedTasks.add(downloadedTask01);
		downloadedTasks.add(downloadedTask02);

		List<SapsImage> createdTasks = new LinkedList<SapsImage>();
		SapsImage createdTask01 = new SapsImage("4", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage createdTask02 = new SapsImage("5", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage createdTask03 = new SapsImage("6", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user3", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		createdTasks.add(createdTask01);
		createdTasks.add(createdTask02);
		createdTasks.add(createdTask03);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(3);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(readyTask01);
		expectedSelectedTasks.add(downloadedTask01);
		expectedSelectedTasks.add(downloadedTask02);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

	@Test
	public void testSelectThreeTasksOneOfEachStateWithThreeSubmissionCapacityWhenAvailableTasksInEachStateOneReadyOneDownloadedThreeCreated()
			throws Exception, SubmitJobException, GetCountsSlotsException {
		Catalog imageStore = mock(Catalog.class);
		Arrebol arrebol = mock(Arrebol.class);
		Scheduler scheduler = createDefaultScheduler(new DefaultRoundRobin(), arrebol, imageStore);

		List<SapsImage> readyTasks = new LinkedList<SapsImage>();
		SapsImage readyTask01 = new SapsImage("1", "landsat_8", "217066", new Date(), ImageTaskState.READY,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		readyTasks.add(readyTask01);

		List<SapsImage> downloadedTasks = new LinkedList<SapsImage>();
		SapsImage downloadedTask01 = new SapsImage("2", "landsat_7", "217066", new Date(), ImageTaskState.DOWNLOADED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		downloadedTasks.add(downloadedTask01);

		List<SapsImage> createdTasks = new LinkedList<SapsImage>();
		SapsImage createdTask01 = new SapsImage("4", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user1", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage createdTask02 = new SapsImage("5", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user2", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		SapsImage createdTask03 = new SapsImage("6", "landsat_5", "217066", new Date(), ImageTaskState.CREATED,
				SapsImage.NONE_ARREBOL_JOB_ID, "", 5, "user3", "nop", "", "nop", "", "aio", "", new Timestamp(1),
				new Timestamp(1), "", "");
		createdTasks.add(createdTask01);
		createdTasks.add(createdTask02);
		createdTasks.add(createdTask03);

		when(imageStore.getTasksByState(ImageTaskState.READY, Catalog.UNLIMITED)).thenReturn(readyTasks);
		when(imageStore.getTasksByState(ImageTaskState.DOWNLOADED, Catalog.UNLIMITED)).thenReturn(downloadedTasks);
		when(imageStore.getTasksByState(ImageTaskState.CREATED, Catalog.UNLIMITED)).thenReturn(createdTasks);
		when(arrebol.getCountSlotsInQueue("default")).thenReturn(3);

		List<SapsImage> selectedTasks = scheduler.selectTasks();

		List<SapsImage> expectedSelectedTasks = new LinkedList<SapsImage>();
		expectedSelectedTasks.add(readyTask01);
		expectedSelectedTasks.add(downloadedTask01);
		expectedSelectedTasks.add(createdTask01);

		Assert.assertEquals(expectedSelectedTasks, selectedTasks);
	}

}
