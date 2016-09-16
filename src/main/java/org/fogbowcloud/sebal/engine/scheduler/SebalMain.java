package org.fogbowcloud.sebal.engine.scheduler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.sebal.engine.scheduler.restlet.SebalScheduleApplication;

public class SebalMain {

	private static String nfsServerIP;
	private static String nfsServerPort;

	private static final Logger LOGGER = Logger.getLogger(SebalMain.class);
	private static final String SEBAL_EXECUTION_PERIOD = "sebal_execution_period";

	public static void main(String[] args) throws Exception {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		String imageStoreIP = args[1];
		String imageStorePort = args[2];
		nfsServerIP = args[3];
		nfsServerPort = args[4];

		LOGGER.debug("Imagestore " + imageStoreIP + ":" + imageStorePort);
		
		if (!checkProperties(properties)) {
			System.err.println("Missing required property, check Log for more information.");
			System.exit(1);
		}

		SebalScheduleApplication restletServer = new SebalScheduleApplication(
				new SchedulerController(properties, nfsServerIP, nfsServerPort));
		restletServer.startServer();

		LOGGER.info("Scheduler working");
	}

	private static boolean checkProperties(Properties properties) {
		if (!properties
				.containsKey(AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING)) {
			LOGGER.error("Required property "
					+ AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.SCHEDULER_PERIOD)) {
			LOGGER.error("Required property "
					+ AppPropertiesConstants.SCHEDULER_PERIOD + " was not set");
			return false;
		}

		if (!properties.containsKey(SEBAL_EXECUTION_PERIOD)) {
			LOGGER.error("Required property " + SEBAL_EXECUTION_PERIOD
					+ " was not set");
			return false;
		}

		if (!properties
				.containsKey(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT)) {
			LOGGER.error("Required property "
					+ AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT
					+ " was not set");
			return false;
		}

		if (!properties.containsKey(AppPropertiesConstants.REST_SERVER_PORT)) {
			LOGGER.error("Required property "
					+ AppPropertiesConstants.REST_SERVER_PORT + " was not set");
			return false;
		}

		if (!properties
				.containsKey(AppPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property "
					+ AppPropertiesConstants.EXECUTION_MONITOR_PERIOD
					+ " was not set");
			return false;
		}

		if (!properties.containsKey(AppPropertiesConstants.INFRA_IS_STATIC)) {
			LOGGER.error("Required property "
					+ AppPropertiesConstants.INFRA_IS_STATIC + " was not set");
			return false;
		}

		if (!properties
				.containsKey(AppPropertiesConstants.INFRA_FOGBOW_USERNAME)) {
			LOGGER.error("Required property "
					+ AppPropertiesConstants.INFRA_FOGBOW_USERNAME
					+ " was not set");
			return false;
		}

		LOGGER.debug("All properties are set");
		return true;
	}
}
