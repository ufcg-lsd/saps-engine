package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
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
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Archiver {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final SwiftAPIClient swiftAPIClient;
	private ScheduledExecutorService sapsExecutor;

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
	protected static boolean checkProperties(Properties properties) {
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

	public void start() throws Exception {
		try {
			sapsExecutor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					archiver();
				}
			}, 0, Long.valueOf(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER)),
					TimeUnit.SECONDS);

		} catch (Exception e) {
			LOGGER.error("Error while starting Archiver component", e);
		}
	}

	private void archiver() {
		try {
			cleanUnfinishedArchivedData();
			List<SapsImage> tasksToArchive = tasksToArchive(ImageDataStore.UNLIMITED);

			for (SapsImage task : tasksToArchive) {
				LOGGER.info("Try to archive task [" + task.getTaskId() + "]");
				tryTaskArchive(task);
			}

		} catch (IOException e) {
			// TODO upgrade erro message
			LOGGER.error("Erro while try archive");
		} catch (Exception e) {
			// TODO upgrade erro message
			LOGGER.error("Erro while try archive");
		}
	}

	private List<SapsImage> tasksToArchive(int limit) {
		return CatalogUtils.getTasks(imageStore, ImageTaskState.FINISHED, ImageDataStore.UNLIMITED,
				"gets tasks with " + ImageTaskState.FINISHED.getValue() + " state");
	}

	private void tryTaskArchive(SapsImage task) throws IOException {
		if (prepareArchive(task) && archiveSwift(task)) {
			LOGGER.info("SUCCESS in archiving task [" + task.getTaskId() + "]");
			finishArchive(task);
		} else {
			LOGGER.info("FAILURE in archiving task [" + task.getTaskId() + "]");
			failedArchive(task);
		}
	}

	private boolean prepareArchive(SapsImage task) {
		LOGGER.info("Preparing task [" + task.getTaskId() + "] to archive");

		String taskId = task.getTaskId();

		updateStateInCatalog(task, ImageTaskState.ARCHIVING, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
				SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] with state [" + ImageTaskState.ARCHIVING.getValue() + "]");

		updateTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");

		LOGGER.info("Task [" + taskId + "] ready to archive");
		return true;
	}

	private boolean archiveSwift(SapsImage task) {
		String taskId = task.getTaskId();

		LOGGER.info("Attempting to archive task [" + taskId + "] with a maximum of " + MAX_ARCHIVE_TRIES
				+ " archiving attempts for each folder (inputdownloading, preprocessing, processing)");

		String sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String swiftExports = properties.getProperty(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX);

		String inputdownloadingLocalDir = sapsExports + File.separator + taskId + File.pathSeparator
				+ "inputdownloading";
		String inputdownloadingSwiftDir = swiftExports + File.separator + taskId + File.pathSeparator
				+ "inputdownloading";

		String preprocessingLocalDir = sapsExports + File.separator + taskId + File.pathSeparator + "preprocessing";
		String preprocessingSwiftDir = swiftExports + File.separator + taskId + File.pathSeparator + "preprocessing";

		String processingLocalDir = sapsExports + File.separator + taskId + File.pathSeparator + "processing";
		String processingSwiftDir = swiftExports + File.separator + taskId + File.pathSeparator + "processing";
		
		LOGGER.info("Inputdownloading local folder: " + inputdownloadingLocalDir);
		LOGGER.info("Inputdownloading swift folder: " + inputdownloadingSwiftDir);
		boolean inputdownloadingSentSuccess = archive(task, inputdownloadingLocalDir, inputdownloadingSwiftDir);
		
		LOGGER.info("Preprocessing local folder: " + preprocessingLocalDir);
		LOGGER.info("Preprocessing swift folder: " + preprocessingSwiftDir);
		boolean preprocessingSentSuccess = inputdownloadingSentSuccess && archive(task, preprocessingLocalDir, preprocessingSwiftDir);
		
		LOGGER.info("Processing local folder: " + processingLocalDir);
		LOGGER.info("Processing swift folder: " + processingSwiftDir);
		boolean processingSentSuccess = preprocessingSentSuccess && archive(task, processingLocalDir, processingSwiftDir);

		LOGGER.info("Archive process result of task [" + task.getTaskId() + ":\nInputdownloading phase: "
				+ (inputdownloadingSentSuccess ? "Sucesso" : "Falha") + "\n" + "Preprocessing phase: "
				+ (preprocessingSentSuccess ? "Sucesso" : "Falha") + "\n" + "Processing phase: "
				+ (processingSentSuccess ? "Sucesso" : "Falha"));

		return inputdownloadingSentSuccess && preprocessingSentSuccess && processingSentSuccess;
	}

	private void finishArchive(SapsImage task) {
		LOGGER.debug("Finishing archive for task [" + task + "]");

		String taskId = task.getTaskId();
		updateStateInCatalog(task, ImageTaskState.ARCHIVED, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
				SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] with state [" + ImageTaskState.ARCHIVED.getValue() + "]");
		updateTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
		deleteAllTaskFilesFromDisk(task);
	}

	private void failedArchive(SapsImage task) {
		// deleteAllTaskFilesFromDisk(task);
	}

	protected void cleanUnfinishedArchivedData() throws Exception {
		LOGGER.info("Starting garbage collector");

		// Collection<SapsImage> taskList = pendingTaskArchiveMap.values();
		/*
		 * for (SapsImage imageTask : taskList) { rollBackArchive(imageTask);
		 * cleanTaskFoldersFromDisk(imageTask); deletePendingFilesFromSwift(imageTask);
		 * }
		 */

		LOGGER.info("Garbage collect finished");
	}

	private void deleteAllTaskFilesFromDisk(SapsImage task) {
		LOGGER.info("Deleting all task [" + task.getTaskId() + "] files from disk");
		
		String sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String taskDirPath = sapsExports + File.separator + task.getTaskId();

		File taskDir = new File(taskDirPath);
		if (taskDir.exists() && taskDir.isDirectory()) {
			try {
				FileUtils.deleteDirectory(taskDir);
			} catch (IOException e) {
				LOGGER.error("Erro while delete all task files from disk: ", e);
			}
		} else
			LOGGER.info("Path " + taskDirPath + " does not exist or is not a directory!");
	}

	private void cleanTaskFoldersFromDisk(SapsImage task) {
		String sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String taskDirPath = sapsExports + File.separator + task.getTaskId();

		File taskDir = new File(taskDirPath);
		if (taskDir.exists() && taskDir.isDirectory()) {
			try {
				FileUtils.cleanDirectory(taskDir);
			} catch (IOException e) {
				LOGGER.error("Erro while clean all task [" + task.getTaskId() + "] folders from disk: ", e);
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
	 * This function updates task time stamp and insert new tuple in time stamp
	 * table.
	 * 
	 * @param task    task to be update
	 * @param message information message
	 */
	private void updateTimestampTaskInCatalog(SapsImage task, String message) {
		CatalogUtils.updateTimestampTask(imageStore, task, message);
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

	protected void rollBackArchive(SapsImage task) {
		String taskId = task.getTaskId();
		LOGGER.info("Rolling back Archiver for task [" + taskId + "]");

		removeTimestampTaskInCatalog(task, "removes task [" + taskId + "] timestamp");

		updateStateInCatalog(task, ImageTaskState.FINISHED, SapsImage.AVAILABLE,
				"Max archive tries" + MAX_ARCHIVE_TRIES + " reached", SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] to failed state");
		updateTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
	}

	private boolean archive(SapsImage task, String localDir, String swiftDir) {
		LOGGER.info("Trying to archive task [" + task.getTaskId() + "] " + localDir + " folder with a maximum of "
				+ MAX_ARCHIVE_TRIES + " archiving attempts");

		File localFileDir = new File(localDir);

		if (!localFileDir.exists() || !localFileDir.isDirectory()) {
			LOGGER.error("Failed to archive task [" + task.getTaskId() + "]. " + localDir
					+ " folder isn't directory or not exists");
			return false;
		}

		String taskId = task.getTaskId();

		for (int itry = 0; itry < MAX_ARCHIVE_TRIES; itry++) {
			if (uploadFilesToSwift(task, localFileDir, swiftDir))
				return true;

		}

		updateStateInCatalog(task, ImageTaskState.FAILED, SapsImage.AVAILABLE,
				"Max archive tries" + MAX_ARCHIVE_TRIES + " reached", SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + taskId + "] to failed state");
		updateTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
		return false;
	}

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

	protected boolean deletePendingFilesFromSwift(SapsImage task) throws Exception {
		LOGGER.debug("Deleting " + task + " input files from swift");
		String containerName = getContainerName();

		List<String> fileNames = swiftAPIClient.listFilesInContainer(containerName);

		for (String file : fileNames) {
			if (file.contains(".TIF") || file.contains("MTL") || file.contains(".tar.gz") || file.contains(".nc")
					|| file.contains(".csv")) {
				LOGGER.debug("Trying to delete file " + file + " from " + containerName);
				String inputdownloadingSwiftTaskFolder = task.getTaskId() + File.separator + "inputdownloading";
				String preprocessingSwiftTaskFolder = task.getTaskId() + File.separator + "preprocessing";
				String processingSwiftTaskFolder = task.getTaskId() + File.separator + "processing";

				swiftAPIClient.deleteFile(containerName, inputdownloadingSwiftTaskFolder, file);
				swiftAPIClient.deleteFile(containerName, preprocessingSwiftTaskFolder, file);
				swiftAPIClient.deleteFile(containerName, processingSwiftTaskFolder, file);
			}
		}

		return true;
	}

	private String getContainerName() {
		return properties.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);
	}
}
