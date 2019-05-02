package org.fogbowcloud.saps.engine.core.scheduler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.JDBCJobDataStore;
import org.fogbowcloud.saps.engine.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.arrebol.ArrebolRequestsHelper;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;
import org.fogbowcloud.saps.engine.core.task.Specification;
import org.fogbowcloud.saps.engine.core.task.Task;
import org.fogbowcloud.saps.engine.core.task.TaskImpl;
import org.fogbowcloud.saps.engine.exceptions.SubmitJobException;
import org.fogbowcloud.saps.engine.scheduler.core.exception.SapsException;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;

import com.google.gson.Gson;

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
	private static ScheduledFuture<?> sapsExecutionTimer;

	public Scheduler(Properties properties) throws SapsException, SQLException {
		this.properties = properties;
		this.sapsExecutor = Executors.newScheduledThreadPool(1);
		this.arrebol = new Arrebol(properties);
		
		try {
			LOGGER.debug("Imagestore " + SapsPropertiesConstants.IMAGE_DATASTORE_IP + ":"
					+ SapsPropertiesConstants.IMAGE_DATASTORE_IP);
			this.imageStore = new JDBCImageDataStore(getProperties());

			if (!checkProperties(properties))
				throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");
		} catch (Exception e) {
			throw new SapsException("Error while initializing Sebal Controller.", e);
		}
	}

	public void start() throws Exception {
		try {
			final Specification workerSpec = getWorkerSpecFromFile(getProperties());

			sapsExecutionTimer = sapsExecutor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						verifyTasksPreprocessed();
						addSapsJobs(properties, workerSpec);
						checkSapsJobs();
					} catch (Exception e) {
						LOGGER.error("Error while adding tasks", e);
					}
				}

			}, 0, Integer.parseInt(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD)),
					TimeUnit.MILLISECONDS);

		} catch (Exception e) {
			LOGGER.error("Error while starting SebalController", e);
		}
	}

	private void verifyTasksPreprocessed() {
		try {
			List<ImageTask> imageTasksInStatePreprocessed = imageStore.getIn(ImageTaskState.PREPROCESSED,
					ImageDataStore.UNLIMITED);
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

	private void addSapsJobs(final Properties properties, final Specification workerSpec)
			throws Exception {
		try {
			List<ImageTask> imageTasksToProcess = imageStore.getIn(ImageTaskState.READY, ImageDataStore.UNLIMITED);
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

				// Getting Worker docker repository and tag
				ExecutionScriptTag workerDockerInfo = ExecutionScriptTagUtil
						.getExecutionScritpTag(imageTask.getAlgorithmExecutionTag(), ExecutionScriptTagUtil.WORKER);
				
				TaskImpl imageTaskToJob = new TaskImpl(imageTask.getTaskId(), workerSpec,
						UUID.randomUUID().toString());
				
				LOGGER.info("Creating saps task");
				SapsTask.createSapsTask(imageTaskToJob, properties, federationMember, nfsServerIP, nfsServerPort, workerDockerInfo.getDockerRepository(), workerDockerInfo.getDockerTag());
				
				List<Task> teste = new LinkedList<Task>();
				teste.add(imageTaskToJob);
				
				SapsJob imageJob = new SapsJob(federationMember, teste, federationMember);
				try {
					String a = arrebol.addJob(imageJob);
					LOGGER.debug("Result submit job: " + a);
				}catch(SubmitJobException e) {
					LOGGER.error("Error while trying to send request for Arrebol with new saps job.", e);
				}
				
				// Create Job
				// Submit Job

				/*imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
				try {
					imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(), imageTask.getUpdateTime());
				} catch (SQLException e) {
					LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
							+ imageTask.getUpdateTime() + " in Catalogue", e);
				}*/
			}
		} catch (SQLException e) {
			LOGGER.error("Error while getting task.", e);
		}
	}

	private void checkSapsJobs() {
		try {
			List<ImageTask> imageTasksInStateRunning = imageStore.getIn(ImageTaskState.RUNNING,
					ImageDataStore.UNLIMITED);
			for (ImageTask imageTask : imageTasksInStateRunning) {

			}
		} catch (SQLException e) {
			LOGGER.error("Error while check saps jobs.", e);
		}
	}

	private static Specification getWorkerSpecFromFile(Properties properties) {
		String workerSpecFile = properties.getProperty(SapsPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);
		List<Specification> specs = new ArrayList<Specification>();

		try {
			specs = Specification.getSpecificationsFromJSonFile(workerSpecFile);
			if (specs != null && !specs.isEmpty()) {
				return specs.get(0);
			}
			return null;
		} catch (IOException e) {
			LOGGER.error("Error while getting spec from file " + workerSpecFile, e);
			return null;
		}
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
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXECUTION_PERIOD + " was not set");
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