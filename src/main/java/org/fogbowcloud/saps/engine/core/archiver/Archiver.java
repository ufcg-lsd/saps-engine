package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Archiver {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final SwiftAPIClient swiftAPIClient;
	private File pendingImageFetchFile;
	private DB pendingImageArchiveDB;
	private ConcurrentMap<String, ImageTask> pendingImageArchiveMap;
	private FTPIntegrationImpl ftpImpl;
	private ArchiverHelper archiverHelper;
	private String archiverVersion;

	private String ftpServerIP;
	private String ftpServerPort;

	private static int MAX_FETCH_TRIES = 2;
	private static int MAX_SWIFT_UPLOAD_TRIES = 2;

	public static final Logger LOGGER = Logger.getLogger(Archiver.class);

	public Archiver(Properties properties) throws SQLException {
		this(properties, new JDBCImageDataStore(properties), new SwiftAPIClient(properties),
				new FTPIntegrationImpl(), new ArchiverHelper());

		LOGGER.info("Creating archiver");
		LOGGER.info("Imagestore " + properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_IP)
						+ ":" + properties.getProperty(SapsPropertiesConstants.IMAGE_DATASTORE_PORT)
						+ " FTPServer " + ftpServerIP + ":" + ftpServerPort);
	}

	protected Archiver(Properties properties, ImageDataStore imageStore,
			SwiftAPIClient swiftAPIClient, FTPIntegrationImpl ftpImpl, ArchiverHelper archiverHelper) {
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

		this.pendingImageFetchFile = new File("pending-image-fetch.db");
		this.pendingImageArchiveDB = DBMaker.newFileDB(pendingImageFetchFile).make();

		if (!pendingImageFetchFile.exists() || !pendingImageFetchFile.isFile()) {
			LOGGER.info("Creating map of pending images to fetch");
			this.pendingImageArchiveMap = pendingImageArchiveDB.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending images to fetch");
			this.pendingImageArchiveMap = pendingImageArchiveDB.getHashMap("map");
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
						archiveAndUpdateImage(imageTask);
					}
				}
				Thread.sleep(Long.valueOf(
						properties.getProperty(SapsPropertiesConstants.DEFAULT_ARCHIVER_PERIOD)));
			}
		} catch (InterruptedException e) {
			LOGGER.error("Error while fetching images", e);
		} catch (IOException e) {
			LOGGER.error("Error while fetching images", e);
		}

		pendingImageArchiveDB.close();
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
		Collection<ImageTask> taskList = pendingImageArchiveMap.values();
		for (ImageTask imageTask : taskList) {
			rollBackArchive(imageTask);
			deleteInputFromDisk(imageTask, properties);
			deleteOutputFromDisk(imageTask, properties);
			deletePendingInputFilesFromSwift(imageTask, properties);
			deletePendingOutputFromSwift(imageTask, properties);
		}
		LOGGER.info("Garbage collect finished");
	}

	private void deletePendingOutputFromSwift(ImageTask imageTask, Properties properties)
			throws Exception {
		LOGGER.debug("Pending task" + imageTask + " still have files in swift");
		deleteOutputFilesFromSwift(imageTask, properties);
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

	protected List<ImageTask> tasksToArchive() {
		try {
			return imageStore.getIn(ImageTaskState.FINISHED);
		} catch (SQLException e) {
			LOGGER.error("Error getting " + ImageTaskState.FINISHED + " tasks from DB", e);
		}
		return Collections.EMPTY_LIST;
	}

	protected void archiveAndUpdateImage(ImageTask imageTask) throws IOException,
			InterruptedException {
		try {
			if (prepareArchive(imageTask)) {
				archive(imageTask);
				if (!archiverHelper.isImageCorrupted(imageTask, pendingImageArchiveMap, imageStore)
						&& !archiverHelper.isImageRolledBack(imageTask)) {
					finishFetch(imageTask);
				} else {
					deleteInputFromDisk(imageTask, properties);
					deleteOutputFromDisk(imageTask, properties);
				}
			} else {
				LOGGER.error("Could not prepare image " + imageTask + " to fetch");
			}
		} catch (Exception e) {
			LOGGER.error("Could not fetch image " + imageTask.getCollectionTierName(), e);
			deleteInputFromDisk(imageTask, properties);
			deleteOutputFromDisk(imageTask, properties);
			rollBackArchive(imageTask);
		}
	}

	protected boolean prepareArchive(ImageTask imageTask) throws SQLException, IOException {
		LOGGER.debug("Preparing task " + imageTask.getTaskId() + " to archive");
		if (imageStore.lockTask(imageTask.getTaskId())) {
			imageTask.setState(ImageTaskState.ARCHIVING);

			archiverHelper.updatePendingMapAndDB(imageTask, pendingImageArchiveDB,
					pendingImageArchiveMap);

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
		if (archiveInputs(imageTask) == 0) {
			fetchOutputs(imageTask);
		}
	}

	protected void getFTPServerInfo(final ImageTask imageTask) throws SQLException {
		ftpServerIP = imageStore.getNFSServerIP(imageTask.getFederationMember());
		ftpServerPort = imageStore.getNFSServerSshPort(imageTask.getFederationMember());
	}

	protected void finishFetch(ImageTask imageTask) throws IOException, SQLException {
		LOGGER.debug("Finishing fetch for image " + imageTask);
		imageTask.setState(ImageTaskState.ARCHIVED);

		String stationId = archiverHelper.getStationId(imageTask, properties);

		imageTask.setStationId(stationId);
		imageTask.setArchiverVersion(archiverVersion);

		try {
			LOGGER.info("Updating image data in DB");
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getName()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image " + imageTask + " in DB", e);
			rollBackArchive(imageTask);
			deleteInputFromDisk(imageTask, properties);
			deleteOutputFromDisk(imageTask, properties);
		}

		try {
			imageStore.addStateStamp(imageTask.getName(), imageTask.getState(),
					imageTask.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
					+ imageTask.getUpdateTime() + " in DB", e);
		}

		LOGGER.debug("Deleting local results file for " + imageTask.getCollectionTierName());

		deleteInputFromDisk(imageTask, properties);
		deleteOutputFromDisk(imageTask, properties);

		archiverHelper.removeImageFromPendingMap(imageTask, pendingImageArchiveDB,
				pendingImageArchiveMap);

		LOGGER.debug("Image " + imageTask.getCollectionTierName() + " fetched");
	}

	protected void rollBackArchive(ImageTask imageTask) {
		LOGGER.debug("Rolling back Archiver for task " + imageTask);
		archiverHelper.removeImageFromPendingMap(imageTask, pendingImageArchiveDB,
				pendingImageArchiveMap);

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
			archiverHelper.updatePendingMapAndDB(imageTask, pendingImageArchiveDB,
					pendingImageArchiveMap);
		}
	}

	protected int archiveInputs(final ImageTask imageTask) throws Exception {
		LOGGER.debug("MAX_FETCH_TRIES " + MAX_FETCH_TRIES);

		int i;
		for (i = 0; i < MAX_FETCH_TRIES; i++) {
			String remoteImageInputsPath = archiverHelper.getRemoteImageInputsPath(imageTask,
					properties);
			String localImageInputsPath = archiverHelper.getLocalImageInputsPath(imageTask,
					properties);
			File localImageInputsDir = new File(localImageInputsPath);

			if (!localImageInputsDir.exists()) {
				LOGGER.debug("Path " + localImageInputsPath
						+ " not valid or nonexistent. Creating " + localImageInputsPath);
				localImageInputsDir.mkdirs();
			} else if (!localImageInputsDir.isDirectory()) {
				LOGGER.debug(localImageInputsPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localImageInputsDir.delete();
				localImageInputsDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP, ftpServerPort,
					remoteImageInputsPath, localImageInputsPath, imageTask);

			if (exitValue == 0) {
				if (uploadInputFilesToSwift(imageTask, localImageInputsDir)) {
					LOGGER.debug("Inputs from " + localImageInputsPath + " uploaded successfully");
					return 0;
				}
			} else {
				deleteInputFromDisk(imageTask, properties);
				rollBackArchive(imageTask);
			}
		}

		if (i >= MAX_FETCH_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as corrupted.");
			imageTask.setState(ImageTaskState.CORRUPTED);
			archiverHelper.removeImageFromPendingMap(imageTask, pendingImageArchiveDB,
					pendingImageArchiveMap);
			deleteInputFromDisk(imageTask, properties);
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getName()).getUpdateTime());
		}

		return 1;
	}

	protected void fetchOutputs(final ImageTask imageTask) throws Exception, IOException,
			SQLException {
		// FIXME: doc-it (we want to know the max tries logic)
		LOGGER.debug("MAX_FETCH_TRIES " + MAX_FETCH_TRIES);

		int i;
		for (i = 0; i < MAX_FETCH_TRIES; i++) {
			String remoteImageResultsPath = archiverHelper.getRemoteImageResultsPath(imageTask,
					properties);
			String localImageResultsPath = archiverHelper.getLocalImageResultsPath(imageTask,
					properties);
			File localImageResultsDir = new File(localImageResultsPath);

			if (!localImageResultsDir.exists()) {
				LOGGER.debug("Path " + localImageResultsPath
						+ " not valid or nonexistent. Creating " + localImageResultsPath);
				localImageResultsDir.mkdirs();
			} else if (!localImageResultsDir.isDirectory()) {
				LOGGER.debug(localImageResultsPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localImageResultsDir.delete();
				localImageResultsDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP, ftpServerPort,
					remoteImageResultsPath, localImageResultsPath, imageTask);

			if (exitValue == 0) {
				if (archiverHelper.resultsChecksumOK(imageTask, localImageResultsDir)) {
					archiverHelper.createTimeoutAndMaxTriesFiles(localImageResultsDir);

					if (uploadOutputFilesToSwift(imageTask, localImageResultsDir)) {
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

		if (i >= MAX_FETCH_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as corrupted.");
			imageTask.setState(ImageTaskState.CORRUPTED);
			archiverHelper.removeImageFromPendingMap(imageTask, pendingImageArchiveDB,
					pendingImageArchiveMap);
			deleteOutputFromDisk(imageTask, properties);
			// TODO: see if this have to be in try-catch
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getName()).getUpdateTime());
		}
	}

	protected boolean uploadInputFilesToSwift(ImageTask imageTask, File localImageInputFilesDir)
			throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
		String pseudoFolder = getInputPseudoFolder(localImageInputFilesDir);
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
					continue;
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

	protected boolean uploadOutputFilesToSwift(ImageTask imageTask, File localImageOutputFilesDir)
			throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
		String pseudoFolder = getOutputPseudoFolder(localImageOutputFilesDir);
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
					continue;
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

	protected boolean deletePendingInputFilesFromSwift(ImageTask imageTask, Properties properties)
			throws Exception {
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

	protected boolean deleteOutputFilesFromSwift(ImageTask imageTask, Properties properties)
			throws Exception {
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

	private String getInputPseudoFolder(File localImageInputsDir) {
		if (properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				.endsWith(File.separator)) {
			return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
					+ localImageInputsDir.getName() + File.separator;
		}

		return properties.getProperty(SapsPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
				+ File.separator + localImageInputsDir.getName() + File.separator;
	}

	private String getOutputPseudoFolder(File localImageResultsDir) {
		if (properties.getProperty(SapsPropertiesConstants.SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX)
				.endsWith(File.separator)) {
			return properties
					.getProperty(SapsPropertiesConstants.SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX)
					+ localImageResultsDir.getName() + File.separator;
		}

		return properties.getProperty(SapsPropertiesConstants.SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX)
				+ File.separator + localImageResultsDir.getName() + File.separator;
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
