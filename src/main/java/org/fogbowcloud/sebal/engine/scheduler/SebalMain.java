package org.fogbowcloud.sebal.engine.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.blowout.scheduler.core.ManagerTimer;
import org.fogbowcloud.blowout.scheduler.core.Scheduler;
import org.fogbowcloud.blowout.scheduler.core.model.Job;
import org.fogbowcloud.blowout.scheduler.core.model.Resource;
import org.fogbowcloud.blowout.scheduler.core.model.Specification;
import org.fogbowcloud.blowout.scheduler.core.model.TaskImpl;
import org.fogbowcloud.blowout.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.blowout.scheduler.core.util.Constants;
import org.fogbowcloud.blowout.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.blowout.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.sebal.engine.scheduler.core.model.SebalJob;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;

public class SebalMain {

	private static final String BLOWOUT_DIR_PATH = "blowout_dir_path";
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(
			Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(
			Executors.newScheduledThreadPool(1));
	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(
			Executors.newScheduledThreadPool(1));

	// private static Map<String, ImageData> pendingImageExecution = new
	// ConcurrentHashMap<String, ImageData>();
	private static ImageDataStore imageStore;
	private static String nfsServerIP;
	private static String nfsServerPort;
	private static InfrastructureManager infraManager;
	// FIXME: change this later
	private static int maxAllowedResources = 5;

	private static final Logger LOGGER = Logger.getLogger(SebalMain.class);

	public static void main(String[] args) throws Exception {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		String imageStoreIP = args[1];
		String imageStorePort = args[2];

		LOGGER.debug("Imagestore " + imageStoreIP + ":" + imageStorePort);

		imageStore = new JDBCImageDataStore(properties);

		final Job job = new SebalJob(imageStore);

		boolean blockWhileInitializing = new Boolean(
				properties
						.getProperty(AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING))
				.booleanValue();

		boolean isElastic = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC))
				.booleanValue();
		List<Specification> initialSpecs = getInitialSpecs(properties);

		InfrastructureProvider infraProvider = createInfraProviderInstance(properties);

		LOGGER.info("Calling infrastructure manager");
		infraManager = new InfrastructureManager(initialSpecs, isElastic,
				infraProvider, properties);
		infraManager.start(blockWhileInitializing, true);

		Scheduler scheduler = new Scheduler(infraManager, job);
		ExecutionMonitor execMonitor = new SebalExecutionMonitor(scheduler, null, imageStore);

		final Specification sebalSpec = getSebalSpecFromFile(properties);
		
		// In case of the process has been stopped before finishing the images running 
		// in the next restart all images in running state will be reseted to queued state
		resetImagesRunningToQueued();

		addRTasks(properties, job, sebalSpec, ImageState.QUEUED, ImageDataStore.UNLIMITED);

		executionMonitorTimer.scheduleAtFixedRate(execMonitor, 0,
				Integer.parseInt(properties.getProperty("execution_monitor_period")));

