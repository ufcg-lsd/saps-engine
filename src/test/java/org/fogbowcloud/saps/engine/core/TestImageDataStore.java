package org.fogbowcloud.saps.engine.core;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.junit.Assert;
import org.junit.Test;

public class TestImageDataStore {

	@Test
	public void testGetImageToDownload() throws SQLException {
		String federationMember = "fake-federation-member";
		int limit = 1;

		Properties properties = new Properties();
		properties.setProperty("datastore_ip", "localhost");
		properties.setProperty("datastore_port", "8000");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_username", "testuser");
		properties.setProperty("datastore_password", "testuser");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_name", "testdb");

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);
		ImageTask taskOne = new ImageTask("task-id-1", "image1", "link1", ImageTaskState.CREATED,
				"NE", 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()),
				"available", "", "None");
		ImageTask taskTwo = new ImageTask("task-id-2", "image1", "link1", ImageTaskState.CREATED,
				"NE", 0, "NE", "NE", "NE", "NE", "NE", "NE", "NE",
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()),
				"available", "", "None");
		
		imageStore.addImageTask(taskOne);
		imageStore.addImageTask(taskTwo);

		List<ImageTask> imageTaskList = imageStore.getImagesToDownload(federationMember, limit);
		
		Assert.assertTrue(imageTaskList.size() == 1);
	}

}
