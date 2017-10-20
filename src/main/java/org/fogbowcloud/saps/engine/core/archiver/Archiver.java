package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
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
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Archiver {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final SwiftAPIClient swiftAPIClient;
	private File pendingImageFetchFile;
	private DB pendingImageFetchDB;
	private ConcurrentMap<String, ImageTask> pendingImageFetchMap;
	private FTPIntegrationImpl ftpImpl;
	private ArchiverHelper archiverHelper;
	private String fetcherVersion;

	private String ftpServerIP;
	private String ftpServerPort;

	private static int MAX_FETCH_TRIES = 2;
	private static int MAX_SWIFT_UPLOAD_TRIES = 2;

	public static final Logger LOGGER = Logger.getLogger(Archiver.class);

	public Archiver(Properties properties) throws SQLException {
		this(properties, new JDBCImageDataStore(properties), new SwiftAPIClient(properties),
				new FTPIntegrationImpl(), new ArchiverHelper());

		LOGGER.info("Creating fetcher");
		LOGGER.debug("Imagestore " + properties.getProperty("datastore_ip") + ":"
				+ properties.getProperty("datastore_port") + " FTPServer " + ftpServerIP + ":"
				+ ftpServerPort);
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
		this.pendingImageFetchDB = DBMaker.newFileDB(pendingImageFetchFile).make();

		if (!pendingImageFetchFile.exists() || !pendingImageFetchFile.isFile()) {
			LOGGER.info("Creating map of pending images to fetch");
			this.pendingImageFetchMap = pendingImageFetchDB.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending images to fetch");
			this.pendingImageFetchMap = pendingImageFetchDB.getHashMap("map");
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
				cleanUnfinishedFetchedData(properties);
				List<ImageTask> imagesToFetch = imagesToFetch();
				for (ImageTask imageData : imagesToFetch) {
					if (!imageData.getStatus().equals(ImageTask.PURGED)) {
						fetchAndUpdateImage(imageData);
					}
				}
				Thread.sleep(Long.valueOf(properties
						.getProperty(SapsPropertiesConstants.DEFAULT_FETCHER_PERIOD)));
			}
		} catch (InterruptedException e) {
			LOGGER.error("Error while fetching images", e);
		} catch (IOException e) {
			LOGGER.error("Error while fetching images", e);
		}

		pendingImageFetchDB.close();
	}

	protected boolean versionFileExists() {
		this.fetcherVersion = getFetcherVersion();

		if (fetcherVersion == null || fetcherVersion.isEmpty()) {
			LOGGER.error("Fmask version file does not exist");
			LOGGER.info("Restart Fetcher infrastructure");

			return false;
		}

		return true;
	}

	protected void cleanUnfinishedFetchedData(Properties properties) throws Exception {
		LOGGER.info("Starting garbage collector");
		Collection<ImageTask> data = pendingImageFetchMap.values();
		for (ImageTask imageData : data) {
			rollBackFetch(imageData);
			deleteInputsFromDisk(imageData, properties);
			deleteResultsFromDisk(imageData, properties);
			deletePendingInputFilesFromSwift(imageData, properties);
			deletePendingResultsFromSwift(imageData, properties);
		}
		LOGGER.info("Garbage collect finished");
	}

	private void deletePendingResultsFromSwift(ImageTask imageData, Properties properties)
			throws Exception {
		LOGGER.debug("Pending image" + imageData + " still have files in swift");
		deleteResultFilesFromSwift(imageData, properties);
	}

	protected void deleteInputsFromDisk(final ImageTask imageData, Properties properties)
			throws IOException {
		String exportPath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String inputsDirPath = exportPath + File.separator + "images" + File.separator
				+ imageData.getCollectionTierName();
		File inputsDir = new File(inputsDirPath);

		if (inputsDir.exists() && inputsDir.isDirectory()) {
			FileUtils.deleteDirectory(inputsDir);
		} else {
			LOGGER.info("Path " + inputsDirPath + " does not exist or is not a directory!");
		}
	}

	protected void deleteResultsFromDisk(final ImageTask imageData, Properties properties)
			throws IOException {
		String exportPath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String resultsDirPath = exportPath + File.separator + "results" + File.separator
				+ imageData.getCollectionTierName();
		File resultsDir = new File(resultsDirPath);

		if (resultsDir.exists() && resultsDir.isDirectory()) {
			FileUtils.deleteDirectory(resultsDir);
		} else {
			LOGGER.info("Path " + resultsDirPath + " does not exist or is not a directory!");
		}

	}

	protected List<ImageTask> imagesToFetch() {
		try {
			return imageStore.getIn(ImageTaskState.FINISHED);
		} catch (SQLException e) {
			LOGGER.error("Error getting " + ImageTaskState.FINISHED + " images from DB", e);
		}
		return Collections.EMPTY_LIST;
	}

	protected void fetchAndUpdateImage(ImageTask imageData) throws IOException,
			InterruptedException {
		try {
			if (prepareFetch(imageData)) {
				fetch(imageData);
				if (!archiverHelper.isImageCorrupted(imageData, pendingImageFetchMap, imageStore)
						&& !archiverHelper.isImageRolledBack(imageData)) {
					finishFetch(imageData);
				} else {
					deleteInputsFromDisk(imageData, properties);
					deleteResultsFromDisk(imageData, properties);
				}
			} else {
				LOGGER.error("Could not prepare image " + imageData + " to fetch");
			}
		} catch (Exception e) {
			LOGGER.error("Could not fetch image " + imageData.getCollectionTierName(), e);
			deleteInputsFromDisk(imageData, properties);
			deleteResultsFromDisk(imageData, properties);
			rollBackFetch(imageData);
		}
	}

	protected boolean prepareFetch(ImageTask imageData) throws SQLException, IOException {
		LOGGER.debug("Preparing image " + imageData.getCollectionTierName() + " to fetch");
		if (imageStore.lockTask(imageData.getName())) {
			imageData.setState(ImageTaskState.ARCHIVING);

			archiverHelper.updatePendingMapAndDB(imageData, pendingImageFetchDB,
					pendingImageFetchMap);

			try {
				LOGGER.info("Updating image data in DB");
				imageStore.updateImageTask(imageData);
				imageData.setUpdateTime(imageStore.getTask(imageData.getName()).getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while updating image " + imageData + " in DB", e);
				rollBackFetch(imageData);
				return false;
			}

			try {
				imageStore.addStateStamp(imageData.getName(), imageData.getState(),
						imageData.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while adding state " + imageData.getState() + " timestamp "
						+ imageData.getUpdateTime() + " in DB", e);
			}

			imageStore.unlockTask(imageData.getName());
			LOGGER.debug("Image " + imageData.getCollectionTierName() + " ready to fetch");
		}
		return true;
	}

	protected void fetch(final ImageTask imageTask) throws Exception {
		LOGGER.debug("Federation member is " + imageTask.getFederationMember());

		getFTPServerInfo(imageTask);

		LOGGER.debug("Using FTP Server IP " + ftpServerIP + " and port " + ftpServerPort);
		if (fetchInputs(imageTask) == 0) {
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
		imageTask.setArchiverVersion(fetcherVersion);

		try {
			LOGGER.info("Updating image data in DB");
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getName()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image " + imageTask + " in DB", e);
			rollBackFetch(imageTask);
			deleteInputsFromDisk(imageTask, properties);
			deleteResultsFromDisk(imageTask, properties);
		}

		try {
			imageStore.addStateStamp(imageTask.getName(), imageTask.getState(),
					imageTask.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
					+ imageTask.getUpdateTime() + " in DB", e);
		}

		LOGGER.debug("Deleting local results file for " + imageTask.getCollectionTierName());

		deleteInputsFromDisk(imageTask, properties);
		deleteResultsFromDisk(imageTask, properties);

		archiverHelper.removeImageFromPendingMap(imageTask, pendingImageFetchDB,
				pendingImageFetchMap);

		LOGGER.debug("Image " + imageTask.getCollectionTierName() + " fetched");
	}

	protected void rollBackFetch(ImageTask imageTask) {
		LOGGER.debug("Rolling back Fetcher for image " + imageTask);
		archiverHelper.removeImageFromPendingMap(imageTask, pendingImageFetchDB,
				pendingImageFetchMap);

		try {
			imageStore.removeStateStamp(imageTask.getName(), imageTask.getState(),
					imageTask.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while removing state " + imageTask.getState() + " timestamp", e);
		}

		imageTask.setState(ImageTaskState.FINISHED);

		try {
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getName()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image data.", e);
			imageTask.setState(ImageTaskState.ARCHIVING);
			archiverHelper.updatePendingMapAndDB(imageTask, pendingImageFetchDB,
					pendingImageFetchMap);
		}
	}

	protected int fetchInputs(final ImageTask imageTask) throws Exception {
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
				deleteInputsFromDisk(imageTask, properties);
				rollBackFetch(imageTask);
			}
		}

		if (i >= MAX_FETCH_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as corrupted.");
			imageTask.setState(ImageTaskState.CORRUPTED);
			archiverHelper.removeImageFromPendingMap(imageTask, pendingImageFetchDB,
					pendingImageFetchMap);
			deleteInputsFromDisk(imageTask, properties);
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
					deleteResultsFromDisk(imageTask, properties);
				}
			} else {
				rollBackFetch(imageTask);
				deleteResultsFromDisk(imageTask, properties);
				break;
			}
		}

		if (i >= MAX_FETCH_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageTask + " as corrupted.");
			imageTask.setState(ImageTaskState.CORRUPTED);
			archiverHelper.removeImageFromPendingMap(imageTask, pendingImageFetchDB,
					pendingImageFetchMap);
			deleteResultsFromDisk(imageTask, properties);
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

				rollBackFetch(imageTask);
				deleteResultsFromDisk(imageTask, properties);
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

				rollBackFetch(imageTask);
				deleteResultsFromDisk(imageTask, properties);
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
			if (file.contains(imageTask.getCollectionTierName())
					&& (file.contains(".TIF") || file.contains("MTL") || file.contains(".tar.gz"))) {
				try {
					LOGGER.debug("Trying to delete file " + file + " from " + containerName);
					String localImageInputsPath = properties.get("fetcher_volume_path")
							+ File.separator + "images" + File.separator
							+ imageTask.getCollectionTierName();
					swiftAPIClient.deleteFile(containerName, getOutputPseudoFolder(new File(
							localImageInputsPath)), file);
				} catch (Exception e) {
					LOGGER.error("Error while deleting files from swift", e);
					return false;
				}
			}
		}

		return true;
	}

	protected boolean deleteResultFilesFromSwift(ImageTask imageTask, Properties properties)
			throws Exception {
		LOGGER.debug("Deleting " + imageTask + " result files from swift");
		String containerName = getContainerName();

		List<String> fileNames = swiftAPIClient.listFilesInContainer(containerName);

		for (String file : fileNames) {
			if (file.contains(imageTask.getCollectionTierName()) && !file.contains(".TIF")
					&& !file.contains("MTL") && !file.contains(".tar.gz")
					&& !file.contains("README")) {
				try {
					LOGGER.debug("Trying to delete file " + file + " from " + containerName);
					String localImageResultsPath = properties.get("fetcher_volume_path")
							+ File.separator + "results" + File.separator
							+ imageTask.getCollectionTierName();
					swiftAPIClient.deleteFile(containerName, getOutputPseudoFolder(new File(
							localImageResultsPath)), file);
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

	protected String getFetcherVersion() {
		String sebalEngineDirPath = System.getProperty("user.dir");
		File sebalEngineDir = new File(sebalEngineDirPath);

		if (sebalEngineDir.exists() && sebalEngineDir.isDirectory()) {
			for (File file : sebalEngineDir.listFiles()) {
				if (file.getName().startsWith("sebal-engine.version.")) {
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
