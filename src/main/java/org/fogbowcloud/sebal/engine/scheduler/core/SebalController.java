package org.fogbowcloud.sebal.engine.scheduler.core;

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
import org.fogbowcloud.sebal.engine.scheduler.core.exception.SebalException;
import org.fogbowcloud.sebal.engine.scheduler.monitor.SebalTaskMonitor;
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;


public class SebalController extends BlowoutController {
	
	// Constants
	public static final Logger LOGGER = Logger.getLogger(SebalController.class);
	
	// Sebal Controller Variables
	private static String nfsServerIP;
	private static String nfsServerPort;
	private static Properties properties;
	private static ImageDataStore imageStore;
	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	
	public SebalController(Properties properties) throws SebalException, BlowoutException {
		super(properties);
		
		this.setProperties(properties);
		try {			
			if (!checkProperties(properties)) {
				throw new SebalException("Error on validate the file");
			} else if (getBlowoutVersion(properties).isEmpty()
					|| getBlowoutVersion(properties) == null) {
				throw new SebalException("Error while reading blowout version file");
			}
		} catch (Exception e) {
			throw new SebalException(
					"Error while initializing Sebal Controller.", e);
		}
	}
	
	@Override
	public void start(boolean removePreviousResouces) throws Exception {
		try {
			imageStore = new JDBCImageDataStore(getProperties());
			LOGGER.debug("Imagestore "
					+ SebalPropertiesConstants.IMAGE_DATASTORE_IP + ":"
					+ SebalPropertiesConstants.IMAGE_DATASTORE_IP);

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
			addSebalTasks(properties, sebalSpec, ImageState.QUEUED);
		} catch (Exception e) {
			LOGGER.error("Error while adding previous tasks", e);
		}
	}

