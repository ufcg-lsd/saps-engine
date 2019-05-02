package org.fogbowcloud.saps.engine.scheduler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.scheduler.core.SapsController;

public class SchedulerMain {

	private static final Logger LOGGER = Logger.getLogger(SchedulerMain.class);

	public static void main(String[] args) throws Exception {
		
		LOGGER.info("Loading properties...");
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		SapsController sebalController = new SapsController(properties);
		sebalController.start(true);
		LOGGER.info("Sebal Controller started.");
	}
}
