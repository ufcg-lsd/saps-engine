package org.fogbowcloud.sebal.engine.sebal.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.blowout.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.sebal.engine.sebal.FMask;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.USGSNasaRepository;
import org.fogbowcloud.sebal.engine.sebal.crawler.Crawler;
import org.fogbowcloud.sebal.engine.swift.SwiftClient;
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
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);

		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
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
		// When there is no file to fetch in finishFetch
		// then
		// finishFetch must roll back image from FETCHING to FINISHED
		
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
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(sebalExportPath).when(fetcherHelper).getRemoteImageResultsPath(imageData, properties);
		Mockito.doReturn(fetcherVolumePath).when(fetcherHelper).getLocalImageResultsPath(imageData, properties);

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
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
		// When no image is returned in getImageToFetch call
		// then
		// will be returned an empty list of images
		// no image will be set to FETCHING
		// the two images (that were not able to be in list) will remain as FINISHED
		
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
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

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
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
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
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		DB pendingImageFetchDB = Mockito.mock(DB.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
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

		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
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
	
	@Test
	public void testMaxTriesReached() throws Exception {
		// When Fetcher reaches maximum fetch tries for an image
		// then
		// it must set image state from FETCHING to CORRUPTED
		// delete all result files from disk
		
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
				ImageState.FETCHING, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
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
	
	//FIXME: Test does not get actual file mocks, so it does not return what is expected (states: finished, finished)
	@Test
	public void testFailWhileUploadingToSwift() throws Exception {
		// When Fetcher fails to upload image results to swift
		// then
		// it must try again for MAX_SWIFT_UPLOAD_TRIES
		// (if not succeed) roll back image from FETCHING to FINISHED
		// delete results from disk
		
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
				ImageState.FETCHING, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

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
		//Assert.assertEquals(ImageState.FINISHED, imageData.getState());
		//Assert.assertEquals(ImageState.FINISHED, imageData2.getState());
	}
	
	@Test
	public void testCheckSumFail() throws Exception {
		// When checksum does not match for a given image result file
		// then
		// it must try again for MAX_FETCH_TRIES
		// (if not succeed) roll back image from FETCHING to FINISHED
		// delete results from disk

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
				ImageState.FETCHING, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");
		ImageData imageData2 = new ImageData("image2", "link2",
				ImageState.FETCHING, federationMember, 0, "NE", "NE", "NE", "NE",
				"NE", "NE", "NE", new Timestamp(date.getTime()), new Timestamp(
						date.getTime()), "available", "");

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
		
		Mockito.doReturn(imageData).when(imageStore).getImage(imageData.getName());
		Mockito.doReturn(false).when(fetcherHelper)
				.resultsChecksumOK(imageData, fetcherVolumeResultsDir);

		Mockito.doReturn(0)
				.when(ftpImpl)
				.getFiles(properties, ftpServerIP, ftpServerPort,
						sebalExportPath, fetcherVolumePath, imageData2);
		Mockito.doReturn(imageData2).when(imageStore).getImage(imageData2.getName());
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
	
	@Test
	public void testGetSebalVersion() throws FileNotFoundException, UnsupportedEncodingException {
		// setup
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		Properties properties = Mockito.mock(Properties.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		String fakeLocalImageDirPath = System.getProperty("user.dir");
		
		PrintWriter writer = new PrintWriter("SEBAL.version.VERSION", "UTF-8");
		writer.println("VERSION");
		writer.close();
		
		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
		// exercise
		String sebalVersion = fetcher.getSebalVersion(fakeLocalImageDirPath);
		
		// expect
		Assert.assertEquals("VERSION", sebalVersion);
		
		File file = new File("SEBAL.version.VERSION");
		file.delete();
	}
	
//	@Test
//	// TODO: run this test
//	public void testDeleteFilesFromSwift() throws Exception {
//		// setup
//		Properties properties = new Properties();
//		FileInputStream input = new FileInputStream("config/sebal.conf");
//		properties.load(input);
//		
//		SwiftClient swiftClient = new SwiftClient(properties);
//		
//		File tempFile1 = folder.newFile("file1.nc");
//		File tempFile2 = folder.newFile("file2.nc");
//		
//		FileInputStream file1InputStream = new FileInputStream(tempFile1);
//		FileInputStream file2InputStream = new FileInputStream(tempFile2);
//		
//		String checkSum1 = DigestUtils.md5Hex(IOUtils
//				.toByteArray(file1InputStream));
//		String checkSum2 = DigestUtils.md5Hex(IOUtils
//				.toByteArray(file2InputStream));
//		
//		file1InputStream.close();
//		file2InputStream.close();
//		
//		folder.newFile(tempFile1.getName() + "." + checkSum1 + ".md5");
//		folder.newFile(tempFile2.getName() + "." + checkSum2 + ".md5");
//		
//		String containerName = "container-test";
//		String pseudFolder = "test/images";
//		
//		swiftClient.createContainer(containerName);
//		
//		swiftClient.uploadFile(containerName, tempFile1, pseudFolder);
//		swiftClient.uploadFile(containerName, tempFile2, pseudFolder);
//		
//		Date date = new Date(Calendar
//				.getInstance().getTimeInMillis());			
//		
//		ImageData imageData1 = new ImageData(tempFile1.getName(), "",
//				ImageState.FETCHED, "", 0, "", "", "", "", "", "",
//				new Timestamp(date.getTime()), new Timestamp(date.getTime()),
//				"");
//		ImageData imageData2 = new ImageData(tempFile2.getName(), "",
//				ImageState.FETCHED, "", 0, "", "", "", "", "", "",
//				new Timestamp(date.getTime()), new Timestamp(date.getTime()),
//				"");		
//		
//		Fetcher fetcher = Mockito.mock(Fetcher.class);			
//		
//		// exercise
//		fetcher.deleteFilesFromSwift(imageData1, properties);
//		fetcher.deleteFilesFromSwift(imageData2, properties);
//		
//		// expect
//		Assert.assertTrue(swiftClient.isContainerEmpty(containerName));
//		
//		// clean environment
//		swiftClient.deleteContainer(containerName);
//	}
	
	@Test
	public void testGetFetcherVersion() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		FTPIntegrationImpl ftpImpl = Mockito.mock(FTPIntegrationImpl.class);
		FetcherHelper fetcherHelper = Mockito.mock(FetcherHelper.class);
		SwiftClient swiftClient = Mockito.mock(SwiftClient.class);
		String ftpServerIP = "fake-IP";
		String ftpServerPort = "fake-PORT";
		
		PrintWriter writer = new PrintWriter("sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a", "UTF-8");
		writer.println("0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		writer.close();
		
		Fetcher fetcher = new Fetcher(properties, imageStore, ftpServerIP,
				ftpServerPort, swiftClient, ftpImpl, fetcherHelper);
		
		// exercise
		String versionReturn = fetcher.getFetcherVersion();
		
		// expect
		Assert.assertEquals("0c26f092e976389c593953a1ad8ddaadb5c2ab2a", versionReturn);
		
		File file = new File("sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a");		
		file.delete();
	}
}
