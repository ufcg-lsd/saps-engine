package org.fogbowcloud.sebal.engine.scheduler;

import java.io.File;
import java.io.FileInputStream;
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
import org.fogbowcloud.blowout.core.SchedulerInterface;
import org.fogbowcloud.blowout.core.StandardScheduler;
import org.fogbowcloud.blowout.core.model.Job;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.util.Constants;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.fogbowcloud.blowout.infrastructure.manager.DefaultInfrastructureManager;
import org.fogbowcloud.blowout.infrastructure.manager.InfrastructureManager;
import org.fogbowcloud.blowout.infrastructure.monitor.ResourceMonitor;
import org.fogbowcloud.blowout.infrastructure.provider.fogbow.FogbowInfrastructureProvider;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.blowout.pool.DefaultBlowoutPool;
import org.fogbowcloud.sebal.engine.scheduler.core.model.SebalJob;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;

public class SebalMain {

	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(
			Executors.newScheduledThreadPool(1));
//	private static ManagerTimer taskMapUpdateTimer = new ManagerTimer(
//			Executors.newScheduledThreadPool(1));

	private static String nfsServerIP;
	private static String nfsServerPort;
	private static ImageDataStore imageStore;
	private static InfrastructureManager infraManager;
	
//	private static Map<String, Integer> maxAllowedTasksPerFederationMap;
//	private static Map<String, Collection<String>> submissionControlMap;

	private static final Logger LOGGER = Logger.getLogger(SebalMain.class);
	
//	private static final String FEDERATION_TASK_LIMIT_FILE = "federation_task_limit_file";
//	private static final int DEFAULT_TASK_LIMIT_PER_FEDERATION = 5;

	public static void main(String[] args) throws Exception {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		String imageStoreIP = args[1];
		String imageStorePort = args[2];

		LOGGER.debug("Imagestore " + imageStoreIP + ":" + imageStorePort);

		imageStore = new JDBCImageDataStore(properties);

		final Job job = new SebalJob(imageStore);

		FogbowInfrastructureProvider infraProvider = new FogbowInfrastructureProvider(properties, true);

		LOGGER.info("Calling infrastructure manager");
		
		final BlowoutPool pool = new DefaultBlowoutPool();
		
		SebalTaskMonitor execMonitor = new SebalTaskMonitor(pool, imageStore, Integer.parseInt(properties
						.getProperty(SebalPropertiesConstants.EXECUTION_MONITOR_PERIOD)));
		
		ResourceMonitor resourceMonitor = new ResourceMonitor(infraProvider, pool, properties);
		resourceMonitor.start();
		execMonitor.start();
		
		infraManager = new DefaultInfrastructureManager(infraProvider, resourceMonitor);
		SchedulerInterface scheduler = new StandardScheduler(execMonitor);
		
		pool.start(infraManager, scheduler);
		
		final Specification sebalSpec = getSebalSpecFromFile(properties);
		
//		maxAllowedTasksPerFederationMap = getMaxAllowedTasksPerFederation(properties);
//		submissionControlMap = new HashMap<String, Collection<String>>();
		
		// In case of the process has been stopped before finishing the images running 
		// in the next restart all images in running state will be reseted to queued state
		resetImagesRunningToQueued();

		addRTasks(properties, job, sebalSpec, ImageState.QUEUED, ImageDataStore.UNLIMITED, pool);
		
		sebalExecutionTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					addRTasks(properties, job, sebalSpec, ImageState.DOWNLOADED, 1, pool);
				} catch (InterruptedException e) {
					LOGGER.error("Error while adding R tasks", e);
				}
			}

		}, 0, Integer.parseInt(properties.getProperty(SebalPropertiesConstants.SEBAL_EXECUTION_PERIOD)));

		LOGGER.info("Scheduler working");
//		taskMapUpdateTimer.scheduleAtFixedRate(new Runnable() {
//			@Override
//			public void run() {
//				updateSubmissionControlMap(scheduler);
//			}
//		}, 0, Integer.parseInt(properties.getProperty("task_map_update_period")));
	}

