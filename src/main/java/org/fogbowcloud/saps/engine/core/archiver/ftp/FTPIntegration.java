package org.fogbowcloud.saps.engine.core.archiver.ftp;

import java.util.Properties;

import org.fogbowcloud.saps.engine.core.model.SapsImage;

public interface FTPIntegration {
	
	public int getFiles(Properties properties, String ftpServerIP, String ftpServerPort,
			String remoteImageResultsPath, String localImageResultsPath, SapsImage imageData);
}
