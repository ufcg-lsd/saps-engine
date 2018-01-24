package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.ProvenanceUtilImpl;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openprovenance.prov.model.Document;

public class Archiver {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final SwiftAPIClient swiftAPIClient;
	private File pendingTaskArchiveFile;
	private DB pendingTaskArchiveDB;
	private ConcurrentMap<String, ImageTask> pendingTaskArchiveMap;
	private FTPIntegrationImpl ftpImpl;
	private ArchiverHelper archiverHelper;
	private String archiverVersion;

	private String ftpServerIP;
	private String ftpServerPort;

	private static int MAX_ARCHIVE_TRIES = 2;
	private static int MAX_SWIFT_UPLOAD_TRIES = 2;

	public static final Logger LOGGER = Logger.getLogger(Archiver.class);

	public Archiver(Properties properties) throws SQLException {
		this(properties, new JDBCImageDataStore(properties), new SwiftAPIClient(properties),
				new FTPIntegrationImpl(), new ArchiverHelper());

		LOGGER.info("Creating Archiver");
		LOGGER.info("Imagestore "
				+ properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_IP) + ":"
				+ properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_PORT));
	}

	protected Archiver(Properties properties, ImageDataStore imageStore,
			SwiftAPIClient swiftAPIClient, FTPIntegrationImpl ftpImpl,
			ArchiverHelper archiverHelper) {
		if (properties == null) {
			throw new IllegalArgumentException("Properties arg must not be null.");
		}

		if (imageStore == null) {
			throw new IllegalArgumentException("Imagestore arg must not be null.");
		}

		this.properties = properties;
		this.imageStore = imageStore;
		this.swiftAPIClient = swiftAPIClient;
		this.ftpImpl = ftpImpl;
		this.archiverHelper = archiverHelper;

		this.pendingTaskArchiveFile = new File("pending-task-archive.db");
		this.pendingTaskArchiveDB = DBMaker.newFileDB(pendingTaskArchiveFile).make();

		if (!pendingTaskArchiveFile.exists() || !pendingTaskArchiveFile.isFile()) {
			LOGGER.info("Creating map of pending tasks to archive");
			this.pendingTaskArchiveMap = pendingTaskArchiveDB.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending tasks to archive");
			this.pendingTaskArchiveMap = pendingTaskArchiveDB.getHashMap("map");
		}

		// Creating Swift container
		this.swiftAPIClient.createContainer(getContainerName());
	}

	public void exec() throws Exception {
		try {
			if (!versionFileExists()) {
				System.exit(1);
			}

			while (true) {
				cleanUnfinishedArchivedData(properties);
				List<ImageTask> tasksToArchive = tasksToArchive();
				for (ImageTask imageTask : tasksToArchive) {
					if (!imageTask.getStatus().equals(ImageTask.PURGED)) {
						archiveAndUpdateTask(imageTask);
					}
				}
				Thread.sleep(Long.valueOf(properties.getProperty(SapsPropertiesConstants.DEFAULT_ARCHIVER_PERIOD)));
			}
		} catch (InterruptedException | IOException e) {
			LOGGER.error("Error while archiving tasks", e);
		}

		pendingTaskArchiveDB.close();
	}

	protected boolean versionFileExists() {
		this.archiverVersion = getArchiverVersion();

		if (archiverVersion == null || archiverVersion.isEmpty()) {
			LOGGER.error("Archiver version file does not exist...Restart Archiver infrastructure");
			return false;
		}

		return true;
	}

	protected void cleanUnfinishedArchivedData(Properties properties) throws Exception {
		LOGGER.info("Starting garbage collector");
		Collection<ImageTask> taskList = pendingTaskArchiveMap.values();
		for (ImageTask imageTask : taskList) {
			rollBackArchive(imageTask);
			deleteAllTaskFilesFromDisk(imageTask, properties);
			deletePendingInputFilesFromSwift(imageTask);
			deletePendingOutputFromSwift(imageTask);
		}
		LOGGER.info("Garbage collect finished");
	}

	private void deleteAllTaskFilesFromDisk(ImageTask imageTask, Properties properties)
			throws IOException {
		String exportPath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String taskDirPath = exportPath + File.separator + imageTask.getTaskId();
		File taskDir = new File(taskDirPath);

		if (taskDir.exists() && taskDir.isDirectory()) {
			FileUtils.deleteDirectory(taskDir);
		} else {
			LOGGER.info("Path " + taskDirPath + " does not exist or is not a directory!");
		}
	}

	private void deletePendingOutputFromSwift(ImageTask imageTask) {
		LOGGER.debug("Pending task" + imageTask + " still have files in swift");
		deleteOutputFilesFromSwift(imageTask);
	}

	protected void deleteInputFromDisk(final ImageTask imageTask, Properties properties)
			throws IOException {
		String exportPath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String inputsDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "input";
		File inputsDir = new File(inputsDirPath);

		if (inputsDir.exists() && inputsDir.isDirectory()) {
			FileUtils.deleteDirectory(inputsDir);
		} else {
			LOGGER.info("Path " + inputsDirPath + " does not exist or is not a directory!");
		}
	}

	private void deletePreProcessFromDisk(ImageTask imageTask, Properties properties)
			throws IOException {
		String exportPath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String preProcessDirPath = exportPath + File.separator + imageTask.getTaskId()
				+ File.separator + "data" + File.separator + "preprocessing";
		File preProcessDir = new File(preProcessDirPath);

		if (preProcessDir.exists() && preProcessDir.isDirectory()) {
			FileUtils.deleteDirectory(preProcessDir);
		} else {
			LOGGER.info("Path " + preProcessDirPath + " does not exist or is not a directory!");
		}
	}

	protected void deleteOutputFromDisk(final ImageTask imageTask, Properties properties)
			throws IOException {
		String exportPath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String outputDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "output";
		File outputDir = new File(outputDirPath);

		if (outputDir.exists() && outputDir.isDirectory()) {
			FileUtils.deleteDirectory(outputDir);
		} else {
			LOGGER.info("Path " + outputDirPath + " does not exist or is not a directory!");
		}
	}

	private void deleteMetadataFromDisk(ImageTask imageTask, Properties properties)
			throws IOException {
		String exportPath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String metadataDirPath = exportPath + File.separator + imageTask.getTaskId()
				+ File.separator + "metadata";
		File metadataDir = new File(metadataDirPath);

		if (metadataDir.exists() && metadataDir.isDirectory()) {
			FileUtils.deleteDirectory(metadataDir);
		} else {
			LOGGER.info("Path " + metadataDirPath + " does not exist or is not a directory!");
		}
	}

	protected List<ImageTask> tasksToArchive() {
		try {
			return imageStore.getIn(ImageTaskState.FINISHED);
		} catch (SQLException e) {
			LOGGER.error("Error getting " + ImageTaskState.FINISHED + " tasks from DB", e);
		}
		return Collections.emptyList();
	}

	protected void archiveAndUpdateTask(ImageTask imageTask) throws IOException {
		try {
			if (prepareArchive(imageTask)) {
				archive(imageTask);
				if (!archiverHelper.isTaskFailed(imageTask, pendingTaskArchiveMap, imageStore)
						&& !archiverHelper.isTaskRolledBack(imageTask)) {
					finishArchive(imageTask);
				} else {
					deleteAllTaskFilesFromDisk(imageTask, properties);
				}
			} else {
				LOGGER.error("Could not prepare task " + imageTask + " to archive");
			}
		} catch (Exception e) {
			LOGGER.error("Could not archive task " + imageTask.getTaskId(), e);
			deleteAllTaskFilesFromDisk(imageTask, properties);
			rollBackArchive(imageTask);
		}
	}

	protected boolean prepareArchive(ImageTask imageTask) throws SQLException {
		LOGGER.debug("Preparing task " + imageTask.getTaskId() + " to archive");
		if (imageStore.lockTask(imageTask.getTaskId())) {
			imageTask.setState(ImageTaskState.ARCHIVING);

			archiverHelper.updatePendingMapAndDB(imageTask, pendingTaskArchiveDB,
					pendingTaskArchiveMap);

			try {
				LOGGER.info("Updating task data in DB");
				imageStore.updateImageTask(imageTask);
				imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while updating task " + imageTask + " in DB", e);
				rollBackArchive(imageTask);
				return false;
			}

			try {
				imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
						imageTask.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
						+ imageTask.getUpdateTime() + " in DB", e);
			}

			imageStore.unlockTask(imageTask.getTaskId());
			LOGGER.debug("Task " + imageTask.getTaskId() + " ready to archive");
		}
		return true;
	}

	protected void archive(final ImageTask imageTask) throws Exception {
		LOGGER.debug("Federation member is " + imageTask.getFederationMember());

		getFTPServerInfo(imageTask);

		LOGGER.debug("Using FTP Server IP " + ftpServerIP + " and port " + ftpServerPort);
		if (archiveInputs(imageTask) == 0 && archivePreProcess(imageTask) == 0
				&& generateProvenance(imageTask) == 0 && archiveMetadata(imageTask) == 0) {
			archiveOutputs(imageTask);
		}
	}

	protected void getFTPServerInfo(final ImageTask imageTask) throws SQLException {
		ftpServerIP = imageStore.getNFSServerIP(imageTask.getFederationMember());
		ftpServerPort = imageStore.getNFSServerSshPort(imageTask.getFederationMember());
	}

	protected void finishArchive(ImageTask imageTask) throws IOException {
		LOGGER.debug("Finishing archive for task " + imageTask);
		imageTask.setState(ImageTaskState.ARCHIVED);

		String stationId = archiverHelper.getStationId(imageTask, properties);

		imageTask.setStationId(stationId);
		imageTask.setArchiverVersion(archiverVersion);

		try {
			LOGGER.info("Updating task data in DB");
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating task " + imageTask + " in DB", e);
			rollBackArchive(imageTask);
			deleteAllTaskFilesFromDisk(imageTask, properties);
		}

		try {
			imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
					imageTask.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
					+ imageTask.getUpdateTime() + " in DB", e);
		}

		LOGGER.debug("Deleting local output files for task " + imageTask.getTaskId());
		deleteAllTaskFilesFromDisk(imageTask, properties);

		archiverHelper.removeTaskFromPendingMap(imageTask, pendingTaskArchiveDB,
				pendingTaskArchiveMap);
		LOGGER.debug("Task " + imageTask.getTaskId() + " archived");
	}

	protected void rollBackArchive(ImageTask imageTask) {
		LOGGER.debug("Rolling back Archiver for task " + imageTask);
		archiverHelper.removeTaskFromPendingMap(imageTask, pendingTaskArchiveDB,
				pendingTaskArchiveMap);

		try {
			imageStore.removeStateStamp(imageTask.getTaskId(), imageTask.getState(),
					imageTask.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while removing state " + imageTask.getState() + " timestamp", e);
		}

		imageTask.setState(ImageTaskState.FINISHED);

		try {
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating task.", e);
			imageTask.setState(ImageTaskState.ARCHIVING);
			archiverHelper.updatePendingMapAndDB(imageTask, pendingTaskArchiveDB,
					pendingTaskArchiveMap);
		}
	}

	protected int archiveInputs(final ImageTask imageTask) throws Exception {
		LOGGER.debug("MAX_ARCHIVE_TRIES " + MAX_ARCHIVE_TRIES);

		int i;
		for (i = 0; i < MAX_ARCHIVE_TRIES; i++) {
			String remoteTaskInputPath = archiverHelper.getRemoteTaskInputPath(imageTask,
					properties);
			String localTaskInputPath = archiverHelper.getLocalTaskInputPath(imageTask, properties);
			File localTaskInputDir = new File(localTaskInputPath);

			if (!localTaskInputDir.exists()) {
				LOGGER.debug("Path " + localTaskInputPath + " not valid or nonexistent. Creating "
						+ localTaskInputPath);
				localTaskInputDir.mkdirs();
			} else if (!localTaskInputDir.isDirectory()) {
				LOGGER.debug(localTaskInputPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localTaskInputDir.delete();
				localTaskInputDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP, ftpServerPort,
					remoteTaskInputPath, localTaskInputPath, imageTask);

			if (exitValue == 0) {
				if (uploadInputFilesToSwift(imageTask, localTaskInputDir)) {
					LOGGER.debug("Inputs from " + localTaskInputPath + " uploaded successfully");
					return 0;
				}
			} else {
				deleteInputFromDisk(imageTask, properties);
				rollBackArchive(imageTask);
			}
		}

		if (i >= MAX_ARCHIVE_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as failed.");
			imageTask.setState(ImageTaskState.FAILED);
			imageTask.setError("Max archive tries" + MAX_ARCHIVE_TRIES + " reached");
			
			archiverHelper.removeTaskFromPendingMap(imageTask, pendingTaskArchiveDB,
					pendingTaskArchiveMap);
			
			deleteInputFromDisk(imageTask, properties);
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
			
			generateProvenance(imageTask);
			uploadMetadataFilesToSwift(imageTask,
					new File(archiverHelper.getLocalTaskMetadataPath(imageTask, properties)));
		}

		return 1;
	}

	private int archivePreProcess(ImageTask imageTask) throws Exception {
		LOGGER.debug("MAX_ARCHIVE_TRIES " + MAX_ARCHIVE_TRIES);

		int i;
		for (i = 0; i < MAX_ARCHIVE_TRIES; i++) {
			String remoteTaskPreProcessPath = archiverHelper.getRemoteTaskPreProcessPath(imageTask,
					properties);
			String localTaskPreProcessPath = archiverHelper.getLocalTaskPreProcessPath(imageTask,
					properties);
			File localTaskPreProcessDir = new File(localTaskPreProcessPath);

			if (!localTaskPreProcessDir.exists()) {
				LOGGER.debug("Path " + localTaskPreProcessPath
						+ " not valid or nonexistent. Creating " + localTaskPreProcessPath);
				localTaskPreProcessDir.mkdirs();
			} else if (!localTaskPreProcessDir.isDirectory()) {
				LOGGER.debug(localTaskPreProcessPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localTaskPreProcessDir.delete();
				localTaskPreProcessDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP, ftpServerPort,
					remoteTaskPreProcessPath, localTaskPreProcessPath, imageTask);

			if (exitValue == 0) {
				if (uploadPreProcessFilesToSwift(imageTask, localTaskPreProcessDir)) {
					LOGGER.debug("PreProcess from " + localTaskPreProcessPath
							+ " uploaded successfully");
					return 0;
				}
			} else {
				deletePreProcessFromDisk(imageTask, properties);
				rollBackArchive(imageTask);
			}
		}

		if (i >= MAX_ARCHIVE_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as corrupted.");
			imageTask.setState(ImageTaskState.FAILED);
			imageTask.setError("Max archive tries" + MAX_ARCHIVE_TRIES + " reached");
			
			archiverHelper.removeTaskFromPendingMap(imageTask, pendingTaskArchiveDB,
					pendingTaskArchiveMap);
			
			deletePreProcessFromDisk(imageTask, properties);
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
			
			generateProvenance(imageTask);
			uploadMetadataFilesToSwift(imageTask,
					new File(archiverHelper.getLocalTaskMetadataPath(imageTask, properties)));
		}

		return 1;
	}

	private int archiveMetadata(ImageTask imageTask) throws Exception {
		LOGGER.debug("MAX_ARCHIVE_TRIES " + MAX_ARCHIVE_TRIES);

		int i;
		for (i = 0; i < MAX_ARCHIVE_TRIES; i++) {
			String remoteTaskMetadataPath = archiverHelper.getRemoteTaskMetadataPath(imageTask,
					properties);
			String localTaskMetadataPath = archiverHelper.getLocalTaskMetadataPath(imageTask,
					properties);
			File localTaskMetadataDir = new File(localTaskMetadataPath);

			if (!localTaskMetadataDir.exists()) {
				LOGGER.debug("Path " + localTaskMetadataPath
						+ " not valid or nonexistent. Creating " + localTaskMetadataPath);
				localTaskMetadataDir.mkdirs();
			} else if (!localTaskMetadataDir.isDirectory()) {
				LOGGER.debug(localTaskMetadataPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localTaskMetadataDir.delete();
				localTaskMetadataDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP, ftpServerPort,
					remoteTaskMetadataPath, localTaskMetadataPath, imageTask);

			if (exitValue == 0) {
				if (uploadMetadataFilesToSwift(imageTask, localTaskMetadataDir)) {
					LOGGER.debug(
							"Metadata from " + localTaskMetadataPath + " uploaded successfully");
					return 0;
				}
			} else {
				deleteMetadataFromDisk(imageTask, properties);
				rollBackArchive(imageTask);
			}
		}

		if (i >= MAX_ARCHIVE_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as corrupted.");
			imageTask.setState(ImageTaskState.FAILED);
			imageTask.setError("Max archive tries" + MAX_ARCHIVE_TRIES + " reached");
			
			archiverHelper.removeTaskFromPendingMap(imageTask, pendingTaskArchiveDB,
					pendingTaskArchiveMap);
			
			deleteMetadataFromDisk(imageTask, properties);
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
		}

		return 1;
	}

	private int generateProvenance(ImageTask imageTask) throws SQLException {
		String localTaskProvenancePath = archiverHelper.getLocalTaskMetadataPath(imageTask,
				properties);
		File localTaskProvenanceDir = new File(localTaskProvenancePath);

		if (!localTaskProvenanceDir.exists()) {
			LOGGER.debug("Path " + localTaskProvenancePath + " not valid or nonexistent. Creating "
					+ localTaskProvenancePath);
			localTaskProvenanceDir.mkdirs();
		} else if (!localTaskProvenanceDir.isDirectory()) {
			LOGGER.debug(localTaskProvenancePath
					+ " is a file, not a directory. Deleting it and creating a actual directory");
			localTaskProvenanceDir.delete();
			localTaskProvenanceDir.mkdirs();
		}

		if (!generateProvenanceFile(imageTask)) {
			return 1;
		}
		
		return 0;
	}

	private boolean generateProvenanceFile(ImageTask imageTask) throws SQLException {
		ProvenanceUtilImpl provUtilImpl = new ProvenanceUtilImpl();
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd");

		String inputMetadata = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.INPUT_DOWNLOADER_COMPONENT_TYPE,
				SapsPropertiesConstants.METADATA_TYPE);
		String inputOS = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.INPUT_DOWNLOADER_COMPONENT_TYPE,
				SapsPropertiesConstants.OS_TYPE);
		String inputKernelVersion = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.INPUT_DOWNLOADER_COMPONENT_TYPE,
				SapsPropertiesConstants.KERNEL_TYPE);

		String preprocessingMetadata = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.PREPROCESSOR_COMPONENT_TYPE,
				SapsPropertiesConstants.METADATA_TYPE);
		String preprocessingOS = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.PREPROCESSOR_COMPONENT_TYPE,
				SapsPropertiesConstants.OS_TYPE);
		String preprocessingKernelVersion = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.PREPROCESSOR_COMPONENT_TYPE,
				SapsPropertiesConstants.KERNEL_TYPE);

		String outputMetadata = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.WORKER_COMPONENT_TYPE,
				SapsPropertiesConstants.METADATA_TYPE);
		String outputOS = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.WORKER_COMPONENT_TYPE, SapsPropertiesConstants.OS_TYPE);
		String outputKernelVersion = imageStore.getMetadataInfo(imageTask.getTaskId(),
				SapsPropertiesConstants.WORKER_COMPONENT_TYPE, SapsPropertiesConstants.KERNEL_TYPE);

		try {
			LOGGER.debug("Generating provenance for task " + imageTask.getTaskId());
			Document document = provUtilImpl.makeDocument(imageTask.getRegion(),
					df.format(imageTask.getImageDate()), imageTask.getInputGatheringTag(),
					imageTask.getInputPreprocessingTag(), imageTask.getAlgorithmExecutionTag(),
					inputMetadata, inputOS, inputKernelVersion, preprocessingMetadata,
					preprocessingOS, preprocessingKernelVersion, outputMetadata, outputOS,
					outputKernelVersion);

			LOGGER.debug("Writing provenance for task " + imageTask.getTaskId() + " in file "
					+ getProvenanceFilePath(imageTask));
			provUtilImpl.writePROVNProvenanceFile(document, getProvenanceFilePath(imageTask));
		} catch (Exception e) {
			LOGGER.error("Error while generating provenance for task " + imageTask.getTaskId());
			return false;
		}

		return true;
	}

	protected void archiveOutputs(final ImageTask imageTask) throws Exception {
		LOGGER.debug("MAX_ARCHIVE_TRIES " + MAX_ARCHIVE_TRIES);

		int i;
		for (i = 0; i < MAX_ARCHIVE_TRIES; i++) {
			String remoteTaskOutputPath = archiverHelper.getRemoteTaskOutputPath(imageTask,
					properties);
			String localTaskOutputPath = archiverHelper.getLocalTaskOutputPath(imageTask,
					properties);
			File localTaskOutputDir = new File(localTaskOutputPath);

			if (!localTaskOutputDir.exists()) {
				LOGGER.debug("Path " + localTaskOutputPath + " not valid or nonexistent. Creating "
						+ localTaskOutputPath);
				localTaskOutputDir.mkdirs();
			} else if (!localTaskOutputDir.isDirectory()) {
				LOGGER.debug(localTaskOutputPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localTaskOutputDir.delete();
				localTaskOutputDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP, ftpServerPort,
					remoteTaskOutputPath, localTaskOutputPath, imageTask);

			if (exitValue == 0) {
				if (archiverHelper.resultsChecksumOK(imageTask, localTaskOutputDir)) {
					archiverHelper.createTimeoutAndMaxTriesFiles(localTaskOutputDir);

					if (uploadOutputFilesToSwift(imageTask, localTaskOutputDir)) {
						break;
					} else {
						return;
					}
				} else {
					deleteOutputFromDisk(imageTask, properties);
				}
			} else {
				rollBackArchive(imageTask);
				deleteOutputFromDisk(imageTask, properties);
				break;
			}
		}

		if (i >= MAX_ARCHIVE_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as corrupted.");
			imageTask.setState(ImageTaskState.FAILED);
			imageTask.setError("Max archive tries" + MAX_ARCHIVE_TRIES + " reached");
			
			archiverHelper.removeTaskFromPendingMap(imageTask, pendingTaskArchiveDB,
					pendingTaskArchiveMap);
			
			deleteOutputFromDisk(imageTask, properties);
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
			
			generateProvenance(imageTask);
			uploadMetadataFilesToSwift(imageTask,
					new File(archiverHelper.getLocalTaskMetadataPath(imageTask, properties)));
		}
	}

	protected boolean uploadInputFilesToSwift(ImageTask imageTask, File localImageInputFilesDir)
			throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
		String pseudoFolder = getInputPseudoFolder(imageTask);
		String containerName = getContainerName();

		for (File actualFile : localImageInputFilesDir.listFiles()) {
			LOGGER.debug("Actual file " + actualFile.getName());
			int uploadFileTries;
			for (uploadFileTries = 0; uploadFileTries < MAX_SWIFT_UPLOAD_TRIES; uploadFileTries++) {
				try {
					LOGGER.debug("Trying to upload file " + actualFile.getName() + " to "
							+ pseudoFolder + " in " + containerName);
					swiftAPIClient.uploadFile(containerName, actualFile, pseudoFolder);
					break;
				} catch (Exception e) {
					LOGGER.error("Error while uploading files to swift", e);
				}
			}

			if (uploadFileTries >= MAX_SWIFT_UPLOAD_TRIES) {
				LOGGER.debug("Upload tries to swift for file " + actualFile + " has passed max "
						+ MAX_SWIFT_UPLOAD_TRIES);

				rollBackArchive(imageTask);
				deleteOutputFromDisk(imageTask, properties);
				return false;
			}
		}

		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}

	protected boolean uploadPreProcessFilesToSwift(ImageTask imageTask,
			File localImagePreProcessFilesDir) throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
		String pseudoFolder = getPreProcessPseudoFolder(imageTask);
		String containerName = getContainerName();

		for (File actualFile : localImagePreProcessFilesDir.listFiles()) {
			LOGGER.debug("Actual file " + actualFile.getName());
			int uploadFileTries;
			for (uploadFileTries = 0; uploadFileTries < MAX_SWIFT_UPLOAD_TRIES; uploadFileTries++) {
				try {
					LOGGER.debug("Trying to upload file " + actualFile.getName() + " to "
							+ pseudoFolder + " in " + containerName);
					swiftAPIClient.uploadFile(containerName, actualFile, pseudoFolder);
					break;
				} catch (Exception e) {
					LOGGER.error("Error while uploading files to swift", e);
				}
			}

			if (uploadFileTries >= MAX_SWIFT_UPLOAD_TRIES) {
				LOGGER.debug("Upload tries to swift for file " + actualFile + " has passed max "
						+ MAX_SWIFT_UPLOAD_TRIES);

				rollBackArchive(imageTask);
				deletePreProcessFromDisk(imageTask, properties);
				return false;
			}
		}

		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}

	protected boolean uploadMetadataFilesToSwift(ImageTask imageTask,
			File localImageMetadataFilesDir) throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
		String pseudoFolder = getMetadataPseudoFolder(imageTask);
		String containerName = getContainerName();

		for (File actualFile : localImageMetadataFilesDir.listFiles()) {
			LOGGER.debug("Actual file " + actualFile.getName());
			int uploadFileTries;
			for (uploadFileTries = 0; uploadFileTries < MAX_SWIFT_UPLOAD_TRIES; uploadFileTries++) {
				try {
					LOGGER.debug("Trying to upload file " + actualFile.getName() + " to "
							+ pseudoFolder + " in " + containerName);
					swiftAPIClient.uploadFile(containerName, actualFile, pseudoFolder);
					break;
				} catch (Exception e) {
					LOGGER.error("Error while uploading files to swift", e);
				}
			}

			if (uploadFileTries >= MAX_SWIFT_UPLOAD_TRIES) {
				LOGGER.debug("Upload tries to swift for file " + actualFile + " has passed max "
						+ MAX_SWIFT_UPLOAD_TRIES);

				rollBackArchive(imageTask);
				deleteMetadataFromDisk(imageTask, properties);
				return false;
			}
		}

		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}

	protected boolean uploadOutputFilesToSwift(ImageTask imageTask, File localImageOutputFilesDir)
			throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
		String pseudoFolder = getOutputPseudoFolder(imageTask);
		String containerName = getContainerName();

		for (File actualFile : localImageOutputFilesDir.listFiles()) {
			LOGGER.debug("Actual file " + actualFile.getName());
			int uploadFileTries;
			for (uploadFileTries = 0; uploadFileTries < MAX_SWIFT_UPLOAD_TRIES; uploadFileTries++) {
				try {
					LOGGER.debug("Trying to upload file " + actualFile.getName() + " to "
							+ pseudoFolder + " in " + containerName);
					swiftAPIClient.uploadFile(containerName, actualFile, pseudoFolder);
					break;
				} catch (Exception e) {
					LOGGER.error("Error while uploading files to swift", e);
				}
			}

			if (uploadFileTries >= MAX_SWIFT_UPLOAD_TRIES) {
				LOGGER.debug("Upload tries to swift for file " + actualFile + " has passed max "
						+ MAX_SWIFT_UPLOAD_TRIES);

				rollBackArchive(imageTask);
				deleteOutputFromDisk(imageTask, properties);
				return false;
			}
		}

		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}

	protected boolean deletePendingInputFilesFromSwift(ImageTask imageTask) {
		LOGGER.debug("Deleting " + imageTask + " input files from swift");
		String containerName = getContainerName();

		List<String> fileNames = swiftAPIClient.listFilesInContainer(containerName);

		for (String file : fileNames) {
			if ((file.contains(".TIF") || file.contains("MTL") || file.contains(".tar.gz"))) {
				try {
					LOGGER.debug("Trying to delete file " + file + " from " + containerName);
					String swiftTaskInputPseudoFolder = imageTask.getTaskId() + File.separator
							+ "data" + File.separator + "input";
					swiftAPIClient.deleteFile(containerName, swiftTaskInputPseudoFolder, file);
				} catch (Exception e) {
					LOGGER.error("Error while deleting files from swift", e);
					return false;
				}
			}
		}

		return true;
	}

	protected boolean deleteOutputFilesFromSwift(ImageTask imageTask) {
		LOGGER.debug("Deleting " + imageTask + " output files from swift");
		String containerName = getContainerName();

		List<String> fileNames = swiftAPIClient.listFilesInContainer(containerName);

		for (String file : fileNames) {
			if (!file.contains(".TIF") && !file.contains("MTL") && !file.contains(".tar.gz")
					&& !file.contains("README")) {
				try {
					LOGGER.debug("Trying to delete file " + file + " from " + containerName);
					String swiftTaskOutputPseudoFolder = imageTask.getTaskId() + File.separator
							+ "data" + File.separator + "output";
					swiftAPIClient.deleteFile(containerName, swiftTaskOutputPseudoFolder, file);
				} catch (Exception e) {
					LOGGER.error("Error while deleting files from swift", e);
					return false;
				}
			}
		}

		return true;
	}

	private String getContainerName() {
		return properties.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);
	}

	private String getInputPseudoFolder(ImageTask imageTask) {
		if (properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				.endsWith(File.separator)) {
			return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
					+ imageTask.getTaskId() + File.separator + "data" + File.separator + "input"
					+ File.separator;
		}

		return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				+ File.separator + imageTask.getTaskId() + File.separator + "data" + File.separator
				+ "input" + File.separator;
	}

	private String getPreProcessPseudoFolder(ImageTask imageTask) {
		if (properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				.endsWith(File.separator)) {
			return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
					+ imageTask.getTaskId() + File.separator + "data" + File.separator
					+ "preprocessing" + File.separator;
		}

		return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				+ File.separator + imageTask.getTaskId() + File.separator + "data" + File.separator
				+ "preprocessing" + File.separator;
	}

	private String getMetadataPseudoFolder(ImageTask imageTask) {
		if (properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				.endsWith(File.separator)) {
			return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
					+ imageTask.getTaskId() + File.separator + "metadata";
		}

		return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				+ File.separator + imageTask.getTaskId() + File.separator + "metadata";
	}

	private String getOutputPseudoFolder(ImageTask imageTask) {
		if (properties.getProperty(SapsPropertiesConstants.SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX)
				.endsWith(File.separator)) {
			return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
					+ imageTask.getTaskId() + File.separator + "data" + File.separator + "output"
					+ File.separator;
		}

		return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				+ File.separator + imageTask.getTaskId() + File.separator + "data" + File.separator
				+ "output" + File.separator;
	}
	
	private String getProvenanceFilePath(ImageTask imageTask) {
		return properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "metadata"
				+ File.separator + "saps.provn";
	}

	protected String getArchiverVersion() {
		String sebalEngineDirPath = System.getProperty("user.dir");
		File sebalEngineDir = new File(sebalEngineDirPath);

		if (sebalEngineDir.exists() && sebalEngineDir.isDirectory()) {
			for (File file : sebalEngineDir.listFiles()) {
				if (file.getName().startsWith("saps-engine.version.")) {
					String[] sebalEngineVersionFileSplit = file.getName().split("\\.");
					return sebalEngineVersionFileSplit[2];
				}
			}
		}

		return "";
	}

	public String getFtpServerIP() {
		return this.ftpServerIP;
	}

	public void setFtpServerIP(String ftpServerIP) {
		this.ftpServerIP = ftpServerIP;
	}

	public String getFtpServerPort() {
		return this.ftpServerPort;
	}

	public void setFtpServerPort(String ftpServerPort) {
		this.ftpServerPort = ftpServerPort;
	}
}
