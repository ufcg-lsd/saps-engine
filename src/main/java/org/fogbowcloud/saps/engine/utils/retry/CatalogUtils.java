package org.fogbowcloud.saps.engine.utils.retry;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.utils.retry.catalog.CatalogRetry;
import org.fogbowcloud.saps.engine.utils.retry.catalog.GetProcessingTasksRetry;
import org.fogbowcloud.saps.engine.utils.retry.catalog.GetTasksRetry;
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
	public static List<SapsImage> getTasks(ImageDataStore imageStore, ImageTaskState state, int limit,
			String message) {
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
	 * This function updates task with Arrebol job ID in catalog component.
	 *
	 * @param imageStore   catalog component
	 * @param task         task to be updated
	 * @param arrebolJobId Arrebol job ID of task submitted
	 * @param message      information message
	 * @return boolean representation reporting success (true) or failure (false) in
	 *         update state task in catalog
	 */
	public static void writeArrebolJobId(ImageDataStore imageStore, SapsImage task, String message) {
		retry(new UpdateTaskRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function updates task time stamp and insert new tuple in time stamp
	 * table.
	 * 
	 * @param imageStore   catalog component
	 * @param task    task to be update
	 * @param message information message
	 */
	public static void updateTimestampTask(ImageDataStore imageStore, SapsImage task, String message) {
		retry(new UpdateTimestampRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}
}