//	private static Map<String, Integer> getMaxAllowedTasksPerFederation(Properties properties) {
//
//		Map<String, Integer> maxAllowedTasksPerFederationMap = new HashMap<String, Integer>();
//		File federationTaskLimitFile = new File(properties.getProperty(FEDERATION_TASK_LIMIT_FILE));
//
//		List<String> taskLimitLines = new ArrayList<String>();
//
//		try {
//			taskLimitLines = FileUtils.readLines(federationTaskLimitFile,
//					Charsets.UTF_8);
//			
//			for (String line : taskLimitLines) {
//				String[] lineSplit = line.split("\\s+");
//				maxAllowedTasksPerFederationMap.put(lineSplit[0],
//						Integer.valueOf(lineSplit[1]));
//				LOGGER.debug("Mapping federation " + lineSplit[0]
//						+ " with task limit " + Integer.valueOf(lineSplit[1]));
//			}
//		} catch (IOException e) {
//			LOGGER.error("Error while reading task limit file "
//					+ federationTaskLimitFile, e);
//		}
//
//
//		return maxAllowedTasksPerFederationMap;
//	}

//	private static void updateSubmissionControlMap(Scheduler scheduler) {
//		LOGGER.info("Updating task submission control map");		
//		List<TaskProcess> allTaskProcesses = scheduler.getAllProcs();
//		
//		// TODO: see if this toString works
//		LOGGER.debug("Current task processes active " + allTaskProcesses.toString());
//		for(TaskProcess taskProcess : allTaskProcesses) {
//			String federationMemberId = taskProcess.getSpecification()
//					.getRequirementValue("Glue2CloudComputeManagerID");
//			Collection<String> federationTasks = submissionControlMap.get(federationMemberId);
//			
//			if (taskProcess.getStatus().equals(TaskProcessImpl.State.FINNISHED)) {
//				federationTasks.remove(taskProcess.getTaskId());
//			}
//			
//			submissionControlMap.put(federationMemberId, federationTasks);
//		}
//	}
	
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

	private static void addRTasks(final Properties properties, final Job job,
			final Specification sebalSpec, ImageState imageState, int limit, BlowoutPool pool)
			throws InterruptedException {

		try {
			List<ImageData> imagesToExecute = imageStore.getIn(imageState,
					limit);
			List<Task> taskList = new ArrayList<Task>();
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

//				if (federationHasQuota(imageData.getFederationMember())) {
				// Temporary
				if(true) {

					if (ImageState.QUEUED.equals(imageState)
							|| ImageState.DOWNLOADED.equals(imageState)) {

						TaskImpl taskImpl = new TaskImpl(UUID.randomUUID()
								.toString(), tempSpec);
						
//						addTaskIntoSubmissionControlMap(taskImpl,
//								imageData.getFederationMember());
						
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
						taskList.add(taskImpl);

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
			pool.addTasks(taskList);
		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}
	}
	
//	private static void addTaskIntoSubmissionControlMap(TaskImpl taskImpl,
//			String federationMember) {
//		LOGGER.debug("Adding task " + taskImpl.getId() + " for "
//				+ federationMember + " into submission control map");
//		Collection<String> federationTaskCollection = submissionControlMap
//				.get(federationMember);
//
//		if (federationTaskCollection == null) {
//			federationTaskCollection = new ArrayList<String>();
//		}
//
//		federationTaskCollection.add(taskImpl.getId());
//		submissionControlMap.put(federationMember, federationTaskCollection);
//		
//		LOGGER.debug("Task " + taskImpl.getId()
//				+ " added to submission control map");
//	}

//	private static boolean federationHasQuota(String federationMember) {
//		if (submissionControlMap.containsKey(federationMember)) {
//			int maxAllowedTasks = DEFAULT_TASK_LIMIT_PER_FEDERATION;
//			if (maxAllowedTasksPerFederationMap.containsKey(federationMember)) {
//				maxAllowedTasks = maxAllowedTasksPerFederationMap
//						.get(federationMember);
//			}
//
//			LOGGER.debug("Using task limit " + maxAllowedTasks + " for "
//					+ federationMember);
//			if (submissionControlMap.get(federationMember).size() >= maxAllowedTasks) {
//				return false;
//			}
//		}
//
//		return true;
//	}

	private static String getBlowoutVersion(Properties properties) {

		//String blowoutDirPath = System.getProperty("user.dir");
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
}
