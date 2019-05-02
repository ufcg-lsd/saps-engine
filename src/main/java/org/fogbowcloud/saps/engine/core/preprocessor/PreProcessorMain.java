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
		
		PreProcessor preProcessor = new PreProcessor(properties);
		preProcessor.exec();
	}

}
