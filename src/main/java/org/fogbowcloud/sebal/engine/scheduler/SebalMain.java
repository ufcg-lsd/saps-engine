package org.fogbowcloud.sebal.engine.scheduler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.scheduler.core.SebalController;

public class SebalMain {

	private static final Logger LOGGER = Logger.getLogger(SebalMain.class);

	public static void main(String[] args) throws Exception {
		
		LOGGER.info("Loading properties...");
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		SebalController sebalController = new SebalController(properties);
		sebalController.start(true);
		LOGGER.info("Sebal Controller started.");
	}
}
