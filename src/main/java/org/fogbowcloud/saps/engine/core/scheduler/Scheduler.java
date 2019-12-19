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
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsJob;
import org.fogbowcloud.saps.engine.core.model.SapsTask;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.ArrebolUtils;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.DefaultArrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.JobSubmitted;
import org.fogbowcloud.saps.engine.core.scheduler.selector.DefaultRoundRobin;
import org.fogbowcloud.saps.engine.core.scheduler.selector.Selector;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.utils.ExecutionScriptTagUtil;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.retry.CatalogUtils;

public class Scheduler {

	// Constants
	public static final Logger LOGGER = Logger.getLogger(Scheduler.class);

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
				throw new SapsException(
						"Error on validate the file. Missing properties for start Scheduler Component.");
		} catch (Exception e) {
			throw new SapsException("Error while initializing Scheduler component.", e);
		}

		this.properties = properties;
		this.sapsExecutor = Executors.newScheduledThreadPool(1);
		this.arrebol = new DefaultArrebol(properties);
		this.selector = new DefaultRoundRobin();
	}

	public Scheduler(Properties properties, ImageDataStore imageStore, ScheduledExecutorService sapsExecutor,
			Arrebol arrebol, Selector selector) throws SapsException, SQLException {
		if (!checkProperties(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Scheduler Component.");

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
	private static boolean checkProperties(Properties properties) {
		if (properties == null) {
			LOGGER.error("Properties arg must not be null.");
			return false;
		}
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
		recovery();

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
		List<SapsImage> tasksInProcessingState = getProcessingTasksInCatalog();
		List<SapsImage> tasksForPopulateSubmittedJobList = new LinkedList<SapsImage>();

		for (SapsImage task : tasksInProcessingState) {
			if (task.getArrebolJobId().equals(SapsImage.NONE_ARREBOL_JOB_ID)) {
				String jobName = task.getState().getValue() + "-" + task.getTaskId();
				List<JobResponseDTO> jobsWithEqualJobName = getJobByNameInArrebol(jobName,
						"gets job by name [" + jobName + "]");
				if (jobsWithEqualJobName.size() == 0)
					rollBackTaskState(task);
				else if (jobsWithEqualJobName.size() == 1) { // TODO add check jobName ==
																// jobsWithEqualJobName.get(0).get...
					String arrebolJobId = jobsWithEqualJobName.get(0).getId();
					updateStateInCatalog(task, task.getState(), SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
							arrebolJobId,
							"updates task [" + task.getTaskId() + "] with Arrebol job ID [" + arrebolJobId + "]");
					tasksForPopulateSubmittedJobList.add(task);
				} else {
					// TODO ????
				}
			} else {
				String arrebolJobId = task.getArrebolJobId();
				arrebol.addJobInList(new JobSubmitted(arrebolJobId, task));
			}
		}

		arrebol.populateJobList(tasksForPopulateSubmittedJobList);
	}

	/**
	 * This function apply rollback in task state and updates in Catalog
	 * 
	 * @param task task to be apply rollback
	 */
	private void rollBackTaskState(SapsImage task) {
		ImageTaskState previousState = getPreviousState(task.getState());
		updateStateInCatalog(task, previousState, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
				SapsImage.NONE_ARREBOL_JOB_ID,
				"updates task [" + task.getTaskId() + "] with previus state [" + previousState.getValue() + "]");
	}

	/**
	 * This function schedules up to tasks.
	 * 
	 */
	private void schedule() {
		List<SapsImage> selectedTasks = selectTasks();
		submitTasks(selectedTasks);
	}

	/**
	 * This function selects tasks following a strategy for submit in Arrebol.
	 * 
	 * @param count count of available slots in Arrebol queue
	 * @return selected tasks list
	 */
	protected List<SapsImage> selectTasks() {
		List<SapsImage> selectedTasks = new LinkedList<SapsImage>();
		ImageTaskState[] states = { ImageTaskState.READY, ImageTaskState.DOWNLOADED, ImageTaskState.CREATED };

		int countUpToTasks = getCountSlotsInArrebol("default");

		for (ImageTaskState state : states) {
			List<SapsImage> selectedTasksInCurrentState = selectTasks(countUpToTasks, state);
			selectedTasks.addAll(selectedTasksInCurrentState);
			countUpToTasks -= selectedTasksInCurrentState.size();
		}

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
	private List<SapsImage> selectTasks(int count, final ImageTaskState state) {
		List<SapsImage> selectedTasks = new LinkedList<SapsImage>();

		if (count <= 0) {
			LOGGER.info("There will be no selection of tasks in the " + state.getValue()
					+ " state because there is no capacity for new jobs in Arrebol");
			return selectedTasks;
		}

		LOGGER.info("Trying select up to " + count + " tasks in state " + state);

		List<SapsImage> tasks = getTasksInCatalog(state, ImageDataStore.UNLIMITED,
				"gets tasks with " + state.getValue() + " state");

		Map<String, List<SapsImage>> tasksByUsers = mapUsers2Tasks(tasks);

		LOGGER.info("Tasks by users: " + tasksByUsers);

		selectedTasks = selector.select(count, tasksByUsers);

		LOGGER.info("Selected tasks using " + selector.version() + ": " + selectedTasks);
		return selectedTasks;
	}

	/**
	 * This function submits tasks for Arrebol and updates state and job IDs in BD.
	 * 
	 * @param selectedTasks selected task list for submit to Arrebol
	 */
	protected void submitTasks(List<SapsImage> selectedTasks) {
		for (SapsImage task : selectedTasks) {
			ImageTaskState nextState = getNextState(task.getState());

			updateStateInCatalog(task, nextState, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
					SapsImage.NONE_ARREBOL_JOB_ID,
					"updates task [" + task.getTaskId() + "] state for " + nextState.getValue());
			String arrebolJobId = submitTaskToArrebol(task, nextState);
			updateStateInCatalog(task, task.getState(), SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA, arrebolJobId,
					"updates task [" + task.getTaskId() + "] with Arrebol job ID [" + arrebolJobId + "]");
			addTimestampTaskInCatalog(task, "updates task [" + task.getTaskId() + "] timestamp");
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
	protected Map<String, List<SapsImage>> mapUsers2Tasks(List<SapsImage> tasks) {
		Map<String, List<SapsImage>> mapUsersToTasks = new TreeMap<String, List<SapsImage>>();

		for (SapsImage task : tasks) {
			String user = task.getUser();
			if (!mapUsersToTasks.containsKey(user))
				mapUsersToTasks.put(user, new ArrayList<SapsImage>());

			mapUsersToTasks.get(user).add(task);
		}

		for (String user : mapUsersToTasks.keySet()) {
			mapUsersToTasks.get(user).sort(new Comparator<SapsImage>() {
				@Override
				public int compare(SapsImage task01, SapsImage task02) {
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
	private String submitTaskToArrebol(SapsImage task, ImageTaskState state) {
		LOGGER.info("Trying submit task id [" + task.getTaskId() + "] in state " + task.getState().getValue()
				+ " to arrebol");

		String repository = getRepository(state);
		ExecutionScriptTag imageDockerInfo = getExecutionScriptTag(task, repository);

		String formatImageWithDigest = getFormatImageWithDigest(imageDockerInfo, state, task);

		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put("image", formatImageWithDigest);

		List<String> commands = SapsTask.buildCommandList(task, repository);

		Map<String, String> metadata = new HashMap<String, String>();

		LOGGER.info("Creating SAPS task ...");
		SapsTask sapsTask = new SapsTask(task.getTaskId() + "#" + formatImageWithDigest, requirements, commands,
				metadata);
		LOGGER.info("SAPS task: " + sapsTask.toJSON().toString());

		LOGGER.info("Creating SAPS job ...");
		List<SapsTask> tasks = new LinkedList<SapsTask>();
		tasks.add(sapsTask);

		SapsJob imageJob = new SapsJob(task.getTaskId(), tasks);
		LOGGER.info("SAPS job: " + imageJob.toJSON().toString());

		String jobId = submitJobInArrebol(imageJob, "add new job");
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
	 * @param task       task to be submitted
	 * @param repository task repository
	 * @return task information (tag, repository, type and name)
	 */
	private ExecutionScriptTag getExecutionScriptTag(SapsImage task, String repository) {
		try {
			String tag = null;
			if (repository == ExecutionScriptTagUtil.PROCESSING)
				tag = task.getProcessingTag();
			else if (repository == ExecutionScriptTagUtil.PRE_PROCESSING)
				tag = task.getPreprocessingTag();
			else
				tag = task.getInputdownloadingTag();

			return ExecutionScriptTagUtil.getExecutionScriptTag(tag, repository);
		} catch (SapsException e) {
			LOGGER.error("Error while trying get tag and repository Docker.", e);
			return null;
		}
	}

	private String getFormatImageWithDigest(ExecutionScriptTag imageDockerInfo, ImageTaskState state, SapsImage task) {
		if (state == ImageTaskState.RUNNING)
			return imageDockerInfo.getDockerRepository() + "@" + task.getDigestProcessing();
		else if (state == ImageTaskState.PREPROCESSING)
			return imageDockerInfo.getDockerRepository() + "@" + task.getDigestPreprocessing();
		else
			return imageDockerInfo.getDockerRepository() + "@" + task.getDigestInputdownloading();
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
		LOGGER.info("Submmitteds jobs list: " + submittedJobs.toString());

		for (JobSubmitted job : submittedJobs) {
			String jobId = job.getJobId();
			SapsImage task = job.getImageTask();

			JobResponseDTO jobResponse = getJobByIdInArrebol(jobId, "gets job by ID [" + jobId + "]");
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
					LOGGER.info("Job [" + jobId + "] has been finished with success");

					ImageTaskState nextState = getNextState(task.getState());
					updateStateInCatalog(task, nextState, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
							SapsImage.NONE_ARREBOL_JOB_ID,
							"updates task [" + task.getTaskId() + "] with next state [" + nextState.getValue() + "]");
				} else {
					LOGGER.info("Job [" + jobId + "] has been finished with failure");

					updateStateInCatalog(task, ImageTaskState.FAILED, SapsImage.AVAILABLE,
							"error while execute " + task.getState().getValue() + " phase",
							SapsImage.NONE_ARREBOL_JOB_ID, "updates task [" + task.getTaskId() + "] with failed state");
				}

				addTimestampTaskInCatalog(task, "updates task [" + task.getTaskId() + "] timestamp");

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
		String jobId = jobResponse.getId();
		String jobState = jobResponse.getJobState().toUpperCase();

		LOGGER.info("Checking if job [" + jobId + "] was finished");
		LOGGER.info("State job [" + jobId + "]: " + jobState);

		if (jobState.compareTo(TaskResponseDTO.STATE_FAILED) != 0
				&& jobState.compareTo(TaskResponseDTO.STATE_FINISHED) != 0)
			return false;

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

				if (commandExitCode != 0 || !commandState.equals(TaskResponseDTO.STATE_FINISHED))
					return false;
			}
		}

		return true;
	}

	/**
	 * This function gets Arrebol capacity for add new jobs.
	 * 
	 * @param queueId queue identifier in Arrebol
	 * @return Arrebol queue capacity in queue with identifier
	 */
	private int getCountSlotsInArrebol(String queueId) {
		return ArrebolUtils.getCountSlots(arrebol, queueId);
	}

	/**
	 * This function gets job in Arrebol that matching with id.
	 * 
	 * @param jobId   job id to be used for matching
	 * @param message information message
	 * @return job response that matching with id
	 */
	private JobResponseDTO getJobByIdInArrebol(String jobId, String message) {
		return ArrebolUtils.getJobById(arrebol, jobId, message);
	}

	/**
	 * This function gets job list in Arrebol that matching with name.
	 * 
	 * @param jobName job label to be used for matching
	 * @param message information message
	 * @return job response list that matching with label
	 */
	private List<JobResponseDTO> getJobByNameInArrebol(String jobName, String message) {
		return ArrebolUtils.getJobByName(arrebol, jobName, message);
	}

	/**
	 * This function submit job in Arrebol service.
	 * 
	 * @param imageJob SAPS job to be submitted
	 * @param message  information message
	 * @return job id returned from Arrebol
	 */
	private String submitJobInArrebol(SapsJob imageJob, String message) {
		return ArrebolUtils.submitJob(arrebol, imageJob, message);
	}

	/**
	 * This function gets tasks in specific state in Catalog.
	 * 
	 * @param state   specific state for get tasks
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	private List<SapsImage> getTasksInCatalog(ImageTaskState state, int limit, String message) {
		return CatalogUtils.getTasks(imageStore, state, limit, message);
	}

	/**
	 * This function gets tasks in processing state in catalog component.
	 * 
	 * @return processing tasks list
	 */
	private List<SapsImage> getProcessingTasksInCatalog() {
		return CatalogUtils.getProcessingTasks(imageStore, "gets tasks in processing state");
	}

	/**
	 * This function updates task state in catalog component.
	 *
	 * @param task         task to be updated
	 * @param state        new task state
	 * @param status       new task status
	 * @param error        new error message
	 * @param arrebolJobId new Arrebol job id
	 * @param message      information message
	 * @return boolean representation reporting success (true) or failure (false) in
	 *         update state task in catalog
	 */
	private boolean updateStateInCatalog(SapsImage task, ImageTaskState state, String status, String error,
			String arrebolJobId, String message) {
		task.setState(state);
		task.setStatus(status);
		task.setError(error);
		task.setArrebolJobId(arrebolJobId);
		return CatalogUtils.updateState(imageStore, task,
				"updates task [" + task.getTaskId() + "] state for " + state.getValue());
	}

	/**
	 * This function add new tuple in time stamp table and updates task time stamp.
	 * 
	 * @param task    task to be update
	 * @param message information message
	 */
	private void addTimestampTaskInCatalog(SapsImage task, String message) {
		CatalogUtils.addTimestampTask(imageStore, task, message);
	}

}
