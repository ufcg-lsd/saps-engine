package org.fogbowcloud.scheduler.client;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionMonitorWithDB;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.scheduler.restlet.JDFSchedulerApplication;
import org.fogbowcloud.scheduler.restlet.JDFSchedulerApplicationWithPersistence;
import org.mapdb.DB;
import org.mapdb.DBMaker;
/**
 * This class works as a translator. It receives a JDF file as an input
 * and creates a JDL file as the output.
 * @author Ricardo Araujo Santos - ricardo@lsd.ufcg.edu.br
 */
public class JDFMain {


	

	public static final Logger LOGGER = Logger.getLogger(JDFMain.class);

	private static boolean blockWhileInitializing;
	private static boolean isElastic;
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	private static ConcurrentMap<String, JDFJob> jobMapDB;
	
	private static Properties properties;
	/**
	 * This method receives a JDF file as input and requests the mapping of
	 * its attributes to JDL attributes, generating a JDL file at the end
	 * @param args 
	 * @throws Exception 
	 */
	public static void main( String[ ] args ) throws Exception {
		properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		loadConfigFromProperties();

		// Initialize a MapDB database
		final File pendingImageDownloadFile = new File(AppPropertiesConstants.DB_FILE_NAME);
		final DB pendingImageDownloadDB = DBMaker.newFileDB(pendingImageDownloadFile).make();
		pendingImageDownloadDB.checkShouldCreate(AppPropertiesConstants.DB_MAP_NAME);
		jobMapDB = pendingImageDownloadDB.getHashMap(AppPropertiesConstants.DB_MAP_NAME);
		
		InfrastructureProvider infraProvider = createInfraProvaiderInstance();
		InfrastructureManager infraManager = new InfrastructureManager(null, isElastic, infraProvider,
				properties);
		infraManager.start(blockWhileInitializing);
		
		ArrayList<JDFJob> legacyJobs = new ArrayList<JDFJob>();
		
		for (String key : jobMapDB.keySet()) {
			legacyJobs.add((JDFJob) jobMapDB.get(key));
		}
			
		Scheduler scheduler = new Scheduler(infraManager, legacyJobs.toArray(new JDFJob[legacyJobs.size()]));

		LOGGER.debug("Propertie: " +properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH));
		
		LOGGER.debug("Application to be started on port: " +properties.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
		ExecutionMonitorWithDB executionMonitor = new ExecutionMonitorWithDB(scheduler, pendingImageDownloadDB);
		JDFSchedulerApplicationWithPersistence app = new JDFSchedulerApplicationWithPersistence(scheduler, properties, pendingImageDownloadDB);
		app.startServer();

		

		LOGGER.debug("Starting Scheduler and Execution Monitor, execution monitor period: " + properties.getProperty(AppPropertiesConstants.EXECUTION_MONITOR_PERIOD));
		schedulerTimer.scheduleAtFixedRate(scheduler, 0, 30000);
		executionMonitorTimer.scheduleAtFixedRate(executionMonitor, 0, 30000);
	}

	private static void loadConfigFromProperties() {

		blockWhileInitializing = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING)).booleanValue();
		isElastic = new Boolean(properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();

	}

	private static InfrastructureProvider createInfraProvaiderInstance() throws Exception {

		String providerClassName = properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception("Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}
	
	
	
}