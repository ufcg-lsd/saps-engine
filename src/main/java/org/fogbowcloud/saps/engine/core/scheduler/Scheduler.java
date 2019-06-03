package org.fogbowcloud.saps.engine.core.scheduler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.arrebol.JobSubmitted;
import org.fogbowcloud.saps.engine.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.dto.*;
import org.fogbowcloud.saps.engine.core.job.JobState;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;
import org.fogbowcloud.saps.engine.core.task.Specification;
import org.fogbowcloud.saps.engine.core.task.TaskImpl;
import org.fogbowcloud.saps.engine.core.task.Task;
import org.fogbowcloud.saps.engine.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.scheduler.core.exception.SapsException;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.h2.command.Command;

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
		this.workerSpec = new Specification(properties.getProperty(SapsPropertiesConstants.IMAGE_WORKER), new HashMap<String, String>());
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
				imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.READY);

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

				if (verifyTaskOK(imageNfsServerIP, imageNfsServerPort, imageTask.getAlgorithmExecutionTag())) {
					imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.READY);
					imageTask.setState(ImageTaskState.READY);
				}else {
					imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.FAILED);
					imageTask.setState(ImageTaskState.FAILED);
				}

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
				imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.RUNNING);

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

		/*Task task01 = new TaskImpl("001", workerSpec, UUID.randomUUID().toString());
		task01.addCommand(new CommandRequestDTO("sleep 10", CommandRequestDTO.Type.REMOTE));

		Task task02 = new TaskImpl("002", workerSpec, UUID.randomUUID().toString());
		task02.addCommand(new CommandRequestDTO("ls", CommandRequestDTO.Type.REMOTE));

		Task task03 = new TaskImpl("003", workerSpec, UUID.randomUUID().toString());
		task03.addCommand(new CommandRequestDTO("r", CommandRequestDTO.Type.REMOTE));

		SapsJob sapsJob01 = new SapsJob(UUID.randomUUID().toString(), "Thiago", new LinkedList<Task>(), "0001");
		sapsJob01.addTask(task01);

		SapsJob sapsJob02 = new SapsJob(UUID.randomUUID().toString(), "Thiago", new LinkedList<Task>(), "0001");
		sapsJob02.addTask(task02);

		SapsJob sapsJob03 = new SapsJob(UUID.randomUUID().toString(), "Thiago", new LinkedList<Task>(), "0001");
		sapsJob03.addTask(task03);

        try {
			String jobId01 = arrebol.addJob(sapsJob01);
			LOGGER.debug("Result submit job: " + jobId01);

			arrebol.addJobInList(new JobSubmitted(jobId01, null));
			LOGGER.info("Adding job " + jobId01 + " in list");

			String jobId02 = arrebol.addJob(sapsJob02);
			LOGGER.debug("Result submit job: " + jobId02);

			arrebol.addJobInList(new JobSubmitted(jobId02, null));
			LOGGER.info("Adding job " + jobId02 + " in list");

			String jobId03 = arrebol.addJob(sapsJob03);
			LOGGER.debug("Result submit job: " + jobId03);

			arrebol.addJobInList(new JobSubmitted(jobId03, null));
			LOGGER.info("Adding job " + jobId03 + " in list");
		}catch(SubmitJobException e) {
			LOGGER.error("Error while trying to send request for Arrebol with new saps job.", e);
		}*/
	}

	private void checkSapsJobs() {
		try {
			ImageTask imageTask = null;
			List<JobSubmitted> jobsSubmitted = arrebol.returnAllJobsSubmitted();
			List<JobSubmitted> jobSubmittedsFinish = new LinkedList<JobSubmitted>();

			LOGGER.debug("Checking jobs " + jobsSubmitted.size() +" submitted for arrebol service");

			for (JobSubmitted job : jobsSubmitted) {
				String jobId = job.getJobId();

				JobResponseDTO jobResponse = arrebol.checkStatusJob(jobId);

				imageTask = job.getImageTask();

                LOGGER.debug("Checking job " + jobId + " ...");

                boolean checkFinish = checkFinish(jobResponse);

				if(checkFinish) {

                    boolean checkOK = checkJobFinish(jobResponse);

                    if(checkOK) {
                        LOGGER.debug("Job " + jobId + " finished");
                        imageTask.setState(ImageTaskState.FINISHED);
						imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.FINISHED);
                    }else{
                        LOGGER.debug("Job " + jobId + " failed");
                        imageTask.setState(ImageTaskState.FAILED);
						imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.FAILED);
                    }

                    jobSubmittedsFinish.add(job);

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

			for(JobSubmitted jobFinished : jobSubmittedsFinish){
			    LOGGER.info("Removing job " + jobFinished.getJobId() + " from the list");
                arrebol.removeJob(jobFinished);
            }
		}catch(GetJobException e){
			LOGGER.error("Error while trying check status jobs submitted.", e);
		}catch(SQLException e){
			LOGGER.error("Error while trying update image state.", e);
		}

	}

    private boolean checkFinish(JobResponseDTO jobResponse){

        for(TaskResponseDTO task : jobResponse.getTasks()) {

            LOGGER.debug("State task: " + task.getState());

            if (task.getState().compareTo("FINISHED") != 0)
                return false;
        }

        return true;
    }

	private boolean checkJobFinish(JobResponseDTO jobResponse){

		for(TaskResponseDTO task : jobResponse.getTasks()){
			TaskSpecResponseDTO taskSpec = task.getTaskSpec();

			for(CommandResponseDTO command: taskSpec.getCommands()){

				String commandDesc = command.getCommand();
				String commandState = command.getState();
				Integer commandExitCode = command.getExitCode();

				LOGGER.info("Command: " + commandDesc);
				LOGGER.info("State:" + commandState);
				LOGGER.info("Exit code: " + commandExitCode);

				if(commandExitCode != 0)
					return false;
			}
		}

		return true;
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