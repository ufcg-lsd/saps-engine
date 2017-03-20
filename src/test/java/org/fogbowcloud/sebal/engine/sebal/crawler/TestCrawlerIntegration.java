package org.fogbowcloud.sebal.engine.sebal.crawler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.fogbowcloud.sebal.engine.sebal.FMask;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.USGSNasaRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TestCrawlerIntegration {
	
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
	public void testReSubmitImages() throws SQLException {
		
		// set up
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		ImageDataStore imageStore = mock(ImageDataStore.class);
		FMask fmask = mock(FMask.class);
		String crawlerIp = "fake-crawler-ip";
		String nfsPort = "fake-nfs-port";
		String oldDownloadLinkOne = "old-download-link-one";
		String oldDownloadLinkTwo = "old-download-link-two";
		String oldDownloadLinkThree = "old-download-link-three";
		
		String newDownloadLinkTwo = "new-download-link-two";
		
		Date date = new Date(10000854);

		ImageData errorImageOne = new ImageData("error-image-one",
				oldDownloadLinkOne, ImageState.ERROR,
				SebalPropertiesConstants.LSD_FEDERATION_MEMBER, 0,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), ImageData.NON_EXISTENT,
				"fake-error-msg-one");
		ImageData errorImageTwo = new ImageData("error-image-two",
				oldDownloadLinkTwo, ImageState.ERROR,
				SebalPropertiesConstants.AZURE_FEDERATION_MEMBER, 0,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), ImageData.NON_EXISTENT,
				"fake-error-msg-two");
		ImageData errorImageThree = new ImageData("error-image-three",
				oldDownloadLinkThree, ImageState.ERROR, "rnp-federation", 0,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, ImageData.NON_EXISTENT,
				ImageData.NON_EXISTENT, new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), ImageData.NON_EXISTENT,
				"fake-error-msg-three");
		
		List<ImageData> errorImages = new ArrayList<ImageData>();
		errorImages.add(errorImageOne);
		errorImages.add(errorImageTwo);
		errorImages.add(errorImageThree);

		File imageOneDir = mock(File.class);
		File imageTwoDir = mock(File.class);

		Crawler crawler = spy(new Crawler(properties, imageStore,
				usgsRepository, crawlerIp, nfsPort,
				SebalPropertiesConstants.LSD_FEDERATION_MEMBER, fmask));
		
		doReturn(errorImages).when(imageStore).getIn(ImageState.ERROR);
		
		doReturn(imageOneDir).when(crawler).getImageDir(properties, errorImageOne);
		doReturn(true).when(crawler).isThereImageInputs(imageOneDir);
		doReturn(errorImageOne).when(imageStore).getImage(errorImageOne.getName());
		doNothing().when(imageStore).updateImage(errorImageOne);
		doNothing().when(imageStore).addStateStamp(errorImageOne.getName(), errorImageOne.getState(), errorImageOne.getUpdateTime());

		doReturn(imageTwoDir).when(crawler).getImageDir(properties, errorImageTwo);
		doReturn(false).when(crawler).isThereImageInputs(imageTwoDir);
		doReturn(newDownloadLinkTwo).when(usgsRepository).getImageDownloadLink(errorImageTwo.getName());		
		doReturn(errorImageTwo).when(imageStore).getImage(errorImageTwo.getName());
		doNothing().when(imageStore).updateImage(errorImageTwo);
		doNothing().when(imageStore).addStateStamp(errorImageTwo.getName(), errorImageTwo.getState(), errorImageTwo.getUpdateTime());
		
		doReturn("10").when(properties).getProperty(SebalPropertiesConstants.MAX_USGS_DOWNLOAD_LINK_REQUESTS);
		
		// exercise
		crawler.reSubmitErrorImages(properties);
		
		// expect
		Assert.assertEquals(ImageState.DOWNLOADED, errorImageOne.getState());
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, errorImageTwo.getState());
		Assert.assertEquals(ImageState.ERROR, errorImageThree.getState());
		
		Assert.assertEquals(oldDownloadLinkOne, errorImageOne.getDownloadLink());
		Assert.assertNotEquals(oldDownloadLinkTwo, errorImageTwo.getDownloadLink());
		Assert.assertEquals(oldDownloadLinkThree, errorImageThree.getDownloadLink());
	}

	@Test
	public void testStepOverImageWhenDownloadFails() throws SQLException,
			IOException, InterruptedException {

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
		String crawlerIP = "fake-crawler-ip";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		FMask fmask = mock(FMask.class);
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.NOT_DOWNLOADED, federationMember, 0, "NE", "NE",
				"NE", "NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.NOT_DOWNLOADED, federationMember, 1, "NE", "NE",
				"NE", "NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");

		imageList.add(image1);
		imageList.add(image2);

		doReturn(imageList).when(imageStore).getImagesToDownload(
				federationMember, maxImagesToDownload);
		doReturn(image1).when(imageStore).getImage(image1.getName());
		doReturn(image2).when(imageStore).getImage(image2.getName());
		doThrow(new IOException()).when(usgsRepository).downloadImage(image1);
		doNothing().when(usgsRepository).downloadImage(image2);

		Crawler crawler = new Crawler(properties, imageStore, usgsRepository,
				crawlerIP, nfsPort, federationMember, fmask);
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image1.getState());
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image2.getState());
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());

		// exercise
		crawler.download(maxImagesToDownload);

		// expect
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image1.getState());
		Assert.assertEquals(ImageState.DOWNLOADED, image2.getState());
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());
	}

	@Test
	public void testErrorOnFmask() throws SQLException, IOException,
			InterruptedException {
		// 1. we have 2 NOT_DOWNLOADED images
		// 2. the 2 images are downloaded
		// 2. run fmask for both images
		// 3. fmask return error in execution of image 1
		// 4. image 1 is marked as with error and is put to ERROR state along
		// with an error message
		// 5. image 2 is downloaded and marked as DOWNLOADED

		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		FMask fmask = mock(FMask.class);
		String crawlerIP = "fake-crawler-ip";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		String fmaskScriptPath = "fake-script-path";
		String fmaskToolsPath = "fake-tool-path";
		String sebalExportPath = "fake-export-path";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.NOT_DOWNLOADED, federationMember, 0, "NE", "NE",
				"NE", "NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.NOT_DOWNLOADED, federationMember, 1, "NE", "NE",
				"NE", "NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");

		imageList.add(image1);
		imageList.add(image2);

		doReturn(fmaskScriptPath).when(properties).getProperty(
				SebalPropertiesConstants.FMASK_SCRIPT_PATH);
		doReturn(fmaskToolsPath).when(properties).getProperty(
				SebalPropertiesConstants.FMASK_TOOL_PATH);
		doReturn(sebalExportPath).when(properties).getProperty(
				SebalPropertiesConstants.SEBAL_EXPORT_PATH);

		doReturn(imageList).when(imageStore).getImagesToDownload(
				federationMember, maxImagesToDownload);
		doReturn(image1).when(imageStore).getImage(image1.getName());
		doNothing().when(usgsRepository).downloadImage(image1);
		doReturn(1).when(fmask).runFmask(image1, fmaskScriptPath,
				fmaskToolsPath, sebalExportPath);

		doReturn(image2).when(imageStore).getImage(image2.getName());
		doNothing().when(usgsRepository).downloadImage(image2);
		doReturn(0).when(fmask).runFmask(image2, fmaskScriptPath,
				fmaskToolsPath, sebalExportPath);

		Crawler crawler = new Crawler(properties, imageStore, usgsRepository,
				crawlerIP, nfsPort, federationMember, fmask);
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image1.getState());
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image2.getState());
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());

		// exercise
		crawler.download(maxImagesToDownload);

		// expect
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());
		Assert.assertEquals(ImageState.ERROR, image1.getState());
		Assert.assertEquals(ImageState.DOWNLOADED, image2.getState());
	}

	@Test
	public void testCrawlerErrorWhileGetImagesNotDownloaded()
			throws SQLException, IOException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		FMask fmask = mock(FMask.class);
		String crawlerIP = "fake-crawler-ip";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.NOT_DOWNLOADED, federationMember, 0, "NE", "NE",
				"NE", "NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.NOT_DOWNLOADED, federationMember, 1, "NE", "NE",
				"NE", "NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");

		imageList.add(image1);
		imageList.add(image2);

		Crawler crawler = new Crawler(properties, imageStore, usgsRepository,
				crawlerIP, nfsPort, federationMember, fmask);

		doThrow(new SQLException()).when(imageStore).getImagesToDownload(
				federationMember, maxImagesToDownload);
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());

		// exercise
		crawler.download(maxImagesToDownload);

		// expect
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image1.getState());
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image2.getState());
	}

	@Test
	public void testPurgeImagesFromVolume() throws SQLException, IOException,
			InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		FMask fmask = mock(FMask.class);
		String crawlerIP = "fake-crawler-ip";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE", "NE",
				"NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");
		image1.setImageStatus(ImageData.PURGED);
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 1, "NE", "NE", "NE",
				"NE", "NE", "NE", "NE", new Timestamp(date.getTime()),
				new Timestamp(date.getTime()), "available", "");

		imageList.add(image1);
		imageList.add(image2);

		doReturn(imageList).when(imageStore).getIn(ImageState.FINISHED);

		doReturn(sebalExportPath).when(properties).getProperty(
				SebalPropertiesConstants.SEBAL_EXPORT_PATH);

		Crawler crawler = new Crawler(properties, imageStore, usgsRepository,
				crawlerIP, nfsPort, federationMember, fmask);

		// exercise
		crawler.purgeImagesFromVolume(properties);
	}

	@Test
	public void testFederationMemberCheck() throws SQLException, IOException,
			InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		FMask fmask = mock(FMask.class);
		String crawlerIP = "fake-crawler-ip";
		String nfsPort = "fake-nfs-port";
		String federationMember1 = "fake-fed-member-1";
		String federationMember2 = "fake-fed-member-2";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1", ImageState.FETCHED,
				federationMember1, 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");
		ImageData image2 = new ImageData("image2", "link2", ImageState.FETCHED,
				federationMember2, 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		imageList.add(image1);
		imageList.add(image2);

		doReturn(sebalExportPath).when(properties).getProperty(
				SebalPropertiesConstants.SEBAL_EXPORT_PATH);

		doReturn(imageList).when(imageStore).getAllImages();

		Crawler crawler = new Crawler(properties, imageStore, usgsRepository,
				crawlerIP, nfsPort, federationMember1, fmask);

		// exercise
		crawler.deleteFetchedResultsFromVolume(properties);

		// expect
		Assert.assertNotEquals(image1.getFederationMember(),
				image2.getFederationMember());
	}

	@Test
	public void testGetCrawlerVersion() throws SQLException, IOException,
			InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		FMask fmask = mock(FMask.class);
		String crawlerIP = "fake-crawler-ip";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		PrintWriter writer = new PrintWriter(
				"sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a",
				"UTF-8");
		writer.println("0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		writer.close();

		Crawler crawler = new Crawler(properties, imageStore, usgsRepository,
				crawlerIP, nfsPort, federationMember, fmask);

		// exercise
		String versionReturn = crawler.getCrawlerVersion();

		// expect
		Assert.assertEquals("0c26f092e976389c593953a1ad8ddaadb5c2ab2a",
				versionReturn);

		File file = new File(
				"sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		file.delete();
	}
}
