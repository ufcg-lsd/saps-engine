package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.junit.Assert;
import org.junit.Test;

public class TestSubmissionDispatcher {

	@Test
	public void testInsertTasksWithSubmission() throws SQLException, IOException {
		Submission submissionOne = new Submission("sub-1");
		Submission submissionTwo = new Submission("sub-2");
		Task taskOne = new Task("task-1");
		Task taskTwo = new Task("task-2");
		Task taskThree = new Task("task-3");

		ImageTask imageTaskOne = new ImageTask(taskOne.getId(), "img-1", ImageTask.NON_EXISTENT,
				ImageTaskState.CREATED, ImageTask.NON_EXISTENT, 0, ImageTask.NON_EXISTENT,
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT,
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT,
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()),
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT);
		ImageTask imageTaskTwo = new ImageTask(taskTwo.getId(), "img-2", ImageTask.NON_EXISTENT,
				ImageTaskState.CREATED, ImageTask.NON_EXISTENT, 0, ImageTask.NON_EXISTENT,
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT,
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT,
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()),
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT);
		ImageTask imageTaskThree = new ImageTask(taskThree.getId(), "img-3", ImageTask.NON_EXISTENT,
				ImageTaskState.CREATED, ImageTask.NON_EXISTENT, 0, ImageTask.NON_EXISTENT,
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT,
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT,
				new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()),
				ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT, ImageTask.NON_EXISTENT);

		taskOne.setImageTask(imageTaskOne);
		taskTwo.setImageTask(imageTaskTwo);
		taskThree.setImageTask(imageTaskThree);

		submissionOne.addTask(taskOne);
		submissionOne.addTask(taskTwo);
		submissionTwo.addTask(taskThree);

		Properties properties = new Properties();
		properties.setProperty("datastore_ip", "localhost");
		properties.setProperty("datastore_port", "8000");
		properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
		properties.setProperty("datastore_username", "testuser");
		properties.setProperty("datastore_password", "testuser");
		properties.setProperty("datastore_driver", "org.h2.Driver");
		properties.setProperty("datastore_name", "testdb");

		JDBCImageDataStore imageStore = new JDBCImageDataStore(properties);
		imageStore.addImageTask(imageTaskOne);
		imageStore.addImageTask(imageTaskTwo);
		imageStore.addImageTask(imageTaskThree);

		SubmissionDispatcherImpl subDispatcherImpl = new SubmissionDispatcherImpl(imageStore);
		subDispatcherImpl.addTaskNotificationIntoDB(submissionOne.getId(), taskOne.getId(),
				imageTaskOne.getName(), "email-1");
		subDispatcherImpl.addTaskNotificationIntoDB(submissionOne.getId(), taskTwo.getId(),
				imageTaskTwo.getName(), "email-1");
		subDispatcherImpl.addTaskNotificationIntoDB(submissionTwo.getId(), taskThree.getId(),
				imageTaskThree.getName(), "email-2");

		Submission actualSubmissionOne = imageStore.getSubmission(submissionOne.getId());
		Submission actualSubmissionTwo = imageStore.getSubmission(submissionTwo.getId());

		Assert.assertEquals(submissionOne, actualSubmissionOne);
		Assert.assertEquals(submissionTwo, actualSubmissionTwo);
	}
}
