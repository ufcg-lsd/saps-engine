package org.fogbowcloud.saps.engine.scheduler.core;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.fogbowcloud.blowout.infrastructure.monitor.ResourceMonitor;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;
import org.fogbowcloud.saps.engine.scheduler.core.exception.SapsException;
import org.fogbowcloud.saps.engine.scheduler.monitor.SapsTaskMonitor;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;

public class SapsController extends BlowoutController {

	// Constants
	public static final Logger LOGGER = Logger.getLogger(SapsController.class);

	// Sebal Controller Variables
	private static String nfsServerIP;
	private static String nfsServerPort;
	private static Properties properties;
	private static ImageDataStore imageStore;
	private static ManagerTimer sapsExecutionTimer = new ManagerTimer(
			Executors.newScheduledThreadPool(1));

	public SapsController(Properties properties) throws SapsException, BlowoutException {
		super(properties);

		this.setProperties(properties);
		try {
			if (!checkProperties(properties)) {
				throw new SapsException("Error on validate the file");
			} else if (getBlowoutVersion(properties) == null || 
				getBlowoutVersion(properties).isEmpty()) {
				throw new SapsException("Error while reading blowout version file");
			}
		} catch (Exception e) {
			throw new SapsException("Error while initializing Sebal Controller.", e);
		}
	}

	@Override
	public void start(boolean removePreviousResouces) throws Exception {
		try {
			imageStore = new JDBCImageDataStore(getProperties());
			LOGGER.debug("Imagestore " + SapsPropertiesConstants.IMAGE_DATASTORE_IP + ":"
					+ SapsPropertiesConstants.IMAGE_DATASTORE_IP);

			final Specification workerSpec = getWorkerSpecFromFile(getProperties());

			blowoutControllerStart(removePreviousResouces);
			schedulePreviousTasks(workerSpec);
			scheduleTasksPeriodically(workerSpec);
		} catch (Exception e) {
			LOGGER.error("Error while starting SebalController", e);
		}
	}

	private void schedulePreviousTasks(final Specification workerSpec) {
		// In case of the process has been stopped before finishing the images
		// running
		// in the next restart all images in running state will be reseted to
		// queued state
		try {
			resetImagesRunningToQueued();
			addSapsTasks(properties, workerSpec, ImageTaskState.READY);
		} catch (Exception e) {
			LOGGER.error("Error while adding previous tasks", e);
		}
	}

	private void blowoutControllerStart(boolean removePreviousResouces) throws Exception {
		setStarted(true);

		setBlowoutPool(createBlowoutInstance());
		setInfraProvider(createInfraProviderInstance(removePreviousResouces));
		setTaskMonitor(new SapsTaskMonitor(getBlowoutPool(), imageStore));
		getTaskMonitor().start();

		setResourceMonitor(
				new ResourceMonitor(getInfraProvider(), getBlowoutPool(), getProperties()));
		getResourceMonitor().start();

		setSchedulerInterface(createSchedulerInstance(getTaskMonitor()));
		setInfraManager(createInfraManagerInstance(getInfraProvider(), getResourceMonitor()));

		getBlowoutPool().start(getInfraManager(), getSchedulerInterface());
	}

