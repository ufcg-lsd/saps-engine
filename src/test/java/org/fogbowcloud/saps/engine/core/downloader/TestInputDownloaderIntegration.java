package org.fogbowcloud.saps.engine.core.downloader;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.repository.USGSNasaRepository;
import org.fogbowcloud.saps.engine.core.util.DockerUtil;
import org.fogbowcloud.saps.engine.scheduler.core.exception.SapsException;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapdb.DB;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.configuration.GlobalConfiguration;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.ClassLoaderUtil;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ExecutionScriptTagUtil.class, DockerUtil.class })
public class TestInputDownloaderIntegration {

	private String FEDERATION_MEMBER_DEFAULT = "FED_MEMBER";
	
	@SuppressWarnings("unused")
	private Properties properties;
	private ImageTask imageTaskDefault;
	
	private InputDownloader inputDownloaderDefault;

	@Before
	public void setUp() {
		// setup
		Properties properties = Mockito.mock(Properties.class);
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "ip";
		String inputDownloaderPort = "port";
		String nfsPort = "port";
		String federationMember = "fake-fed-member";
		
		// mock
		this.inputDownloaderDefault = Mockito.spy(new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember));
		
		java.util.Date date = new java.util.Date();
		this.imageTaskDefault = new ImageTask("default-task-id-1", "LT5", "region-53", date,
				"link1", ImageTaskState.CREATED, FEDERATION_MEMBER_DEFAULT, 0,
				ImageTask.NON_EXISTENT_DATA, "Default", "Default", "Default",
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA, 
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), ImageTask.AVAILABLE, "");
		
		this.properties = mock(Properties.class);
	}

	@After
	public void clean() {
		MockitoStateCleaner mockitoStateCleaner = new MockitoStateCleaner();
		mockitoStateCleaner.run();
		
		String[] pendingTasks = { "pending-task-download.db", "pending-task-download.db.p",
				"pending-task-download.db.t" };
		for (String pendingTask : pendingTasks) {
			File pendingImageDBFile = new File(pendingTask);
			if (pendingImageDBFile.exists()) {
				pendingImageDBFile.delete();
			}
		}
	}

	@Test
	public void testStepOverImageWhenDownloadFails()
			throws SQLException, IOException, InterruptedException, SapsException {
		clean();

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
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask taskOne = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "Default", "Default", "Default",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		imageList.add(taskOne);

		doReturn(imageList).when(imageStore).getImagesToDownload(federationMember,
				InputDownloader.MAX_IMAGES_TO_DOWNLOAD);
		doReturn(taskOne).when(imageStore).getTask(taskOne.getTaskId());
		doReturn("link-1").when(usgsRepository).getImageDownloadLink(taskOne.getName());
		doThrow(new IOException()).when(usgsRepository).downloadImage(taskOne);

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);
		Assert.assertEquals(ImageTaskState.CREATED, taskOne.getState());
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());

		// exercise
		inputDownloader.download();

		// expect
		Assert.assertEquals(ImageTaskState.CREATED, taskOne.getState());
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());
	}

	@Test
	public void testinputDownloaderErrorWhileGetCreatedImages()
			throws SQLException, IOException, SapsException {
		clean();
		
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);

		doThrow(new SQLException()).when(imageStore).getImagesToDownload(federationMember,
				InputDownloader.MAX_IMAGES_TO_DOWNLOAD);
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());

		// exercise
		inputDownloader.download();

		// expect
		Assert.assertTrue(inputDownloader.pendingTaskDownloadMap.isEmpty());
	}

	@Test
	public void testPurgeImagesFromVolume() throws SQLException, IOException, InterruptedException {
		clean();
		
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask taskOne = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.FINISHED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");
		taskOne.setStatus(ImageTask.PURGED);
		ImageTask taskTwo = new ImageTask("task-id-2", "LT5", "region-53", date, "link2",
				ImageTaskState.FINISHED, federationMember, 1, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		imageList.add(taskOne);
		imageList.add(taskTwo);

		doReturn(imageList).when(imageStore).getIn(ImageTaskState.FINISHED);

		doReturn(sebalExportPath).when(properties)
				.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);

		// exercise
		inputDownloader.purgeTasksFromVolume(properties);
	}

	@Test
	public void testFederationMemberCheck() throws SQLException, IOException, InterruptedException {
		clean();
		
		// setup
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember1 = "fake-fed-member-1";
		String federationMember2 = "fake-fed-member-2";
		String sebalExportPath = "fake-export-path";

		Date date = new Date(10000854);

		List<ImageTask> imageList = new ArrayList<ImageTask>();
		ImageTask taskOne = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.ARCHIVED, federationMember1, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");
		ImageTask taskTwo = new ImageTask("task-id-2", "LT5", "region-53", date, "link2",
				ImageTaskState.ARCHIVED, federationMember2, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		imageList.add(taskOne);
		imageList.add(taskTwo);

		doReturn(sebalExportPath).when(properties)
				.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);

		doReturn(imageList).when(imageStore).getAllTasks();

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember1);

		// exercise
		inputDownloader.deleteArchivedTasksFromDisk(properties);

		// expect
		// Assert.assertNotEquals(taskOne.getFederationMember(),
		// taskTwo.getFederationMember());
	}

	@Test
	public void testPendingTaskMap() throws SQLException, IOException {
		clean();
		
		Properties properties = mock(Properties.class);
		ImageDataStore imageStore = mock(JDBCImageDataStore.class);
		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		Date date = new Date(10000854);

		ImageTask task = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(new java.util.Date().getTime()),
				new Timestamp(new java.util.Date().getTime()), "available", "");

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);

		inputDownloader.addTaskToPendingMap(task);
		ConcurrentMap<String, ImageTask> pendingTaskMap = inputDownloader.getPendingTaskMap();

		Assert.assertEquals(task, pendingTaskMap.get(task.getTaskId()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFailsAndRemovesImage() throws Exception {
		clean();
		
		Properties properties = new Properties();
		setUpProperties(properties);

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);

		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		Date date = new Date(10000854);

		ImageTask task = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "Default", "Default", "Default",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		imageStore.addImageTask(task);

		InputDownloader inputDownloader = spy(new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember));
		
		doNothing().when(inputDownloader).prepareTaskDirStructure(Mockito.any(ImageTask.class));
		PowerMockito.mockStatic(ExecutionScriptTagUtil.class);
		ExecutionScriptTag executionScriptTag = new ExecutionScriptTag("", "", "", "");
		BDDMockito.given(ExecutionScriptTagUtil.getExecutionScritpTag(Mockito.anyString(),
				Mockito.anyString())).willReturn(executionScriptTag);

		PowerMockito.mockStatic(DockerUtil.class);
		boolean notImportantBoolean = true;
		BDDMockito.given(DockerUtil.pullImage(Mockito.anyString(), Mockito.anyString()))
				.willReturn(notImportantBoolean);
		String containerId = "1";
		BDDMockito.given(DockerUtil.runMappedContainer(Mockito.anyString(), Mockito.anyString(),
				Mockito.anyMap())).willReturn(containerId);
		BDDMockito.given(DockerUtil.execDockerCommand(Mockito.eq(containerId), Mockito.anyString()))
				.willReturn(1);
		BDDMockito.given(DockerUtil.removeImage(Mockito.anyString()))
				.willReturn(notImportantBoolean);

		Assert.assertEquals(0, imageStore.getIn(ImageTaskState.FAILED).size()); // There's no failed
																				// image tasks
		Assert.assertEquals(1, imageStore.getIn(ImageTaskState.CREATED).size()); // There's 1 image
																					// created
		Assert.assertEquals(1, imageStore.getAllTasks().size()); // Total image tasks == 1

		inputDownloader.addTaskToPendingMap(task);
		inputDownloader.download();

		Assert.assertEquals(1, imageStore.getIn(ImageTaskState.FAILED).size()); // There's 1 failed
																				// image tasks after
																				// trying download
																				// it
		Assert.assertEquals(0, imageStore.getIn(ImageTaskState.CREATED).size()); // There's 0 image
																					// created
		Assert.assertEquals(1, imageStore.getAllTasks().size()); // Total image tasks == 1

		Assert.assertEquals("Error while downloading task...download retries "
				+ properties.getProperty(SapsPropertiesConstants.MAX_DOWNLOAD_ATTEMPTS)
				+ " exceeded.", imageStore.getTask("task-id-1").getError());

		imageStore.dispose();
	}

	@Test
	public void testTaskChangingState() throws Exception {
		clean();
		
		Properties properties = new Properties();
		setUpProperties(properties);

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);

		String inputDownloaderIP = "fake-inputDownloader-ip";
		String inputDownloaderPort = "fake-inputDownloader-port";
		String nfsPort = "fake-nfs-port";
		String federationMember = "fake-fed-member";

		Date date = new Date(10000854);

		ImageTask task = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(date.getTime()), new Timestamp(date.getTime()), "available", "");

		imageStore.addImageTask(task);

		InputDownloader inputDownloader = new InputDownloader(properties, imageStore,
				inputDownloaderIP, inputDownloaderPort, nfsPort, federationMember);

		Assert.assertEquals(0, imageStore.getIn(ImageTaskState.FAILED).size()); // There's no failed
																				// image tasks
		Assert.assertEquals(1, imageStore.getIn(ImageTaskState.CREATED).size()); // There's 1 image
																					// created
		Assert.assertEquals(1, imageStore.getAllTasks().size()); // Total image tasks == 1
		inputDownloader.addTaskToPendingMap(task);

		Assert.assertEquals(1, inputDownloader.getPendingTaskMap().size()); // Total image tasks ==
																			// 1
		inputDownloader.removeTaskFromPendingMap(task);

		Assert.assertEquals(0, inputDownloader.getPendingTaskMap().size());

		inputDownloader.addTaskToPendingMap(task);
		Assert.assertEquals(1, inputDownloader.getPendingTaskMap().size());

		inputDownloader.updateTaskStateToFailed(task, "error");
		Assert.assertEquals(1, inputDownloader.getPendingTaskMap().size());
		Assert.assertEquals(1, imageStore.getIn(ImageTaskState.FAILED).size());

		imageStore.dispose();
	}

	@Test
	public void testDownloadImageNotFoundImage() throws Exception {
		clean();

		Mockito.doNothing().when(this.inputDownloaderDefault)
				.prepareTaskDirStructure(Mockito.eq(this.imageTaskDefault));

		PowerMockito.mockStatic(ExecutionScriptTagUtil.class);
		ExecutionScriptTag executionScriptTag = new ExecutionScriptTag("", "", "", "");
		BDDMockito.given(ExecutionScriptTagUtil.getExecutionScritpTag(Mockito.anyString(),
				Mockito.anyString())).willReturn(executionScriptTag);

		PowerMockito.mockStatic(DockerUtil.class);
		boolean notImportantBoolean = true;
		BDDMockito.given(DockerUtil.pullImage(Mockito.anyString(), Mockito.anyString()))
				.willReturn(notImportantBoolean);
		String containerId = "1";

		BDDMockito.given(DockerUtil.runMappedContainer(Mockito.anyString(), Mockito.anyString(),
				Mockito.anyMapOf(String.class, String.class))).willReturn(containerId);
		BDDMockito.given(DockerUtil.execDockerCommand(Mockito.eq(containerId), Mockito.anyString()))
				.willReturn(InputDownloader.NOT_FOUNT_SCRIPT_CODE);
		BDDMockito.given(DockerUtil.removeImage(Mockito.anyString()))
				.willReturn(notImportantBoolean);

		DB pendingTaskDB = this.inputDownloaderDefault.getPendingTaskDB();
		ConcurrentMap<String, ImageTask> pendingTaskMap = this.inputDownloaderDefault
				.getPendingTaskMap();

		Assert.assertEquals(0, pendingTaskMap.values().size());

		pendingTaskMap.put(this.imageTaskDefault.getTaskId(), this.imageTaskDefault);
		pendingTaskDB.commit();

		Assert.assertEquals(1, pendingTaskMap.values().size());
		Assert.assertEquals(ImageTask.AVAILABLE, this.imageTaskDefault.getStatus());

		boolean returnDownloadImage = this.inputDownloaderDefault
				.downloadImage(this.imageTaskDefault);

		Assert.assertFalse(returnDownloadImage);
		Assert.assertEquals(0, pendingTaskMap.values().size());
		Mockito.verify(this.inputDownloaderDefault).updateTaskStateToFailed(
				Mockito.eq(this.imageTaskDefault),
				Mockito.eq(InputDownloader.IMAGE_NOT_FOUND_FAILED_MSG));
		Mockito.verify(this.inputDownloaderDefault).updateTaskStatus(
				Mockito.eq(this.imageTaskDefault), Mockito.eq(ImageTask.UNAVAILABLE));
	}
	
	@Test
	public void testDirectorySetUp() throws Exception {
		// Image task info
		String taskId = "fake-task-id";
		String dataset = "fake-dataset";
		String region = "fake-region";
		String downloadLink = "link1";
		String federationMember = "fake-task-id";
		String status = "available";

		ImageTask imageTask = new ImageTask(taskId, dataset, region, new java.util.Date(),
				downloadLink, ImageTaskState.CREATED, federationMember, 0,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				new Timestamp(new java.util.Date().getTime()),
				new Timestamp(new java.util.Date().getTime()), status, "");

		// Input downloader info
		String inputDownloderIp = "fake-ip";
		String InputDownloaderSshPort = "fake-ssh-port";
		String inputDownloaderNfsPort = "fake-nfs-port";

		Properties properties = new Properties();
		setUpProperties(properties);

		InputDownloader inputDownloader = new InputDownloader(properties, inputDownloderIp,
				InputDownloaderSshPort, inputDownloaderNfsPort, federationMember);

		// Expected paths and files
		String exportDirPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String inputDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "data" + File.separator + "input";
		String outputDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "data" + File.separator + "output";
		String preProcessDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "data" + File.separator + "preprocessing";
		String logsDirPath = exportDirPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "logs";
		String metadataDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "metadata";

		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		File preProcessDir = new File(preProcessDirPath);
		File metadataDir = new File(metadataDirPath);
		File logsDir = new File(logsDirPath);

		// Exercise
		inputDownloader.prepareTaskDirStructure(imageTask);

		// Assert
		Assert.assertTrue(inputDir.exists());
		Assert.assertTrue(outputDir.exists());
		Assert.assertTrue(preProcessDir.exists());
		Assert.assertTrue(metadataDir.exists());
		Assert.assertTrue(logsDir.exists());

		// Cleaning env
		String taskDirPath = exportDirPath + File.separator + imageTask.getTaskId();
		FileUtils.deleteDirectory(new File(taskDirPath));
	}

	@Test
	public void testDirectorySetUpWhenAlreadyExists() throws Exception {
		// Image task info
		String taskId = "fake-task-id";
		String dataset = "fake-dataset";
		String region = "fake-region";
		String downloadLink = "link1";
		String federationMember = "fake-task-id";
		String status = "available";

		ImageTask imageTask = new ImageTask(taskId, dataset, region, new java.util.Date(),
				downloadLink, ImageTaskState.CREATED, federationMember, 0,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				new Timestamp(new java.util.Date().getTime()),
				new Timestamp(new java.util.Date().getTime()), status, "");

		// Input downloader info
		String inputDownloderIp = "fake-ip";
		String InputDownloaderSshPort = "fake-ssh-port";
		String inputDownloaderNfsPort = "fake-nfs-port";

		Properties properties = new Properties();
		setUpProperties(properties);

		InputDownloader inputDownloader = new InputDownloader(properties, inputDownloderIp,
				InputDownloaderSshPort, inputDownloaderNfsPort, federationMember);

		// Expected paths and files
		String exportDirPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String inputDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "data" + File.separator + "input";
		String outputDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "data" + File.separator + "output";
		String preProcessDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "data" + File.separator + "preprocessing";
		String logsDirPath = exportDirPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "logs";
		String metadataDirPath = exportDirPath + File.separator + imageTask.getTaskId()
				+ File.separator + "metadata";

		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		File preProcessDir = new File(preProcessDirPath);
		File metadataDir = new File(metadataDirPath);
		File logsDir = new File(logsDirPath);

		inputDir.mkdirs();
		outputDir.mkdirs();
		preProcessDir.mkdirs();
		metadataDir.mkdirs();
		logsDir.mkdirs();

		// Exercise
		inputDownloader.prepareTaskDirStructure(imageTask);

		// Assert
		Assert.assertTrue(inputDir.exists());
		Assert.assertTrue(outputDir.exists());
		Assert.assertTrue(preProcessDir.exists());
		Assert.assertTrue(metadataDir.exists());
		Assert.assertTrue(logsDir.exists());

		// Cleaning env
		String taskDirPath = exportDirPath + File.separator + imageTask.getTaskId();
		FileUtils.deleteDirectory(new File(taskDirPath));
	}
	
	private void setUpProperties(Properties properties) {
		properties.setProperty(SapsPropertiesConstants.MAX_DOWNLOAD_ATTEMPTS, "3");

		properties.setProperty(ImageDataStore.DATASTORE_IP, "");
		properties.setProperty(ImageDataStore.DATASTORE_PORT, "");
		properties.setProperty(ImageDataStore.DATASTORE_URL_PREFIX, "jdbc:h2:mem:testdb");
		properties.setProperty(ImageDataStore.DATASTORE_USERNAME, "testuser");
		properties.setProperty(ImageDataStore.DATASTORE_PASSWORD, "testuser");
		properties.setProperty(ImageDataStore.DATASTORE_DRIVER, "org.h2.Driver");
		properties.setProperty(ImageDataStore.DATASTORE_NAME, "testdb");

		// TODO check this dependency. Change /local/exports. Bad dependency
		properties.setProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH, "/local/exports");
		properties.setProperty(SapsPropertiesConstants.MAX_NUMBER_OF_TASKS, "1");
	}
	
	private static class MockitoStateCleaner implements Runnable {
	    public void run() {
	        clearMockProgress();
	        clearConfiguration();
	    }

	    private void clearMockProgress() {
	        clearThreadLocalIn(ThreadSafeMockingProgress.class);
	    }

	    private void clearConfiguration() {
	        clearThreadLocalIn(GlobalConfiguration.class);
	    }

	    @SuppressWarnings("unchecked")
		private void clearThreadLocalIn(Class<?> cls) {
	        Whitebox.getInternalState(cls, ThreadLocal.class).set(null);
	        final Class<?> clazz = ClassLoaderUtil.loadClass(cls, ClassLoader.getSystemClassLoader());
	        Whitebox.getInternalState(clazz, ThreadLocal.class).set(null);
	    }
	}		

}
