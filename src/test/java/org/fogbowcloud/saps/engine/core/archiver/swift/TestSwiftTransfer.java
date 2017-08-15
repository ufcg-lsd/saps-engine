package org.fogbowcloud.saps.engine.core.archiver.swift;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftClient;

public class TestSwiftTransfer {

	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("config/sebal.conf");
		properties.load(input);
		
		SwiftClient swiftClient = new SwiftClient(properties);
		
		String containerName = "container-test";
		String pseudFolder = "test";
		File file = new File("/tmp/test-file");
		
		swiftClient.createContainer(containerName);
		
		try {
			Date beginTime = new Date(Calendar.getInstance().getTimeInMillis());
			System.out.println(new Timestamp(beginTime.getTime()));
			swiftClient.uploadFile(containerName, file, pseudFolder);
			Date endTime = new Date(Calendar.getInstance().getTimeInMillis());
			System.out.println(new Timestamp(endTime.getTime()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		swiftClient.deleteFile(containerName, pseudFolder, file.getName());
		swiftClient.deleteContainer(containerName);
	}

}
