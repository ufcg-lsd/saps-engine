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
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.DefaultArrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.JobSubmitted;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetCountsSlotsException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol.ArrebolRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol.GetJobByIdRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol.GetJobByNameRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol.LenQueueRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol.SubmitJobRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.catalog.CatalogRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.catalog.GetProcessingTasksRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.catalog.GetTasksRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.catalog.UpdateTaskRetry;
import org.fogbowcloud.saps.engine.core.scheduler.retry.catalog.UpdateTimestampRetry;
import org.fogbowcloud.saps.engine.core.scheduler.selector.DefaultRoundRobin;
import org.fogbowcloud.saps.engine.core.scheduler.selector.Selector;
import org.fogbowcloud.saps.engine.core.task.TaskImpl;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;
import org.fogbowcloud.saps.engine.util.SapsPropertiesConstants;

public class Scheduler {

	// Constants
	public static final Logger LOGGER = Logger.getLogger(Scheduler.class);

	private static final int ARREBOL_DEFAULT_SLEEP_SECONDS = 5;
	private static final int CATALOG_DEFAULT_SLEEP_SECONDS = 5;
	private static final int ARREBOL_MAX_WAITING_JOBS = 20;

	// Saps Controller Variables
	private ScheduledExecutorService sapsExecutor;
	private Selector selector;

