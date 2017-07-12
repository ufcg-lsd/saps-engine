package org.fogbowcloud.sebal.engine.sebal.fetcher;

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
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.swift.SwiftAPIClient;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Fetcher {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final SwiftAPIClient swiftAPIClient;
	private File pendingImageFetchFile;
	private DB pendingImageFetchDB;
	private ConcurrentMap<String, ImageData> pendingImageFetchMap;
	private FTPIntegrationImpl ftpImpl;
	private FetcherHelper fetcherHelper;
	private String fetcherVersion;

	private String ftpServerIP;
	private String ftpServerPort;

	private static int MAX_FETCH_TRIES = 2;
	private static int MAX_SWIFT_UPLOAD_TRIES = 2;
	
	public static final Logger LOGGER = Logger.getLogger(Fetcher.class);

	public Fetcher(Properties properties) throws SQLException {

		this(properties, new JDBCImageDataStore(properties), new SwiftAPIClient(
				properties), new FTPIntegrationImpl(), new FetcherHelper());

		LOGGER.info("Creating fetcher");
		LOGGER.debug("Imagestore " + properties.getProperty("datastore_ip") + ":" + properties.getProperty("datastore_port")
				+ " FTPServer " + ftpServerIP + ":" + ftpServerPort);
	}

	protected Fetcher(Properties properties, ImageDataStore imageStore, SwiftAPIClient swiftAPIClient,
			FTPIntegrationImpl ftpImpl, FetcherHelper fetcherHelper) {

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}

		if (imageStore == null) {
			throw new IllegalArgumentException(
					"Imagestore arg must not be null.");
		}

		this.properties = properties;
		this.imageStore = imageStore;
		this.swiftAPIClient = swiftAPIClient;
		this.ftpImpl = ftpImpl;
		this.fetcherHelper = fetcherHelper;

		this.pendingImageFetchFile = new File("pending-image-fetch.db");
		this.pendingImageFetchDB = DBMaker.newFileDB(pendingImageFetchFile)
				.make();

		if (!pendingImageFetchFile.exists() || !pendingImageFetchFile.isFile()) {
			LOGGER.info("Creating map of pending images to fetch");
			this.pendingImageFetchMap = pendingImageFetchDB
					.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending images to fetch");
			this.pendingImageFetchMap = pendingImageFetchDB.getHashMap("map");
		}
		
		// Creating Swift container
		this.swiftAPIClient.createContainer(getContainerName());
	}

	public void exec() throws Exception {

		try {
			if(!versionFileExists()) {
				System.exit(1);
			}
			
			while (true) {
				cleanUnfinishedFetchedData(properties);
				List<ImageData> imagesToFetch = imagesToFetch();
				for (ImageData imageData : imagesToFetch) {
					if (!imageData.getImageStatus().equals(ImageData.PURGED)) {
						fetchAndUpdateImage(imageData);
					}
				}
				Thread.sleep(Long.valueOf(properties
						.getProperty(SebalPropertiesConstants.DEFAULT_FETCHER_PERIOD)));
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
		
		if(fetcherVersion == null || fetcherVersion.isEmpty()) {
			LOGGER.error("Fmask version file does not exist");
			LOGGER.info("Restart Fetcher infrastructure");
						
			return false;
		}
		
		return true;
	}

	protected void cleanUnfinishedFetchedData(Properties properties)
			throws Exception {
		LOGGER.info("Starting garbage collector");
		Collection<ImageData> data = pendingImageFetchMap.values();
		for (ImageData imageData : data) {
			rollBackFetch(imageData);
			deleteInputsFromDisk(imageData, properties);
			deleteResultsFromDisk(imageData, properties);
			deletePendingInputFilesFromSwift(imageData, properties);
			deletePendingResultsFromSwift(imageData, properties);
		}
		LOGGER.info("Garbage collect finished");
	}

	private void deletePendingResultsFromSwift(ImageData imageData,
			Properties properties) throws Exception {
		LOGGER.debug("Pending image" + imageData + " still have files in swift");
		deleteResultFilesFromSwift(imageData, properties);
	}
	
	protected void deleteInputsFromDisk(final ImageData imageData,
			Properties properties) throws IOException {
		String exportPath = properties.getProperty(SebalPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String inputsDirPath = exportPath + File.separator + "images"
				+ File.separator + imageData.getCollectionTierName();
		File inputsDir = new File(inputsDirPath);

		if (inputsDir.exists() && inputsDir.isDirectory()) {
			FileUtils.deleteDirectory(inputsDir);
		} else {
			LOGGER.info("Path " + inputsDirPath
					+ " does not exist or is not a directory!");
		}
	}

	protected void deleteResultsFromDisk(final ImageData imageData,
			Properties properties) throws IOException {
		String exportPath = properties.getProperty(SebalPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH);
		String resultsDirPath = exportPath + File.separator + "results"
				+ File.separator + imageData.getCollectionTierName();
		File resultsDir = new File(resultsDirPath);

		if (resultsDir.exists() && resultsDir.isDirectory()) {
			FileUtils.deleteDirectory(resultsDir);
		} else {
			LOGGER.info("Path " + resultsDirPath
					+ " does not exist or is not a directory!");
		}

	}

	protected List<ImageData> imagesToFetch() {
		try {
			return imageStore.getIn(ImageState.FINISHED);
		} catch (SQLException e) {
			LOGGER.error("Error getting " + ImageState.FINISHED + " images from DB", e);
		}
		return Collections.EMPTY_LIST;
	}

	protected void fetchAndUpdateImage(ImageData imageData) throws IOException,
			InterruptedException {
		try {
			if(prepareFetch(imageData)) {				
				fetch(imageData);
				if (!fetcherHelper.isImageCorrupted(imageData, pendingImageFetchMap,
						imageStore) && !fetcherHelper.isImageRolledBack(imageData)) {
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

	protected boolean prepareFetch(ImageData imageData) throws SQLException,
			IOException {
		LOGGER.debug("Preparing image " + imageData.getCollectionTierName() + " to fetch");
		if (imageStore.lockImage(imageData.getName())) {
			
			imageData.setState(ImageState.FETCHING);
			
			fetcherHelper.updatePendingMapAndDB(imageData,
					pendingImageFetchDB, pendingImageFetchMap);

			try {
				LOGGER.info("Updating image data in DB");
				imageStore.updateImage(imageData);
				imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while updating image " + imageData
						+ " in DB", e);
				rollBackFetch(imageData);
				return false;
			}

			try {
				imageStore.addStateStamp(imageData.getName(),
						imageData.getState(), imageData.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while adding state " + imageData.getState()
						+ " timestamp " + imageData.getUpdateTime() + " in DB", e);
			}
			imageStore.unlockImage(imageData.getName());

			LOGGER.debug("Image " + imageData.getCollectionTierName() + " ready to fetch");
		}
		return true;
	}
	
	protected void fetch(final ImageData imageData) throws Exception {
		ftpServerIP = imageStore.getNFSServerIP(imageData.getFederationMember());

		LOGGER.debug("Federation member is " + imageData.getFederationMember());
		if(imageData.getFederationMember().equals(SebalPropertiesConstants.AZURE_FEDERATION_MEMBER)) {			
			ftpServerPort = properties.getProperty(SebalPropertiesConstants.AZURE_FTP_SERVER_PORT);
		} else {
			ftpServerPort = properties.getProperty(SebalPropertiesConstants.DEFAULT_FTP_SERVER_PORT);
		}
		
		LOGGER.debug("Using FTP Server IP " + ftpServerIP + " and port " + ftpServerPort);		
		if (fetchInputs(imageData) == 0) {
			fetchOutputs(imageData);
		}
	}

	protected void finishFetch(ImageData imageData) throws IOException,
			SQLException {

		LOGGER.debug("Finishing fetch for image " + imageData);
		imageData.setState(ImageState.FETCHED);

		String stationId = fetcherHelper.getStationId(imageData, properties);

		imageData.setStationId(stationId);
		imageData.setFetcherVersion(fetcherVersion);

		try {
			LOGGER.info("Updating image data in DB");
			imageStore.updateImage(imageData);
			imageData.setUpdateTime(imageStore.getImage(imageData.getName())
					.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image " + imageData + " in DB",
					e);
			rollBackFetch(imageData);
			deleteInputsFromDisk(imageData, properties);
			deleteResultsFromDisk(imageData, properties);
		}

		try {
			imageStore.addStateStamp(imageData.getName(), imageData.getState(),
					imageData.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state " + imageData.getState()
					+ " timestamp " + imageData.getUpdateTime() + " in DB", e);
		}

		LOGGER.debug("Deleting local results file for " + imageData.getCollectionTierName());

		deleteInputsFromDisk(imageData, properties);
		deleteResultsFromDisk(imageData, properties);

		fetcherHelper.removeImageFromPendingMap(imageData, pendingImageFetchDB,
				pendingImageFetchMap);

		LOGGER.debug("Image " + imageData.getCollectionTierName() + " fetched");
	}

	protected void rollBackFetch(ImageData imageData) {
		LOGGER.debug("Rolling back Fetcher for image " + imageData);

		fetcherHelper.removeImageFromPendingMap(imageData, pendingImageFetchDB, pendingImageFetchMap);
		try {
			imageStore.removeStateStamp(imageData.getName(),
					imageData.getState(), imageData.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while removing state " + imageData.getState()
					+ " timestamp", e);
		}
		imageData.setState(ImageState.FINISHED);

		try {
			imageStore.updateImage(imageData);
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image data.", e);
			imageData.setState(ImageState.FETCHING);
			fetcherHelper.updatePendingMapAndDB(imageData, pendingImageFetchDB, pendingImageFetchMap);
		}
	}
	
	protected int fetchInputs(final ImageData imageData)
			throws Exception {
		LOGGER.debug("MAX_FETCH_TRIES " + MAX_FETCH_TRIES);
		
		int i;
		for (i = 0; i < MAX_FETCH_TRIES; i++) {

			String remoteImageInputsPath = fetcherHelper
					.getRemoteImageInputsPath(imageData, properties);

			String localImageInputsPath = fetcherHelper
					.getLocalImageInputsPath(imageData, properties);

			File localImageInputsDir = new File(localImageInputsPath);

			if (!localImageInputsDir.exists()) {
				LOGGER.debug("Path " + localImageInputsPath
						+ " not valid or nonexistent. Creating "
						+ localImageInputsPath);
				localImageInputsDir.mkdirs();
			} else if (!localImageInputsDir.isDirectory()) {
				LOGGER.debug(localImageInputsPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localImageInputsDir.delete();
				localImageInputsDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP,
					ftpServerPort, remoteImageInputsPath, localImageInputsPath,
					imageData);

			if (exitValue == 0) {
				if (uploadInputFilesToSwift(imageData, localImageInputsDir)) {
					LOGGER.debug("Inputs from " + localImageInputsPath
							+ " uploaded successfully");
					return 0;
				}
			} else {
				deleteInputsFromDisk(imageData, properties);
				rollBackFetch(imageData);
			}
		}
		
		if (i >= MAX_FETCH_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageData
					+ " as corrupted.");
			imageData.setState(ImageState.CORRUPTED);
			fetcherHelper.removeImageFromPendingMap(imageData,
					pendingImageFetchDB, pendingImageFetchMap);
			deleteInputsFromDisk(imageData, properties);
			imageStore.updateImage(imageData);
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		}		

		return 1;
	}

	protected void fetchOutputs(final ImageData imageData)
			throws Exception, IOException, SQLException {
		// FIXME: doc-it (we want to know the max tries logic)
		LOGGER.debug("MAX_FETCH_TRIES " + MAX_FETCH_TRIES);

		int i;
		for (i = 0; i < MAX_FETCH_TRIES; i++) {

			String remoteImageResultsPath = fetcherHelper
					.getRemoteImageResultsPath(imageData, properties);

			String localImageResultsPath = fetcherHelper
					.getLocalImageResultsPath(imageData, properties);

			File localImageResultsDir = new File(localImageResultsPath);	

			if (!localImageResultsDir.exists()) {
				LOGGER.debug("Path " + localImageResultsPath
						+ " not valid or nonexistent. Creating "
						+ localImageResultsPath);
				localImageResultsDir.mkdirs();
			} else if (!localImageResultsDir.isDirectory()) {
				LOGGER.debug(localImageResultsPath
						+ " is a file, not a directory. Deleting it and creating a actual directory");
				localImageResultsDir.delete();
				localImageResultsDir.mkdirs();
			}

			int exitValue = ftpImpl.getFiles(properties, ftpServerIP,
					ftpServerPort, remoteImageResultsPath,
					localImageResultsPath, imageData);

			if (exitValue == 0) {
				if (fetcherHelper.resultsChecksumOK(imageData, localImageResultsDir)) {
					fetcherHelper.createTimeoutAndMaxTriesFiles(localImageResultsDir);
					
					if(uploadOutputFilesToSwift(imageData, localImageResultsDir)) {						
						break;
					} else {
						return;
					}
				} else {
					deleteResultsFromDisk(imageData, properties);
				}
			} else {
				rollBackFetch(imageData);
				deleteResultsFromDisk(imageData, properties);
				break;
			}
		}
		
		if (i >= MAX_FETCH_TRIES) {
			LOGGER.info("Max tries was reached. Marking " + imageData
					+ " as corrupted.");
			imageData.setState(ImageState.CORRUPTED);
			fetcherHelper.removeImageFromPendingMap(imageData,
					pendingImageFetchDB, pendingImageFetchMap);
			deleteResultsFromDisk(imageData, properties);
			// TODO: see if this have to be in try-catch
			imageStore.updateImage(imageData);
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		}
	}

	protected boolean uploadInputFilesToSwift(ImageData imageData, File localImageInputFilesDir) throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
				
		String pseudoFolder = getInputPseudoFolder(localImageInputFilesDir);
		
		String containerName = getContainerName();

		for (File actualFile : localImageInputFilesDir.listFiles()) {
			LOGGER.debug("Actual file " + actualFile.getName());
			int uploadFileTries;
			for (uploadFileTries = 0; uploadFileTries < MAX_SWIFT_UPLOAD_TRIES; uploadFileTries++) {
				try {
					LOGGER.debug("Trying to upload file "
							+ actualFile.getName() + " to " + pseudoFolder
							+ " in " + containerName);
					swiftAPIClient.uploadFile(containerName, actualFile,
							pseudoFolder);
					break;
				} catch (Exception e) {
					LOGGER.error("Error while uploading files to swift", e);
					continue;
				}
			}
			
			if(uploadFileTries >= MAX_SWIFT_UPLOAD_TRIES) {
				LOGGER.debug("Upload tries to swift for file " + actualFile
						+ " has passed max " + MAX_SWIFT_UPLOAD_TRIES);
				
				rollBackFetch(imageData);
				deleteResultsFromDisk(imageData, properties);
				return false;
			}
		}
		
		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}

	protected boolean uploadOutputFilesToSwift(ImageData imageData, File localImageOutputFilesDir) throws Exception {
		LOGGER.debug("maxSwiftUploadTries=" + MAX_SWIFT_UPLOAD_TRIES);
				
		String pseudoFolder = getOutputPseudoFolder(localImageOutputFilesDir);
		
		String containerName = getContainerName();

		for (File actualFile : localImageOutputFilesDir.listFiles()) {
			LOGGER.debug("Actual file " + actualFile.getName());
			int uploadFileTries;
			for (uploadFileTries = 0; uploadFileTries < MAX_SWIFT_UPLOAD_TRIES; uploadFileTries++) {
				try {
					LOGGER.debug("Trying to upload file "
							+ actualFile.getName() + " to " + pseudoFolder
							+ " in " + containerName);
					swiftAPIClient.uploadFile(containerName, actualFile,
							pseudoFolder);
					break;
				} catch (Exception e) {
					LOGGER.error("Error while uploading files to swift", e);
					continue;
				}
			}
			
			if(uploadFileTries >= MAX_SWIFT_UPLOAD_TRIES) {
				LOGGER.debug("Upload tries to swift for file " + actualFile
						+ " has passed max " + MAX_SWIFT_UPLOAD_TRIES);
						
				rollBackFetch(imageData);
				deleteResultsFromDisk(imageData, properties);
				return false;
			}
		}
		
		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}
	
	protected boolean deletePendingInputFilesFromSwift(ImageData imageData,
			Properties properties) throws Exception {
		LOGGER.debug("Deleting " + imageData + " input files from swift");
		String containerName = getContainerName();

		List<String> fileNames = swiftAPIClient
				.listFilesInContainer(containerName);

		for (String file : fileNames) {
			if (file.contains(imageData.getCollectionTierName())
					&& (file.contains(".TIF") || file.contains("MTL")
							|| file.contains(".tar.gz"))) {
				try {
					LOGGER.debug("Trying to delete file " + file + " from "
							+ containerName);
					String localImageInputsPath = properties
							.get("fetcher_volume_path")
							+ File.separator
							+ "images" + File.separator + imageData.getCollectionTierName();
					swiftAPIClient.deleteFile(containerName,
							getOutputPseudoFolder(new File(localImageInputsPath)),
							file);
				} catch (Exception e) {
					LOGGER.error("Error while deleting files from swift", e);
					return false;
				}
			}
		}

		return true;
	}
	
	protected boolean deleteResultFilesFromSwift(ImageData imageData, Properties properties) throws Exception {
		LOGGER.debug("Deleting " + imageData + " result files from swift");
		String containerName = getContainerName();
		
		List<String> fileNames = swiftAPIClient.listFilesInContainer(containerName);
        
        for(String file : fileNames) {
			if (file.contains(imageData.getCollectionTierName()) && !file.contains(".TIF")
					&& !file.contains("MTL") && !file.contains(".tar.gz")
					&& !file.contains("README")) {
				try {
					LOGGER.debug("Trying to delete file " + file + " from " + containerName);
					String localImageResultsPath = properties
							.get("fetcher_volume_path")
							+ File.separator
							+ "results" + File.separator + imageData.getCollectionTierName();
					swiftAPIClient.deleteFile(containerName, getOutputPseudoFolder(new File(localImageResultsPath)), file);
				} catch (Exception e) {
					LOGGER.error("Error while deleting files from swift", e);
					return false;
				}
			}
        }
		
		return true;
	}

	private String getContainerName() {
		return properties
				.getProperty(SebalPropertiesConstants.SWIFT_CONTAINER_NAME);
	}
	
	private String getInputPseudoFolder(File localImageInputsDir) {
		
		if(properties.getProperty(SebalPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX).endsWith(File.separator)) {			
			return properties
					.getProperty(SebalPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX)
					+ localImageInputsDir.getName() + File.separator;
		}
		
		return properties
				.getProperty(SebalPropertiesConstants.SWIFT_INPUT_PSEUDO_FOLDER_PREFIX) + 
				File.separator + localImageInputsDir.getName() + File.separator;
	}

	private String getOutputPseudoFolder(File localImageResultsDir) {

		if (properties.getProperty(SebalPropertiesConstants.SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX)
				.endsWith(File.separator)) {
			return properties
					.getProperty(SebalPropertiesConstants.SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX)
					+ localImageResultsDir.getName() + File.separator;
		}

		return properties.getProperty(SebalPropertiesConstants.SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX)
				+ File.separator
				+ localImageResultsDir.getName()
				+ File.separator;
	}
	
	protected String getFetcherVersion() {
		
		String sebalEngineDirPath = System.getProperty("user.dir");
		File sebalEngineDir = new File(sebalEngineDirPath);
		
		if (sebalEngineDir.exists() && sebalEngineDir.isDirectory()) {
			for (File file : sebalEngineDir.listFiles()) {
				if (file.getName().startsWith("sebal-engine.version.")) {
					String[] sebalEngineVersionFileSplit = file.getName()
							.split("\\.");
					return sebalEngineVersionFileSplit[2];
				}
			}
		}
		
		return "";
	}
}
