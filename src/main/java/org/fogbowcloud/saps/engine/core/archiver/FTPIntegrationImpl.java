package org.fogbowcloud.saps.engine.core.archiver;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;

public class FTPIntegrationImpl implements FTPIntegration{
	
	public static final Logger LOGGER = Logger.getLogger(FTPIntegrationImpl.class);
	
	public int getFiles(Properties properties, String ftpServerIP, String ftpServerPort,
			String remoteTaskFilesPath, String localTaskFilesPath, ImageTask imageTask) {
		if (localTaskFilesPath == null || remoteTaskFilesPath == null) {
			return 1;
		}

		LOGGER.info("Getting " + remoteTaskFilesPath + " in FTPserver" + ftpServerIP + ":"
				+ ftpServerPort + " to " + localTaskFilesPath + " in localhost");
		LOGGER.debug("Task " + imageTask.getTaskId());

		ProcessBuilder builder;
		builder = new ProcessBuilder("/bin/bash",
				properties.getProperty(SapsPropertiesConstants.SAPS_SFTP_SCRIPT_PATH),
				properties.getProperty(SapsPropertiesConstants.DEFAULT_FTP_SERVER_USER),
				ftpServerIP, ftpServerPort, remoteTaskFilesPath, localTaskFilesPath,
				imageTask.getTaskId());

		try {
			Process p = builder.start();
			p.waitFor();

			if (p.exitValue() != 0) {
				LOGGER.error("Error while executing sftp-access script");
				return 1;
			}
		} catch (InterruptedException e) {
			LOGGER.error("Could not get task " + imageTask + " files from FTP server", e);
			return 1;
		} catch (IOException e) {
			LOGGER.error("Could not get task " + imageTask + " files from FTP server", e);
			return 1;
		}
		return 0;
	}
}
