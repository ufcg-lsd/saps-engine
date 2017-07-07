package org.fogbowcloud.sebal.engine.sebal.fetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.fogbowcloud.sebal.engine.sebal.CheckSumMD5ForFile;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.mapdb.DB;

public class FetcherHelper {

	protected static final int NUMBER_OF_INPUT_FILES = 11;
	protected static final int NUMBER_OF_RESULT_FILES = 7;

	public static final Logger LOGGER = Logger.getLogger(FetcherHelper.class);
	
	protected void updatePendingMapAndDB(ImageData imageData,
			DB pendingImageFetchDB,
			ConcurrentMap<String, ImageData> pendingImageFetchMap) {

		LOGGER.debug("Adding image " + imageData + " to pending database");
		pendingImageFetchMap.put(imageData.getName(), imageData);
		pendingImageFetchDB.commit();
	}
	
	protected void removeImageFromPendingMap(ImageData imageData,
			DB pendingImageFetchDB,
			ConcurrentMap<String, ImageData> pendingImageFetchMap) {		
		LOGGER.info("Removing image " + imageData + " from pending map.");
		pendingImageFetchMap.remove(imageData.getName());
		pendingImageFetchDB.commit();
		
		if(pendingImageFetchMap.containsKey(imageData.getName())) {
			LOGGER.debug("There is still register for image " + imageData
					+ " into Map DB");
		}
	}

	protected String getStationId(ImageData imageData, Properties properties)
			throws IOException {
		String stationFilePath = properties.getProperty(SebalPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ "/results/" + imageData.getName() + "/" + imageData.getName()
				+ "_station.csv";
		File stationFile = new File(stationFilePath);

		if (stationFile.exists() && stationFile.isFile()) {
			BufferedReader reader = new BufferedReader(new FileReader(
					stationFile));
			String lineOne = reader.readLine();
			String[] stationAtt = lineOne.split(";");

			String stationId = stationAtt[0];
			reader.close();
			return stationId;
		} else {
			LOGGER.debug("Station file for image " + imageData.getName()
					+ " does not exist or is not a file!");
			return null;
		}
	}
	
	protected String getRemoteImageInputsPath(final ImageData imageData,
			Properties properties) {
		return properties.getProperty(SebalPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator + "images" + File.separator
				+ imageData.getName();
	}
	
	protected String getLocalImageInputsPath(ImageData imageData,
			Properties properties) {
		String localImageInputsPath = properties
				.getProperty(SebalPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator
				+ "images" + File.separator + imageData.getName();
		return localImageInputsPath;
	}

	protected String getRemoteImageResultsPath(final ImageData imageData,
			Properties properties) {
		return properties.getProperty(SebalPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH) + "/results/"
				+ imageData.getName();
	}

	protected String getLocalImageResultsPath(ImageData imageData,
			Properties properties) {
		String localImageResultsPath = properties
				.getProperty(SebalPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator
				+ "results" + File.separator + imageData.getName();
		return localImageResultsPath;
	}

	// TODO: see how to deal with this exception
	protected boolean isImageCorrupted(ImageData imageData,
			ConcurrentMap<String, ImageData> pendingImageFetchMap,
			ImageDataStore imageStore) throws SQLException {
		if (imageData.getState().equals(ImageState.CORRUPTED)) {
			pendingImageFetchMap.remove(imageData.getName());
			imageStore.updateImage(imageData);
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			return true;
		}
		return false;
	}
	
	protected boolean isImageRolledBack(ImageData imageData) {
		if(imageData.getState().equals(ImageState.FINISHED)){
			return true;
		}
		return false;
	}
	
	protected boolean isThereNonFetchedInputFiles(String localImageInputsPath) {
		File localImageInputsDir = new File(localImageInputsPath);

		if (localImageInputsDir.exists() && localImageInputsDir.isDirectory()) {
			if (localImageInputsDir.list().length >= NUMBER_OF_INPUT_FILES) {
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	protected boolean isThereNonFetchedResultFiles(String localImageResultsPath) {
		File localImageResultsDir = new File(localImageResultsPath);

		if (localImageResultsDir.exists() && localImageResultsDir.isDirectory()) {
			if (localImageResultsDir.list().length >= NUMBER_OF_RESULT_FILES) {
				return true;
			} else {
				return false;
			}
		}

		return false;
	}
	
	protected boolean resultsChecksumOK(ImageData imageData,
			File localImageResultsDir) throws Exception {
		LOGGER.info("Checksum of " + imageData + " result files");
		if(isThereNonFetchedResultFiles(localImageResultsDir.getAbsolutePath())) {
			if (CheckSumMD5ForFile.isFileCorrupted(localImageResultsDir)) {
				return false;
			}
		} else {
			return false;
		}
		
		return true;
	}
	
	protected void createTimeoutAndMaxTriesFiles(File localImageResultsDir) {
		LOGGER.debug("Generating timeout and max tries files");
		ProcessBuilder builder;
		Process p;
		
		for(File file : localImageResultsDir.listFiles()) {
			if(file.getName().startsWith("temp-worker-run-") && file.getName().endsWith(".out")) {
				
				try {
					builder = new ProcessBuilder("/bin/bash", "scripts/create_timeout_file.sh", file.getAbsolutePath());
					p = builder.start();
					p.waitFor();
					
					builder = new ProcessBuilder("/bin/bash", "scripts/create_max_tries_file.sh", file.getAbsolutePath());
					p = builder.start();
					p.waitFor();
				} catch (Exception e) {
					LOGGER.debug("Error while generating timeout and max tries files", e);
				}				
			}
		}	
	}
}
