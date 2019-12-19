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
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
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
	private final SwiftAPIClient swiftAPIClient;
	private ScheduledExecutorService sapsExecutor;
	private final boolean executionMode;

	private static int MAX_ARCHIVE_TRIES = 1;
	private static int MAX_SWIFT_UPLOAD_TRIES = 2;

	public static final Logger LOGGER = Logger.getLogger(Archiver.class);

	public Archiver(Properties properties) throws SapsException, SQLException {
		this(properties, new JDBCImageDataStore(properties), new SwiftAPIClient(properties), new ArchiverHelper());

		LOGGER.info("Creating Archiver");
		LOGGER.info("Imagestore " + properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_IP) + ":"
				+ properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_PORT));
	}

	protected Archiver(Properties properties, ImageDataStore imageStore, SwiftAPIClient swiftAPIClient,
			ArchiverHelper archiverHelper) throws SapsException {
		if (!checkProperties(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");

		this.properties = properties;
		this.imageStore = imageStore;
		this.swiftAPIClient = swiftAPIClient;
		this.sapsExecutor = Executors.newScheduledThreadPool(1);
		this.executionMode = properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE) && properties
				.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER).toLowerCase().equals("true");

		if (this.executionMode && !checkPropertiesDebugMode(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");

		// Creating Swift container
		this.swiftAPIClient.createContainer(getContainerName());
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
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_FOLDER_PREFIX + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_CONTAINER_NAME)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_CONTAINER_NAME + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PROJECT_ID)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PROJECT_ID + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_USER_ID)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.FOGBOW_KEYSTONEV3_USER_ID + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PASSWORD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PASSWORD + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_AUTH_URL)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.FOGBOW_KEYSTONEV3_AUTH_URL + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL + " was not set");
			return false;
		}

		LOGGER.debug("All properties are set");
		return true;
	}

	/**
	 * This function checks if properties for debug mode have been set.
	 * 
	 * @param properties saps properties to be check
	 * @return boolean representation, true (case all properties been set) or false
	 *         (otherwise)
	 */
	private boolean checkPropertiesDebugMode(Properties properties) {
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS
					+ " was not set (it's necessary when debug mode)");
			return false;
		}

		LOGGER.debug("All properties for debug mode are set");
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
			deletePendingFilesFromSwift(task);
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
				"Max archive tries" + MAX_ARCHIVE_TRIES + " reached", SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] to failed state");

		addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
	}

	/**
	 * This function delete pending files from task in Swift.
	 * 
	 * @param task task with files information to be deleted
	 * @return boolean representation, success (true) or failure (false) to delete
	 *         files
	 * @throws Exception
	 */
	private boolean deletePendingFilesFromSwift(SapsImage task) throws Exception {
		String containerName = getContainerName();
		String taskId = task.getTaskId();

		LOGGER.debug("Deleting files from task [" + taskId + "] in Swift [" + containerName + "]");

		String prefix = properties.getProperty(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX) + File.separator + taskId;

		List<String> fileNames = swiftAPIClient.listFilesWithPrefix(containerName, prefix);

		LOGGER.info("Files List: " + fileNames);

		for (String file : fileNames) {
			LOGGER.debug("Trying to delete file " + file + " from " + containerName);
			swiftAPIClient.deleteFile(containerName, file);
		}

		return true;
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
				&& archiveSwift(task, properties.getProperty(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX))) {
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
	 * This function tries to archive a task trying each folder in order
	 * (inputdownloading -> preprocessing -> processing).
	 * 
	 * @param task task to be archived
	 * @return boolean representation, success (true) or failure (false) in to
	 *         archive the three folders.
	 */
	private boolean archiveSwift(SapsImage task, String swiftExports) {
		String taskId = task.getTaskId();

		LOGGER.info("Attempting to archive task [" + taskId + "] with a maximum of " + MAX_ARCHIVE_TRIES
				+ " archiving attempts for each folder (inputdownloading, preprocessing, processing)");

		String sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);

		String inputdownloadingLocalDir = sapsExports + File.separator + taskId + File.separator + "inputdownloading";
		String inputdownloadingSwiftDir = swiftExports + File.separator + taskId + File.separator + "inputdownloading";

		String preprocessingLocalDir = sapsExports + File.separator + taskId + File.separator + "preprocessing";
		String preprocessingSwiftDir = swiftExports + File.separator + taskId + File.separator + "preprocessing";

		String processingLocalDir = sapsExports + File.separator + taskId + File.separator + "processing";
		String processingSwiftDir = swiftExports + File.separator + taskId + File.separator + "processing";

		LOGGER.info("Inputdownloading local folder: " + inputdownloadingLocalDir);
		LOGGER.info("Inputdownloading swift folder: " + inputdownloadingSwiftDir);
		boolean inputdownloadingSentSuccess = archive(task, inputdownloadingLocalDir, inputdownloadingSwiftDir);

		LOGGER.info("Preprocessing local folder: " + preprocessingLocalDir);
		LOGGER.info("Preprocessing swift folder: " + preprocessingSwiftDir);
		boolean preprocessingSentSuccess = inputdownloadingSentSuccess
				&& archive(task, preprocessingLocalDir, preprocessingSwiftDir);

		LOGGER.info("Processing local folder: " + processingLocalDir);
		LOGGER.info("Processing swift folder: " + processingSwiftDir);
		boolean processingSentSuccess = preprocessingSentSuccess
				&& archive(task, processingLocalDir, processingSwiftDir);

		LOGGER.info("Archive process result of task [" + task.getTaskId() + ":\nInputdownloading phase: "
				+ (inputdownloadingSentSuccess ? "Sucesso" : "Falha") + "\n" + "Preprocessing phase: "
				+ (preprocessingSentSuccess ? "Sucesso" : "Falha") + "\n" + "Processing phase: "
				+ (processingSentSuccess ? "Sucesso" : "Falha"));

		return inputdownloadingSentSuccess && preprocessingSentSuccess && processingSentSuccess;
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
				"Max archive tries" + MAX_ARCHIVE_TRIES + " reached", SapsImage.NONE_ARREBOL_JOB_ID,
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
			if (this.executionMode && task.getState().equals(ImageTaskState.FAILED))
				archiveSwift(task,
						properties.getProperty(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS));
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

	/**
	 * This function tries to archive a task folder in Swift.
	 * 
	 * @param task     task in archive process
	 * @param localDir task folder to be archived
	 * @param swiftDir directory swift to archive new data
	 * @return boolean representation, success (true) or failure (false) to archive
	 */
	private boolean archive(SapsImage task, String localDir, String swiftDir) {
		LOGGER.info("Trying to archive task [" + task.getTaskId() + "] " + localDir + " folder with a maximum of "
				+ MAX_ARCHIVE_TRIES + " archiving attempts");

		File localFileDir = new File(localDir);

		if (!localFileDir.exists() || !localFileDir.isDirectory()) {
			LOGGER.error("Failed to archive task [" + task.getTaskId() + "]. " + localDir
					+ " folder isn't directory or not exists");
			return false;
		}

		for (int itry = 0; itry < MAX_ARCHIVE_TRIES; itry++) {
			if (uploadFilesToSwift(task, localFileDir, swiftDir))
				return true;
		}

		return false;
	}

	/**
	 * This function tries upload task folder files to Swift.
	 * 
	 * @param task     task in archive process
	 * @param localDir task folder to be archived
	 * @param swiftDir directory swift to archive new data
	 * @return boolean representation, success (true) or failure (false) to archive
	 */
	private boolean uploadFilesToSwift(SapsImage task, File localDir, String swiftDir) {
		LOGGER.info("Trying to archive task [" + task.getTaskId() + "] " + localDir + " folder for swift");
		for (File actualFile : localDir.listFiles()) {
			if (!uploadFileToSwift(actualFile, swiftDir)) {
				LOGGER.info("Failure in archiving file [" + actualFile.getAbsolutePath() + "]");
				return false;
			}
		}

		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}

	/**
	 * This function tries upload a task folder file to Swift.
	 * 
	 * @param actualFile file to be uploaded
	 * @param swiftDir   directory swift to archive
	 * @return boolean representation, success (true) or failure (false) to archive
	 */
	private boolean uploadFileToSwift(File actualFile, String swiftDir) {
		String containerName = getContainerName();
		LOGGER.info("Trying to archive file [" + actualFile.getAbsolutePath() + "] for swift container ["
				+ containerName + "] with a maximum of " + MAX_SWIFT_UPLOAD_TRIES + " uploading attempts");

		for (int itry = 0; itry < MAX_SWIFT_UPLOAD_TRIES; itry++) {
			try {
				swiftAPIClient.uploadFile(containerName, actualFile, swiftDir);
				return true;
			} catch (Exception e) {
				LOGGER.error("Error while uploading file " + actualFile.getAbsolutePath() + " to swift", e);
			}
		}

		return false;
	}

	private String getContainerName() {
		return properties.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);
	}
}
