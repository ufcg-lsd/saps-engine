package org.fogbowcloud.saps.engine.core.scheduler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.arrebol.JobSubmitted;
import org.fogbowcloud.saps.engine.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.dto.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.dto.TaskResponseDTO;
import org.fogbowcloud.saps.engine.core.job.JobState;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;
import org.fogbowcloud.saps.engine.core.dto.CommandRequestDTO;
import org.fogbowcloud.saps.engine.core.task.Specification;
import org.fogbowcloud.saps.engine.core.task.TaskImpl;
import org.fogbowcloud.saps.engine.core.task.Task;
import org.fogbowcloud.saps.engine.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.scheduler.core.exception.SapsException;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;

public class Scheduler {

	// Constants
	public static final Logger LOGGER = Logger.getLogger(Scheduler.class);

	// Saps Controller Variables
	private static String nfsServerIP;
	private static String nfsServerPort;
	private final Properties properties;
	private final ImageDataStore imageStore;
	private final ScheduledExecutorService sapsExecutor;
	private final Arrebol arrebol;
	private final Specification workerSpec;
	private static ScheduledFuture<?> sapsSubmissor;
	private static ScheduledFuture<?> sapsChecker;

	public Scheduler(Properties properties) throws SapsException, SQLException {
		try {
			LOGGER.debug("Imagestore " + SapsPropertiesConstants.IMAGE_DATASTORE_IP + ":"
					+ SapsPropertiesConstants.IMAGE_DATASTORE_IP);
			this.imageStore = new JDBCImageDataStore(properties);

			if (!checkProperties(properties))
				throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");
		} catch (Exception e) {
			throw new SapsException("Error while initializing Sebal Controller.", e);
		}

		this.properties = properties;
		this.sapsExecutor = Executors.newScheduledThreadPool(1);
		this.arrebol = new Arrebol(properties);
		this.workerSpec = new Specification(properties.getProperty(SapsPropertiesConstants.IMAGE_WORKER));
	}

