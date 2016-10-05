package org.fogbowcloud.sebal.engine.sebal.fetcher;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.ImageData;

public class FTPIntegrationImpl implements FTPIntegration{
	
	private static final String FTP_SERVER_USER = "ftp_server_user";
	private static final String SEBAL_SFTP_SCRIPT_PATH = "sebal_sftp_script_path";
	public static final Logger LOGGER = Logger.getLogger(FTPIntegrationImpl.class);
	
	public int getFiles(Properties properties, String ftpServerIP,
			String ftpServerPort, String remoteImageResultsPath,
			String localImageResultsPath, ImageData imageData) {
		if(localImageResultsPath == null || remoteImageResultsPath == null) {
			return 1;
		}
		
		LOGGER.info("Getting " + remoteImageResultsPath + " in FTPserver"
				+ ftpServerIP + ":" + ftpServerPort + " to "
				+ localImageResultsPath + " in localhost");
		
		LOGGER.debug("Image " + imageData.getName());
		
		ProcessBuilder builder = new ProcessBuilder("/bin/bash",
				properties.getProperty(SEBAL_SFTP_SCRIPT_PATH),
				properties.getProperty(FTP_SERVER_USER), ftpServerIP,
				ftpServerPort, remoteImageResultsPath, localImageResultsPath,
				imageData.getName());
		
		try {
			Process p = builder.start();
			p.waitFor();
			
			if(p.exitValue() != 0) {
				LOGGER.error("Error while executing sftp-access script");
				return 1;
			}
		} catch (InterruptedException e) {
			LOGGER.error("Could not get image " + imageData
					+ " results from FTP server", e);
			return 1;
		} catch (IOException e) {
			LOGGER.error("Could not get image " + imageData
					+ " results from FTP server", e);
			return 1;
		}
		return 0;
	}
}
