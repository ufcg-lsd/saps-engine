package org.fogbowcloud.sebal.crawler;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.sebal.FMask;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestCrawlerIntegration {

	@Before
	public void clean() {
		String pendingImageFileName = "pending-image-download.db";
		File pendingImageDBFile = new File(pendingImageFileName);

		if (pendingImageDBFile.exists()) {
			FileUtils.deleteQuietly(pendingImageDBFile);
		}
	}

	@Test
	public void testStepOverImageWhenDownloadFails() throws SQLException,
			IOException {

		// 1. we have 2 NOT_DOWNLOADED images, pendingDB is empty
		// 2. we proceed to download them
		// 3. we face a download error for the first. then we step over
		// downloading it
		// 4. we are able to download the second one
		// 5. in the end, we shall 1 DOWNLOADED and 1 NOT_DOWNLOADED and the
		// pendingDB is empty

		// setup
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		NASARepository nasaRepository = Mockito.mock(NASARepository.class);
		String federationMember = "fake-fed-member";
		FMask fmask = Mockito.mock(FMask.class);
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.NOT_DOWNLOADED, federationMember, 0, "NE", "NE",
				date, date, "");
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.NOT_DOWNLOADED, federationMember, 1, "NE", "NE",
				date, date, "");

		imageList.add(image1);
		imageList.add(image2);

		Mockito.doReturn(imageList).when(imageStore)
				.getImagesToDownload(federationMember, maxImagesToDownload);
		Mockito.doThrow(new IOException()).when(nasaRepository)
				.downloadImage(image1);
		Mockito.doNothing().when(nasaRepository).downloadImage(image2);

		Crawler crawler = new Crawler(properties, imageStore, nasaRepository,
				federationMember, fmask);
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
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		NASARepository nasaRepository = Mockito.mock(NASARepository.class);
		FMask fmask = Mockito.mock(FMask.class);
		String federationMember = "fake-fed-member";
		String fmaskScriptPath = "fake-script-path";
		String fmaskToolsPath = "fake-tool-path";
		String sebalExportPath = "fake-export-path";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.NOT_DOWNLOADED, federationMember, 0, "NE", "NE",
				date, date, "");
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.NOT_DOWNLOADED, federationMember, 1, "NE", "NE",
				date, date, "");

		imageList.add(image1);
		imageList.add(image2);

		Mockito.doReturn(fmaskScriptPath).when(properties)
				.getProperty(Crawler.FMASK_SCRIPT_PATH);
		Mockito.doReturn(fmaskToolsPath).when(properties)
				.getProperty(Crawler.FMASK_TOOL_PATH);
		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Crawler.SEBAL_EXPORT_PATH);

		Mockito.doReturn(imageList).when(imageStore)
				.getImagesToDownload(federationMember, maxImagesToDownload);
		Mockito.doNothing().when(nasaRepository).downloadImage(image1);
		Mockito.doReturn(1)
				.when(fmask)
				.runFmask(image1, fmaskScriptPath, fmaskToolsPath,
						sebalExportPath);

		Mockito.doNothing().when(nasaRepository).downloadImage(image2);
		Mockito.doReturn(0)
				.when(fmask)
				.runFmask(image2, fmaskScriptPath, fmaskToolsPath,
						sebalExportPath);

		Crawler crawler = new Crawler(properties, imageStore, nasaRepository,
				federationMember, fmask);
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
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		NASARepository nasaRepository = Mockito.mock(NASARepository.class);
		FMask fmask = Mockito.mock(FMask.class);
		String federationMember = "fake-fed-member";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.NOT_DOWNLOADED, federationMember, 0, "NE", "NE",
				date, date, "");
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.NOT_DOWNLOADED, federationMember, 1, "NE", "NE",
				date, date, "");

		imageList.add(image1);
		imageList.add(image2);

		Crawler crawler = new Crawler(properties, imageStore, nasaRepository,
				federationMember, fmask);

		Mockito.doThrow(new SQLException()).when(imageStore)
				.getImagesToDownload(federationMember, maxImagesToDownload);
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());

		// exercise
		crawler.download(maxImagesToDownload);

		// expect
		Assert.assertTrue(crawler.pendingImageDownloadMap.isEmpty());
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image1.getState());
		Assert.assertEquals(ImageState.NOT_DOWNLOADED, image2.getState());
	}

	@Test
	public void testUpdateImageDataInDB() throws SQLException, IOException,
			InterruptedException {
		// setup
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		NASARepository nasaRepository = Mockito.mock(NASARepository.class);
		FMask fmask = Mockito.mock(FMask.class);
		String federationMember = "fake-fed-member";
		String fmaskScriptPath = "fake-script-path";
		String fmaskToolsPath = "fake-tool-path";
		String sebalExportPath = "fake-export-path";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.NOT_DOWNLOADED, federationMember, 0, "NE", "NE",
				date, date, "");
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.NOT_DOWNLOADED, federationMember, 1, "NE", "NE",
				date, date, "");

		imageList.add(image1);
		imageList.add(image2);

		Mockito.doReturn(imageList).when(imageStore).getImagesToDownload(federationMember, maxImagesToDownload);
		
		Mockito.doNothing().when(nasaRepository).downloadImage(image1);
		Mockito.doReturn(0).when(fmask).runFmask(image1, fmaskScriptPath, fmaskToolsPath,
						sebalExportPath);
		Mockito.doThrow(new SQLException()).when(imageStore).updateImage(image1);
		
		Mockito.doNothing().when(nasaRepository).downloadImage(image1);		
		Mockito.doReturn(0).when(fmask).runFmask(image2, fmaskScriptPath, fmaskToolsPath,
				sebalExportPath);
		Mockito.doNothing().when(imageStore).updateImage(image2);
		
		Crawler crawler = new Crawler(properties, imageStore, nasaRepository,
				federationMember, fmask);

		// exercise
		crawler.download(maxImagesToDownload);

		// expect
		Assert.assertEquals(ImageState.DOWNLOADING, image1.getState());
		Assert.assertEquals(ImageState.DOWNLOADED, image2.getState());
	}
	
	@Test
	public void testPurgeImagesFromVolume() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		NASARepository nasaRepository = Mockito.mock(NASARepository.class);
		FMask fmask = Mockito.mock(FMask.class);
		String federationMember = "fake-fed-member";
		String sebalExportPath = "fake-export-path";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageData> imageList = new ArrayList<ImageData>();
		ImageData image1 = new ImageData("image1", "link1",
				ImageState.FINISHED, federationMember, 0, "NE", "NE",
				date, date, "");
		image1.setImageStatus(ImageData.PURGED);
		ImageData image2 = new ImageData("image2", "link2",
				ImageState.FINISHED, federationMember, 1, "NE", "NE",
				date, date, "");

		imageList.add(image1);
		imageList.add(image2);

		Mockito.doReturn(imageList).when(imageStore)
				.getIn(ImageState.FINISHED);
		
		Mockito.doReturn(sebalExportPath).when(properties)
				.getProperty(Crawler.SEBAL_EXPORT_PATH);
		
		Crawler crawler = new Crawler(properties, imageStore, nasaRepository,
				federationMember, fmask);

		// exercise
		crawler.purgeImagesFromVolume(properties);
	}
}
