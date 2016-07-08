package org.fogbowcloud.sebal.fetcher;

import java.util.Properties;

import org.fogbowcloud.sebal.ImageData;

public interface FTPIntegration {
	
	public int getFiles(Properties properties, String ftpServerIP,
			String ftpServerPort, String remoteImageResultsPath,
			String localImageResultsPath, ImageData imageData);
}
