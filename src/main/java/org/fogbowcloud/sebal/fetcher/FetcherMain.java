package org.fogbowcloud.sebal.fetcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FetcherMain {

	public static void main(String[] args) throws IOException, InterruptedException {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String imageStoreIP = args[1];
		String imageStorePort = args[2];
		String ftpServerIP = args[3];
		String ftpServerPort = args[4];
		
		Fetcher Fetcher = new Fetcher(properties, imageStoreIP, imageStorePort, ftpServerIP, ftpServerPort);
		Fetcher.exec();
	}
}
