package org.fogbowcloud.saps.engine.swift;

import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class LogStorer {

	private Properties properties;
	private SwiftClient swiftClient;
	private List<String> storedLogs;
	
	private static final int MAX_DAYS_TO_STORE = 7;
	private static final long SLEEP_PERIOD = 60000;
	private static final String LOG_DIR_PATH = "log_dir_path";
	private static final Logger LOGGER = Logger.getLogger(LogStorer.class);	
	private static final String SWIFT_LOG_CONTAINER_NAME = "swift_container_name";
	private static final String SWIFT_LOG_PSEUD_FOLDER = "swift_log_pseud_folder_prefix";
	
	public LogStorer(Properties properties) {
		this(properties, new SwiftClient(properties));
	}
	
	public LogStorer(Properties properties, SwiftClient swiftClient) {
		this.properties = properties;
		this.swiftClient = swiftClient;
		this.storedLogs = new ArrayList<String>();
	}
	
	protected void exec() {
		
		while(true) {						
			try {
				
				deleteOlderLog();				
				storeLogsInSwift();
				Thread.sleep(SLEEP_PERIOD);
			} catch (Exception e) {
				LOGGER.error("Error while executing logStorer", e);
			}			
		}
	}
					
	protected void storeLogsInSwift() throws Exception {

		File logFolder = new File(properties.getProperty(LOG_DIR_PATH));
		LOGGER.debug("Local log dir path " + logFolder.getAbsolutePath());
		
		for(File file : logFolder.listFiles()) {
			LOGGER.debug("Uploading file " + file.getName() + " to swift");
			
			swiftClient.uploadFile(properties.getProperty(SWIFT_LOG_CONTAINER_NAME), file,
					properties.getProperty(SWIFT_LOG_PSEUD_FOLDER));

			storedLogs.add(file.getName());
			LOGGER.debug("File" + file.getName() + " succssessfully uploaded");
		}
	}
	
	protected void deleteOlderLog() {
		// TODO: implement
		// it must remove older log files from swift and from storedLogDates
		if(swiftClient.numberOfFilesInContainer(SWIFT_LOG_CONTAINER_NAME) > MAX_DAYS_TO_STORE) {			
			LOGGER.info("Deleting older log file from swift");
			swiftClient.deleteFile(SWIFT_LOG_CONTAINER_NAME, SWIFT_LOG_PSEUD_FOLDER, storedLogs.get(0));
		}		
	}
	
	protected boolean isDateOlder(Date date) {
		
		return false;
	}
	
	protected Date getDateFromLogFile(String fileName) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		String[] fileNameSplit = fileName.split("\\.");
		String dateString = fileNameSplit[2];
		
		java.util.Date utilDate = null;
		java.sql.Date sqlDate = null;
		try {
			utilDate = simpleDateFormat.parse(dateString);
			sqlDate = new Date(utilDate.getTime());
		} catch (ParseException e) {
			LOGGER.error("Error while parsing simple format " + dateString + " to util Date", e);
			// TODO: see how to deal with this
		}
		
		return sqlDate;
	}
}
