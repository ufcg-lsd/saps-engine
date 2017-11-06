package org.fogbowcloud.saps.engine.core.archiver;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapdb.DB;

public class TestArchiverIntegration {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void clean() {
		String pendingImageFileName = "pending-image-fetch.db";
		File pendingImageDBFile = new File(pendingImageFileName);

		if (pendingImageDBFile.exists()) {
			FileUtils.deleteQuietly(pendingImageDBFile);
		}
	}

	@Test
	public void testFailArchive() throws Exception {
		// When sftp file transfer fail
		// then
		// rollback of image data from ARCHIVING to FINISHED
		// and (if exists) delete archived result files

		// setup
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);
		Properties properties = mock(Properties.class);
		String federationMember = "fake-federation-member";
		String archiverVolumeInputPath = "fake-fetcher-volume-input-path";

		Date date = new Date();

		ImageTask imageTask = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		doReturn(archiverVolumeInputPath).when(archiverHelper).getLocalTaskInputPath(imageTask,
				properties);

		Archiver archiver = mock(Archiver.class);
		doReturn(imageTask).when(imageStore).getTask(imageTask.getTaskId());
		doReturn(1).when(archiver).archiveInputs(imageTask);

		Assert.assertEquals(ImageTaskState.FINISHED, imageTask.getState());

		// exercise
		archiver.archive(imageTask);

