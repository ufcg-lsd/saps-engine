package org.fogbowcloud.saps.engine.core.downloader;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.repository.USGSNasaRepository;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestInputDownloaderIntegration {

	Properties properties;

	@Before
	public void setUp() {
		properties = mock(Properties.class);
	}

	@Before
	public void clean() {
		String pendingImageFileName = "pending-image-download.db";
		File pendingImageDBFile = new File(pendingImageFileName);

		if (pendingImageDBFile.exists()) {
			FileUtils.deleteQuietly(pendingImageDBFile);
		}
	}

	@Test
	public void testStepOverImageWhenDownloadFails()
			throws SQLException, IOException, InterruptedException {

		// 1. we have 2 NOT_DOWNLOADED images, pendingDB is empty
		// 2. we proceed to download them
		// 3. we face a download error for the first. then we step over
		// downloading it
		// 4. we are able to download the second one
		// 5. in the end, we shall 1 DOWNLOADED and 1 NOT_DOWNLOADED and the
		// pendingDB is empty

		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask taskOne = new ImageTask("task-id-1", "dataset-1", "region-1", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		imageList.add(taskOne);

		doReturn(imageList).when(imageStore).getImagesToDownload(federationMember,
				InputDownloader.MAX_IMAGES_TO_DOWNLOAD);
		doReturn(taskOne).when(imageStore).getTask(taskOne.getName());
		doReturn("link-1").when(usgsRepository).getImageDownloadLink(taskOne.getName());
		doThrow(new IOException()).when(usgsRepository).downloadImage(taskOne);

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);
		Assert.assertEquals(ImageTaskState.CREATED, taskOne.getState());
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());

		// exercise
		inputDownloader.download();

		// expect
		Assert.assertEquals(ImageTaskState.CREATED, taskOne.getState());
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());
	}

	@Test
	public void testinputDownloaderErrorWhileGetCreatedImages() throws SQLException, IOException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);

		doThrow(new SQLException()).when(imageStore).getImagesToDownload(federationMember,
				InputDownloader.MAX_IMAGES_TO_DOWNLOAD);
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());

		// exercise
		inputDownloader.download();

		// expect
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());
	}

	@Test
	public void testPurgeImagesFromVolume() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask taskOne = new ImageTask("task-id-1", "dataset-1", "region-1", date, "link1",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");
		taskOne.setImageStatus(ImageTask.PURGED);
		ImageTask taskTwo = new ImageTask("task-id-2", "dataset-1", "region-2", date, "link2",
				ImageTaskState.FINISHED, federationMember, 1, "NE", "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		imageList.add(taskOne);
		imageList.add(taskTwo);

		doReturn(imageList).when(imageStore).getIn(ImageTaskState.FINISHED);

		doReturn(sebalExportPath).when(properties)
				.getProperty(SapsPropertiesConstants.SEBAL_EXPORT_PATH);

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);

		// exercise
		inputDownloader.purgeTasksFromVolume(properties);
	}

	@Test
	public void testFederationMemberCheck() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember1 = "fake-fed-member-1";
		String federationMember2 = "fake-fed-member-2";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask taskOne = new ImageTask("task-id-1", "dataset-1", "region-1", date, "link1",
				ImageTaskState.ARCHIVED, federationMember1, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");
		ImageTask taskTwo = new ImageTask("task-id-2", "dataset-1", "region-2", date, "link2",
				ImageTaskState.ARCHIVED, federationMember2, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		imageList.add(taskOne);
		imageList.add(taskTwo);

		doReturn(sebalExportPath).when(properties)
				.getProperty(SapsPropertiesConstants.SEBAL_EXPORT_PATH);

		doReturn(imageList).when(imageStore).getAllTasks();

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember1);

		// exercise
		inputDownloader.deleteArchivedOutputsFromDisk(properties);

		// expect
		Assert.assertNotEquals(taskOne.getFederationMember(), taskTwo.getFederationMember());
	}

	@Test
	public void testPendingTaskMap() throws SQLException, IOException {
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		Date date = new Date(10000854);

		ImageTask task = new ImageTask("task-id-1", "dataset-1", "region-1", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(new java.util.Date().getTime()),
				new Timestamp(new java.util.Date().getTime()), "available", "");

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);

		inputDownloader.addTaskToPendingMap(task);
		ConcurrentMap<String, ImageTask> pendingTaskMap = inputDownloader.getPendingTaskMap();

		Assert.assertEquals(task, pendingTaskMap.get(task.getTaskId()));
	}
}
