package org.fogbowcloud.saps.engine.core.downloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class InputDownloaderMain {
	
	public static final Logger LOGGER = Logger.getLogger(InputDownloaderMain.class);

	public static void main(String[] args) throws IOException, InterruptedException, SQLException {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		try {
			String crawlerIp = args[1];
			String nfsSshPort = args[2];
			String nfsPort = args[3];
			String federationMember = args[4];

			InputDownloader crawler = new InputDownloader(properties, crawlerIp, nfsSshPort, nfsPort, federationMember);
			crawler.exec();
		} catch(NullPointerException e) {
			LOGGER.error("Invalid args", e);
		}		
	}
}
