package org.fogbowcloud.sebal.fetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.sebal.CheckSumMD5ForFile;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.swift.SwiftClient;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Fetcher {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final SwiftClient swiftClient;
	private File pendingImageFetchFile;
	private DB pendingImageFetchDB;
	private ConcurrentMap<String, ImageData> pendingImageFetchMap;

	private String ftpServerIP;
	private String ftpServerPort;

	private static int MAX_FETCH_TRIES = 2;
	private static final long DEFAULT_SCHEDULER_PERIOD = 60000; // 5 minutes

	public static final Logger LOGGER = Logger.getLogger(Fetcher.class);

	public Fetcher(Properties properties, String imageStoreIP,
			String imageStorePort, String ftpServerIP, String ftpServerPort) {
		
		this(properties, new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort), ftpServerIP, ftpServerPort);
		
		LOGGER.debug("Creating fetcher"); 
		LOGGER.debug("Imagestore " + imageStoreIP + ":" + imageStorePort
				+ " FTPServer " + ftpServerIP + ":" + ftpServerPort);
	}
	
	public Fetcher(Properties properties, ImageDataStore imageStore, String ftpServerIP, String ftpServerPort) {
		
		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}
		
		if(imageStore == null) {
			throw new IllegalArgumentException(
					"Imagestore arg must not be null.");
		}

		this.properties = properties;
		this.imageStore = imageStore;
		this.ftpServerIP = ftpServerIP;
		this.ftpServerPort = ftpServerPort;
		this.swiftClient = new SwiftClient(properties);
				
		this.pendingImageFetchFile = new File("pending-image-fetch.db");
		this.pendingImageFetchDB = DBMaker.newFileDB(pendingImageFetchFile).make();
		
		if(!pendingImageFetchFile.exists() || !pendingImageFetchFile.isFile()) {
			LOGGER.info("Creating map of pending images to fetch");
			this.pendingImageFetchMap = pendingImageFetchDB.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending images to fetch");
			this.pendingImageFetchMap = pendingImageFetchDB.getHashMap("map");
		}
	}

	public void exec() {

		try {
			while (true) {
				cleanUnfinishedFetchedData(properties);
				List<ImageData> imagesToFetch = imagesToFetch();
				for (ImageData imageData : imagesToFetch) {
					if (!imageData.getImageStatus().equals(ImageData.PURGED)) {
						fetchAndUpdateImage(imageData);
					}
				}
				Thread.sleep(DEFAULT_SCHEDULER_PERIOD);
			}
		} catch (InterruptedException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		}
		
		pendingImageFetchDB.close();
	}

	private void cleanUnfinishedFetchedData(Properties properties) throws InterruptedException, IOException {
		LOGGER.info("Starting garbage collector");
		Collection<ImageData> data = pendingImageFetchMap.values();
		for (ImageData imageData : data) {
			removeFromPendingAndUpdateState(imageData, properties);
		}
		LOGGER.info("Garbage collect finished");
	}

	private void removeFromPendingAndUpdateState(final ImageData imageData, Properties properties) throws IOException {		
		try {
			LOGGER.debug("Bringing " + imageData + " back to "
					+ ImageState.FINISHED);
			imageStore.removeStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
			imageData.setState(ImageState.FINISHED);
			imageData.setUpdateTime(new Date(Calendar.getInstance().getTimeInMillis()));
			imageStore.updateImage(imageData);
			
			LOGGER.info("Deleting image " + imageData);
			
			try {
				delelteResultsFromDisk(imageData, properties);
			} catch (IOException e) {
				LOGGER.error("Error while deleting " + imageData, e);
			}
			
			LOGGER.debug("Removing image " + imageData + " from pending image map");
			pendingImageFetchMap.remove(imageData.getName());
			
			LOGGER.info("Image " + imageData.getName() + " deleted");
		} catch (SQLException e1) {
			Fetcher.LOGGER.error("Error while updating image data "
					+ imageData.getName(), e1);
		}		
	}

	private void delelteResultsFromDisk(final ImageData imageData,
			Properties properties) throws IOException {
		String exportPath = properties.getProperty("sebal_export_path");
		String resultsDirPath = exportPath + "/results/"
				+ imageData.getName();
		File resultsDir = new File(resultsDirPath);

		if (!resultsDir.exists() || !resultsDir.isDirectory()) {
			LOGGER.debug("This file does not exist!");
			return;
		}
		
		FileUtils.deleteDirectory(resultsDir);
	}

	private List<ImageData> imagesToFetch() {
		try {
			return imageStore.getIn(ImageState.FINISHED);
		} catch (SQLException e) {
			LOGGER.error("Error getting finished images.", e);
		}
		return Collections.EMPTY_LIST;
	}

	private void fetchAndUpdateImage(ImageData imageData) throws IOException, InterruptedException {
		try {
			prepareFetch(imageData);
			fetch(imageData, 0);
			
			if(!isFileCorrupted(imageData)) {
				finishFetch(imageData);			
			}
		} catch (Exception e) {
			LOGGER.error("Couldn't fetch image " + imageData.getName() + ".", e);
			rollBackFetch(imageData);
		}
	}
	
	private boolean isFileCorrupted(ImageData imageData) throws SQLException {
		if(imageData.getState().equals(ImageState.CORRUPTED)) {
			imageData.setUpdateTime(new Date(Calendar.getInstance().getTimeInMillis()));
			pendingImageFetchMap.remove(imageData.getName());
			imageStore.updateImage(imageData);
			return true;
		}
		return false;
	}

	private void prepareFetch(ImageData imageData) throws SQLException {
		LOGGER.debug("Preparing image " + imageData.getName() + " to fetch");
		if (imageStore.lockImage(imageData.getName())) {
			imageData.setState(ImageState.FETCHING);
			
			LOGGER.debug("Adding image" + imageData + " to pending database");
			pendingImageFetchMap.put(imageData.getName(), imageData);
			pendingImageFetchDB.commit();

			Date lastUpdateTime = new Date(Calendar.getInstance().getTimeInMillis());
			imageData.setUpdateTime(lastUpdateTime);
			
			LOGGER.debug("Updating image data in DB");
			imageStore.updateImage(imageData);
			try {
				imageStore.addStateStamp(imageData.getName(),
						imageData.getState(), imageData.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while adding state "
						+ imageData.getState() + " timestamp "
						+ imageData.getUpdateTime() + " in DB");
			}
			imageStore.unlockImage(imageData.getName());
			
			LOGGER.debug("Image " + imageData.getName() + " ready to fetch");
		}
	}

	private void finishFetch(ImageData imageData) throws SQLException, IOException {
		LOGGER.debug("Finishing fetch for image " + imageData.getName());
		imageData.setState(ImageState.FETCHED);
		
		String stationId = getStationId(imageData);
		imageData.setStationId(stationId);
		imageData.setSebalVersion(properties.getProperty("sebal_version"));
		
		Date lastUpdateTime = new Date(Calendar.getInstance().getTimeInMillis());
		imageData.setUpdateTime(lastUpdateTime);
		
		LOGGER.info("IMAGE = " + imageData.getName());
		LOGGER.info("STATION ID = " +  stationId);
		LOGGER.info("UPDATE TIME = " + lastUpdateTime);
		LOGGER.info("SEBAL VERSION = " + properties.getProperty("sebal_version"));
		
		LOGGER.info("Updating image data in DB");
		imageStore.updateImage(imageData);
		try {
			imageStore.addStateStamp(imageData.getName(), imageData.getState(),
					imageData.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state " + imageData.getState()
					+ " timestamp " + imageData.getUpdateTime() + " in DB");
		}
		
		LOGGER.info("Removing image from pending map.");
		pendingImageFetchMap.remove(imageData.getName());
		
		LOGGER.debug("Image " + imageData.getName() + " fetched!");
	}
	
	private String getStationId(ImageData imageData) throws IOException {
		String stationFilePath = properties.getProperty("fetcher_volume_path")
				+ "/results/" + imageData.getName() + "/" + imageData.getName()
				+ "_station.csv";
		File stationFile = new File(stationFilePath);
		
		if(stationFile.exists() && stationFile.isFile()) {
			BufferedReader reader = new BufferedReader(new FileReader(stationFile));
			String lineOne = reader.readLine();
			String[] stationAtt = lineOne.split(";");
			
			String stationId = stationAtt[0];
			reader.close();
			return stationId;
		} else {
			LOGGER.error("Station file for image " + imageData.getName()
					+ " does not exist or is not a file!");
			return null;
		}
	}

	private void rollBackFetch(ImageData imageData) {
		pendingImageFetchMap.remove(imageData.getName());
		try {
			imageStore.removeStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
			imageData.setState(ImageState.FINISHED);
			imageData.setUpdateTime(new Date(Calendar.getInstance().getTimeInMillis()));
			imageStore.updateImage(imageData);
		} catch (SQLException e1) {
			Fetcher.LOGGER.error("Error while updating image data.", e1);
		}
	}

	// FIXME: reduce code
	public void fetch(final ImageData imageData, int tries) throws Exception {
		
		LOGGER.debug("MAX_FETCH_TRIES " + MAX_FETCH_TRIES);
		if (tries > MAX_FETCH_TRIES) {
			LOGGER.info("Max tries reached");
			LOGGER.info("File is corrupted");
			imageData.setState(ImageState.CORRUPTED);
			return;
		}
		
		String remoteImageResultsPath = properties
				.getProperty("sebal_export_path")
				+ "/results/"
				+ imageData.getName();

		String localImageResultsPath = properties
				.getProperty("fetcher_volume_path") + "/results/" + imageData.getName();
		
		File localImageResultsDir = new File(localImageResultsPath);

		if (!localImageResultsDir.exists()
				&& !localImageResultsDir.isDirectory()) {
			LOGGER.debug("Path " + localImageResultsPath + " not valid or nonexistent");
			LOGGER.info("Creating " + localImageResultsPath);
			localImageResultsDir.mkdirs();
		}

		LOGGER.debug("Getting " + remoteImageResultsPath + " in FTPserver"
				+ ftpServerIP + ":" + ftpServerPort + " to "
				+ localImageResultsPath + " in localhost");
		ProcessBuilder builder = new ProcessBuilder("/bin/bash", "scripts/sftp-access.sh", properties.getProperty("ftp_server_user"), ftpServerIP, ftpServerPort, 
				remoteImageResultsPath, localImageResultsPath, imageData.getName());

		Process p = builder.start();
		p.waitFor();

		LOGGER.debug("Checksum of " + imageData + " result files");
		if (CheckSumMD5ForFile.isFileCorrupted(imageData, localImageResultsDir)) {
			fetch(imageData, tries++);
		}else{
			
			String pseudFolder = properties.getProperty(AppPropertiesConstants.SWIFT_PSEUD_FOLDER_PREFIX) 
					+ localImageResultsDir.getName() + "/";
			String containerName = properties.getProperty(AppPropertiesConstants.SWIFT_CONTAINER_NAME);
			
			for(File actualFile : localImageResultsDir.listFiles()){
				swiftClient.uploadFile(containerName, actualFile, pseudFolder);
				actualFile.delete();
			}
		}
	}
}