		// expect
		Assert.assertEquals(ImageTaskState.FINISHED, imageTask.getState());
	}

	@Test
	public void testArchiverErrorWhileGettingTasksToFetch() throws SQLException, IOException {
		// When no task is returned in getTaskToArchive call
		// then
		// will be returned an empty list of task
		// no task will be set to ARCHIVING
		// the two tasks (that were not able to be in list) will remain as
		// FINISHED

		// setup
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		Properties properties = mock(Properties.class);
		String sapsExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String archiverVolumePath = "fake-fetcher-volume-path";

		Date date = mock(Date.class);

		ImageTask imageTask = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");
		ImageTask imageTask2 = new ImageTask("task-id-2", "LT5", "region-53", date, "link2",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		doReturn(sapsExportPath).when(archiverHelper).getRemoteTaskOutputPath(imageTask,
				properties);
		doReturn(archiverVolumePath).when(archiverHelper).getLocalTaskOutputPath(imageTask,
				properties);

		Archiver archiver = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				archiverHelper);

		doThrow(new SQLException()).when(imageStore).getIn(ImageTaskState.FINISHED);

		Assert.assertEquals(ImageTaskState.FINISHED, imageTask.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageTask2.getState());

		// exercise
		archiver.tasksToArchive();

		// expect
		Assert.assertEquals(ImageTaskState.FINISHED, imageTask.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageTask2.getState());
	}

	@Test
	public void testPrepareTaskToArchiveFail() throws SQLException, IOException {
		// prepareToArchive:
		// When it cannot update task to DB to ARCHIVING state
		// then
		// modify task data back to FINISHED state
		// remove task from pending map/DB

		// setup
		ConcurrentMap<String, ImageTask> pendingTaskArchiveMap = mock(ConcurrentMap.class);
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		Properties properties = mock(Properties.class);
		DB pendingTaskArchiveDB = mock(DB.class);
		String federationMember = "fake-federation-member";

		Date date = new Date();

		ImageTask imageTask = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");
		ImageTask imageTask2 = new ImageTask("task-id-2", "LT5", "region-53", date, "link2",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		Archiver archiver = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				archiverHelper);

		doReturn(imageTask).when(imageStore).getTask(imageTask.getTaskId());

		doReturn(true).when(imageStore).lockTask(imageTask.getTaskId());
		doThrow(new SQLException()).when(imageStore).updateImageTask(imageTask);

		doReturn(imageTask2).when(imageStore).getTask(imageTask2.getTaskId());

		doReturn(true).when(imageStore).lockTask(imageTask2.getTaskId());
		doNothing().when(archiverHelper).updatePendingMapAndDB(imageTask2, pendingTaskArchiveDB,
				pendingTaskArchiveMap);
		doNothing().when(imageStore).updateImageTask(imageTask2);

		Assert.assertEquals(ImageTaskState.FINISHED, imageTask.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageTask2.getState());

		// exercise
		archiver.prepareArchive(imageTask);
		archiver.prepareArchive(imageTask2);

		// expect
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageTask.getState());
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageTask2.getState());
	}

	@Test
	public void testAddStateStampFail() throws SQLException, IOException {
		// When the update of task state and stamp into DB fail
		// then
		// prepareToArchive must try to roll back task from ARCHIVING to FINISHED
		// (if it not succeed) prepareToArchive must leave task state as ARCHIVING
		// for posterity

		// setup
		ConcurrentMap<String, ImageTask> pendingTaskArchiveMap = mock(ConcurrentMap.class);
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		Properties properties = mock(Properties.class);
		DB pendingTaskArchiveDB = mock(DB.class);
		String federationMember = "fake-federation-member";

		Date date = new Date();

		ImageTask imageTask = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");
		ImageTask imageTask2 = new ImageTask("task-id-2", "LT5", "region-53", date, "link2",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		Archiver archiver = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				archiverHelper);

		doReturn(imageTask).when(imageStore).getTask(imageTask.getTaskId());
		doReturn(true).when(imageStore).lockTask(imageTask.getTaskId());
		doNothing().when(imageStore).updateImageTask(imageTask);
		doThrow(new SQLException()).when(imageStore).addStateStamp(imageTask.getTaskId(),
				imageTask.getState(), imageTask.getUpdateTime());

		doReturn(imageTask2).when(imageStore).getTask(imageTask2.getTaskId());
		doReturn(true).when(imageStore).lockTask(imageTask2.getTaskId());
		doNothing().when(archiverHelper).updatePendingMapAndDB(imageTask2, pendingTaskArchiveDB,
				pendingTaskArchiveMap);
		doNothing().when(imageStore).updateImageTask(imageTask2);

		Assert.assertEquals(ImageTaskState.FINISHED, imageTask.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageTask2.getState());

		// exercise
		archiver.prepareArchive(imageTask);
		archiver.prepareArchive(imageTask2);

		// expect
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageTask.getState());
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageTask2.getState());
	}

	@Test
	public void testFailWhileUploadingToSwift() throws Exception {
		// When Archiver fails to upload task output to swift
		// then
		// it must try again for MAX_SWIFT_UPLOAD_TRIES
		// (if not succeed) roll back task from ARCHIVING to FINISHED
		// delete output from disk

		// setup
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);
		Properties properties = mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sapsInputExportPath = "fake-input-export-path";
		String federationMember = "fake-federation-member";
		String archiverVolumeInputPath = "fake-fetcher-volume-input-path";

		Date date = mock(Date.class);

		ImageTask imageTask = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.ARCHIVING, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		doReturn(sapsInputExportPath).when(archiverHelper).getRemoteTaskInputPath(imageTask,
				properties);
		doReturn(archiverVolumeInputPath).when(archiverHelper).getLocalTaskInputPath(imageTask,
				properties);
		doReturn(0).when(ftpImpl).getFiles(properties, ftpServerIP, ftpServerPort,
				sapsInputExportPath, archiverVolumeInputPath, imageTask);
		File archiverVolumeInputDir = mock(File.class);
		doReturn(true).when(archiverHelper).resultsChecksumOK(imageTask, archiverVolumeInputDir);

		Archiver fetcher = mock(Archiver.class);

		doReturn(false).when(fetcher).uploadInputFilesToSwift(imageTask, archiverVolumeInputDir);

		// exercise
		fetcher.archive(imageTask);

		// expect
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageTask.getState());
	}

	@Test
	public void testGetArchiverVersion() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);

		PrintWriter writer = new PrintWriter(
				"saps-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a", "UTF-8");
		writer.println("0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		writer.close();

		Archiver archiver = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				archiverHelper);

		// exercise
		String versionReturn = archiver.getArchiverVersion();

		// expect
		Assert.assertEquals("0c26f092e976389c593953a1ad8ddaadb5c2ab2a", versionReturn);

		File file = new File("saps-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		file.delete();
	}

	@Test
	public void testGetInputDownloaderSSHPort() throws Exception {
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		Properties properties = new Properties();
		properties.setProperty("datastore_username", "testdb");
		properties.setProperty("datastore_password", "testdb");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_name", "testdb");
		properties.setProperty("datastore_ip", "localhost");
		properties.setProperty("datastore_port", "8000");

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);
		imageStore.addDeployConfig(inputDownloaderIP, inputDownloaderPort, nfsPort,
				federationMember);

		ImageTask imageTask = mock(ImageTask.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		FTPIntegrationImpl ftpIntegrationImpl = mock(FTPIntegrationImpl.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);

		Archiver archiver = new Archiver(properties, imageStore, swiftAPIClient, ftpIntegrationImpl,
				archiverHelper);

		doReturn(federationMember).when(imageTask).getFederationMember();

		archiver.getFTPServerInfo(imageTask);

		Assert.assertEquals(inputDownloaderIP, archiver.getFtpServerIP());
		Assert.assertEquals(inputDownloaderPort, archiver.getFtpServerPort());

		imageStore.dispose();
	}
}