	private void blowoutControllerStart(boolean removePreviousResouces)
			throws Exception {
		setStarted(true);

		setBlowoutPool(createBlowoutInstance());
		setInfraProvider(createInfraProviderInstance(removePreviousResouces));
		setTaskMonitor(new SebalTaskMonitor(getBlowoutPool(), imageStore));
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
					addSebalTasks(properties, sebalSpec, ImageState.DOWNLOADED);
				} catch (InterruptedException e) {
					LOGGER.error("Error while adding R tasks", e);
				}
			}

		}, 0, Integer.parseInt(properties.getProperty(SebalPropertiesConstants.SEBAL_EXECUTION_PERIOD)));
	}

	private static void resetImagesRunningToQueued() throws SQLException {
		List<ImageData> imagesRunning = imageStore.getIn(ImageState.RUNNING);
		for (ImageData imageData : imagesRunning) {
			imageData.setState(ImageState.QUEUED);
			imageStore.updateImage(imageData);
		}
	}
	
	private void addSebalTasks(final Properties properties,
			final Specification sebalSpec, ImageState imageState)
			throws InterruptedException {

		try {
			List<ImageData> imagesToProcess = imageStore.getIn(imageState,
					ImageDataStore.UNLIMITED);			
			for (ImageData imageData : imagesToProcess) {
				LOGGER.debug("The image " + imageData.getName()
						+ " is in the execution state "
						+ imageData.getState().getValue() + " (not finished).");

				LOGGER.debug("Adding " + imageState + " task for image "
						+ imageData.getName());

				Specification specWithFederation = generateModifiedSpec(imageData, sebalSpec);

				LOGGER.debug("specWithFederation " + specWithFederation.toString());

				if (ImageState.QUEUED.equals(imageState)
						|| ImageState.DOWNLOADED.equals(imageState)) {

					TaskImpl taskImpl = new TaskImpl(UUID.randomUUID()
							.toString(), specWithFederation);

					Map<String, String> nfsConfig = imageStore
							.getFederationNFSConfig(imageData
									.getFederationMember());

					Iterator it = nfsConfig.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();
						nfsServerIP = pair.getKey().toString();
						nfsServerPort = pair.getValue().toString();
						it.remove(); // avoids a ConcurrentModificationException
					}

					LOGGER.debug("Creating Sebal task " + taskImpl.getId());

					taskImpl = SebalTasks.createSebalTask(taskImpl, properties,
							imageData.getName(), specWithFederation,
							imageData.getFederationMember(), nfsServerIP,
							nfsServerPort, imageData.getSebalVersion(),
							imageData.getSebalTag());
					imageData.setState(ImageState.QUEUED);

					imageData.setBlowoutVersion(getBlowoutVersion(properties));
					getBlowoutPool().putTask(taskImpl);

					imageStore.updateImage(imageData);
					imageData.setUpdateTime(imageStore.getImage(
							imageData.getName()).getUpdateTime());
					try {
						imageStore
								.addStateStamp(imageData.getName(),
										imageData.getState(),
										imageData.getUpdateTime());
					} catch (SQLException e) {
						LOGGER.error(
								"Error while adding state "
										+ imageData.getState() + " timestamp "
										+ imageData.getUpdateTime() + " in DB",
								e);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}
	}
	
	private Specification generateModifiedSpec(ImageData imageData,
			Specification sebalSpec) {
		Specification specWithFederation = new Specification(
				sebalSpec.getImage(), sebalSpec.getUsername(),
				sebalSpec.getPublicKey(), sebalSpec.getPrivateKeyFilePath(),
				sebalSpec.getUserDataFile(), sebalSpec.getUserDataType());
		specWithFederation.putAllRequirements(sebalSpec.getAllRequirements());
		setFederationMemberIntoSpec(sebalSpec, specWithFederation,
				imageData.getFederationMember());
		
		return specWithFederation;
	}

	private static void setFederationMemberIntoSpec(Specification spec,
			Specification tempSpec, String federationMember) {
		String fogbowRequirements = spec
				.getRequirementValue("FogbowRequirements");
		LOGGER.debug("Setting federationmember " + federationMember
				+ " into FogbowRequirements");
		String requestType = spec.getRequirementValue("RequestType");
		String newRequirements = fogbowRequirements
				+ " && Glue2CloudComputeManagerID==\"" + federationMember
				+ "\"";
		tempSpec.addRequirement("FogbowRequirements", newRequirements);
		tempSpec.addRequirement("RequestType", requestType);
	}
	
	private static String getBlowoutVersion(Properties properties) {

		String blowoutDirPath = properties.getProperty(SebalPropertiesConstants.BLOWOUT_DIR_PATH);
		File blowoutDir = new File(blowoutDirPath);

		if (blowoutDir.exists() && blowoutDir.isDirectory()) {
			for (File file : blowoutDir.listFiles()) {
				if (file.getName().startsWith("blowout.version.")) {
					String[] blowoutVersionFileSplit = file.getName().split(
							"\\.");
					return blowoutVersionFileSplit[2];
				}
			}
		}

		return "";
	}
	
	private static Specification getSebalSpecFromFile(Properties properties) {
		String sebalSpecFile = properties
				.getProperty(SebalPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);
		List<Specification> specs = new ArrayList<Specification>();
		try {
			specs = Specification.getSpecificationsFromJSonFile(sebalSpecFile);
			if (specs != null && !specs.isEmpty()) {
				return specs.get(Constants.LIST_ARRAY_FIRST_ELEMENT);
			}
			return null;
		} catch (IOException e) {
			LOGGER.error("Error while getting spec from file " + sebalSpecFile,
					e);
			return null;
		}
	}
	
	protected static boolean checkProperties(Properties properties) {
		if (!properties.containsKey(SebalPropertiesConstants.IMAGE_DATASTORE_IP)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.IMAGE_DATASTORE_IP + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.IMAGE_DATASTORE_PORT)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.IMAGE_DATASTORE_PORT + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.INFRA_SPECS_BLOCK_CREATING)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.INFRA_SPECS_BLOCK_CREATING + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.INFRA_IS_STATIC)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.INFRA_IS_STATIC + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.SEBAL_EXECUTION_PERIOD)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.SEBAL_EXECUTION_PERIOD + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.SEBAL_EXPORT_PATH)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.SEBAL_EXPORT_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SebalPropertiesConstants.BLOWOUT_DIR_PATH)) {
			LOGGER.error("Required property " + SebalPropertiesConstants.BLOWOUT_DIR_PATH + " was not set");
			return false;
		}
		
		LOGGER.debug("All properties are set");
		return true;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		SebalController.properties = properties;
	}
}