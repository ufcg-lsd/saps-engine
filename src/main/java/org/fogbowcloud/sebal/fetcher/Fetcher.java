package org.fogbowcloud.sebal.fetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
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

	// FIXME: test - 1
	// FIXME: FTP - 0

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final SwiftClient swiftClient;
	private File pendingImageFetchFile;
	private DB pendingImageFetchDB;
	private ConcurrentMap<String, ImageData> pendingImageFetchMap;

	private String ftpServerIP;
	private String ftpServerPort;

	private static int MAX_FETCH_TRIES = 2;
	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes

	public static final Logger LOGGER = Logger.getLogger(Fetcher.class);

	public Fetcher(Properties properties, String imageStoreIP,
			String imageStorePort, String ftpServerIP, String ftpServerPort) {

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}

		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort);
		this.ftpServerIP = ftpServerIP;
		this.ftpServerPort = ftpServerPort;
		this.swiftClient = new SwiftClient(properties);
		
		this.pendingImageFetchFile = new File("pending-image-fetch.db");
		this.pendingImageFetchDB = DBMaker.newFileDB(pendingImageFetchFile).make();
		
		if(!pendingImageFetchFile.exists() || !pendingImageFetchFile.isFile()) {			
			this.pendingImageFetchMap = pendingImageFetchDB.createHashMap("map").make();
		} else {
			this.pendingImageFetchMap = pendingImageFetchDB.getHashMap("map");
		}
	}

	public void exec() {

		LOGGER.info("Initializing fetcher... ");

		try {
			cleanUnfinishedFetchedData(properties);

			while (true) {
				List<ImageData> imagesToFetch = imagesToFetch();
				for (ImageData imageData : imagesToFetch) {
					if (!imageData.getImageStatus().equals(ImageData.PURGED)) {
						fetchAndUpdateImage(imageData);
					}
				}
				Thread.sleep(DEFAULT_SCHEDULER_PERIOD);
			}
		} catch (InterruptedException e) {
			LOGGER.error("Execution interrupted!\n" + e);
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.error("Failed I/O operation!\n" + e);
			e.printStackTrace();
		}
		
		pendingImageFetchDB.close();
	}

	private void cleanUnfinishedFetchedData(Properties properties) throws InterruptedException, IOException {
		Collection<ImageData> data = pendingImageFetchMap.values();
		for (ImageData imageData : data) {
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	private void removeFromPendingAndUpdateState(final ImageData imageData, Properties properties) throws IOException {
		// FIXME: add log
		try {
			imageData.setState(ImageState.FINISHED);
			imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
			imageStore.updateImage(imageData);
			
			String exportPath = properties.getProperty("sebal_export_path");
			String resultsDirPath = exportPath + "/results/"
					+ imageData.getName();
			File resultsDir = new File(resultsDirPath);

			if (!resultsDir.exists() || !resultsDir.isDirectory()) {
				LOGGER.debug("This file does not exist!");
				return;
			}
			
			FileUtils.deleteDirectory(resultsDir);			
			pendingImageFetchMap.remove(imageData.getName());
		} catch (SQLException e1) {
			Fetcher.LOGGER.error("Error while updating image data: "
					+ imageData.getName(), e1);
		}
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
			imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
			pendingImageFetchMap.remove(imageData.getName());
			imageStore.updateImage(imageData);
			return true;
		}
		return false;
	}

	private void prepareFetch(ImageData imageData) throws SQLException {

		if (imageStore.lockImage(imageData.getName())) {
			imageData.setState(ImageState.FETCHING);
			pendingImageFetchMap.put(imageData.getName(), imageData);
			pendingImageFetchDB.commit();

			imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
			imageStore.updateImage(imageData);
			imageStore.unlockImage(imageData.getName());
		}
	}

	private void finishFetch(ImageData imageData) throws SQLException, IOException {		
		imageData.setState(ImageState.FETCHED);
		imageData.setStationId(getStationId(imageData));
		imageData.setSebalVersion(properties.getProperty("sebal_version"));
		imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
		imageStore.updateImage(imageData);
		pendingImageFetchMap.remove(imageData.getName());
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
			imageData.setState(ImageState.FINISHED);
			imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
			imageStore.updateImage(imageData);
		} catch (SQLException e1) {
			Fetcher.LOGGER.error("Error while updating image data.", e1);
		}
	}

	// TODO: See if this is correct
	// FIXME: reduce code
	public void fetch(final ImageData imageData, int tries) throws Exception {

		if (tries > MAX_FETCH_TRIES) {
			LOGGER.debug("Max tries reached!\nFile is corrupted.");
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
			LOGGER.debug("This folder doesn't exist or is not a directory...Creating a new one.");
			localImageResultsDir.mkdirs();
		}

		ProcessBuilder builder = new ProcessBuilder("bash", "scripts/sftp-access.sh", properties.getProperty("ftp_server_user"), ftpServerIP, ftpServerPort, 
				remoteImageResultsPath, localImageResultsPath, imageData.getName());

		Process p = builder.start();
		p.waitFor();

		if (CheckSumMD5ForFile.isFileCorrupted(imageData, localImageResultsDir)) {
			fetch(imageData, tries++);
		}
		
		//TODO
		/*
		 * Get each image on localVolumeResultsPath+imageData.getName() directory and upload this image to Swift on:
		 * 
		 * {container/results/imageData.getName()/acutalFileName}
		 */
		File imageDirectory =  new File(localImageResultsPath+imageData.getName());
		String pseudFolder = "/results/"+imageDirectory.getName()+"/";
		String containerName = properties.getProperty(AppPropertiesConstants.SWIFT_CONTAINER_NAME);
		
		for(File actualFile : imageDirectory.listFiles()){
			swiftClient.uploadFile(containerName, actualFile, pseudFolder);
		}
	}
}
