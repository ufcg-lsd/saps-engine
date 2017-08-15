package org.fogbowcloud.saps.engine.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;

public class StationStorer {
		
	private Properties properties;
	private String noaaFTPUrl;
	private SwiftAPIClient swiftAPIClient;
	
	// Constants
	private static final int FIRST_RECORD_YEAR = 1984;
	private static final int LAST_RECORD_YEAR = 2017;
	private static final Logger LOGGER = Logger.getLogger(StationStorer.class);
	
	public StationStorer(Properties properties) {
		Validate.notNull(properties);
		
		this.properties = properties;
		this.noaaFTPUrl = properties.getProperty(SapsPropertiesConstants.NOAA_FTP_URL);
		this.swiftAPIClient = new SwiftAPIClient(properties);
	}
	
	protected void init() {
		List<String> stations = getStationsFromJSON();
		
		if(stations != null && !stations.isEmpty()) {
			try {
				downloadUploadStationFiles(stations);
			} catch (Exception e) {
				LOGGER.error("Error while downloading/uploading station files", e);
			}
		}
	}

	private List<String> getStationsFromJSON() {
		List<String> stations = new ArrayList<String>();						
		return getStations(stations);
	}

	private List<String> getStations(List<String> stations) {
		try {
			String stationsFilePath = properties
					.getProperty(SapsPropertiesConstants.STATIONS_FILE_PATH);

			File stationsFile = new File(stationsFilePath);

			BufferedReader br = new BufferedReader(new FileReader(stationsFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				stations.add(line);
			}

			br.close();
		} catch (IOException e) {
			LOGGER.error("Error while reading stations file", e);
		}

		return stations;
	}

	private void downloadUploadStationFiles(List<String> stations) throws Exception {
		for(int year = FIRST_RECORD_YEAR; year <= LAST_RECORD_YEAR; year++) {
			
	        String yearDirPath = getYearDirPath(properties.getProperty(SapsPropertiesConstants.BASE_YEAR_DIR_PATH), year);
			File yearDir = new File(yearDirPath);
	        yearDir.mkdirs();
	        
	        if(yearDir.exists()) {
	        	for (int j = 0; j < stations.size(); j++) {
					if (!isStationFileAlreadyStored(stations.get(j), year)) {
						if (downloadStationFile(stations, year, yearDirPath, j)) {
							uploadStationFile(stations, year, yearDirPath, j);
						}
					}
	        	}
	        	
	        	deleteDirectoryForYear(year, yearDirPath);
	        }
		}
	}
	
	private boolean downloadStationFileToPublicHtml(List<String> stations, int year,
			String yearDirPath, int j) {
		LOGGER.info("Downloading station file " + stations.get(j) + " into " + yearDirPath);
		
		String containerName = properties
				.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);
		String pseudoFolder = properties
				.getProperty(SapsPropertiesConstants.SWIFT_STATIONS_PSEUDO_FOLDER_PREFIX)
				+ File.separator + year;
		
			swiftAPIClient.downloadFile(containerName, stations.get(j) + "0-99999-" + year, pseudoFolder, yearDirPath);
		
		File stationFile = new File(yearDirPath + File.separator + stations.get(j) + "0-99999-" + year);
		
		if(stationFile.exists()) {
			return true;
		}
		
		return false;
	}
	
