package org.fogbowcloud.sebal.crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class CrawlerMain {
	
	public static final Logger LOGGER = Logger.getLogger(CrawlerMain.class);

	public static void main(String[] args) throws IOException, InterruptedException {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		//TODO: check param
		try {
			String imageStoreIP = args[1];
			String imageStorePort = args[2];
			String federationMember = args[3];

			Crawler crawler = new Crawler(properties, imageStoreIP, imageStorePort, federationMember);
			crawler.exec();
		} catch(NullPointerException e) {
			LOGGER.error("Data store IP/PORT is null", e);
		}
		
	}
}
