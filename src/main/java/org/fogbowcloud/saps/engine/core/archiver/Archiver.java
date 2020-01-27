package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.NfsPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.retry.CatalogUtils;

public class Archiver {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final PermanentStorage permanentStorage;
	private ScheduledExecutorService sapsExecutor;
	private final boolean executionDebugMode;

	public static final Logger LOGGER = Logger.getLogger(Archiver.class);

	public Archiver(Properties properties) throws SapsException, SQLException {
		this(properties, new JDBCImageDataStore(properties), new ArchiverHelper());

		LOGGER.info("Creating Archiver");
		LOGGER.info("Imagestore " + properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_IP) + ":"
				+ properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_PORT));
	}

	protected Archiver(Properties properties, ImageDataStore imageStore, ArchiverHelper archiverHelper)
			throws SapsException {
		if (!checkProperties(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");

		this.properties = properties;
		this.imageStore = imageStore;
		this.sapsExecutor = Executors.newScheduledThreadPool(1);
		this.permanentStorage = createStorageInstance(
				properties.getProperty(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE));
		this.executionDebugMode = properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE) && properties
				.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE).toLowerCase().equals("true");

		
	}

	/**
	 * This function create an instance from declared permanent storage type.
	 * 
	 * @param type permanent storage type to be created
	 * @return permanent storage instance
	 * @throws SapsException
	 */
	private PermanentStorage createStorageInstance(String type) throws SapsException {
		String lType = type.toLowerCase();
		if (lType.equals("swift"))
			return new SwiftPermanentStorage(properties);
		if (lType.equals("nfs"))
			return new NfsPermanentStorage();

		throw new SapsException("Failed to recognize type of permanent storage");
	}

	/**
	 * This function checks if the essential properties have been set.
	 * 
	 * @param properties saps properties to be check
	 * @return boolean representation, true (case all properties been set) or false
	 *         (otherwise)
	 */
	private boolean checkProperties(Properties properties) {
		if (properties == null) {
			LOGGER.error("Properties arg must not be null.");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.IMAGE_DATASTORE_IP)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.IMAGE_DATASTORE_IP + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.IMAGE_DATASTORE_PORT)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.IMAGE_DATASTORE_PORT + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER)) {
			LOGGER.error(
					"Required property " + SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXPORT_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXPORT_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE + " was not set");
			return false;
		}

		LOGGER.debug("All properties are set");
		return true;
	}

	/**
	 * Start Archiver component
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		cleanUnfinishedArchivedData();

		sapsExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				garbageCollector();
			}
		}, 0, Long.valueOf(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR)),
				TimeUnit.SECONDS);

		sapsExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				archiver();
			}
		}, 0, Long.valueOf(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER)),
				TimeUnit.SECONDS);
	}

	/**
	 * This function is an garbage collector deleting data directory from failed
	 * tasks.
	 */
	private void garbageCollector() {
		List<SapsImage> failedTasks = tasksInFailedState(ImageDataStore.UNLIMITED);

		LOGGER.info("Deleting data directory from " + failedTasks.size() + " failed tasks");

		for (SapsImage task : failedTasks)
			deleteAllTaskFilesFromDisk(task);

	}

	/**
	 * This function gets tasks in failed state in Catalog.
	 * 
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	private List<SapsImage> tasksInFailedState(int limit) {
		return CatalogUtils.getTasks(imageStore, ImageTaskState.FAILED, limit,
				"gets tasks with " + ImageTaskState.FAILED.getValue() + " state");
	}

	/**
	 * This function cleans unfinished data from incomplete archiving.
	 * 
	 * @throws Exception
	 */
	private void cleanUnfinishedArchivedData() throws Exception {
		List<SapsImage> archivingTasks = tasksInArchivingState(ImageDataStore.UNLIMITED);

		LOGGER.info("Rollback in " + archivingTasks.size() + " tasks in archiving state");

		for (SapsImage task : archivingTasks) {
			LOGGER.info("Applying task [" + task.getTaskId() + "] rollback");
			rollBackArchive(task);
			permanentStorage.delete(task);
			// deletePendingFilesFromSwift(task);
		}
	}

	/**
	 * This function gets tasks in finished state in Catalog.
	 * 
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	private List<SapsImage> tasksInArchivingState(int limit) {
		return CatalogUtils.getTasks(imageStore, ImageTaskState.ARCHIVING, limit,
				"gets tasks with " + ImageTaskState.ARCHIVING.getValue() + " state");
	}

	/**
	 * This function applies rollback in specific task, returning for finished
	 * state.
	 * 
	 * @param task task to be rollbacked
	 */
	private void rollBackArchive(SapsImage task) {
		String taskId = task.getTaskId();
		LOGGER.info("Reversing the archiving done in task [" + taskId + "] and returning to state ["
				+ ImageTaskState.FINISHED.getValue() + "]");

		removeTimestampTaskInCatalog(task, "removes task [" + taskId + "] timestamp");

		updateStateInCatalog(task, ImageTaskState.FINISHED, SapsImage.AVAILABLE,
				"Max archive tries reached", SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] to failed state");

		addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
	}

	/**
	 * This function archives finished tasks.
	 * 
	 */
	private void archiver() {
		try {
			List<SapsImage> tasksToArchive = tasksToArchive(ImageDataStore.UNLIMITED);

			LOGGER.info("Trying to archive " + tasksToArchive.size() + " finished tasks: " + tasksToArchive);

			for (SapsImage task : tasksToArchive) {
				LOGGER.info("Try to archive task [" + task.getTaskId() + "]");
				tryTaskArchive(task);
				deleteAllTaskFilesFromDisk(task);
			}

		} catch (Exception e) {
			LOGGER.error("Erro while try archive");
		}
	}

	/**
	 * This function gets tasks in finished state in Catalog.
	 * 
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	private List<SapsImage> tasksToArchive(int limit) {
		return CatalogUtils.getTasks(imageStore, ImageTaskState.FINISHED, limit,
				"gets tasks with " + ImageTaskState.FINISHED.getValue() + " state");
	}

	/**
	 * This function try to archive a task.
	 * 
	 * @param task task to be archived
	 * @throws IOException
	 */
	private void tryTaskArchive(SapsImage task) throws IOException {
		if (prepareArchive(task)
				&& permanentStorage.archive(task)) {
			LOGGER.info("SUCCESS in archiving task [" + task.getTaskId() + "]");
			finishArchive(task);
		} else {
			LOGGER.info("FAILURE in archiving task [" + task.getTaskId() + "]");
			failedArchive(task);
		}
	}

	/**
	 * This function prepare a task for archive.
	 * 
	 * @param task task to be prepared for archive
	 * @return boolean representation, success (true) or failure (false) in
	 *         preparing the task.
	 */
	private boolean prepareArchive(SapsImage task) {
		LOGGER.info("Preparing task [" + task.getTaskId() + "] to archive");

		String taskId = task.getTaskId();

		updateStateInCatalog(task, ImageTaskState.ARCHIVING, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
				SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] with state [" + ImageTaskState.ARCHIVING.getValue() + "]");

		addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");

		LOGGER.info("Task [" + taskId + "] ready to archive");
		return true;
	}

	/**
	 * This function finishes a success task.
	 * 
	 * @param task task to be finished
	 */
	private void finishArchive(SapsImage task) {
		LOGGER.debug("Finishing archive for task [" + task + "]");

		String taskId = task.getTaskId();
		updateStateInCatalog(task, ImageTaskState.ARCHIVED, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
				SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] with state [" + ImageTaskState.ARCHIVED.getValue() + "]");
		addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
	}

	/**
	 * This function finishes a failed task.
	 * 
	 * @param task task to be finished
	 */
	private void failedArchive(SapsImage task) {
		String taskId = task.getTaskId();

		updateStateInCatalog(task, ImageTaskState.FAILED, SapsImage.AVAILABLE,
				"Max archive tries reached", SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] to failed state");
		addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
	}

	/**
	 * This function delete directory from task.
	 * 
	 * @param task task that contains information to delete your folder
	 */
	private void deleteAllTaskFilesFromDisk(SapsImage task) {
		LOGGER.info("Deleting all task [" + task.getTaskId() + "] files from disk");

		String sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String taskDirPath = sapsExports + File.separator + task.getTaskId();

		File taskDir = new File(taskDirPath);
		if (taskDir.exists() && taskDir.isDirectory()) {
			if (this.executionDebugMode && task.getState().equals(ImageTaskState.FAILED))
				permanentStorage.archive(task);
			try {
				FileUtils.deleteDirectory(taskDir);
			} catch (IOException e) {
				LOGGER.error("Erro while delete all task files from disk: ", e);
			}
		} else
			LOGGER.info("Path " + taskDirPath + " does not exist or is not a directory!");
	}

	/**
	 * This function updates task state in catalog component.
	 *
	 * @param task         task to be updated
	 * @param state        new task state
	 * @param status       new task status
	 * @param error        new error message
	 * @param arrebolJobId new Arrebol job id
	 * @param message      information message
	 * @return boolean representation reporting success (true) or failure (false) in
	 *         update state task in catalog
	 */
	private boolean updateStateInCatalog(SapsImage task, ImageTaskState state, String status, String error,
			String arrebolJobId, String message) {
		task.setState(state);
		task.setStatus(status);
		task.setError(error);
		task.setArrebolJobId(arrebolJobId);
		return CatalogUtils.updateState(imageStore, task,
				"updates task[" + task.getTaskId() + "] state for " + state.getValue());
	}

	/**
	 * This function add new tuple in time stamp table and updates task time stamp.
	 * 
	 * @param task    task to be update
	 * @param message information message
	 */
	private void addTimestampTaskInCatalog(SapsImage task, String message) {
		CatalogUtils.addTimestampTask(imageStore, task, message);
	}

	/**
	 * This function updates task time stamp and insert new tuple in time stamp
	 * table.
	 * 
	 * @param task    task to be update
	 * @param message information message
	 */
	private void removeTimestampTaskInCatalog(SapsImage task, String message) {
		CatalogUtils.removeTimestampTask(imageStore, task, message);
	}
}