	private boolean downloadStationFile(List<String> stations, int year,
			String yearDirPath, int j) throws IOException {
		LOGGER.info("Downloading station file " + stations.get(j) + " into " + yearDirPath);
		
		ProcessBuilder builder = new ProcessBuilder("wget", "-P", yearDirPath,
				this.noaaFTPUrl + File.separator + year + File.separator
						+ stations.get(j) + "0-99999-" + year + ".gz");
		
		try {
			Process p = builder.start();
			p.waitFor();					
		} catch (IOException e) {
			LOGGER.error("Error while writing file for station", e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while downloading file for station", e);
		}
		
		return downloadStationSucceeded(getStationCompressedFilePath(stations.get(j), year, yearDirPath));
	}

	private String getStationCompressedFilePath(String stationId, int year, String yearDirPath) {
		return yearDirPath + File.separator + stationId + "0-99999-" + year + ".gz";
	}

	private String getYearDirPath(String baseYearDirPath, int year) {
		return baseYearDirPath + File.separator + year;
	}
	
	private boolean downloadStationSucceeded(String stationCompressedFilePath) {
		File stationCompressedFile = new File(stationCompressedFilePath);		
		if(stationCompressedFile.exists()) {
			LOGGER.debug("File " + stationCompressedFilePath + " downloaded successfully");
			return true;
		}
		
		LOGGER.debug("File " + stationCompressedFilePath + " was not downloaded");
		return false;
	}
	
	private void uploadStationFile(List<String> stations, int year,
			String yearDirPath, int j) throws Exception {		
			// Getting compressed station file
			String localCompressedStationFilePath = getStationCompressedFilePath(
					stations.get(j), year, yearDirPath);
			File localCompressedStationFile = new File(
					localCompressedStationFilePath);

			// Uncompressing station file
			File localUncompressedStationFile = unGzip(
					localCompressedStationFile, true);

			// Uploading uncompressed station file
			if (localUncompressedStationFile.exists()) {
				uploadStationFileToSwift(localUncompressedStationFile, year);
			}
	}
	
	private boolean isStationFileAlreadyStored(String stationId, int year) {
		String containerName = properties
				.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);
		String pseudoFolder = properties
				.getProperty(SapsPropertiesConstants.SWIFT_STATIONS_PSEUDO_FOLDER_PREFIX)
				+ File.separator + year;
		
		List<String> filesWithPrefix = swiftAPIClient.listFilesWithPrefix(
				containerName, pseudoFolder + File.separator + stationId);
		
		for(String storedFile : filesWithPrefix) {
			if(storedFile.contains(stationId)) {
				return true;
			}
		}
		
		return false;
	}

	public static File unGzip(File file, boolean deleteGzipfileOnSuccess) throws IOException {
	    GZIPInputStream gin = new GZIPInputStream(new FileInputStream(file));
	    FileOutputStream fos = null;
	    try {
	        File outFile = new File(file.getParent(), file.getName().replaceAll("\\.gz$", ""));
	        fos = new FileOutputStream(outFile);
	        byte[] buf = new byte[100000];
	        int len;
	        while ((len = gin.read(buf)) > 0) {
	            fos.write(buf, 0, len);
	        }

	        fos.close();
	        if (deleteGzipfileOnSuccess) {
	            file.delete();
	        }
	        return outFile; 
	    } finally {
	        if (gin != null) {
	            gin.close();    
	        }
	        if (fos != null) {
	            fos.close();    
	        }
	    }       
	}

	private void uploadStationFileToSwift(File localStationFile, int year)
			throws Exception {
		String containerName = properties
				.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);
		String pseudoFolder = properties
				.getProperty(SapsPropertiesConstants.SWIFT_STATIONS_PSEUDO_FOLDER_PREFIX)
				+ File.separator + year;

		swiftAPIClient.uploadFile(containerName, localStationFile, pseudoFolder);
	}

	private void deleteDirectoryForYear(int year, String yearDirPath) throws IOException {	
		File yearDir = new File(yearDirPath);		
		LOGGER.info("Removing year " + year + " data under path " + yearDirPath);

		if (yearDirectoryExists(yearDirPath, yearDir)) {
			FileUtils.deleteDirectory(yearDir);
		}
	}

	private boolean yearDirectoryExists(String yearDirPath, File yearDir) {
		if (!yearDir.exists() || !yearDir.isDirectory()) {
			LOGGER.info("Path " + yearDirPath + " does not exist");
			return false;
		}
		return true;
	}
	
	public static void main(String[] args) {
		Properties properties = new Properties();
		FileInputStream input;
		try {
			input = new FileInputStream("config/sebal.conf");
			properties.load(input);
		} catch (FileNotFoundException e) {
			LOGGER.error("Error while reading conf file", e);
		} catch (IOException e) {
			LOGGER.error("Error while loading properties", e);
		}
		
		StationStorer stationStorer = new StationStorer(properties);
		stationStorer.init();
	}
}