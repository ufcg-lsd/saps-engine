package org.fogbowcloud.infrastructure;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class InfrastructureMain {
	
	private static final Logger LOGGER = Logger.getLogger(InfrastructureMain.class);

	public static void main(String[] args) throws Exception {
		LOGGER.debug("Starting infrastructure creation process...");

		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String infraType = args[1];
		
		InfrastructureHelper.createInfrastrucute(properties, infraType);
		
		InfrastructureHelper.writeInstanceDataFile(infraType);
		
		LOGGER.debug("Infrastructure created.");
	}
	
}
