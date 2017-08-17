package org.fogbowcloud.saps.engine.scheduler.core;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.util.Constants;
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


public class SapsController extends BlowoutController {

	// Constants
	public static final Logger LOGGER = Logger.getLogger(SapsController.class);
	
	// Sebal Controller Variables
	private static String nfsServerIP;
	private static String nfsServerPort;
	private static Properties properties;
	private static ImageDataStore imageStore;
	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	
	public SapsController(Properties properties) throws SapsException, BlowoutException {
		super(properties);
		
		this.setProperties(properties);
		try {			
			if (!checkProperties(properties)) {
				throw new SapsException("Error on validate the file");
			} else if (getBlowoutVersion(properties).isEmpty()
					|| getBlowoutVersion(properties) == null) {
				throw new SapsException("Error while reading blowout version file");
			}
		} catch (Exception e) {
			throw new SapsException(
					"Error while initializing Sebal Controller.", e);
		}
	}
	
	@Override
	public void start(boolean removePreviousResouces) throws Exception {
		try {
			imageStore = new JDBCImageDataStore(getProperties());
			LOGGER.debug("Imagestore " + SapsPropertiesConstants.IMAGE_DATASTORE_IP + ":"
					+ SapsPropertiesConstants.IMAGE_DATASTORE_IP);

			final Specification sebalSpec = getSebalSpecFromFile(getProperties());

			blowoutControllerStart(removePreviousResouces);
			schedulePreviousTasks(sebalSpec);
			scheduleTasksPeriodically(sebalSpec);
		} catch (Exception e) {
			LOGGER.error("Error while starting SebalController", e);
		}
	}

