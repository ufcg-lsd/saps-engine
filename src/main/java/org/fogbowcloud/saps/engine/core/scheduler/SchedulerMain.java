package org.fogbowcloud.saps.engine.core.scheduler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class SchedulerMain {

	private static final Logger LOGGER = Logger.getLogger(SchedulerMain.class);

	public static void main(String[] args) throws Exception {
		
		LOGGER.info("Loading properties...");
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		LOGGER.info("Trying to start Saps Controller");
		Scheduler sapsController = new Scheduler(properties);
		sapsController.start();
		
		LOGGER.info("Saps Controller started.");
	}
}
