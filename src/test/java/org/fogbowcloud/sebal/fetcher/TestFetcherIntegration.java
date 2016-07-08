package org.fogbowcloud.sebal.fetcher;

import java.sql.Date;
import java.util.Properties;

import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.swift.SwiftClient;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestFetcherIntegration {
	
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
				ImageState.FINISHED, federationMember, 0, "NE", "NE",
				date, date, "");
		
		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Fetcher.SEBAL_EXPORT_PATH);
		Mockito.doReturn(fetcherVolumePath).when(properties)
		.getProperty(Fetcher.FETCHER_VOLUME_PATH);
		
		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP, ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
		Mockito.doReturn(1).when(ftpImpl).getFiles(properties, ftpServerIP, ftpServerPort, sebalExportPath, fetcherVolumePath, imageData);
		
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
				ImageState.FINISHED, federationMember, 0, "NE", "NE",
				date, date, "");
		
		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Fetcher.SEBAL_EXPORT_PATH);
		Mockito.doReturn(fetcherVolumePath).when(properties)
		.getProperty(Fetcher.FETCHER_VOLUME_PATH);
		
		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP, ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
		Mockito.doThrow(new NullPointerException()).when(ftpImpl).getFiles(properties, null, ftpServerPort, null,
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
				.getProperty(Fetcher.FETCHER_VOLUME_PATH);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);
		Mockito.doReturn(false).when(fetcherHelper).isThereFetchedFiles(sebalExportPath);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());

		// exercise
		fetcher.finishFetch(imageData);

		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
	}
}
