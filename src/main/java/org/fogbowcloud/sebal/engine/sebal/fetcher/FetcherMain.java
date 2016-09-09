package org.fogbowcloud.sebal.engine.sebal.fetcher;

import java.io.FileInputStream;
import java.util.Properties;

public class FetcherMain {

	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String imageStoreIP = args[1];
		String imageStorePort = args[2];
		String ftpServerIP = args[3];
		String ftpServerPort = args[4];
		
		properties.put("datastore_ip", imageStoreIP);
		properties.put("datastore_port", imageStorePort);
		
		Fetcher Fetcher = new Fetcher(properties, ftpServerIP, ftpServerPort);
		Fetcher.exec();
	}
}
