package org.fogbowcloud.saps.engine.core.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestImageDataStore {

	private JDBCImageDataStore imageStore;
	private ImageTask taskDefault;
	
	@Before
	public void setUp() throws SQLException {
		Properties properties = new Properties();
		properties.setProperty(ImageDataStore.DATASTORE_IP, "");
		properties.setProperty(ImageDataStore.DATASTORE_PORT, "");
		properties.setProperty(ImageDataStore.DATASTORE_URL_PREFIX, "jdbc:h2:mem:testdb");
		properties.setProperty(ImageDataStore.DATASTORE_USERNAME, "testuser");
		properties.setProperty(ImageDataStore.DATASTORE_PASSWORD, "testuser");
		properties.setProperty(ImageDataStore.DATASTORE_DRIVER, "org.h2.Driver");
		properties.setProperty(ImageDataStore.DATASTORE_NAME, "testdb");
		
		this.imageStore = new JDBCImageDataStore(properties);
		
		this.taskDefault = new ImageTask("task-default", "LT5", "region-53", new Date(), "link-default",
				ImageTaskState.CREATED, ImageTask.NON_EXISTENT_DATA, 0,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA, new Timestamp(
						new java.util.Date().getTime()), new Timestamp(
						new java.util.Date().getTime()), ImageTask.AVAILABLE, "");
	}
	
	@Test
	public void testGetImageToDownload() throws SQLException {
		String federationMember = "fake-federation-member";
		int limit = 1;

		Date date = new Date();

		List<ImageTask> imageTaskList = this.imageStore.getImagesToDownload(federationMember, limit);

		Assert.assertTrue(imageTaskList.size() == 0);
		
		List<ImageTask> imageTaskList = this.imageStore.getImagesToDownload(federationMember, limit);
		Assert.assertTrue(imageTaskList.size() == 0);		
		
		ImageTask taskOne = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, "NE", 0, "NE", "NE", "NE", "NE", "NE", "NE", new Timestamp(
						new java.util.Date().getTime()), new Timestamp(
						new java.util.Date().getTime()), ImageTask.AVAILABLE, "");
		ImageTask taskTwo = new ImageTask("task-id-2", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, "NE", 0, "NE", "NE", "NE", "NE", "NE", "NE", new Timestamp(
						new java.util.Date().getTime()), new Timestamp(
						new java.util.Date().getTime()), ImageTask.AVAILABLE, "");
    
		this.imageStore.addImageTask(taskOne);
		this.imageStore.addImageTask(taskTwo);

		imageTaskList = this.imageStore.getImagesToDownload(federationMember, limit);

		Assert.assertTrue(imageTaskList.size() == 1);

		this.imageStore.dispose();
	}
	
	@Test
	public void testUpdateTaskStatus() throws SQLException {
		this.imageStore.addImageTask(this.taskDefault);
		
		List<ImageTask> tasks = this.imageStore.getAllTasks();
		ImageTask imageTask = tasks.get(0);
		Assert.assertEquals(1, tasks.size());
		Assert.assertEquals(imageTask.getStatus(), ImageTask.AVAILABLE);
		
		this.imageStore.updateTaskStatus(this.taskDefault.getTaskId(), ImageTask.UNAVAILABLE);
		
		tasks = this.imageStore.getAllTasks();
		imageTask = tasks.get(0);
		Assert.assertEquals(imageTask.getStatus(), ImageTask.UNAVAILABLE);
	}
	
	@Test
	public void testUpdateMetadata() throws SQLException {
		String metadataFilePath = "fake-metadata-file-path";
		String operatingSystem = "fake-operating-system";
		String kernelVersion = "fake-kernel-version";

		Properties properties = new Properties();
		properties.setProperty("datastore_ip", "");
		properties.setProperty("datastore_port", "");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_username", "testuser");
		properties.setProperty("datastore_password", "testuser");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_name", "testdb");

		Date date = new Date();

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);
		ImageTask task = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, ImageTask.NON_EXISTENT_DATA, 0, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, ImageTask.NON_EXISTENT_DATA,
				ImageTask.NON_EXISTENT_DATA, new Timestamp(new java.util.Date().getTime()),
				new Timestamp(new java.util.Date().getTime()), "available", "");

		imageStore.dispatchMetadataInfo(task.getTaskId());
		imageStore.updateMetadataInfo(metadataFilePath, operatingSystem, kernelVersion,
				"input_downloader", task.getTaskId());

		String metadataInfo = imageStore.getMetadataInfo(task.getTaskId(),
				"input_downloader", SapsPropertiesConstants.METADATA_TYPE);
		String osInfo = imageStore.getMetadataInfo(task.getTaskId(),
				"input_downloader", SapsPropertiesConstants.OS_TYPE);
		String kernelInfo = imageStore.getMetadataInfo(task.getTaskId(),
				"input_downloader", SapsPropertiesConstants.KERNEL_TYPE);
		
		Assert.assertEquals(metadataFilePath, metadataInfo);
		Assert.assertEquals(operatingSystem, osInfo);
		Assert.assertEquals(kernelVersion, kernelInfo);
	}
	
	@Test
	public void testMetadataRegisterExist() throws SQLException {
		String fakeTaskId = "fake-task-id-exist";

		Properties properties = new Properties();
		properties.setProperty("datastore_ip", "");
		properties.setProperty("datastore_port", "");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_username", "testuser");
		properties.setProperty("datastore_password", "testuser");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_name", "testdb");

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);
		imageStore.dispatchMetadataInfo(fakeTaskId);
		
		boolean exist = imageStore.metadataRegisterExist(fakeTaskId);
		
		Assert.assertTrue(exist);
	}
	
	@Test
	public void testMetadataRegisterNotExist() throws SQLException {
		String fakeTaskId = "fake-task-id-not-exist";

		Properties properties = new Properties();
		properties.setProperty("datastore_ip", "");
		properties.setProperty("datastore_port", "");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_username", "testuser");
		properties.setProperty("datastore_password", "testuser");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_name", "testdb");

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);

		boolean exist = imageStore.metadataRegisterExist(fakeTaskId);

		Assert.assertFalse(exist);
	}
}
