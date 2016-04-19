package org.fogbowcloud.infrastructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;

public class InfrastructureMain {
	
	private static String instanceUser;
	private static String instanceIP;
	private static String instancePort;
	private static String instanceExtraPorts;
	
	private static final String INFRA_CRAWLER = "crawler";
	private static final String INFRA_SCHEDULER = "scheduler";
	private static final Logger LOGGER = Logger.getLogger(InfrastructureMain.class);
	

	public static String main(String[] args) throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String infraType = args[1];
		
		//TODO:
		/* 
		 * See how the tunneling will handle with IP address and port,
		 * if the tunneling alredy does the job, there's no need for return
		 * the IP address (or port)		 
		 */
		createInfrastrucute(properties, infraType);
		//crawlerIP = createInfrastrucute(properties, INFRA_CRAWLER);
		//schedulerIP = createInfrastrucute(properties, INFRA_SCHEDULER);
		//fetcherIP = createInfrastrucute(properties, INFRA_FETCHER);
		
		//TODO: handle with this in a way that the method createInfrastructure returns the instance IP
		return instanceIP;
	}
	
	private static void createInfrastrucute(Properties properties,
			String infraType) throws Exception {

		boolean blockWhileInitializing = new Boolean(
				properties
						.getProperty(AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING))
				.booleanValue();

		boolean isElastic = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC))
				.booleanValue();

		List<Specification> specs = getSpecs(properties, infraType);

		InfrastructureProvider infraProvider = createInfraProviderInstance(properties);

		InfrastructureManager infraManager = new InfrastructureManager(
				specs, isElastic, infraProvider, properties, infraType);
		infraManager.start(blockWhileInitializing);
		
		Resource resource = infraManager.getCurrentResource();
		
		instanceUser = resource.getMetadataValue(Resource.METADATA_SSH_USERNAME_ATT);
		instanceIP = resource.getMetadataValue(Resource.METADATA_SSH_HOST);
		instancePort = resource.getMetadataValue(Resource.METADATA_SSH_PORT);
		instanceExtraPorts = resource.getMetadataValue(Resource.METADATA_EXTRA_PORTS_ATT);
		
		if(infraType.equals(INFRA_SCHEDULER)) {
			StorageInitializer storageInitializer = new StorageInitializer(resource.getId());
			storageInitializer.init();						
		}
	}
	
	private static List<Specification> getSpecs(Properties properties,
			String specTypeFile) throws IOException {

		if (specTypeFile.equals(INFRA_CRAWLER)) {
			String crawlerSpecsFilePath = properties
					.getProperty(AppPropertiesConstants.INFRA_CRAWLER_SPECS_FILE_PATH);
			LOGGER.info("Getting crawler spec from file "
					+ crawlerSpecsFilePath);
			return Specification
					.getSpecificationsFromJSonFile(crawlerSpecsFilePath);
		} else if (specTypeFile.equals(INFRA_SCHEDULER)) {
			String schedulerSpecsFilePath = properties
					.getProperty(AppPropertiesConstants.INFRA_SCHEDULER_SPECS_FILE_PATH);
			LOGGER.info("Getting scheduler spec from file "
					+ schedulerSpecsFilePath);
			return Specification
					.getSpecificationsFromJSonFile(schedulerSpecsFilePath);
		} else {
			String fetcherSpecsFilePath = properties
					.getProperty(AppPropertiesConstants.INFRA_FETCHER_SPECS_FILE_PATH);
			LOGGER.info("Getting fetcher spec from file "
					+ fetcherSpecsFilePath);
			return Specification
					.getSpecificationsFromJSonFile(fetcherSpecsFilePath);
		}

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
