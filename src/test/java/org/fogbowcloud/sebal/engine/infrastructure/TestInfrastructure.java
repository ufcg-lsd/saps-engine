package org.fogbowcloud.sebal.engine.infrastructure;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Test;

public class TestInfrastructure {
	
	private static final String INFRA_CRAWLER = "crawler";
	private static final String INFRA_SCHEDULER = "scheduler";
	
	private static final Logger LOGGER = Logger.getLogger(TestInfrastructure.class);

	@Test
	public void test() throws Exception {
		LOGGER.debug("Starting infrastructure creation process...");

		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("src/main/resources/sebal.conf");
		properties.load(input);
		
		String infraType = INFRA_SCHEDULER;
		//String infraType = INFRA_CRAWLER;
		//String infraType = INFRA_FETCHER;
/*		
		InfrastructureHelper.createInfrastrucute(properties, infraType);
		
		InfrastructureHelper.writeInstanceDataFile(infraType);*/
		
		LOGGER.debug("Infrastructure created.");
	}

}
