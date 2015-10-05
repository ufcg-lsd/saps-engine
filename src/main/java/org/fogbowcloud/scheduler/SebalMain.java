package org.fogbowcloud.scheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.SebalJob;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.SebalTasks;

import com.google.gson.Gson;

public class SebalMain {
	
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	private static Map<String, ImageData> pendingImageExecution = new ConcurrentHashMap<String, ImageData>();
	private static ImageDataStore imageStore;
	
	private static final Logger LOGGER = Logger.getLogger(InfrastructureManager.class);

	public static void main(String[] args) throws Exception {

		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		imageStore = new JDBCImageDataStore(properties);

		final Job job = new SebalJob(imageStore);

		boolean blockWhileInitializing = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING))
				.booleanValue();

		boolean isElastic = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();
		List<Specification> initialSpecs = getInitialSpecs(properties);

		InfrastructureProvider infraProvider = createInfraProvaiderInstance(properties);
		InfrastructureManager infraManager = new InfrastructureManager(initialSpecs, isElastic,
				infraProvider, properties);
		infraManager.start(blockWhileInitializing);
		
		Scheduler scheduler = new Scheduler(job, infraManager);
		ExecutionMonitor execMonitor = new ExecutionMonitor(job, scheduler);

		executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0,
				Integer.parseInt(properties.getProperty("execution_monitor_period")));

		schedulerTimer.scheduleAtFixedRate(scheduler, 0,
				Integer.parseInt(properties.getProperty("scheduler_period")));

		final Specification sebalSpec = getSebalSpecFromFile(properties);
		
		// scheduling previous image executions
		try {
			List<ImageData> notFinishedImages = imageStore.getIn(ImageState.RUNNING);
			for (ImageData imageData : notFinishedImages) {
				LOGGER.debug("The image " + imageData.getName() + " is in a execution state "
						+ imageData.getState().getValue() + "(not finished).");
				pendingImageExecution.put(imageData.getName(), imageData);

				List<Task> f1Tasks = SebalTasks.createF1Tasks(properties, imageData.getName(),
						sebalSpec);

				for (Task task : f1Tasks) {
					job.addTask(task);
				}
			}
			
			//Looking for REDUCING images and add task c

		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}		
		
		sebalExecutionTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				ImageData imageData;
				try {
					imageData = selectImageToRun();
					LOGGER.debug("Image selected to execute is " + imageData);
					if (imageData == null) {
						LOGGER.debug("There is not image to run.");
						return;
					}

					// TODO implement lock when exists more than one scheduler
					imageData.setState(ImageState.RUNNING);
					imageStore.update(imageData);
					pendingImageExecution.put(imageData.getName(), imageData);

					// adding f1 tasks to image
					List<Task> f1Tasks = SebalTasks.createF1Tasks(properties, imageData.getName(),
							sebalSpec);
					for (Task task : f1Tasks) {
						job.addTask(task);
					}
				} catch (SQLException e) {
					LOGGER.error("Error while trying connecting images DB.", e);
				}
			}
		}, 0, Integer.parseInt(properties.getProperty("sebal_execution_period")));

	}
	
	private static Specification getSebalSpecFromFile(Properties properties) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static ImageData selectImageToRun() throws SQLException {		
		LOGGER.debug("Searching for image to run.");
		//TODO Should we need to select image specifying the federation member?!
		List<ImageData> imageDataList = imageStore.getIn(ImageState.DOWNLOADED, 1);
		if (imageDataList != null && imageDataList.size() > 0) {
			return imageDataList.get(0);
		}
		return null;
	}

	private static List<Specification> getInitialSpecs(Properties properties)
			throws FileNotFoundException {
		String initialSpecsFilePath = properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);		
		LOGGER.info("Getting initial spec from file " + initialSpecsFilePath);
		
		List<Specification> specifications = new ArrayList<Specification>();
		if (initialSpecsFilePath != null && new File(initialSpecsFilePath).exists()) {
			BufferedReader br = new BufferedReader(new FileReader(initialSpecsFilePath));

			Gson gson = new Gson();
			specifications = Arrays.asList(gson.fromJson(br, Specification[].class));
		}
		return specifications;
	}
	
	private static InfrastructureProvider createInfraProvaiderInstance(Properties properties)
			throws Exception {

		String providerClassName = properties
				.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class)
				.newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception(
					"Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}
}
