package org.fogbowcloud.saps.engine.core.preprocessor;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

public class PreProcessorMain {

	public static void main(String[] args) throws IOException, SQLException {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String imageStoreIP = args[1];
		String imageStorePort = args[2];
		
		properties.put("datastore_ip", imageStoreIP);
		properties.put("datastore_port", imageStorePort);
		PreProcessor preProcessor = new PreProcessorImpl(properties);
		preProcessor.exec();
	}

}
