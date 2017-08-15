package org.fogbowcloud.saps.engine.core.fetcher;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageData;
import org.fogbowcloud.saps.engine.scheduler.util.SebalPropertiesConstants;

public class FTPIntegrationImpl implements FTPIntegration{
	
	public static final Logger LOGGER = Logger.getLogger(FTPIntegrationImpl.class);
	
	public int getFiles(Properties properties, String ftpServerIP, String ftpServerPort,
			String remoteImageResultsPath, String localImageResultsPath, ImageData imageData) {
		if(localImageResultsPath == null || remoteImageResultsPath == null) {
			return 1;
		}
		
		LOGGER.info("Getting " + remoteImageResultsPath + " in FTPserver" + ftpServerIP + ":"
				+ ftpServerPort + " to " + localImageResultsPath + " in localhost");
		LOGGER.debug("Image " + imageData.getCollectionTierName());
				
		ProcessBuilder builder;
		if (imageData.getFederationMember()
				.equals(SebalPropertiesConstants.AZURE_FEDERATION_MEMBER)) {
			builder = new ProcessBuilder("/bin/bash",
					properties.getProperty(SebalPropertiesConstants.SEBAL_SFTP_SCRIPT_PATH),
					properties.getProperty(SebalPropertiesConstants.AZURE_FTP_SERVER_USER),
					ftpServerIP, ftpServerPort, remoteImageResultsPath, localImageResultsPath,
					imageData.getCollectionTierName());
		} else {
			builder = new ProcessBuilder("/bin/bash",
					properties.getProperty(SebalPropertiesConstants.SEBAL_SFTP_SCRIPT_PATH),
					properties.getProperty(SebalPropertiesConstants.DEFAULT_FTP_SERVER_USER),
					ftpServerIP, ftpServerPort, remoteImageResultsPath, localImageResultsPath,
					imageData.getCollectionTierName());
		}
		
		try {
			Process p = builder.start();
			p.waitFor();
			
			if(p.exitValue() != 0) {
				LOGGER.error("Error while executing sftp-access script");
				return 1;
			}
		} catch (InterruptedException e) {
			LOGGER.error("Could not get image " + imageData + " results from FTP server", e);
			return 1;
		} catch (IOException e) {
			LOGGER.error("Could not get image " + imageData + " results from FTP server", e);
			return 1;
		}
		return 0;
	}
}