	private void schedulePreviousTasks(final Specification sebalSpec) {
		// In case of the process has been stopped before finishing the images running 
		// in the next restart all images in running state will be reseted to queued state
		try {
			resetImagesRunningToQueued();
			addSebalTasks(properties, sebalSpec, ImageTaskState.READY);
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
		
		setResourceMonitor(new ResourceMonitor(getInfraProvider(), getBlowoutPool(), getProperties()));
		getResourceMonitor().start();

		setSchedulerInterface(createSchedulerInstance(getTaskMonitor()));
		setInfraManager(createInfraManagerInstance());

		getBlowoutPool().start(getInfraManager(), getSchedulerInterface());
	}
	
	private void scheduleTasksPeriodically(final Specification sebalSpec) {
		sebalExecutionTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					addSebalTasks(properties, sebalSpec, ImageTaskState.DOWNLOADED);
				} catch (Exception e) {
					LOGGER.error("Error while adding R tasks", e);
				}
			}

		}, 0, Integer.parseInt(properties.getProperty(SapsPropertiesConstants.SEBAL_EXECUTION_PERIOD)));
	}

	private static void resetImagesRunningToQueued() throws SQLException {
		List<ImageTask> imagesRunning = imageStore.getIn(ImageTaskState.RUNNING);
		for (ImageTask imageData : imagesRunning) {
			imageData.setState(ImageTaskState.READY);
			imageStore.updateImageTask(imageData);
		}
	}
	
	private void addSebalTasks(final Properties properties, final Specification sebalSpec,
			ImageTaskState imageState) throws InterruptedException, SapsException {

		try {
			List<ImageTask> imagesToProcess = imageStore.getIn(imageState,
					ImageDataStore.UNLIMITED);
			for (ImageTask imageData : imagesToProcess) {
				LOGGER.debug("The image " + imageData.getName() + " is in the execution state "
						+ imageData.getState().getValue() + " (not finished).");
				LOGGER.debug("Adding " + imageState + " task for image " + imageData.getName());
				
				Specification specWithFederation = generateModifiedSpec(imageData, sebalSpec);
				LOGGER.debug("specWithFederation " + specWithFederation.toString());
				
				if (ImageTaskState.READY.equals(imageState)
						|| ImageTaskState.DOWNLOADED.equals(imageState)) {
					TaskImpl taskImpl = new TaskImpl(UUID.randomUUID().toString(),
							specWithFederation);
					Map<String, String> nfsConfig = imageStore.getFederationNFSConfig(imageData
							.getFederationMember());

					Iterator it = nfsConfig.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();
						nfsServerIP = pair.getKey().toString();
						nfsServerPort = pair.getValue().toString();
						it.remove(); // avoids a ConcurrentModificationException
					}

					LOGGER.debug("Creating Sebal task " + taskImpl.getId());

					taskImpl = SapsTask.createSebalTask(taskImpl, properties,
							imageData.getName(), imageData.getCollectionTierName(),
							specWithFederation, imageData.getFederationMember(), nfsServerIP,
							nfsServerPort, imageData.getSebalVersion(), imageData.getSebalTag());
					
					imageData.setState(ImageTaskState.READY);
					imageData.setBlowoutVersion(getBlowoutVersion(properties));					
					addTask(taskImpl);

					imageStore.updateImageTask(imageData);
					imageData.setUpdateTime(imageStore.getTask(imageData.getName())
							.getUpdateTime());
					try {
						imageStore.addStateStamp(imageData.getName(), imageData.getState(),
								imageData.getUpdateTime());
					} catch (SQLException e) {
						LOGGER.error("Error while adding state " + imageData.getState()
								+ " timestamp " + imageData.getUpdateTime() + " in DB", e);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}
	}

	private void addTask(TaskImpl taskImpl) throws SapsException {
		if (!started) {
			throw new SapsException("Error while adding new task. BlowoutController not started yet.");
		}
		getBlowoutPool().putTask(taskImpl);
	}
	
	private Specification generateModifiedSpec(ImageTask imageData, Specification sebalSpec) {
		Specification specWithFederation = new Specification(sebalSpec.getImage(),
				sebalSpec.getUsername(), sebalSpec.getPublicKey(),
				sebalSpec.getPrivateKeyFilePath(), sebalSpec.getUserDataFile(),
				sebalSpec.getUserDataType());
		specWithFederation.putAllRequirements(sebalSpec.getAllRequirements());
		setFederationMemberIntoSpec(sebalSpec, specWithFederation, imageData.getFederationMember());
		
		return specWithFederation;
	}

	private static void setFederationMemberIntoSpec(Specification spec, Specification tempSpec,
			String federationMember) {
		String fogbowRequirements = spec.getRequirementValue(SapsPropertiesConstants.SPEC_FOGBOW_REQUIREMENTS);
		LOGGER.debug("Setting federationmember " + federationMember + " into FogbowRequirements");
		String requestType = spec.getRequirementValue(SapsPropertiesConstants.SPEC_REQUEST_TYPE);
		String newRequirements = fogbowRequirements + " && " + SapsPropertiesConstants.SPEC_GLUE2_CLOUD_COMPUTE_MANAGER_ID
				+ "==\"" + federationMember + "\"";
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
	
	private static Specification getSebalSpecFromFile(Properties properties) {
		String sebalSpecFile = properties.getProperty(SapsPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);
		List<Specification> specs = new ArrayList<Specification>();
		
		try {
			specs = Specification.getSpecificationsFromJSonFile(sebalSpecFile);
			if (specs != null && !specs.isEmpty()) {
				return specs.get(Constants.LIST_ARRAY_FIRST_ELEMENT);
			}
			return null;
		} catch (IOException e) {
			LOGGER.error("Error while getting spec from file " + sebalSpecFile, e);
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
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.INFRA_IS_STATIC)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.INFRA_IS_STATIC + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SEBAL_EXECUTION_PERIOD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SEBAL_EXECUTION_PERIOD + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SEBAL_EXPORT_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SEBAL_EXPORT_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.BLOWOUT_DIR_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.BLOWOUT_DIR_PATH + " was not set");
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