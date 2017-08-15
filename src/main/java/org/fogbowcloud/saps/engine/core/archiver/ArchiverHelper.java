package org.fogbowcloud.saps.engine.core.archiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageData;
import org.fogbowcloud.saps.engine.core.model.ImageState;
import org.fogbowcloud.saps.engine.core.util.CheckSumMD5ForFile;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.mapdb.DB;

public class ArchiverHelper {

	protected static final int NUMBER_OF_INPUT_FILES = 11;
	protected static final int NUMBER_OF_RESULT_FILES = 7;

	public static final Logger LOGGER = Logger.getLogger(ArchiverHelper.class);
	
	protected void updatePendingMapAndDB(ImageData imageData, DB pendingImageFetchDB,
			ConcurrentMap<String, ImageData> pendingImageFetchMap) {
		LOGGER.debug("Adding image " + imageData + " to pending database");
		pendingImageFetchMap.put(imageData.getName(), imageData);
		pendingImageFetchDB.commit();
	}
	
	protected void removeImageFromPendingMap(ImageData imageData, DB pendingImageFetchDB,
			ConcurrentMap<String, ImageData> pendingImageFetchMap) {		
		LOGGER.info("Removing image " + imageData + " from pending map.");
		pendingImageFetchMap.remove(imageData.getName());
		pendingImageFetchDB.commit();
		
		if(pendingImageFetchMap.containsKey(imageData.getName())) {
			LOGGER.debug("There is still register for image " + imageData + " into Map DB");
		}
	}

	protected String getStationId(ImageData imageData, Properties properties) throws IOException {
		String stationFilePath = properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ "/results/" + imageData.getCollectionTierName() + "/" + imageData.getCollectionTierName() 
				+ "_station.csv";
		File stationFile = new File(stationFilePath);

		if (stationFile.exists() && stationFile.isFile()) {
			BufferedReader reader = new BufferedReader(new FileReader(stationFile));
			String lineOne = reader.readLine();
			String[] stationAtt = lineOne.split(";");

			String stationId = stationAtt[0];
			reader.close();
			return stationId;
		} else {
			LOGGER.debug("Station file for image " + imageData.getCollectionTierName()
					+ " does not exist or is not a file!");
			return null;
		}
	}
	
	protected String getRemoteImageInputsPath(final ImageData imageData, Properties properties) {
		return properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator + "images" + File.separator + imageData.getCollectionTierName();
	}
	
	protected String getLocalImageInputsPath(ImageData imageData, Properties properties) {
		String localImageInputsPath = properties
				.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator
				+ "images" + File.separator + imageData.getCollectionTierName();
		return localImageInputsPath;
	}

	protected String getRemoteImageResultsPath(final ImageData imageData, Properties properties) {
		return properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ "/results/" + imageData.getCollectionTierName();
	}

	protected String getLocalImageResultsPath(ImageData imageData, Properties properties) {
		String localImageResultsPath = properties
				.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator
				+ "results" + File.separator + imageData.getCollectionTierName();
		return localImageResultsPath;
	}

	// TODO: see how to deal with this exception
	protected boolean isImageCorrupted(ImageData imageData,
			ConcurrentMap<String, ImageData> pendingImageFetchMap, ImageDataStore imageStore)
			throws SQLException {
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
	
	protected boolean resultsChecksumOK(ImageData imageData, File localImageResultsDir)
			throws Exception {
		LOGGER.info("Checksum of " + imageData + " result files");
		if (CheckSumMD5ForFile.isFileCorrupted(localImageResultsDir)) {
			return false;
		}
		
		return true;
	}
	
	protected void createTimeoutAndMaxTriesFiles(File localImageResultsDir) {
		LOGGER.debug("Generating timeout and max tries files");
		ProcessBuilder builder;
		Process p;
		
		for(File file : localImageResultsDir.listFiles()) {
			if (file.getName().startsWith("temp-worker-run-") && file.getName().endsWith(".out")) {				
				try {
					builder = new ProcessBuilder("/bin/bash", "scripts/create_timeout_file.sh",
							file.getAbsolutePath());
					p = builder.start();
					p.waitFor();
					
					builder = new ProcessBuilder("/bin/bash", "scripts/create_max_tries_file.sh",
							file.getAbsolutePath());
					p = builder.start();
					p.waitFor();
				} catch (Exception e) {
					LOGGER.debug("Error while generating timeout and max tries files", e);
				}				
			}
		}	
	}
}
