package org.fogbowcloud.sebal.crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class CrawlerMain {

	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		Crawler crawler = new Crawler(properties);
		
		crawler.init();
	}
}
