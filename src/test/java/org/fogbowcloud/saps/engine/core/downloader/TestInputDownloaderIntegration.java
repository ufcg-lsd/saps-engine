package org.fogbowcloud.saps.engine.core.downloader;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

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
		String crawlerIP = "fake-crawler-ip";
		String crawlerPort = "fake-crawler-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask image1 = new ImageTask("task-id-1", "image1", "link1", ImageTaskState.CREATED,
				federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "",
				"None");

		imageList.add(image1);

		// aqui a imagelist tem uma imagem, mas imageStore ainda tá vazio, porque os testes estão passando?

		List<ImageTask> emptyImageList = new ArrayList<ImageTask>();

		doReturn(emptyImageList).when(imageStore).getImagesToDownload(federationMember,
				maxImagesToDownload);
		doReturn(imageList).when(imageStore).getImagesToDownload(federationMember,
				maxImagesToDownload);

		doReturn(image1).when(imageStore).getTask(image1.getName());

//		doReturn("link-1").when(usgsRepository).getImageDownloadLink(image1.getName());
//		doThrow(new IOException()).when(usgsRepository).downloadImage(image1);

		InputDownloader crawler = new InputDownloader(properties, imageStore,
				crawlerIP, crawlerPort, nfsPort, federationMember);
		Assert.assertEquals(ImageTaskState.CREATED, image1.getState());
		Assert.assertTrue(crawler.pendingTaskDownloadMap.isEmpty());

		// exercise
		crawler.download();

		// expect
		Assert.assertEquals(ImageTaskState.CREATED, image1.getState());
		Assert.assertTrue(crawler.pendingTaskDownloadMap.isEmpty());
	}

	@Test
	public void testCrawlerErrorWhileGetImagesNotDownloaded() throws SQLException, IOException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String crawlerIP = "fake-crawler-ip";
		String crawlerPort = "fake-crawler-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		int maxImagesToDownload = 5;

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask image1 = new ImageTask("task-id-1", "image1", "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "", "None");
		ImageTask image2 = new ImageTask("task-id-2", "image2", "link2",
				ImageTaskState.CREATED, federationMember, 1, "NE", "NE", "NE", "NE", "NE",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "", "None");

		imageList.add(image1);
		imageList.add(image2);

		InputDownloader crawler = new InputDownloader(properties, imageStore,
				crawlerIP, crawlerPort, nfsPort, federationMember);

		doThrow(new SQLException()).when(imageStore).getImagesToDownload(federationMember,
				maxImagesToDownload);
		Assert.assertTrue(crawler.pendingTaskDownloadMap.isEmpty());

		// exercise
		crawler.download();

		// expect
		Assert.assertTrue(crawler.pendingTaskDownloadMap.isEmpty());
		Assert.assertEquals(ImageTaskState.CREATED, image1.getState());
		Assert.assertEquals(ImageTaskState.CREATED, image2.getState());
	}

	@Test
	public void testPurgeImagesFromVolume() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String crawlerIP = "fake-crawler-ip";
		String crawlerPort = "fake-crawler-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask image1 = new ImageTask("task-id-1", "image1", "link1", ImageTaskState.FINISHED,
				federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE", new Timestamp(
						date.getTime()), new Timestamp(date.getTime()), "available", "", "None");
		image1.setImageStatus(ImageTask.PURGED);
		ImageTask image2 = new ImageTask("task-id-2", "image2", "link2", ImageTaskState.FINISHED,
				federationMember, 1, "NE", "NE", "NE", "NE", "NE", "NE", "NE", new Timestamp(
						date.getTime()), new Timestamp(date.getTime()), "available", "", "None");

		imageList.add(image1);
		imageList.add(image2);

		doReturn(imageList).when(imageStore).getIn(ImageTaskState.FINISHED);

		doReturn(sebalExportPath).when(properties).getProperty(
				SapsPropertiesConstants.SEBAL_EXPORT_PATH);

		InputDownloader crawler = new InputDownloader(properties, imageStore,
				crawlerIP, crawlerPort, nfsPort, federationMember);

		// exercise
		crawler.purgeTasksFromVolume(properties);
	}

	@Test
	public void testFederationMemberCheck() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String crawlerIP = "fake-crawler-ip";
		String crawlerPort = "fake-crawler-port";
		String nfsPort = "fake-nfs-port";
		String federationMember1 = "fake-fed-member-1";
		String federationMember2 = "fake-fed-member-2";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask image1 = new ImageTask("task-id-1", "image1", "link1", ImageTaskState.ARCHIVED,
				federationMember1, 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE", new Timestamp(
						date.getTime()), new Timestamp(date.getTime()), "available", "", "None");
		ImageTask image2 = new ImageTask("task-id-2", "image2", "link2", ImageTaskState.ARCHIVED,
				federationMember2, 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE", new Timestamp(
						date.getTime()), new Timestamp(date.getTime()), "available", "", "None");

		imageList.add(image1);
		imageList.add(image2);

		doReturn(sebalExportPath).when(properties).getProperty(
				SapsPropertiesConstants.SEBAL_EXPORT_PATH);

		doReturn(imageList).when(imageStore).getAllTasks();

		InputDownloader crawler = new InputDownloader(properties, imageStore,
				crawlerIP, crawlerPort, nfsPort, federationMember1);

		// exercise
		crawler.deleteFetchedResultsFromVolume(properties);

		// expect
		Assert.assertNotEquals(image1.getFederationMember(), image2.getFederationMember());
	}

	@Test
	public void testGetCrawlerVersion() throws SQLException, IOException, InterruptedException {
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		USGSNasaRepository usgsRepository = mock(USGSNasaRepository.class);
		String crawlerIP = "fake-crawler-ip";
		String crawlerPort = "fake-crawler-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		PrintWriter writer = new PrintWriter(
				"sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a", "UTF-8");
		writer.println("0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		writer.close();

		InputDownloader crawler = new InputDownloader(properties, imageStore,
				crawlerIP, crawlerPort, nfsPort, federationMember);

		// exercise
		String versionReturn = crawler.getCrawlerVersion();

		// expect
		Assert.assertEquals("0c26f092e976389c593953a1ad8ddaadb5c2ab2a", versionReturn);

		File file = new File("sebal-engine.version.0c26f092e976389c593953a1ad8ddaadb5c2ab2a");
		file.delete();
	}
}
