package org.fogbowcloud.sebal.engine.sebal.fetcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.swift.SwiftAPIClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapdb.DB;
import org.mockito.Mockito;

public class TestFetcherIntegration {
	
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
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		Properties properties = Mockito.mock(Properties.class);
		String federationMember = "fake-federation-member";
		String fetcherVolumeInputPath = "fake-fetcher-volume-input-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(fetcherVolumeInputPath).when(fetcherHelper).getLocalImageInputsPath(imageData, properties);

		Fetcher fetcher = Mockito.mock(Fetcher.class);
		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
		Mockito.doReturn(1).when(fetcher).fetchInputs(imageData);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());

		// exercise
		fetcher.fetch(imageData);

		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
	}

	@Test
	public void testNoResultFilesToFetch() throws Exception {
		// When there is no file to fetch in finishFetch
		// then
		// finishFetch must roll back image from FETCHING to FINISHED
		
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftAPIClient swiftAPIClient = Mockito.mock(SwiftAPIClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, swiftAPIClient, ftpImpl, fetcherHelper);
		
		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData, properties);
		Mockito.doReturn(false).when(fetcherHelper)
				.isThereFetchedResultFiles(sebalExportPath);

		Assert.assertEquals(ImageState.FINISHED, imageData.getState());

		// exercise
		fetcher.finishFetch(imageData);

		// expect
		Assert.assertEquals(ImageState.FINISHED, imageData.getState());
	}
	
	@Test
	public void testFetcherErrorWhileGettingImagesToFetch() throws SQLException, IOException {
		// When no image is returned in getImageToFetch call
		// then
		// will be returned an empty list of images
		// no image will be set to FETCHING
		// the two images (that were not able to be in list) will remain as FINISHED
		
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftAPIClient swiftAPIClient = Mockito.mock(SwiftAPIClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String sebalExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumePath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, swiftAPIClient, ftpImpl, fetcherHelper);

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
		SwiftAPIClient swiftAPIClient = Mockito.mock(SwiftAPIClient.class);
		Properties properties = Mockito.mock(Properties.class);
		DB pendingImageFetchDB = Mockito.mock(DB.class);
		String federationMember = "fake-federation-member";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Fetcher fetcher = new Fetcher(properties, imageStore, swiftAPIClient, ftpImpl, fetcherHelper);
		
		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());

		Mockito.doReturn(true).when(imageStore).lockImage(imageData.getName());
		Mockito.doThrow(new SQLException()).when(imageStore)
				.updateImage(imageData);
		
		Mockito.doReturn(imageData2).when(imageStore).getImage(imageData2.getName());
		
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
		// When the update of image state and stamp into DB fail
		// then
		// prepareToFetch must try to roll back image from FETCHING to FINISHED
		// (if it not succeed) prepareToFetch must leave image state as FETCHING for posterity
		
		// setup
		ConcurrentMap<String, ImageData> pendingImageFetchMap = Mockito
				.mock(ConcurrentMap.class);
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftAPIClient swiftAPIClient = Mockito.mock(SwiftAPIClient.class);
		Properties properties = Mockito.mock(Properties.class);
		DB pendingImageFetchDB = Mockito.mock(DB.class);
		String federationMember = "fake-federation-member";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Fetcher fetcher = new Fetcher(properties, imageStore, swiftAPIClient, ftpImpl, fetcherHelper);
		
		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
		Mockito.doReturn(true).when(imageStore).lockImage(imageData.getName());
		Mockito.doNothing().when(imageStore).updateImage(imageData);
		Mockito.doThrow(new SQLException()).when(imageStore)
				.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());

		Mockito.doReturn(imageData2).when(imageStore).getImage(imageData2.getName());
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
	
	// FIXME: image state is FETCHING and not CORRUPTED at the end of the test
	@Test
	public void testMaxTriesReached() throws Exception {
		// When Fetcher reaches maximum fetch tries for an image
		// then
		// it must set image state from FETCHING to CORRUPTED
		// delete all result files from disk
		
		// setup
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		Properties properties = Mockito.mock(Properties.class);
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		String ftpServerIP = "fake-ftp-server-ip";
		String ftpServerPort = "fake-ftp-server-port";
		String sebalResultsExportPath = "fake-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumeOutputPath = "fake-fetcher-volume-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
		Mockito.doReturn(sebalResultsExportPath).when(fetcherHelper)
				.getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumeOutputPath).when(fetcherHelper)
				.getLocalImageResultsPath(imageData, properties);
		Mockito.doReturn(0).when(ftpImpl).getFiles(properties, ftpServerIP, ftpServerPort, sebalResultsExportPath, fetcherVolumeOutputPath, imageData);
		Mockito.doReturn(false).when(fetcherHelper).resultsChecksumOK(imageData, new File(fetcherVolumeOutputPath));
		Mockito.doReturn(false).when(fetcherHelper).isThereFetchedResultFiles(fetcherVolumeOutputPath);

		Fetcher fetcher = Mockito.mock(Fetcher.class);
		
		Mockito.doReturn(0).when(fetcher).fetchInputs(imageData);

		// exercise
		fetcher.fetch(imageData);

		// expect
		Assert.assertEquals(ImageState.CORRUPTED, imageData.getState());
	}
	
	@Test
	public void testFailWhileUploadingToSwift() throws Exception {
		// When Fetcher fails to upload image results to swift
		// then
		// it must try again for MAX_SWIFT_UPLOAD_TRIES
		// (if not succeed) roll back image from FETCHING to FINISHED
		// delete results from disk
		
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		Properties properties = Mockito.mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String sebalInputExportPath = "fake-input-export-path";
		String federationMember = "fake-federation-member";
		String fetcherVolumeInputPath = "fake-fetcher-volume-input-path";

		Date date = Mockito.mock(Date.class);

		ImageData imageData = new ImageData("image1", "link1",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(sebalInputExportPath).when(fetcherHelper)
				.getRemoteImageInputsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumeInputPath).when(fetcherHelper)
				.getLocalImageInputsPath(imageData, properties);
		Mockito.doReturn(0)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalInputExportPath, fetcherVolumeInputPath, imageData);
		File fetcherVolumeInputsDir = Mockito.mock(File.class);
		Mockito.doReturn(true).when(fetcherHelper)
				.resultsChecksumOK(imageData, fetcherVolumeInputsDir);
		
		Fetcher fetcher = Mockito.mock(Fetcher.class);
		
		Mockito.doReturn(false).when(fetcher).uploadInputFilesToSwift(imageData, fetcherVolumeInputsDir);

		// exercise
		fetcher.fetch(imageData);
		
		// expect
		Assert.assertEquals(ImageState.FETCHING, imageData.getState());
	}
	
	@Test
	public void testGetFetcherVersion() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftAPIClient swiftAPIClient = Mockito.mock(SwiftAPIClient.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		
		PrintWriter writer = new PrintWriter("sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a", "UTF-8");
		writer.println("0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		writer.close();
		
		Fetcher fetcher = new Fetcher(properties, imageStore, swiftAPIClient, ftpImpl, fetcherHelper);
		
		// exercise
		String versionReturn = fetcher.getFetcherVersion();
		
		// expect
		Assert.assertEquals("0c26f092e976389c593953a1ad8ddaadb5c2ab2a", versionReturn);
		
		File file = new File("sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a");		
		file.delete();
	}
}
