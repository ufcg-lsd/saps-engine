package org.fogbowcloud.saps.engine.core.scheduler;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.catalog.jdbc.JDBCCatalog;
import org.fogbowcloud.saps.engine.core.scheduler.executor.JobExecutionService;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.ArrebolJobExecutionService;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.request.ArrebolRequestsHelper;
import org.fogbowcloud.saps.engine.core.scheduler.selector.DefaultRoundRobin;
import org.fogbowcloud.saps.engine.core.scheduler.selector.Selector;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;

public class SchedulerMain {

	private static final Logger LOGGER = Logger.getLogger(SchedulerMain.class);

	public static void main(String[] args) throws Exception {
		
		LOGGER.info("Loading properties...");
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		LOGGER.info("Trying to start Saps Controller");
		Scheduler sapsController = createScheduler(properties);
		sapsController.start();
		
		LOGGER.info("Saps Controller started.");
	}

	private static Scheduler createScheduler(Properties properties) throws SapsException {
		Catalog catalog = new JDBCCatalog(properties);
		ScheduledExecutorService ses =  Executors.newScheduledThreadPool(1);
		ArrebolRequestsHelper helper = new ArrebolRequestsHelper(properties.getProperty(SapsPropertiesConstants.ARREBOL_BASE_URL));
		JobExecutionService jobExecutionService = new ArrebolJobExecutionService(helper);
		Selector selector =  new DefaultRoundRobin();
		Scheduler scheduler = new Scheduler(properties, catalog, ses, jobExecutionService, selector);
		return scheduler;
	}
}
