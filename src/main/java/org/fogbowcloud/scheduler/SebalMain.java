package org.fogbowcloud.scheduler;

import java.io.FileInputStream;
import java.sql.SQLException;
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
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;

public class SebalMain {
	
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	private static Map<String, ImageData> pendingImageExecution = new ConcurrentHashMap<String, ImageData>();
	private static ImageDataStore imageStore;
	
	private static final Logger LOGGER = Logger.getLogger(InfrastructureManager.class);

	public static void main(String[] args) throws Exception {

		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		imageStore = new JDBCImageDataStore(properties);

		Job job = new Job();

		InfrastructureManager infraManager = new InfrastructureManager(properties);
		Scheduler scheduler = new Scheduler(job, infraManager);
		ExecutionMonitor execMonitor = new ExecutionMonitor(job, scheduler);

		executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0,
				Integer.parseInt(properties.getProperty("execution_monitor_period")));

		schedulerTimer.scheduleAtFixedRate(scheduler, 0,
				Integer.parseInt(properties.getProperty("scheduler_period")));

		// scheduling previous image executions
		try {
			List<ImageData> notFinishedImages = imageStore.getIn(ImageState.RUNNING);

			for (ImageData imageData : notFinishedImages) {
				LOGGER.debug("The image " + imageData.getName() + " is in a execution state "
						+ imageData.getState().getValue() + "(not finished).");
				pendingImageExecution.put(imageData.getName(), imageData);

				// TODO create f1 tasks and add to the job

				// sdexsExecutor.submitImageExecution(imageData);

				// LOGGER.error("Error while submiting image execution.", e);
				// pendingImageExecution.remove(imageData.getName());
			}

		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}		
		
		sebalExecutionTimer.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				//			TODO imagens prontas para executar
				//		 	Cria tasks para imagem
				
				//TODO Check concluded executions and create next phase task if yes
				
				//TODO look for other image to execute

				
			}			
		}, 0, Integer.parseInt(properties.getProperty("sebal_execution_period")));

	}
	
//	public void init() throws IOException {
//		LOGGER.info("Initializing scheduler... ");
//
//		// scheduling previous not_finished execution to be
//		// finished
//		try {
//			schedulePreviousDownloadsNotFinished();
//		} catch (SQLException e) {
//			LOGGER.error("Error while scheduling previous downloads not finished.", e);
//		}
//
//		String schedulerPeriodStr = properties.getProperty("scheduler_period");
//		long schedulerPeriod = (schedulerPeriodStr == null ? DEFAULT_SCHEDULER_PERIOD : Long
//				.parseLong(schedulerPeriodStr));
//
//		LOGGER.debug("scheduler period: " + schedulerPeriod);
//		executionScheduler.scheduleAtFixedRate(new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					selectImageAndExecute();
//				} catch (Throwable e) {
//					LOGGER.error("Failed while execution task.", e);
//				}
//			}
//
//		}, 0, schedulerPeriod, TimeUnit.MILLISECONDS);
//		
//		triggerExecutionMonitor();
//	}

}
