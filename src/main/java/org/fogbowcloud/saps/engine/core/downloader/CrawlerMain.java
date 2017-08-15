package org.fogbowcloud.saps.engine.core.downloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class CrawlerMain {
	
	public static final Logger LOGGER = Logger.getLogger(CrawlerMain.class);

	public static void main(String[] args) throws IOException, InterruptedException, SQLException {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		try {
			String crawlerIp = args[1];
			String nfsPort = args[2];
			String federationMember = args[3];

			Crawler crawler = new Crawler(properties, crawlerIp, nfsPort, federationMember);
			crawler.exec();
		} catch(NullPointerException e) {
			LOGGER.error("Invalid args", e);
		}		
	}
}
