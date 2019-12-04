package org.fogbowcloud.saps.engine.utils.retry;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.utils.retry.catalog.AddNewTask;
import org.fogbowcloud.saps.engine.utils.retry.catalog.AddNewUser;
import org.fogbowcloud.saps.engine.utils.retry.catalog.CatalogRetry;
import org.fogbowcloud.saps.engine.utils.retry.catalog.GetProcessingTasksRetry;
import org.fogbowcloud.saps.engine.utils.retry.catalog.GetTasksRetry;
import org.fogbowcloud.saps.engine.utils.retry.catalog.GetUser;
import org.fogbowcloud.saps.engine.utils.retry.catalog.RemoveTimestampRetry;
import org.fogbowcloud.saps.engine.utils.retry.catalog.UpdateTaskRetry;
import org.fogbowcloud.saps.engine.utils.retry.catalog.UpdateTimestampRetry;

public class CatalogUtils {

	public static final Logger LOGGER = Logger.getLogger(CatalogUtils.class);
	private static final int CATALOG_DEFAULT_SLEEP_SECONDS = 5;

	/**
	 * This function tries countless times to successfully execute the passed
	 * function.
	 * 
	 * @param <T>            Return type
	 * @param function       Function passed for execute
	 * @param sleepInSeconds Time sleep in seconds (case fail)
	 * @param message        Information message about function passed
	 * @return Function return
	 */
	@SuppressWarnings("unchecked")
	private static <T> T retry(CatalogRetry<?> function, int sleepInSeconds, String message) {
		LOGGER.info(
				"[Retry Catalog function] Trying " + message + " using " + sleepInSeconds + " seconds with time sleep");

		while (true) {
			try {
				return (T) function.run();
			} catch (SQLException e) {
				LOGGER.error("Failed while " + message);
				e.printStackTrace();
			}

			try {
				LOGGER.info("Sleeping for " + sleepInSeconds + " seconds");
				Thread.sleep(Long.valueOf(sleepInSeconds) * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function gets tasks in specific state in Catalog.
	 * 
	 * @param imageStore catalog component
	 * @param state      specific state for get tasks
	 * @param limit      limit value of tasks to take
	 * @param message    information message
	 * @return tasks in specific state
	 */
	public static List<SapsImage> getTasks(ImageDataStore imageStore, ImageTaskState state, int limit, String message) {
		return retry(new GetTasksRetry(imageStore, state, limit), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function updates task state in catalog component.
	 *
	 * @param imageStore catalog component
	 * @param task       task to be updated
	 * @param state      new task state
	 * @param message    information message
	 * @return boolean representation reporting success (true) or failure (false) in
	 *         update state task in catalog
	 */
	public static boolean updateState(ImageDataStore imageStore, SapsImage task, String message) {
		return retry(new UpdateTaskRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function gets tasks in processing state in catalog component.
	 * 
	 * @param imageStore catalog component
	 * @param message    information message
	 * @return processing tasks list
	 */
	public static List<SapsImage> getProcessingTasks(ImageDataStore imageStore, String message) {
		return retry(new GetProcessingTasksRetry(imageStore), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function updates task time stamp and insert new tuple in time stamp
	 * table.
	 * 
	 * @param imageStore catalog component
	 * @param task       task to be update
	 * @param message    information message
	 */
	public static void updateTimestampTask(ImageDataStore imageStore, SapsImage task, String message) {
		retry(new UpdateTimestampRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function remove task time stamp.
	 * 
	 * @param imageStore catalog component
	 * @param task       task to be update
	 * @param message    information message
	 */
	public static void removeTimestampTask(ImageDataStore imageStore, SapsImage task, String message) {
		retry(new RemoveTimestampRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function adds new user.
	 * 
	 * @param imageStore catalog component
	 * @param userEmail  user email
	 * @param userName   user name
	 * @param userPass   user password
	 * @param userState  user state
	 * @param userNotify user notify
	 * @param adminRole  administrator role
	 * @param message    information message
	 */
	public static void addNewUser(JDBCImageDataStore imageStore, String userEmail, String userName, String userPass,
			boolean userState, boolean userNotify, boolean adminRole, String message) {
		retry(new AddNewUser(imageStore, userEmail, userName, userPass, userState, userNotify, adminRole),
				CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function gets user information.
	 * 
	 * @param imageStore catalog component
	 * @param userEmail  user email
	 * @param message    information message
	 */
	public static SapsUser getUser(JDBCImageDataStore imageStore, String userEmail, String message) {
		return retry(new GetUser(imageStore, userEmail), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function adds new task.
	 * 
	 * @param imageStore               catalog component
	 * @param taskId                   task id
	 * @param dataset                  task dataset
	 * @param region                   task region
	 * @param date                     task region
	 * @param priority                 task priority
	 * @param userEmail                user email that is creating task
	 * @param inputdownloadingPhaseTag inputdownloading phase tag
	 * @param preprocessingPhaseTag    preprocessing phase tag
	 * @param processingPhaseTag       processing phase tag
	 * @param message                  information message
	 * @return new saps image
	 */
	public static SapsImage addNewTask(JDBCImageDataStore imageStore, String taskId, String dataset, String region,
			Date date, int priority, String userEmail, String inputdownloadingPhaseTag, String preprocessingPhaseTag,
			String processingPhaseTag, String message) {
		return retry(new AddNewTask(imageStore, taskId, dataset, region, date, priority, userEmail,
				inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag), CATALOG_DEFAULT_SLEEP_SECONDS,
				message);
	}
}
