package org.fogbowcloud.saps.engine.core.archiver;

import java.util.Properties;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public interface FTPIntegration {
	
	public int getFiles(Properties properties, String ftpServerIP, String ftpServerPort,
			String remoteImageResultsPath, String localImageResultsPath, ImageTask imageData);
}