	private void scheduleTasksPeriodically(final Specification workerSpec) {
		sapsExecutionTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					addSapsTasks(properties, workerSpec, ImageTaskState.PREPROCESSED);
				} catch (Exception e) {
					LOGGER.error("Error while adding tasks", e);
				}
			}

		}, 0, Integer
				.parseInt(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD)));
	}

	private static void resetImagesRunningToQueued() throws SQLException {
		List<ImageTask> imageTasksRunning = imageStore.getIn(ImageTaskState.RUNNING);
		for (ImageTask imageTask : imageTasksRunning) {
			imageTask.setState(ImageTaskState.READY);
			imageStore.updateImageTask(imageTask);
		}
	}

	private void addSapsTasks(final Properties properties, final Specification workerSpec,
			ImageTaskState imageTaskState) throws InterruptedException, SapsException {
		try {
			List<ImageTask> imageTasksToProcess = imageStore.getIn(imageTaskState,
					ImageDataStore.UNLIMITED);
			for (ImageTask imageTask : imageTasksToProcess) {
				LOGGER.debug("Image task " + imageTask.getTaskId() + " is in the execution state "
						+ imageTask.getState().getValue() + " (not finished).");
				LOGGER.debug(
						"Adding " + imageTaskState + " task for task " + imageTask.getTaskId());

				Specification specWithFederation = generateModifiedSpec(imageTask, workerSpec);
				LOGGER.debug("specWithFederation " + specWithFederation.toString());

				if (ImageTaskState.READY.equals(imageTaskState)
						|| ImageTaskState.PREPROCESSED.equals(imageTaskState)) {
					
					TaskImpl taskImpl = new TaskImpl(imageTask.getTaskId(), specWithFederation,
							UUID.randomUUID().toString());
					
					Map<String, String> nfsConfig = imageStore
							.getFederationNFSConfig(imageTask.getFederationMember());

					@SuppressWarnings("rawtypes")
					Iterator it = nfsConfig.entrySet().iterator();
					while (it.hasNext()) {
						@SuppressWarnings("rawtypes")
						Map.Entry pair = (Map.Entry) it.next();
						nfsServerIP = pair.getKey().toString();
						nfsServerPort = pair.getValue().toString();
						it.remove(); // avoids a ConcurrentModificationException
					}

					// Getting Worker docker repository and tag
					ExecutionScriptTag workerDockerInfo = ExecutionScriptTagUtil
							.getExecutionScritpTag(imageTask.getAlgorithmExecutionTag(),
									ExecutionScriptTagUtil.WORKER);

					LOGGER.debug("Creating Saps task " + taskImpl.getId() + " for Blowout");
					taskImpl = SapsTask.createSapsTask(taskImpl, properties, specWithFederation,
							imageTask.getFederationMember(), nfsServerIP, nfsServerPort,
							workerDockerInfo.getDockerRepository(),
							workerDockerInfo.getDockerTag());

					imageTask.setState(ImageTaskState.READY);
					imageTask.setBlowoutVersion(getBlowoutVersion(properties));
					addTask(taskImpl);

					imageStore.updateImageTask(imageTask);
					imageTask.setUpdateTime(
							imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
					try {
						imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
								imageTask.getUpdateTime());
					} catch (SQLException e) {
						LOGGER.error("Error while adding state " + imageTask.getState()
								+ " timestamp " + imageTask.getUpdateTime() + " in Catalogue", e);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while getting task.", e);
		}
	}


	private void addTask(TaskImpl taskImpl) throws SapsException {
		if (!super.started) {
			throw new SapsException(
					"Error while adding new task. BlowoutController not started yet.");
		}
		getBlowoutPool().putTask(taskImpl);
	}

	private Specification generateModifiedSpec(ImageTask imageTask, Specification workerSpec) {
		Specification specWithFederation = new Specification(workerSpec.getImage(),
				workerSpec.getUsername(), workerSpec.getPublicKey(),
				workerSpec.getPrivateKeyFilePath(), workerSpec.getUserDataFile(),
				workerSpec.getUserDataType());
		specWithFederation.putAllRequirements(workerSpec.getAllRequirements());
		setFederationMemberIntoSpec(workerSpec, specWithFederation,
				imageTask.getFederationMember());

		return specWithFederation;
	}

	private static void setFederationMemberIntoSpec(Specification spec, Specification tempSpec,
			String federationMember) {
		String fogbowRequirements = spec
				.getRequirementValue(SapsPropertiesConstants.SPEC_FOGBOW_REQUIREMENTS);
		LOGGER.debug("Setting federationmember " + federationMember + " into FogbowRequirements");
		String requestType = spec.getRequirementValue(SapsPropertiesConstants.SPEC_REQUEST_TYPE);
		String newRequirements = fogbowRequirements + " && "
				+ SapsPropertiesConstants.SPEC_GLUE2_CLOUD_COMPUTE_MANAGER_ID + "==\""
				+ federationMember + "\"";
		tempSpec.addRequirement(SapsPropertiesConstants.SPEC_FOGBOW_REQUIREMENTS, newRequirements);
		tempSpec.addRequirement(SapsPropertiesConstants.SPEC_REQUEST_TYPE, requestType);
	}

	private static String getBlowoutVersion(Properties properties) {
		String blowoutDirPath = properties.getProperty(SapsPropertiesConstants.BLOWOUT_DIR_PATH);
		File blowoutDir = new File(blowoutDirPath);

		if (blowoutDir.exists() && blowoutDir.isDirectory()) {
			for (File file : blowoutDir.listFiles()) {
				if (file.getName().startsWith(SapsPropertiesConstants.BLOWOUT_VERSION_PREFIX)) {
					String[] blowoutVersionFileSplit = file.getName().split("\\.");
					return blowoutVersionFileSplit[2];
				}
			}
		}

		return null;
	}

	private static Specification getWorkerSpecFromFile(Properties properties) {
		String workerSpecFile = properties
				.getProperty(SapsPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);
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
			LOGGER.error("Required property " + SapsPropertiesConstants.IMAGE_DATASTORE_IP
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.IMAGE_DATASTORE_PORT)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.IMAGE_DATASTORE_PORT
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_SPECS_BLOCK_CREATING)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.INFRA_SPECS_BLOCK_CREATING
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH)) {
			LOGGER.error("Required property "
					+ SapsPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.INFRA_PROVIDER_CLASS_NAME
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_IS_STATIC)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.INFRA_IS_STATIC
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXECUTION_PERIOD
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SAPS_EXPORT_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SAPS_EXPORT_PATH
					+ " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.BLOWOUT_DIR_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.BLOWOUT_DIR_PATH
					+ " was not set");
			return false;
		}

		LOGGER.debug("All properties are set");
		return true;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		SapsController.properties = properties;
	}
}