	public void start() throws Exception {
		try {
			checkTasksRunning();

			sapsSubmissor = sapsExecutor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						checkTasksPreprocessed();
						addSapsJobs(properties);
					} catch (Exception e) {
						LOGGER.error("Error while adding tasks", e);
					}
				}

			}, 0, Integer.parseInt(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR)),
					TimeUnit.SECONDS);

			sapsChecker = sapsExecutor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					checkSapsJobs();
				}
			}, 0, Integer.parseInt(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER)), TimeUnit.SECONDS);

		} catch (Exception e) {
			LOGGER.error("Error while starting Scheduler component", e);
		}
	}

	private void checkTasksRunning() {
		try {
			List<ImageTask> imageTasksInStateRunning = imageStore.getIn(ImageTaskState.RUNNING,
					ImageDataStore.UNLIMITED);
			LOGGER.debug("Verifying " + imageTasksInStateRunning.size() + " image with state RUNNING");
			for (ImageTask imageTask : imageTasksInStateRunning) {

				imageTask.setState(ImageTaskState.READY);
				imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());

				try {
					imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(), imageTask.getUpdateTime());
				} catch (SQLException e) {
					LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
							+ imageTask.getUpdateTime() + " in Catalogue", e);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while verify tasks in state preprocessed.", e);
		}
	}

	private void checkTasksPreprocessed() {
		try {
			List<ImageTask> imageTasksInStatePreprocessed = imageStore.getIn(ImageTaskState.PREPROCESSED,
					ImageDataStore.UNLIMITED);
			LOGGER.debug("Verifying " + imageTasksInStatePreprocessed.size() + " image with state PREPROCESSED");
			for (ImageTask imageTask : imageTasksInStatePreprocessed) {

				String imageNfsServerIP = "";
				String imageNfsServerPort = "";

				Map<String, String> nfsConfig = imageStore.getFederationNFSConfig(imageTask.getFederationMember());
				@SuppressWarnings("rawtypes")
				Iterator it = nfsConfig.entrySet().iterator();
				while (it.hasNext()) {
					@SuppressWarnings("rawtypes")
					Map.Entry pair = (Map.Entry) it.next();
					imageNfsServerIP = pair.getKey().toString();
					imageNfsServerPort = pair.getValue().toString();
					it.remove();
				}

				if (verifyTaskOK(imageNfsServerIP, imageNfsServerPort, imageTask.getAlgorithmExecutionTag()))
					imageTask.setState(ImageTaskState.READY);
				else
					imageTask.setState(ImageTaskState.FAILED);

				imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
				try {
					imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(), imageTask.getUpdateTime());
				} catch (SQLException e) {
					LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
							+ imageTask.getUpdateTime() + " in Catalogue", e);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while verify tasks in state preprocessed.", e);
		}
	}

	private boolean verifyTaskOK(String imageNfsServerIP, String imageNfsServerPort, String algorithmExecutionTag) {
		if (imageNfsServerIP != "" && imageNfsServerPort != "" && algorithmExecutionTag != "")
			return true;
		else
			return false;
	}

	private void addSapsJobs(final Properties properties)
			throws Exception {
		try {
			List<ImageTask> imageTasksToProcess = imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED);
			LOGGER.debug("Trying add " + imageTasksToProcess.size() + " images with state READY");
			for (ImageTask imageTask : imageTasksToProcess) {
				LOGGER.debug("Image task " + imageTask.getTaskId() + " is in the execution state "
						+ imageTask.getState().getValue() + " (not finished).");
				String federationMember = imageTask.getFederationMember();
				
				Map<String, String> nfsConfig = imageStore.getFederationNFSConfig(federationMember);

				@SuppressWarnings("rawtypes")
				Iterator it = nfsConfig.entrySet().iterator();
				while (it.hasNext()) {
					@SuppressWarnings("rawtypes")
					Map.Entry pair = (Map.Entry) it.next();
					nfsServerIP = pair.getKey().toString();
					nfsServerPort = pair.getValue().toString();
					it.remove();
				}
				
				TaskImpl imageTaskToJob = new TaskImpl(imageTask.getTaskId(), workerSpec, UUID.randomUUID().toString());
				
				LOGGER.info("Creating saps task");
				SapsTask.createTask(imageTaskToJob, imageTask);

				SapsJob imageJob = new SapsJob(UUID.randomUUID().toString(), federationMember, imageTask.getTaskId());
				imageJob.addTask(imageTaskToJob);

				try {
					String jobId = arrebol.addJob(imageJob);
					LOGGER.debug("Result submit job: " + jobId);

					arrebol.addJobInList(new JobSubmitted(jobId, imageTask));
					LOGGER.info("Adding job in list");
				}catch(SubmitJobException e) {
					LOGGER.error("Error while trying to send request for Arrebol with new saps job.", e);
				}


				imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
				imageTask.setState(ImageTaskState.RUNNING);
				try {
					imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(), imageTask.getUpdateTime());
				} catch (SQLException e) {
					LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
							+ imageTask.getUpdateTime() + " in Catalogue", e);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while getting task.", e);
		}

		/*Task task01 = new TaskImpl("001", UUID.randomUUID().toString());
		task01.addCommand(new CommandRequestDTO("sleep 10", CommandRequestDTO.Type.REMOTE));

		SapsJob sapsJob01 = new SapsJob(UUID.randomUUID().toString(), "Thiago", new LinkedList<Task>(), "0001");
		sapsJob01.addTask(task01);

		try {
			String jobId = arrebol.addJob(sapsJob01);
			LOGGER.debug("Result submit job: " + jobId);

			arrebol.addJobInList(new JobSubmitted(jobId, null));
			LOGGER.info("Adding job in list");
		}catch(SubmitJobException e) {
			LOGGER.error("Error while trying to send request for Arrebol with new saps job.", e);
		}*/
	}

	private void checkSapsJobs() {
		try {
			List<JobSubmitted> jobsSubmitted = arrebol.returnAllJobsSubmitted();
			LOGGER.debug("Checking jobs " + jobsSubmitted.size() +" submitted for arrebol service");
			for (JobSubmitted job : jobsSubmitted) {
				String jobId = job.getJobId();

				LOGGER.info("Checking job " + jobId + " ...");

				JobResponseDTO jobResponse = arrebol.checkStatusJob(jobId);

				if(checkJobFinish(jobResponse)){
					LOGGER.debug("Job " + jobId + " finished");

					arrebol.removeJob(job);

					ImageTask imageTask = job.getImageTask();
					imageTask.setState(ImageTaskState.FINISHED);

					try {
						imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
					}catch(SQLException e){
						LOGGER.error("Error while update time in task.", e);
					}

					try {
						imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(), imageTask.getUpdateTime());
					} catch (SQLException e) {
						LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
								+ imageTask.getUpdateTime() + " in Catalogue", e);
					}
				}

				if(checkJobFail(jobResponse)){
					LOGGER.debug("Job " + jobId + " failed");

					arrebol.removeJob(job);

					ImageTask imageTask = job.getImageTask();
					imageTask.setState(ImageTaskState.FAILED);

					try {
						imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
					}catch(SQLException e){
						LOGGER.error("Error while update time in task.", e);
					}

					try {
						imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(), imageTask.getUpdateTime());
					} catch (SQLException e) {
						LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
								+ imageTask.getUpdateTime() + " in Catalogue", e);
					}
				}
			}
		}catch(GetJobException e){
			LOGGER.error("Error while trying check status jobs submitted.", e);
		}

	}

	private boolean checkJobFinish(JobResponseDTO jobResponse){

		for(TaskResponseDTO task : jobResponse.getTasks()){
			if(task.getState().compareTo("FINISHED") != 0)
				return false;
		}

		return true;
	}

	private boolean checkJobFail(JobResponseDTO jobResponse){

		return false;
	}

	protected static boolean checkProperties(Properties properties) {
		if (!properties.containsKey(SapsPropertiesConstants.IMAGE_DATASTORE_IP)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.IMAGE_DATASTORE_IP + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.IMAGE_DATASTORE_PORT)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.IMAGE_DATASTORE_PORT + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_SPECS_BLOCK_CREATING)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.INFRA_SPECS_BLOCK_CREATING + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.IMAGE_WORKER)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.IMAGE_WORKER + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXPORT_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXPORT_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.ARREBOL_BASE_URL)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.ARREBOL_BASE_URL + " was not set");
			return false;
		}

		LOGGER.debug("All properties are set");
		return true;
	}

	public Properties getProperties() {
		return properties;
	}
}