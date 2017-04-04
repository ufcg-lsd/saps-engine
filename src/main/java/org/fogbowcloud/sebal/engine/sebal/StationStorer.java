package org.fogbowcloud.sebal.engine.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.fogbowcloud.sebal.engine.swift.SwiftAPIClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
		this.noaaFTPUrl = properties.getProperty(SebalPropertiesConstants.NOAA_FTP_URL);
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
		JSONParser jsonParser = new JSONParser();
		
		try {
			getStations(stations, jsonParser);
		} catch(Exception e) {
			LOGGER.error("Error while getting stations from JSON file", e);
		}
		
		return stations;
	}

	private void getStations(List<String> stations, JSONParser jsonParser)
			throws IOException, ParseException, FileNotFoundException,
			JSONException {
		
		String stationsFilePath = properties.getProperty(SebalPropertiesConstants.STATIONS_FILE_PATH);
		
		Object object = jsonParser.parse(new FileReader(stationsFilePath));			
		JSONObject jsonObject = (JSONObject) object;
		
		JSONArray jsonArray = (JSONArray) jsonObject.get("stations");
		
		//Reading the String
		Iterator<?> iterator = jsonArray.iterator();
		while (iterator.hasNext()) {		        
			String stationId = (String) jsonObject.get("id");
			stations.add(stationId);
		}
	}

	private void downloadUploadStationFiles(List<String> stations) throws Exception {
		for(int year = FIRST_RECORD_YEAR; year <= LAST_RECORD_YEAR; year++) {
			
	        String yearDirPath = getYearDirPath(properties.getProperty(SebalPropertiesConstants.BASE_YEAR_DIR_PATH), year);
	        boolean wasCreated = createDirectoryForYear(yearDirPath);
	        
	        if (wasCreated) {
				for (int j = 0; j <= stations.size(); j++) {
					downloadStationFile(stations, year, yearDirPath, j);
					uploadStationFile(stations, year, yearDirPath, j);
				}
	        }
	        
	        deleteDirectoryForYear(year, yearDirPath);
		}
	}

	private boolean createDirectoryForYear(String yearDirPath) {
		File yearDir = new File(yearDirPath);
        return yearDir.mkdirs();
	}
	
	private void downloadStationFile(List<String> stations, int year,
			String yearDirPath, int j) throws IOException {
		String localStationFilePath = getStationCompressedFilePath(stations.get(j),
				year, yearDirPath);

		// clean if already exists (garbage collection)
		File localStationFile = new File(localStationFilePath);
		if (localStationFile.exists()) {
			LOGGER.info("File "
					+ localStationFilePath
					+ " already exists. Will be removed before repeating download");
			localStationFile.delete();
		}
		
		downloadInto(year, stations.get(j), localStationFile);
	}
	
	private String getStationCompressedFilePath(String stationId, int year, String yearDirPath) {
		return yearDirPath + File.separator + stationId + "0-99999-" + year + ".gz";
	}

	private String getYearDirPath(String baseYearDirPath, int year) {
		return baseYearDirPath + File.separator + year;
	}

	private void downloadInto(int recordYear, String stationId, File stationFile) throws IOException {

		LOGGER.info("Downloading station file " + stationId + " into file "
				+ stationFile.getAbsolutePath());
		
    	BasicCookieStore cookieStore = new BasicCookieStore();
        HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();    

		HttpGet homeGet = new HttpGet(this.noaaFTPUrl + File.separator
				+ recordYear + File.separator + stationId + "0-99999-"
				+ recordYear + ".gz");
        HttpResponse response = httpClient.execute(homeGet);

        OutputStream outStream = new FileOutputStream(stationFile);
		IOUtils.copy(response.getEntity().getContent(), outStream);
		outStream.close();    
    }
	
	private void uploadStationFile(List<String> stations, int year,
			String yearDirPath, int j) throws Exception {
		
		// Getting compressed station file
		String localCompressedStationFilePath = getStationCompressedFilePath(stations.get(j), year,
				yearDirPath);		
		File localCompressedStationFile = new File(localCompressedStationFilePath);
		
		// Uncompressing station file
		File localUncompressedStationFile = unGzip(localCompressedStationFile, true);

		// Uploading uncompressed station file
		if (localUncompressedStationFile.exists()) {
			uploadStationFileToSwift(localUncompressedStationFile, year);
		}
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
				.getProperty(SebalPropertiesConstants.SWIFT_CONTAINER_NAME);
		String pseudoFolder = properties
				.getProperty(SebalPropertiesConstants.SWIFT_STATIONS_PSEUDO_FOLDER_PREFIX)
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
}