		schedulerTimer.scheduleAtFixedRate(scheduler, 0,
				Integer.parseInt(properties.getProperty("scheduler_period")));
		sebalExecutionTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					addRTasks(properties, job, sebalSpec, ImageState.DOWNLOADED, 1);
				} catch (InterruptedException e) {
					LOGGER.error("Error while adding R tasks", e);
				}
			}
		}, 0, Integer.parseInt(properties.getProperty("sebal_execution_period")));

		LOGGER.info("Scheduler working");
	}

	private static void resetImagesRunningToQueued() throws SQLException {
		List<ImageData> imagesRunning = imageStore.getIn(ImageState.RUNNING);
		for (ImageData imageData : imagesRunning) {
			imageData.setState(ImageState.QUEUED);
			imageStore.updateImage(imageData);
		}
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

	private static Map<String, Collection<Resource>> allocationMap() {
		List<Resource> allResources = infraManager.getAllResources();

		Map<String, Collection<Resource>> allocMap = new HashMap<String, Collection<Resource>>();
		for (Resource resource : allResources) {
			String location = resource
					.getMetadataValue(Resource.METADATA_LOCATION);
			LOGGER.debug("Resource location " + location);
			if (!allocMap.containsKey(location)) {
				LOGGER.debug("Adding " + location + " to location map");
				allocMap.put(location, new LinkedList<Resource>());
			}
			LOGGER.debug("Associating resource " + resource.getId()
					+ " to location " + location);
			allocMap.get(location).add(resource);
		}
		return allocMap;
	}

	private static boolean isQuotaAvailable(String federationMemberId,
			Map<String, Collection<Resource>> allocationMap,
			int maxAllowedResources) {
		LOGGER.debug("maxAllowedResources per location is "
				+ maxAllowedResources);
		if (allocationMap.containsKey(federationMemberId)) {
			int numAllocationPerFederationMember = allocationMap.get(
					federationMemberId).size();
			LOGGER.debug(numAllocationPerFederationMember
					+ " allocated resources to " + federationMemberId);
			return numAllocationPerFederationMember < maxAllowedResources;
		}
		
		return true;
	}

	private static void addRTasks(final Properties properties, final Job job,
			final Specification sebalSpec, ImageState imageState, int limit)
			throws InterruptedException {

		try {
			List<ImageData> imagesToExecute = imageStore.getIn(imageState,
					limit);

			for (ImageData imageData : imagesToExecute) {
				LOGGER.debug("The image " + imageData.getName()
						+ " is in the execution state "
						+ imageData.getState().getValue() + " (not finished).");

				LOGGER.debug("Adding " + imageState + " task for image "
						+ imageData.getName());

				Specification tempSpec = new Specification(
						sebalSpec.getImage(), sebalSpec.getUsername(),
						sebalSpec.getPublicKey(),
						sebalSpec.getPrivateKeyFilePath(),
						sebalSpec.getUserDataFile(),
						sebalSpec.getUserDataType());
				tempSpec.putAllRequirements(sebalSpec.getAllRequirements());
				setFederationMemberIntoSpec(sebalSpec, tempSpec,
						imageData.getFederationMember());

				LOGGER.debug("tempSpec " + tempSpec.toString());

				Map<String, Collection<Resource>> allocationMap = allocationMap();
				LOGGER.info("Checking resource quota");
				if (isQuotaAvailable(imageData.getFederationMember(),
						allocationMap, maxAllowedResources)) {

					if (ImageState.QUEUED.equals(imageState)
							|| ImageState.DOWNLOADED.equals(imageState)) {

						TaskImpl taskImpl = new TaskImpl(UUID.randomUUID()
								.toString(), tempSpec);
						
						Map<String, String> nfsConfig = imageStore
								.getFederationNFSConfig(imageData
										.getFederationMember());
						
						Iterator it = nfsConfig.entrySet().iterator();
						while (it.hasNext()) {
					        Map.Entry pair = (Map.Entry)it.next();
					        nfsServerIP = pair.getKey().toString();
					        nfsServerPort = pair.getValue().toString();
					        it.remove(); // avoids a ConcurrentModificationException
					    }

						LOGGER.debug("Creating R task " + taskImpl.getId());

						taskImpl = SebalTasks.createRTask(taskImpl, properties,
								imageData.getName(), tempSpec,
								imageData.getFederationMember(), nfsServerIP,
								nfsServerPort, imageData.getSebalVersion(),
								imageData.getSebalTag());
						imageData.setState(ImageState.QUEUED);

						imageData.setBlowoutVersion(getBlowoutVersion(properties));
						job.addTask(taskImpl);

						imageStore.updateImage(imageData);
						imageData.setUpdateTime(imageStore.getImage(
								imageData.getName()).getUpdateTime());
						try {
							imageStore.addStateStamp(imageData.getName(),
									imageData.getState(),
									imageData.getUpdateTime());
						} catch (SQLException e) {
							LOGGER.error("Error while adding state "
									+ imageData.getState() + " timestamp "
									+ imageData.getUpdateTime() + " in DB", e);
						}
					}
				} else {
					LOGGER.debug("Not enough quota to allocate instance for <"
							+ imageData.getName() + "> "
							+ "in federationMember <"
							+ imageData.getFederationMember() + ">");
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}
	}

	private static String getBlowoutVersion(Properties properties) {

		//String blowoutDirPath = System.getProperty("user.dir");
		String blowoutDirPath = properties.getProperty(BLOWOUT_DIR_PATH);
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
				.getProperty("infra_initial_specs_file_path");
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

	private static List<Specification> getInitialSpecs(Properties properties)
			throws IOException {
		String initialSpecsFilePath = properties
				.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);
		LOGGER.debug("Getting initial spec from " + initialSpecsFilePath);

		return Specification
				.getSpecificationsFromJSonFile(initialSpecsFilePath);
	}

	private static InfrastructureProvider createInfraProviderInstance(
			Properties properties) throws Exception {
		String providerClassName = properties
				.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName)
				.getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception(
					"Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}
}
