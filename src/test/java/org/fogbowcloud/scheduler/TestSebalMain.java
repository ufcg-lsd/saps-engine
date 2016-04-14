package org.fogbowcloud.scheduler;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.SebalJob;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.util.Constants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.SebalTasks;
import org.fogbowcloud.sebal.crawler.Crawler;
import org.junit.Test;

public class TestSebalMain {
	
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	
	private static ImageDataStore imageStore;
	private static String remoteRepositoryIP;
	private static final Logger LOGGER = Logger.getLogger(TestSebalMain.class);
	
	@Test
	public void runTest() throws Exception {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream("src/main/resources/sebal.conf");
		properties.load(input);
		
		imageStore = new JDBCImageDataStore(properties, null, null);			
		
		initializeCrawlerInstance(properties, remoteRepositoryIP);

		final Job job = new SebalJob(imageStore);
		
		boolean blockWhileInitializing = new Boolean(
				properties
						.getProperty(AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING))
				.booleanValue();

		boolean isElastic = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC))
				.booleanValue();

		List<Specification> schedulerSpecs = getInitialSpecs(properties);

		InfrastructureProvider infraProvider = createInfraProviderInstance(properties);
		
		InfrastructureManager infraManager = new InfrastructureManager(schedulerSpecs, isElastic,
				infraProvider, properties);
		infraManager.start(blockWhileInitializing);
		
		Scheduler scheduler = new Scheduler(infraManager, job);
		ExecutionMonitor execMonitor = new ExecutionMonitor(scheduler, job);

		final Specification sebalSpec = getSebalSpecFromFile(properties);
		
		addFakeRTasks(properties, job, sebalSpec, ImageState.READY_FOR_R);
		addRTasks(properties, job, sebalSpec, ImageState.RUNNING_R, ImageDataStore.UNLIMITED);
		
		executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0,
				Integer.parseInt(properties.getProperty("execution_monitor_period")));

		schedulerTimer.scheduleAtFixedRate(scheduler, 0,
				Integer.parseInt(properties.getProperty("scheduler_period")));
				
		sebalExecutionTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {				
				addRTasks(properties, job, sebalSpec, ImageState.DOWNLOADED, 1);
			}	
		}, 0, Integer.parseInt(properties.getProperty("sebal_execution_period")));
		
		SebalScheduleApplication restletServer = new SebalScheduleApplication((SebalJob)job, imageStore, properties);
		restletServer.startServer();
	}
	
	private static void initializeCrawlerInstance(Properties properties,
			String remoteRepositoryIP) throws Exception {
		boolean blockWhileInitializing = new Boolean(
				properties
						.getProperty(AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING))
				.booleanValue();

		boolean isElastic = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC))
				.booleanValue();

		InfrastructureProvider infraProvider = createInfraProviderInstance(properties);

		List<Specification> crawlerSpecs = getCrawlerSpecs(properties);

		InfrastructureManager infraManager = new InfrastructureManager(
				crawlerSpecs, isElastic, infraProvider, properties);
		infraManager.start(blockWhileInitializing);

		//TODO:
		final Crawler crawler = new Crawler(properties, imageStore, executor,
				remoteRepositoryIP, "imageStoreIP");
		
		ExecutionMonitor execCrawlerMonitor = new ExecutionMonitor(crawler);

		executionMonitorTimer.scheduleAtFixedRate(execCrawlerMonitor, 0,
				Integer.parseInt(properties
						.getProperty("execution_monitor_period")));

		sebalExecutionTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				crawler.init();
			}
		}, 0, Integer.parseInt(properties.getProperty("scheduler_period")));
	}
	
	private static void addFakeRTasks(Properties properties, Job job,
			Specification sebalSpec, ImageState imageState) {
		try {
			List<ImageData> completedImages = imageStore.getIn(imageState);

			for (ImageData imageData : completedImages) {

				LOGGER.info("Adding fake Completed Tasks for image " + imageData.getName());
				
				TaskImpl taskImpl = new TaskImpl(UUID.randomUUID().toString(), sebalSpec);

				//TODO:
				taskImpl = SebalTasks.createRTask(taskImpl, properties, imageData.getName(), sebalSpec,
						imageData.getFederationMember(), "setCrawlerIPHere");
				
				job.addFakeTask(taskImpl);
			}

		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}
	}
	
	private static void addRTasks(final Properties properties, final Job job,
			final Specification sebalSpec, ImageState imageState, int limit) {
		try {
			List<ImageData> imagesToExecute = imageStore.getIn(imageState, limit);				
			
			for (ImageData imageData : imagesToExecute) {
				LOGGER.debug("The image " + imageData.getName() + " is in the execution state "
						+ imageData.getState().getValue() + " (not finished).");

				LOGGER.info("Adding " + imageState + " tasks for image " + imageData.getName());
				
				TaskImpl taskImpl = new TaskImpl(UUID.randomUUID().toString(), sebalSpec);
				
				if (ImageState.RUNNING_R.equals(imageState)
						|| ImageState.DOWNLOADED.equals(imageState)) {
					taskImpl = SebalTasks.createRTask(taskImpl, properties, imageData.getName(),
							sebalSpec, imageData.getFederationMember(), "setCrawlerIPHere");
					imageData.setState(ImageState.RUNNING_R);					
				}
				
				job.addTask(taskImpl);
				
				imageStore.updateImage(imageData);
			}
			
		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}
	}
	
	private static Specification getSebalSpecFromFile(Properties properties) {
		String sebalSpecFile = properties.getProperty("sebal_task_spec_path");
		List<Specification> specs = new ArrayList<Specification>();
		try {
			specs = Specification.getSpecificationsFromJSonFile(sebalSpecFile);
			if(specs!= null && !specs.isEmpty()){
				return specs.get(Constants.LIST_ARRAY_FIRST_ELEMENT);
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}
	
	private static List<Specification> getInitialSpecs(Properties properties)
			throws IOException {
		String initialSpecsFilePath = properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);		
		LOGGER.info("Getting initial spec from file " + initialSpecsFilePath);
		
		return Specification.getSpecificationsFromJSonFile(initialSpecsFilePath);
	}
	
	private static InfrastructureProvider createInfraProviderInstance(Properties properties)
			throws Exception {

		String providerClassName = properties
				.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception(
					"Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}
	
	private static List<Specification> getCrawlerSpecs(Properties properties)
			throws IOException {
		String crawlerSpecsFilePath = properties.getProperty(AppPropertiesConstants.INFRA_CRAWLER_SPECS_FILE_PATH);		
		LOGGER.info("Getting crawler spec from file " + crawlerSpecsFilePath);
		
		return Specification.getSpecificationsFromJSonFile(crawlerSpecsFilePath);
	}
	
}
