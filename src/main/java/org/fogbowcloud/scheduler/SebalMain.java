package org.fogbowcloud.scheduler;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.SebalJob;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.util.Constants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.SebalTasks;

public class SebalMain {
	
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer sebalExecutionTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

//	private static Map<String, ImageData> pendingImageExecution = new ConcurrentHashMap<String, ImageData>();
	private static ImageDataStore imageStore;
	private static String nfsServerIP;
	private static String nfsServerPort;
	private static InfrastructureManager infraManager;
	//FIXME: change this later
	private static int maxAllowedResources = 5;
	
	private static final Logger LOGGER = Logger.getLogger(SebalMain.class);

	public static void main(String[] args) throws Exception {

		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String imageStoreIP = args[1];
		String imageStorePort = args[2];
		nfsServerIP = args[3];
		nfsServerPort = args[4];
		
		imageStore = new JDBCImageDataStore(properties, imageStoreIP, imageStorePort);			

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
		
		infraManager = new InfrastructureManager(initialSpecs, isElastic,
				infraProvider, properties);
		infraManager.start(blockWhileInitializing);
		
		Scheduler scheduler = new Scheduler(infraManager, job);
		ExecutionMonitor execMonitor = new ExecutionMonitor(scheduler, job);

		final Specification sebalSpec = getSebalSpecFromFile(properties);
		
		addRTasks(properties, job, sebalSpec, ImageState.RUNNING_R, ImageDataStore.UNLIMITED);
		
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
					e.printStackTrace();
				}
			}	
		}, 0, Integer.parseInt(properties.getProperty("sebal_execution_period")));
		
		SebalScheduleApplication restletServer = new SebalScheduleApplication((SebalJob)job, imageStore, properties);
		restletServer.startServer();
	}
	
	private static void setFederationMemberIntoSpec(Specification spec, String federationMember) {
		String fogbowRequirements = spec.getRequirementValue(
				"FogbowRequirements");
		String newRequirements = fogbowRequirements + " && Glue2CloudComputeManagerID==\"" + federationMember + "\"";
		spec.addRequirement("FogbowRequirements", newRequirements);
	}
	
	private static Map<String, Collection<Resource>> allocationMap() {
		
		List<Resource> allResources = infraManager.getAllResources();
		
		Map<String, Collection<Resource>> allocMap = new HashMap<String, Collection<Resource>>();
		for(Resource resource : allResources) {						
			String location = resource.getMetadataValue(Resource.METADATA_LOCATION);
			if (!allocMap.containsKey(location)) {
				allocMap.put(location, new LinkedList<Resource>());
			}
			allocMap.get(location).add(resource);			
		}
		return allocMap;
	}
	
	private static boolean isQuotaAvailable(String federationMemberId, 
			Map<String, Collection<Resource>> allocationMap, 
			int maxAllowedResources) {
		
		if (allocationMap.containsKey(federationMemberId)) {
			//debug
			int numAllocationPerFederationMember = allocationMap.get(federationMemberId).size();
			return numAllocationPerFederationMember < maxAllowedResources; 
		}
		return false;
	}
	
	private static void addRTasks(final Properties properties, final Job job,
			final Specification sebalSpec, ImageState imageState, int limit) throws InterruptedException {
		
		try {
			
			List<ImageData> imagesToExecute = imageStore.getIn(imageState, limit);
			
			for (ImageData imageData : imagesToExecute) {
				if(!imageData.getImageStatus().equals(ImageData.PURGED)) {					
					LOGGER.debug("The image " + imageData.getName() + " is in the execution state "
							+ imageData.getState().getValue() + " (not finished).");
					
					LOGGER.info("Adding " + imageState + " tasks for image " + imageData.getName());
					
					Specification tempSpec = new Specification(
							sebalSpec.getImage(), sebalSpec.getUsername(),
							sebalSpec.getPublicKey(),
							sebalSpec.getPrivateKeyFilePath(),
							sebalSpec.getUserDataFile(),
							sebalSpec.getUserDataType());
					setFederationMemberIntoSpec(tempSpec, imageData.getFederationMember());
					
					Map<String, Collection<Resource>> allocationMap = allocationMap();
					
					if(isQuotaAvailable(imageData.getFederationMember(), allocationMap, maxAllowedResources)) {
						
						if (ImageState.RUNNING_R.equals(imageState)
								|| ImageState.DOWNLOADED.equals(imageState)) {
							
							TaskImpl taskImpl = new TaskImpl(UUID.randomUUID().toString(), tempSpec);
							taskImpl = SebalTasks.createRTask(taskImpl, properties,
									imageData.getName(), tempSpec,
									imageData.getFederationMember(),
									nfsServerIP, nfsServerPort);
							imageData.setState(ImageState.RUNNING_R);
							job.addTask(taskImpl);
							imageStore.updateImage(imageData);	
						}					
					} else {
						LOGGER.info("Not enough quota to allocate instance for <" + imageData.getName() + "> " +
								"in federationMember <" + 	imageData.getFederationMember() + ">");
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error while getting image.", e);
		}
	}
	
	private static Specification getSebalSpecFromFile(Properties properties) {
		String sebalSpecFile = properties.getProperty("sebal_task_spec_path");
		List<Specification> specs = new ArrayList<Specification>();
		try {
			specs = Specification.getSpecificationsFromJSonFile(sebalSpecFile);
			if(specs!= null && !specs.isEmpty()){
				return specs.get(Constants.LIST_ARRAY_FIRST_ELEMENT);
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}
	
	private static List<Specification> getInitialSpecs(Properties properties)
			throws IOException {
		String initialSpecsFilePath = properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);		
		LOGGER.info("Getting initial spec from file " + initialSpecsFilePath);
		System.out.println("Getting initial spec from file " + initialSpecsFilePath);
		
		return Specification.getSpecificationsFromJSonFile(initialSpecsFilePath);
	}
	
	private static InfrastructureProvider createInfraProviderInstance(Properties properties)
			throws Exception {

		String providerClassName = properties
				.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception(
					"Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}
}
