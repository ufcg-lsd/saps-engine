package org.fogbowcloud.sebal.engine.sebal.fetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.CheckSumMD5ForFile;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.mapdb.DB;

public class FetcherHelper {

	protected static final int NUMBER_OF_RESULT_FILES = 8;

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
	}

	protected String getStationId(ImageData imageData, Properties properties)
			throws IOException {
		String stationFilePath = properties.getProperty(Fetcher.FETCHER_RESULTS_PATH)
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

	protected String getRemoteImageResultsPath(final ImageData imageData,
			Properties properties) {
		return properties.getProperty(Fetcher.SEBAL_EXPORT_PATH) + "/results/"
				+ imageData.getName();
	}

	protected String getLocalImageResultsPath(ImageData imageData,
			Properties properties) {
		String localImageResultsPath = properties
				.getProperty(Fetcher.FETCHER_RESULTS_PATH)
				+ "/results/"
				+ imageData.getName();
		return localImageResultsPath;
	}

	// TODO: see how to deal with this exception
	protected boolean isFileCorrupted(ImageData imageData,
			ConcurrentMap<String, ImageData> pendingImageFetchMap,
			ImageDataStore imageStore) throws SQLException {
		if (imageData.getState().equals(ImageState.CORRUPTED)) {
			imageData.setUpdateTime(new Date(Calendar.getInstance()
					.getTimeInMillis()));
			pendingImageFetchMap.remove(imageData.getName());
			imageStore.updateImage(imageData);
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

	protected boolean isThereFetchedFiles(String localImageResultsPath) {
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
		if(isThereFetchedFiles(localImageResultsDir.getAbsolutePath())) {
			if (CheckSumMD5ForFile.isFileCorrupted(imageData,
					localImageResultsDir)) {
				return false;
			}
		} else {
			return false;
		}
		
		return true;
	}
}
