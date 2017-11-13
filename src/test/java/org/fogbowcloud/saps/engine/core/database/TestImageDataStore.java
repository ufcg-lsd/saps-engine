package org.fogbowcloud.saps.engine.core.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
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

		List<ImageTask> imageTaskList = this.imageStore.getImagesToDownload(federationMember, limit);

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

}
