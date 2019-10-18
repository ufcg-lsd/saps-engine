package org.fogbowcloud.saps.engine.core.scheduler;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.dto.*;
import org.fogbowcloud.saps.engine.core.exception.SapsException;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.DefaultArrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.JobSubmitted;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.core.task.Specification;
import org.fogbowcloud.saps.engine.core.task.TaskImpl;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;
import org.fogbowcloud.saps.engine.util.SapsPropertiesConstants;

public class Scheduler {

	// Constants
	public static final Logger LOGGER = Logger.getLogger(Scheduler.class);

	// Saps Controller Variables
	private final Properties properties;
	private final ImageDataStore imageStore;
	private final ScheduledExecutorService sapsExecutor;
	private final DefaultArrebol arrebol;

	// REMOVE IT
	public Scheduler() {
		this.imageStore = null;
		this.properties = null;
		this.sapsExecutor = null;
		this.arrebol = null;
	}
	// REMOVE IT

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
		this.arrebol = new DefaultArrebol(properties);
	}

	/**
	 * This function checks if the essential properties have been set.
	 * 
	 * @param properties saps properties to be check
	 * @return boolean representation, true (case all properties been set) or false (otherwise)
	 */
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
			LOGGER.error(
					"Required property " + SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR + " was not set");
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

	/**
	 * Get properties
	 * 
	 * @return properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Start Scheduler component
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		try {
			sapsExecutor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						schedule(arrebol.getCountSlotsInQueue());
					} catch (Exception | SubmitJobException e) {
						LOGGER.error("Error while adding tasks", e);
					}
				}

			}, 0, Integer.parseInt(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR)),
					TimeUnit.SECONDS);

			sapsExecutor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					checker();
				}
			}, 0, Integer.parseInt(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER)),
					TimeUnit.SECONDS);

		} catch (Exception e) {
			LOGGER.error("Error while starting Scheduler component", e);
		}
	}

	/**
	 * This function schedules up to count tasks.
	 * 
	 * @param count slots number in Arrebol queue
	 * @return number of scheduled tasks
	 */
	private int schedule(int count) {
		int remaining;

		remaining = schedule(count, ImageTaskState.READY);
		remaining = schedule(count, ImageTaskState.PREPROCESSED);
		remaining = schedule(remaining, ImageTaskState.DOWNLOADED);
		remaining = schedule(remaining, ImageTaskState.CREATED);

		return remaining;
	}

	/**
	 * This function schedules up to count tasks in specific state.
	 * 
	 * @param count slots number in Arrebol queue
	 * @param state tasks state for schedule
	 * @return number of scheduled tasks in specific state
	 */
	private int schedule(int count, ImageTaskState state) {
		// shortcut. it was solved in the previous schedule
		if (count <= 0)
			return count;

		List<ImageTask> tasks = new ArrayList<ImageTask>();

		try {
			tasks = imageStore.getIn(state, ImageDataStore.UNLIMITED);
		} catch (SQLException e) {
			LOGGER.error("Error while schedule tasks.", e);
		}

		Map<String, List<ImageTask>> tasksByUsers = mapUsers2Tasks(tasks);
		List<ImageTask> selectedTasks = select(count, tasksByUsers);

		LOGGER.info("Selected tasks: " + selectedTasks);

		ImageTaskState nextState = getNextState(state);

		int countFailedSubmit = 0;
		for (ImageTask task : selectedTasks) {
			if (checkPassiveState(task, nextState)) {
				if (updateStateInCatalog(task, nextState)) {
					nextState = getNextState(nextState);
				} else {
					continue;
				}
			}

			if (submitTaskToArrebol(task, nextState)) {
				updateStateInCatalog(task, nextState);
			} else {
				countFailedSubmit++;
			}
		}

		if (countFailedSubmit >= 1)
			return 0;

		return count - selectedTasks.size();
	}

	/**
	 * This function ensures that if the state parameter is READY, it updates in the
	 * catalog and goes to the next state of RUNNING.
	 * 
	 * @param task  task to be check
	 * @param state state to be check
	 * @return boolean representation, true (state is READY) or false (otherwise)
	 */
	private boolean checkPassiveState(ImageTask task, ImageTaskState state) {
		return state == ImageTaskState.READY;
	}

	/**
	 * This function get next state based in current state.
	 * 
	 * @param currentState current state
	 * @return next state
	 */
	private ImageTaskState getNextState(ImageTaskState currentState) {
		if (currentState == ImageTaskState.READY)
			return ImageTaskState.RUNNING;
		else if (currentState == ImageTaskState.PREPROCESSED)
			return ImageTaskState.READY;
		else if (currentState == ImageTaskState.DOWNLOADED)
			return ImageTaskState.PREPROCESSING;
		else
			return ImageTaskState.DOWNLOADING;
	}

	/**
	 * This function update task state in catalog component.
	 *
	 * @param task  task to be updated
	 * @param state new task state
	 * @return boolean representation reporting success (true) or failure (false) in
	 *         update state task in catalog
	 */
	private boolean updateStateInCatalog(ImageTask task, ImageTaskState state) {
		task.setState(state);

		try {
			task.setUpdateTime(imageStore.getTask(task.getTaskId()).getUpdateTime());
			imageStore.updateTaskState(task.getTaskId(), state);
			imageStore.addStateStamp(task.getTaskId(), task.getState(), task.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state " + task.getState() + " timestamp " + task.getUpdateTime()
					+ " in Catalogue", e);
			return false;
		}

		return true;
	}

	/***
	 * This function associate each task with a specific user by building an map.
	 * After that, it sorts each task list by task priority.
	 * 
	 * @param tasks list with tasks
	 * @return user map by tasks
	 */
	protected Map<String, List<ImageTask>> mapUsers2Tasks(List<ImageTask> tasks) {
		Map<String, List<ImageTask>> mapUsersToTasks = new TreeMap<String, List<ImageTask>>();

		for (ImageTask task : tasks) {
			String user = task.getUser();
			if (!mapUsersToTasks.containsKey(user))
				mapUsersToTasks.put(user, new ArrayList<ImageTask>());

			mapUsersToTasks.get(user).add(task);
		}

		for (String user : mapUsersToTasks.keySet()) {
			mapUsersToTasks.get(user).sort(new Comparator<ImageTask>() {
				@Override
				public int compare(ImageTask task01, ImageTask task02) {
					return task02.getPriority() - task01.getPriority();
				}
			});
		}

		return mapUsersToTasks;
	}

	// TODO Change implementation to receive parameter selection algorithm
	/**
	 * This function select tasks for schedule up to count.
	 * 
	 * @param count slots number in Arrebol queue
	 * @param tasks user map by tasks
	 * @return list of selected tasks
	 */
	protected List<ImageTask> select(int count, Map<String, List<ImageTask>> tasks) {
		List<ImageTask> selectedTasks = new LinkedList<ImageTask>();

		while (count > 0 && tasks.size() > 0) {
			for (String user : tasks.keySet()) {
				if (count > 0) {
					selectedTasks.add(tasks.get(user).remove(0));
					count--;

					if (tasks.get(user).size() == 0)
						tasks.remove(user);
				}
			}
		}

		return selectedTasks;
	}

	/**
	 * This function try submit task to Arrebol.
	 * 
	 * @param task task to be submitted
	 * @return boolean representation reporting success or failure in sending the
	 *         task to the Arrebol
	 * @throws Exception
	 */
	private boolean submitTaskToArrebol(ImageTask task, ImageTaskState state) {

		LOGGER.info("Trying submit task id " + task.getTaskId() + " in state " + task.getState().getValue()
				+ " to arrebol");

		String federationMember = task.getFederationMember();
		String repository = getRepository(state);
		ExecutionScriptTag imageDockerInfo = null;

		try {
			imageDockerInfo = ExecutionScriptTagUtil.getExecutionScritpTag(task.getAlgorithmExecutionTag(), repository);
		} catch (SapsException e) {
			LOGGER.error("Error while trying get tag and repository Docker.", e);
			return false;
		}

		Specification workerSpec = new Specification(imageDockerInfo.formatImageDocker(),
				new HashMap<String, String>());

		TaskImpl taskToJob = new TaskImpl(task.getTaskId(), workerSpec, UUID.randomUUID().toString());

		LOGGER.info("Creating saps task");
		SapsTask.createTask(taskToJob, task);

		SapsJob imageJob = new SapsJob(UUID.randomUUID().toString(), federationMember, task.getTaskId());
		imageJob.addTask(taskToJob);

		String jobId = null;
		try {
			jobId = arrebol.addJob(imageJob);
			LOGGER.debug("Result submited job: " + jobId);
		} catch (Exception | SubmitJobException e) {
			LOGGER.error("Error while trying to send request for Arrebol with new saps job.", e);
			return false;
		}

		arrebol.addJobInList(new JobSubmitted(jobId, task));
		LOGGER.info("Adding job in list");

		return true;
	}

	/**
	 * This function get key (repository representation in execution_script_tags
	 * file) based in state parameter.
	 * 
	 * @param state task state to be submitted
	 * @return key (repository representation)
	 */
	private String getRepository(ImageTaskState state) {
		if (state == ImageTaskState.RUNNING)
			return ExecutionScriptTagUtil.WORKER;
		else if (state == ImageTaskState.PREPROCESSING)
			return ExecutionScriptTagUtil.PRE_PROCESSING;
		else
			return ExecutionScriptTagUtil.INPUT_DOWNLOADER;
	}

	/**
	 * This function checks if each submitted job was finished. If exists finished
	 * jobs, for each job is updates state in Catalog and removes a job by list of
	 * submitted jobs to Arrebol.
	 * 
	 */
	private void checker() {

		ImageTask imageTask = null;
		List<JobSubmitted> submittedJobs = arrebol.returnAllJobsSubmitted();
		List<JobSubmitted> finishedJobs = new LinkedList<JobSubmitted>();

		LOGGER.debug("Checking jobs " + submittedJobs.size() + " submitted for arrebol service");

		try {
			for (JobSubmitted job : submittedJobs) {
				String jobId = job.getJobId();

				JobResponseDTO jobResponse = arrebol.checkStatusJob(jobId);

				imageTask = job.getImageTask();

				LOGGER.debug("Checking job " + jobId + " ...");

				boolean checkFinish = checkJobWasFinish(jobResponse);

				if (checkFinish) {

					boolean checkOK = checkJobFinishedWithSucess(jobResponse);

					if (checkOK) {
						LOGGER.debug("Job " + jobId + " finished");
						imageTask.setState(ImageTaskState.FINISHED);
						imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.FINISHED);
					} else {
						LOGGER.debug("Job " + jobId + " failed");
						imageTask.setState(ImageTaskState.FAILED);
						imageStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.FAILED);
					}

					finishedJobs.add(job);

					try {
						imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
					} catch (SQLException e) {
						LOGGER.error("Error while update time in task.", e);
					}

					try {
						imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
								imageTask.getUpdateTime());
					} catch (SQLException e) {
						LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
								+ imageTask.getUpdateTime() + " in Catalogue", e);
					}
				}
			}

			for (JobSubmitted jobFinished : finishedJobs) {
				LOGGER.info("Removing job " + jobFinished.getJobId() + " from the list");
				arrebol.removeJob(jobFinished);
			}
		} catch (GetJobException e) {
			LOGGER.error("Error while trying check status jobs submitted.", e);
		} catch (SQLException e) {
			LOGGER.error("Error while trying update image state.", e);
		}

	}

	/**
	 * This function checks job state was finished.
	 * 
	 * @param jobResponse job to be check
	 * @return boolean representation, true (is was finished) or false (otherwise)
	 */
	private boolean checkJobWasFinish(JobResponseDTO jobResponse) {

		for (TaskResponseDTO task : jobResponse.getTasks()) {
			LOGGER.debug("State task: " + task.getState());

			// TODO Verify values possible for job states
			if (task.getState().compareTo("FINISHED") != 0)
				return false;
		}

		return true;
	}

	/**
	 * This function checks a finished job was completed with success or failure.
	 * 
	 * @param jobResponse job to be check
	 * @return boolean represetation, true (sucess completed) or false (otherwise)
	 */
	private boolean checkJobFinishedWithSucess(JobResponseDTO jobResponse) {

		for (TaskResponseDTO task : jobResponse.getTasks()) {
			TaskSpecResponseDTO taskSpec = task.getTaskSpec();

			for (CommandResponseDTO command : taskSpec.getCommands()) {

				String commandDesc = command.getCommand();
				String commandState = command.getState();
				Integer commandExitCode = command.getExitCode();

				LOGGER.info("Command: " + commandDesc);
				LOGGER.info("State:" + commandState);
				LOGGER.info("Exit code: " + commandExitCode);

				if (commandExitCode != 0)
					return false;
			}
		}

		return true;
	}
}
