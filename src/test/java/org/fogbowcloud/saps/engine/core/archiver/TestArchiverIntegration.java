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
	public void testFailFetch() throws Exception {
		// When sftp file transfer fail
		// then
		// rollback of image data from FETCHING to FINISHED
		// and (if exists) delete fetched result files

		// setup
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper fetcherHelper = mock(ArchiverHelper.class);
		Properties properties = mock(Properties.class);
		String federationMember = "fake-federation-member";
		String fetcherVolumeInputPath = "fake-fetcher-volume-input-path";

		Date date = new Date();

		ImageTask imageData = new ImageTask(
				"task-id-1",
				"LT5",
				"region-53",
				date,
				"link1",
				ImageTaskState.FINISHED,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);

		doReturn(fetcherVolumeInputPath).when(fetcherHelper).getLocalImageInputsPath(imageData,
				properties);

		Archiver fetcher = mock(Archiver.class);
		doReturn(imageData).when(imageStore).getTask(imageData.getName());
		doReturn(1).when(fetcher).archiveInputs(imageData);

		Assert.assertEquals(ImageTaskState.FINISHED, imageData.getState());

		// exercise
		fetcher.archive(imageData);

		// expect
		Assert.assertEquals(ImageTaskState.FINISHED, imageData.getState());
	}

	@Test
	public void testFetcherErrorWhileGettingImagesToFetch() throws SQLException, IOException {
		// When no image is returned in getImageToFetch call
		// then
		// will be returned an empty list of images
		// no image will be set to FETCHING
		// the two images (that were not able to be in list) will remain as
		// FINISHED

		// setup
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper fetcherHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		Properties properties = mock(Properties.class);
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = mock(Date.class);

		ImageTask imageData = new ImageTask(
				"task-id-1",
				"LT5",
				"region-53",
				date,
				"link1",
				ImageTaskState.FINISHED,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);
		ImageTask imageData2 = new ImageTask(
				"task-id-2",
				"LT5",
				"region-53",
				date,
				"link2",
				ImageTaskState.FINISHED,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);

		doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData,
				properties);
		doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData,
				properties);

		Archiver fetcher = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				fetcherHelper);

		doThrow(new SQLException()).when(imageStore).getIn(ImageTaskState.FINISHED);

		Assert.assertEquals(ImageTaskState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageData2.getState());

		// exercise
		fetcher.tasksToArchive();

		// expect
		Assert.assertEquals(ImageTaskState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageData2.getState());
	}

	@Test
	public void testPrepareImageToFetchFail() throws SQLException, IOException {
		// prepareToFetch:
		// When it cannot update image to DB to FETCHING state
		// then
		// modify image data back to FINISHED state
		// remove image from pending map/DB

		// setup
		ConcurrentMap<String, ImageTask> pendingImageFetchMap = mock(ConcurrentMap.class);
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper fetcherHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		Properties properties = mock(Properties.class);
		DB pendingImageFetchDB = mock(DB.class);
		String federationMember = "fake-federation-member";

		Date date = new Date();

		ImageTask imageData = new ImageTask(
				"task-id-1",
				"LT5",
				"region-53",
				date,
				"link1",
				ImageTaskState.FINISHED,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);
		ImageTask imageData2 = new ImageTask(
				"task-id-2",
				"LT5",
				"region-53",
				date,
				"link2",
				ImageTaskState.FINISHED,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);

		Archiver fetcher = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				fetcherHelper);

		doReturn(imageData).when(imageStore).getTask(imageData.getName());

		doReturn(true).when(imageStore).lockTask(imageData.getName());
		doThrow(new SQLException()).when(imageStore).updateImageTask(imageData);

		doReturn(imageData2).when(imageStore).getTask(imageData2.getName());

		doReturn(true).when(imageStore).lockTask(imageData2.getName());
		doNothing().when(fetcherHelper).updatePendingMapAndDB(imageData2, pendingImageFetchDB,
				pendingImageFetchMap);
		doNothing().when(imageStore).updateImageTask(imageData2);

		Assert.assertEquals(ImageTaskState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageData2.getState());

		// exercise
		fetcher.prepareArchive(imageData);
		fetcher.prepareArchive(imageData2);

		// expect
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageData.getState());
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageData2.getState());
	}

	@Test
	public void testAddStateStampFail() throws SQLException, IOException {
		// When the update of image state and stamp into DB fail
		// then
		// prepareToFetch must try to roll back image from FETCHING to FINISHED
		// (if it not succeed) prepareToFetch must leave image state as FETCHING
		// for posterity

		// setup
		ConcurrentMap<String, ImageTask> pendingImageFetchMap = mock(ConcurrentMap.class);
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		ArchiverHelper fetcherHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		Properties properties = mock(Properties.class);
		DB pendingImageFetchDB = mock(DB.class);
		String federationMember = "fake-federation-member";

		Date date = new Date();

		ImageTask imageData = new ImageTask(
				"task-id-1",
				"LT5",
				"region-53",
				date,
				"link1",
				ImageTaskState.FINISHED,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);
		ImageTask imageData2 = new ImageTask(
				"task-id-2",
				"LT5",
				"region-53",
				date,
				"link2",
				ImageTaskState.FINISHED,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);

		Archiver fetcher = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				fetcherHelper);

		doReturn(imageData).when(imageStore).getTask(imageData.getName());
		doReturn(true).when(imageStore).lockTask(imageData.getName());
		doNothing().when(imageStore).updateImageTask(imageData);
		doThrow(new SQLException()).when(imageStore).addStateStamp(imageData.getName(),
				imageData.getState(), imageData.getUpdateTime());

		doReturn(imageData2).when(imageStore).getTask(imageData2.getName());
		doReturn(true).when(imageStore).lockTask(imageData2.getName());
		doNothing().when(fetcherHelper).updatePendingMapAndDB(imageData2, pendingImageFetchDB,
				pendingImageFetchMap);
		doNothing().when(imageStore).updateImageTask(imageData2);

		Assert.assertEquals(ImageTaskState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageTaskState.FINISHED, imageData2.getState());

		// exercise
		fetcher.prepareArchive(imageData);
		fetcher.prepareArchive(imageData2);

		// expect
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageData.getState());
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageData2.getState());
	}

	@Test
	public void testFailWhileUploadingToSwift() throws Exception {
		// When Fetcher fails to upload image results to swift
		// then
		// it must try again for MAX_SWIFT_UPLOAD_TRIES
		// (if not succeed) roll back image from FETCHING to FINISHED
		// delete results from disk

		// setup
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ArchiverHelper fetcherHelper = mock(ArchiverHelper.class);
		Properties properties = mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalInputExportPath = "fake-input-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumeInputPath = "fake-fetcher-volume-input-path";

		Date date = mock(Date.class);

		ImageTask imageData = new ImageTask(
				"task-id-1",
				"LT5",
				"region-53",
				date,
				"link1",
				ImageTaskState.ARCHIVING,
				federationMember,
				0,
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				"NE",
				new Timestamp(date.getTime()),
				new Timestamp(date.getTime()),
				"available",
				""
		);

		doReturn(sebalInputExportPath).when(fetcherHelper).getRemoteImageInputsPath(imageData,
				properties);
		doReturn(fetcherVolumeInputPath).when(fetcherHelper).getLocalImageInputsPath(imageData,
				properties);
		doReturn(0).when(ftpImpl).getFiles(properties, ftpServerIP, ftpServerPort,
				sebalInputExportPath, fetcherVolumeInputPath, imageData);
		File fetcherVolumeInputsDir = mock(File.class);
		doReturn(true).when(fetcherHelper).resultsChecksumOK(imageData, fetcherVolumeInputsDir);

		Archiver fetcher = mock(Archiver.class);

		doReturn(false).when(fetcher).uploadInputFilesToSwift(imageData, fetcherVolumeInputsDir);

		// exercise
		fetcher.archive(imageData);

		// expect
		Assert.assertEquals(ImageTaskState.ARCHIVING, imageData.getState());
	}

	@Test
	public void testGetFetcherVersion() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		FTPIntegrationImpl ftpImpl = mock(FTPIntegrationImpl.class);
		ArchiverHelper fetcherHelper = mock(ArchiverHelper.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);

		PrintWriter writer = new PrintWriter(
				"sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a", "UTF-8");
		writer.println("0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		writer.close();

		Archiver fetcher = new Archiver(properties, imageStore, swiftAPIClient, ftpImpl,
				fetcherHelper);

		// exercise
		String versionReturn = fetcher.getArchiverVersion();

		// expect
		Assert.assertEquals("0c26f092e976389c593953a1ad8ddaadb5c2ab2a", versionReturn);

		File file = new File("sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
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

		ImageTask task = mock(ImageTask.class);
		SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
		FTPIntegrationImpl ftpIntegrationImpl = mock(FTPIntegrationImpl.class);
		ArchiverHelper archiverHelper = mock(ArchiverHelper.class);

		Archiver archiver = new Archiver(properties, imageStore, swiftAPIClient, ftpIntegrationImpl,
				archiverHelper);

		doReturn(federationMember).when(task).getFederationMember();

		archiver.getFTPServerInfo(task);

		Assert.assertEquals(inputDownloaderIP, archiver.getFtpServerIP());
		Assert.assertEquals(inputDownloaderPort, archiver.getFtpServerPort());
	}
}
