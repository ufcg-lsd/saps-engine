package org.fogbowcloud.saps.engine.core.archiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
				ftpServerIP, ftpServerPort, remoteTaskFilesPath, localTaskFilesPath);

		try {
			Process p = builder.start();
			p.waitFor();

			if (p.exitValue() != 0) {
				LOGGER.error(
						"Error while executing sftp-access script...Error: " + getProcessOutput(p));
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
	
	private static String getProcessOutput(Process p) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(System.getProperty("line.separator"));
		}
		return stringBuilder.toString();
	}
}
