package org.fogbowcloud.sebal.fetcher;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
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

		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Fetcher.SEBAL_EXPORT_PATH);
		Mockito.doReturn(fetcherVolumePath).when(properties)
				.getProperty(FetcherHelper.FETCHER_VOLUME_PATH);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doReturn(1)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalExportPath, fetcherVolumePath, imageData);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());

		// exercise
		fetcher.fetch(imageData, 0);

		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
	}

	@Test
	public void testNullPointerExceptionInFetch() throws Exception {
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

		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Fetcher.SEBAL_EXPORT_PATH);
		Mockito.doReturn(fetcherVolumePath).when(properties)
				.getProperty(FetcherHelper.FETCHER_VOLUME_PATH);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doThrow(new NullPointerException())
				.when(ftpImpl)
				.getFiles(properties, null, ftpServerPort, null,
						fetcherVolumePath, imageData);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());

		// exercise
		fetcher.fetch(imageData, 0);

		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
	}

	@Test
	public void testNoResultFilesToFetch() throws Exception {
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

		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Fetcher.SEBAL_EXPORT_PATH);
		Mockito.doReturn(fetcherVolumePath).when(properties)
				.getProperty(FetcherHelper.FETCHER_VOLUME_PATH);

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

		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Fetcher.SEBAL_EXPORT_PATH);
		Mockito.doReturn(fetcherVolumePath).when(properties)
				.getProperty(FetcherHelper.FETCHER_VOLUME_PATH);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doThrow(new SQLException()).when(imageStore).getIn(ImageState.FINISHED);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FINISHED, imageData2.getState());
		
		// exercise
		fetcher.imagesToFetch();
		
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FINISHED, imageData2.getState());
	}
	
	@Test
	public void testFetcherUpdateImage() throws SQLException, IOException {
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
		
		Assert.assertNotEquals(ImageState.FINISHED, imageData.getState());
		Assert.assertEquals(ImageState.FETCHING, imageData2.getState());
	}
	
	// FIXME: addStateStamp is not throwing error
	@Test
	public void testAddStateStampFail() throws SQLException, IOException {
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

		Assert.assertEquals(ImageState.FETCHING, imageData.getState());
		Assert.assertEquals(ImageState.FETCHING, imageData2.getState());
	}
}