	private Properties properties;
	private ImageDataStore imageStore;
	private Arrebol arrebol;

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
		this.selector = new DefaultRoundRobin();
	}

	public Scheduler(Properties properties, ImageDataStore imageStore, ScheduledExecutorService sapsExecutor,
			Arrebol arrebol, Selector selector) throws SapsException, SQLException {
		if (!checkProperties(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");

		this.properties = properties;
		this.imageStore = imageStore;
		this.sapsExecutor = sapsExecutor;
		this.arrebol = arrebol;
		this.selector = selector;
	}

	/**
	 * This function gets image store
	 * 
	 * @return image store
	 */
	public ImageDataStore getImageStore() {
		return imageStore;
	}

	/**
	 * This function sets image store
	 * 
	 * @param imageStore new image store
	 */
	public void setImageStore(ImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	/**
	 * This function gets Arrebol
	 * 
	 * @return arrebol arrebol
	 */
	public Arrebol getArrebol() {
		return arrebol;
	}

	/**
	 * This function sets Arrebol
	 * 
	 * @param arrebol new Arrebol
	 */
	public void setArrebol(Arrebol arrebol) {
		this.arrebol = arrebol;
	}

	/**
	 * This function gets Scheduler properties
	 * 
	 * @return properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * This function sets Scheduler properties
	 * 
	 * @param properties new properties
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * This function checks if the essential properties have been set.
	 * 
	 * @param properties saps properties to be check
	 * @return boolean representation, true (case all properties been set) or false
	 *         (otherwise)
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
	 * Start Scheduler component
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		//recovery();

		try {
			sapsExecutor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					schedule();
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
	 * This function retrieves consistency between the information present in
	 * Catalog and Arrebol, and starts the list of submitted jobs.
	 */
	private void recovery() {
		List<ImageTask> tasksInProcessingState = retry(new GetProcessingTasksRetry(imageStore),
				CATALOG_DEFAULT_SLEEP_SECONDS, "gets tasks in processing state");
		List<ImageTask> tasksForPopulateSubmittedJobList = new LinkedList<ImageTask>();

		for (ImageTask task : tasksInProcessingState) {
			if (task.getArrebolJobId().equals(ImageTask.NONE_ARREBOL_JOB_ID)) {
				String jobName = task.getState().getValue() + "-" + task.getTaskId();
				List<JobResponseDTO> jobsWithEqualJobName = retry(new GetJobByNameRetry(arrebol, jobName),
						ARREBOL_DEFAULT_SLEEP_SECONDS, "gets job by name");
				if (jobsWithEqualJobName.size() == 0)
					rollBackTaskState(task);
				else if (jobsWithEqualJobName.size() == 1) { // TODO add check jobName ==
																// jobsWithEqualJobName.get(0).get...
					String arrebolJobId = jobsWithEqualJobName.get(0).getId();
					writeArrebolJobIdInCatalog(task, arrebolJobId,
							"updates task[" + task.getTaskId() + "] with Arrebol job ID [" + arrebolJobId + "]");
					tasksForPopulateSubmittedJobList.add(task);
				} else {
					// TODO ????
				}
			}
		}

		arrebol.populateJobList(tasksForPopulateSubmittedJobList);
	}

	/**
	 * This function apply rollback in task state and updates in Catalog
	 * 
	 * @param task task to be apply rollback
	 */
	private void rollBackTaskState(ImageTask task) {
		ImageTaskState previousState = getPreviousState(task.getState());
		task.setState(previousState);
		updateStateInCatalog(task, previousState,
				"updates task[" + task.getTaskId() + "] with previus state [" + previousState.getValue() + "]");
	}

	/**
	 * This function schedules up to tasks.
	 * 
	 */
	private void schedule() {
		List<ImageTask> selectedTasks = selectTasks();
		submitTasks(selectedTasks);
	}

	/**
	 * This function selects tasks following a strategy for submit in Arrebol.
	 * 
	 * @param count count of available slots in Arrebol queue
	 * @return selected tasks list
	 */
	protected List<ImageTask> selectTasks() {
		List<ImageTask> selectedTasks = new LinkedList<ImageTask>();
		ImageTaskState[] states = { ImageTaskState.READY, ImageTaskState.DOWNLOADED, ImageTaskState.CREATED };

		for (ImageTaskState state : states)
			selectedTasks.addAll(selectTasks(ARREBOL_MAX_WAITING_JOBS - getCountSlotsInArrebol("default"), state));

		return selectedTasks;
	}

	/**
	 * This function selects tasks in specific state following a strategy for submit
	 * in Arrebol.
	 * 
	 * @param count count of available slots in Arrebol queue
	 * @param state specific state for selection
	 * @return selected tasks list in specific state
	 */
	private List<ImageTask> selectTasks(int count, final ImageTaskState state) {
		LOGGER.info("Trying select up to " + count + " tasks in state " + state);
		
		List<ImageTask> tasks = getTasksInCatalog(state);

		Map<String, List<ImageTask>> tasksByUsers = mapUsers2Tasks(tasks);

		LOGGER.info("Tasks by users: " + tasksByUsers);

		List<ImageTask> selectedTasks = selector.select(count, tasksByUsers);

		LOGGER.info("Selected tasks using " + selector.version() + ": " + selectedTasks);
		return selectedTasks;
	}

	/**
	 * This function submits tasks for Arrebol and updates state and job IDs in BD.
	 * 
	 * @param selectedTasks selected task list for submit to Arrebol
	 */
	protected void submitTasks(List<ImageTask> selectedTasks) {
		for (ImageTask task : selectedTasks) {
			ImageTaskState nextState = getNextState(task.getState());

			updateStateInCatalog(task, nextState,
					"updates task[" + task.getTaskId() + "] state for " + nextState.getValue());
			String arrebolJobId = submitTaskToArrebol(task, nextState);
			writeArrebolJobIdInCatalog(task, arrebolJobId,
					"updates task[" + task.getTaskId() + "] with Arrebol job ID [" + arrebolJobId + "]");
		}
	}

	/**
	 * This function updates task with Arrebol job ID in catalog component.
	 *
	 * @param task         task to be updated
	 * @param arrebolJobId Arrebol job ID of task submitted
	 * @param message      information message
	 * @return boolean representation reporting success (true) or failure (false) in
	 *         update state task in catalog
	 */
	private void writeArrebolJobIdInCatalog(ImageTask task, String arrebolJobId, String message) {
		task.setArrebolJobId(arrebolJobId);
		retry(new UpdateTaskRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function updates task state in catalog component.
	 *
	 * @param task    task to be updated
	 * @param state   new task state
	 * @param message information message
	 * @return boolean representation reporting success (true) or failure (false) in
	 *         update state task in catalog
	 */
	protected boolean updateStateInCatalog(ImageTask task, ImageTaskState state, String message) {
		task.setState(state);
		return retry(new UpdateTaskRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS,
				"updates task[" + task.getTaskId() + "] state for " + state.getValue());
	}

	/**
	 * This function gets Arrebol capacity for add new jobs.
	 * 
	 * @return Arrebol capacity
	 */
	private int getCountSlotsInArrebol(String queueId) {
		return retry(new LenQueueRetry(arrebol, queueId), ARREBOL_DEFAULT_SLEEP_SECONDS,
				"gets Arrebol capacity len for add news jobs");
	}

	/**
	 * This function gets tasks in specific state in Catalog.
	 * 
	 * @param state specific state for get tasks
	 * @return tasks in specific state
	 */
	private List<ImageTask> getTasksInCatalog(ImageTaskState state) {
		return retry(new GetTasksRetry(imageStore, state, ImageDataStore.UNLIMITED), CATALOG_DEFAULT_SLEEP_SECONDS,
				"gets tasks with " + state.getValue() + " state");
	}

	/**
	 * This function updates task time stamp and insert new tuple in time stamp
	 * table.
	 * 
	 * @param task    task to be update
	 * @param message information message
	 */
	private void updateTimestampTask(ImageTask task, String message) {
		retry(new UpdateTimestampRetry(imageStore, task), CATALOG_DEFAULT_SLEEP_SECONDS, message);
	}

	/**
	 * This function tries countless times to successfully execute the passed
	 * function.
	 * 
	 * @param <T>            Return type
	 * @param function       Function passed for execute
	 * @param sleepInSeconds Time sleep in seconds (case fail)
	 * @param message        Information message about function passed
	 * @return Function return
	 */
	@SuppressWarnings("unchecked")
	private <T> T retry(ArrebolRetry<?> function, int sleepInSeconds, String message) {
		LOGGER.info(
				"[Retry Arrebol function] Trying " + message + " using " + sleepInSeconds + " seconds with time sleep");

		while (true) {
			try {
				return (T) function.run();
			} catch (Exception | SubmitJobException | GetJobException | GetCountsSlotsException e) {
				LOGGER.error(message);
				e.printStackTrace();
			}

			try {
				Thread.sleep(Long.valueOf(sleepInSeconds));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function tries countless times to successfully execute the passed
	 * function.
	 * 
	 * @param <T>            Return type
	 * @param function       Function passed for execute
	 * @param sleepInSeconds Time sleep in seconds (case fail)
	 * @param message        Information message about function passed
	 * @return Function return
	 */
	@SuppressWarnings("unchecked")
	private <T> T retry(CatalogRetry<?> function, int sleepInSeconds, String message) {
		LOGGER.info(
				"[Retry Catalog function] Trying " + message + " using " + sleepInSeconds + " seconds with time sleep");

		while (true) {
			try {
				return (T) function.run();
			} catch (SQLException e) {
				LOGGER.error("Failed while " + message);
				e.printStackTrace();
			}

			try {
				LOGGER.info("Sleeping for " + sleepInSeconds + " seconds");
				Thread.sleep(Long.valueOf(sleepInSeconds));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function get next state based in current state.
	 * 
	 * @param currentState current state
	 * @return next state
	 */
	private ImageTaskState getNextState(ImageTaskState currentState) {
		if (currentState == ImageTaskState.CREATED)
			return ImageTaskState.DOWNLOADING;
		if (currentState == ImageTaskState.DOWNLOADING)
			return ImageTaskState.DOWNLOADED;
		if (currentState == ImageTaskState.DOWNLOADED)
			return ImageTaskState.PREPROCESSING;
		if (currentState == ImageTaskState.PREPROCESSING)
			return ImageTaskState.READY;
		if (currentState == ImageTaskState.READY)
			return ImageTaskState.RUNNING;
		if (currentState == ImageTaskState.RUNNING)
			return ImageTaskState.FINISHED;

		return null;
	}

	/**
	 * This function get previous state based in current state.
	 * 
	 * @param currentState current state
	 * @return previous state
	 */
	private ImageTaskState getPreviousState(ImageTaskState currentState) {
		if (currentState == ImageTaskState.RUNNING)
			return ImageTaskState.READY;
		if (currentState == ImageTaskState.PREPROCESSING)
			return ImageTaskState.DOWNLOADED;
		if (currentState == ImageTaskState.DOWNLOADING)
			return ImageTaskState.CREATED;

		return null;
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
					int priorityCompare = task02.getPriority() - task01.getPriority();
					if (priorityCompare != 0)
						return priorityCompare;
					else
						return task02.getCreationTime().compareTo(task02.getCreationTime());
				}
			});
		}

		return mapUsersToTasks;
	}

	/**
	 * This function try submit task to Arrebol.
	 * 
	 * @param task task to be submitted
	 * @return Arrebol job id
	 * @throws Exception
	 */
	private String submitTaskToArrebol(ImageTask task, ImageTaskState state) {
		LOGGER.info("Trying submit task id [" + task.getTaskId() + "] in state " + task.getState().getValue()
				+ " to arrebol");

		String federationMember = task.getFederationMember();
		String repository = getRepository(state);
		ExecutionScriptTag imageDockerInfo = getExecutionScriptTag(task, repository);

		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put("image", imageDockerInfo.formatImageDocker());
		
		TaskImpl taskToJob = new TaskImpl(task.getTaskId(), requirements, UUID.randomUUID().toString());
		
		LOGGER.info("Creating SAPS task ...");
		SapsTask.createTask(taskToJob, task, repository);
		LOGGER.info("SAPS task: " + taskToJob.toJSON().toString());

		LOGGER.info("Creating SAPS job ...");
		SapsJob imageJob = new SapsJob(UUID.randomUUID().toString(), federationMember, task.getTaskId());
		imageJob.addTask(taskToJob);
		LOGGER.info("SAPS job: " + imageJob.toJSON().toString());

		String jobId = retry(new SubmitJobRetry(arrebol, imageJob), ARREBOL_DEFAULT_SLEEP_SECONDS, "add new job");
		LOGGER.debug("Result submited job: " + jobId);

		arrebol.addJobInList(new JobSubmitted(jobId, task));
		LOGGER.info("Adding job in list");

		return jobId;
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
			return ExecutionScriptTagUtil.PROCESSING;
		else if (state == ImageTaskState.PREPROCESSING)
			return ExecutionScriptTagUtil.PRE_PROCESSING;
		else
			return ExecutionScriptTagUtil.INPUT_DOWNLOADER;
	}

	/**
	 * This function gets script tag based in repository.
	 *
	 * @param task task to be submitted
	 * @param repository task repository
	 * @return task information (tag, repository, type and name)
	 */
	private ExecutionScriptTag getExecutionScriptTag(ImageTask task, String repository){
		try {
			String tag = null;
			if(repository == ExecutionScriptTagUtil.PROCESSING)
				tag = task.getProcessingTag();
			else if(repository == ExecutionScriptTagUtil.PRE_PROCESSING)
				tag = task.getPreprocessingTag();
			else
				tag = task.getInputdownloadingTag();

			return ExecutionScriptTagUtil.getExecutionScriptTag(tag, repository);
		} catch (SapsException e) {
			LOGGER.error("Error while trying get tag and repository Docker.", e);
			return null;
		}
	}

	/**
	 * This function checks if each submitted job was finished. If exists finished
	 * jobs, for each job is updates state in Catalog and removes a job by list of
	 * submitted jobs to Arrebol.
	 * 
	 */
	private void checker() {
		List<JobSubmitted> submittedJobs = arrebol.returnAllJobsSubmitted();
		List<JobSubmitted> finishedJobs = new LinkedList<JobSubmitted>();

		LOGGER.info("Checking " + submittedJobs.size() + " submitted jobs for Arrebol service");

		for (JobSubmitted job : submittedJobs) {
			String jobId = job.getJobId();
			ImageTask task = job.getImageTask();

			JobResponseDTO jobResponse = retry(new GetJobByIdRetry(arrebol, jobId), ARREBOL_DEFAULT_SLEEP_SECONDS,
					"gets job by ID [" + jobId + "]");
			LOGGER.debug("Job [" + jobId + "] information returned from Arrebol: " + jobResponse);
			if (!wasJobFound(jobResponse)) {
				LOGGER.info("Job [" + jobId + "] not found in Arrebol service, applying status rollback to task ["
						+ task.getTaskId() + "]");

				rollBackTaskState(task);
				finishedJobs.add(job);
				continue;
			}

			boolean checkFinish = checkJobWasFinish(jobResponse);
			if (checkFinish) {
				LOGGER.info("Job [" + jobId + "] has been finished");

				boolean checkOK = checkJobFinishedWithSucess(jobResponse);

				if (checkOK) {
					LOGGER.info("Job [" + jobId + "] has been finished with sucess");

					ImageTaskState nextState = getNextState(task.getState());
					updateStateInCatalog(task, nextState,
							"updates task[" + task.getTaskId() + "] with next state [" + nextState.getValue() + "]");
				} else {
					LOGGER.info("Job [" + jobId + "] has been finished with failure");

					updateStateInCatalog(task, ImageTaskState.FAILED,
							"updates task[" + task.getTaskId() + "] with failed state");
				}

				updateTimestampTask(task, "updates task [" + jobId + "] timestamp");

				finishedJobs.add(job);
			} else
				LOGGER.info("Job [" + jobId + "] has NOT been finished");
		}

		for (JobSubmitted jobFinished : finishedJobs) {
			LOGGER.info("Removing job [" + jobFinished.getJobId() + "] from the submitted job list");
			arrebol.removeJob(jobFinished);
		}

	}

	// TODO implement method
	/**
	 * This function checks if job was found in Arrebol service.
	 *
	 * @param jobResponse job response
	 * @return boolean representation, true (case job was found) and false
	 *         (otherwise)
	 */
	private boolean wasJobFound(JobResponseDTO jobResponse) {
		return true;
	}

	/**
	 * This function checks job state was finish.
	 * 
	 * @param jobResponse job to be check
	 * @return boolean representation, true (is was finished) or false (otherwise)
	 */
	private boolean checkJobWasFinish(JobResponseDTO jobResponse) {
		LOGGER.info("Checking if job [" + jobResponse.getId() + "] was finished");

		for (TaskResponseDTO task : jobResponse.getTasks()) {
			LOGGER.info("State task: " + task.getState());

			if (task.getState().compareTo(TaskResponseDTO.STATE_FAILED) != 0
					|| task.getState().compareTo(TaskResponseDTO.STATE_FINISHED) != 0)
				return false;
		}

		return true;
	}

	/**
	 * This function checks a finished job was completed with success or failure.
	 * 
	 * @param jobResponse job to be check
	 * @return boolean representation, true (success completed) or false (otherwise)
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

				if (commandExitCode != 0 || commandState != TaskResponseDTO.STATE_FINISHED)
					return false;
			}
		}

		return true;
	}
}
