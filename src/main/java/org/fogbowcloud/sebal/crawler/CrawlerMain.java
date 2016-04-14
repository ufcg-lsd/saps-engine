package org.fogbowcloud.sebal.crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class CrawlerMain {

	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String imageStoreIP = args[1];
		String imageStorePort = args[2];
		
		Crawler crawler = new Crawler(properties, imageStoreIP, imageStorePort);
		
		crawler.init();
	}
}
