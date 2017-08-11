package org.fogbowcloud.sebal.engine.sebal.fetcher;

import java.util.Properties;

import org.fogbowcloud.sebal.engine.sebal.ImageData;

public interface FTPIntegration {
	
	public int getFiles(Properties properties, String ftpServerIP, String ftpServerPort,
			String remoteImageResultsPath, String localImageResultsPath, ImageData imageData);
}
