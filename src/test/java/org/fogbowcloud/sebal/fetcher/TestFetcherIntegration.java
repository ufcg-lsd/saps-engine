package org.fogbowcloud.sebal.fetcher;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.swift.SwiftClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mockito.Mockito;

public class TestFetcherIntegration {

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
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doReturn(1)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalExportPath, fetcherVolumePath, imageData);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());

		// exercise
		fetcher.fetch(imageData, 3);

		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
	}

	@Test
	public void testNoResultFilesToFetch() throws Exception {
		// FIXME: describe
		
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData, properties);
		Mockito.doReturn(false).when(fetcherHelper)
				.isThereFetchedFiles(sebalExportPath);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());

		// exercise
		fetcher.finishFetch(imageData);

		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
	}
	
	@Test
	public void testFetcherErrorWhileGettingImagesToFetch() throws SQLException, IOException {
		// FIXME: describe
		
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doThrow(new SQLException()).when(imageStore).getIn(ImageState.FINISHED);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FINISHED, imageData2.getState());
		
		// exercise
		fetcher.imagesToFetch();
		
		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FINISHED, imageData2.getState());
	}
	
	@Test
	public void testPrepareImageToFetchFail() throws SQLException, IOException {
		// prepareToFetch: 
		// When it cannot update image to DB to FETCHING state
		// then
		//	modify image data back to FINISHED state
		//	remove image from pending map/DB

		// setup
		ConcurrentMap<String, ImageData> pendingImageFetchMap = Mockito.mock(ConcurrentMap.class);
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		DB pendingImageFetchDB = Mockito.mock(DB.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String federationMember = "fake-federation-member";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doReturn(true).when(imageStore).lockImage(imageData.getName());
		Mockito.doThrow(new SQLException()).when(imageStore)
				.updateImage(imageData);
		
		Mockito.doReturn(true).when(imageStore).lockImage(imageData2.getName());
		Mockito.doNothing().when(fetcherHelper).updatePendingMapAndDB(imageData2, 
				pendingImageFetchDB, pendingImageFetchMap);
		Mockito.doNothing().when(imageStore).updateImage(imageData2);
		
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FINISHED, imageData2.getState());
		
		// exercise
		fetcher.prepareFetch(imageData);
		fetcher.prepareFetch(imageData2);
		
		// expect
		Assert.assertEquals(ImageState.FETCHING, imageData.getState());
		Assert.assertEquals(ImageState.FETCHING, imageData2.getState());
	}
	
	@Test
	public void testAddStateStampFail() throws SQLException, IOException {
		// FIXME: describe
		
		// setup
		ConcurrentMap<String, ImageData> pendingImageFetchMap = Mockito
				.mock(ConcurrentMap.class);
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		DB pendingImageFetchDB = Mockito.mock(DB.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String federationMember = "fake-federation-member";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", date,
				date, "");

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doReturn(true).when(imageStore).lockImage(imageData.getName());
		Mockito.doNothing().when(imageStore).updateImage(imageData);
		Mockito.doThrow(new SQLException()).when(imageStore)
				.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());

		Mockito.doReturn(true).when(imageStore).lockImage(imageData2.getName());
		Mockito.doNothing()
				.when(fetcherHelper)
				.updatePendingMapAndDB(imageData2, pendingImageFetchDB,
						pendingImageFetchMap);
		Mockito.doNothing().when(imageStore).updateImage(imageData2);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FINISHED, imageData2.getState());

		// exercise
		fetcher.prepareFetch(imageData);
		fetcher.prepareFetch(imageData2);

		// expect
		Assert.assertEquals(ImageState.FETCHING, imageData.getState());
		Assert.assertEquals(ImageState.FETCHING, imageData2.getState());
	}
	
	@Test
	public void testMaxTriesReached() throws Exception {
		// FIXME: describe
		
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", date,
				date, "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper)
				.getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Assert.assertEquals(ImageState.FETCHING, imageData.getState());
		
		Mockito.doReturn(false).when(fetcherHelper).resultsChecksumOK(imageData, new File(fetcherVolumePath));

		// exercise
		fetcher.fetch(imageData, 3);

		// expect
		Assert.assertEquals(ImageState.CORRUPTED, imageData.getState());
	}
		
	// FIXME: test not corresponding what expected yet
	@Test
	public void testUploadToSwiftFail() throws Exception {
		// FIXME: describe
		
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalExportPath = "fake-export-path";
		String containerName = "fake-container-name";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";
		String pseudFolder = "fake-pseudo-file";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", date,
				date, "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", date,
				date, "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper)
				.getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData, properties);
		Mockito.doReturn(sebalExportPath).when(fetcherHelper)
				.getRemoteImageResultsPath(imageData2, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData2, properties);
		Mockito.doReturn(0)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalExportPath, fetcherVolumePath, imageData);
		File fetcherVolumeResultsDir = new File(fetcherVolumePath);
		Mockito.doReturn(true).when(fetcherHelper)
				.resultsChecksumOK(imageData, fetcherVolumeResultsDir);
		
		Mockito.doReturn(0)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalExportPath, fetcherVolumePath, imageData2);
		Mockito.doReturn(true).when(fetcherHelper)
				.resultsChecksumOK(imageData2, fetcherVolumeResultsDir);
		
		Mockito.doReturn(pseudFolder).when(properties).getProperty(AppPropertiesConstants.SWIFT_PSEUD_FOLDER_PREFIX);
		Mockito.doReturn(containerName).when(properties).getProperty(AppPropertiesConstants.SWIFT_CONTAINER_NAME);

		String foo = pseudFolder + File.separator + fetcherVolumePath + File.separator;
		Mockito.doThrow(new Exception()).when(swiftClient).uploadFile(Mockito.eq(containerName), Mockito.any(File.class),
						Mockito.eq(foo));
		
		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
		// exercise
		fetcher.fetch(imageData, 3);
		fetcher.fetch(imageData2, 3);
		
		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FINISHED, imageData2.getState());
	}
	
	@Test
	public void testCheckSumFail() throws Exception {
		// FIXME: describe

		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", date,
				date, "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", date,
				date, "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper)
				.getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData, properties);
		Mockito.doReturn(sebalExportPath).when(fetcherHelper)
				.getRemoteImageResultsPath(imageData2, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData2, properties);
		Mockito.doReturn(0)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalExportPath, fetcherVolumePath, imageData);
		
		File fetcherVolumeResultsDir = new File(fetcherVolumePath);
		
		Mockito.doReturn(false).when(fetcherHelper)
				.resultsChecksumOK(imageData, fetcherVolumeResultsDir);

		Mockito.doReturn(0)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalExportPath, fetcherVolumePath, imageData2);
		Mockito.doReturn(true).when(fetcherHelper)
				.resultsChecksumOK(imageData2, fetcherVolumeResultsDir);
		
		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
		// exercise
		fetcher.fetch(imageData, 3);
		fetcher.fetch(imageData2, 3);
		
		// expect
		Assert.assertEquals(ImageState.CORRUPTED, imageData.getState());
		Assert.assertEquals(ImageState.FETCHING, imageData2.getState());
	}
}
