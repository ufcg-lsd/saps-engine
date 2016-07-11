package org.fogbowcloud.sebal.fetcher;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.ImageData;

public class FTPIntegrationImpl implements FTPIntegration{
	
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
		
		ProcessBuilder builder = new ProcessBuilder("/bin/bash",
				"scripts/sftp-access.sh",
				properties.getProperty("ftp_server_user"), ftpServerIP,
				ftpServerPort, remoteImageResultsPath, localImageResultsPath,
				imageData.getName());
		
		try {
			Process p = builder.start();
			p.waitFor();
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
