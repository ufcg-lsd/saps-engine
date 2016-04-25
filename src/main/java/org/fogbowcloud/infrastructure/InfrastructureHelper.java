package org.fogbowcloud.infrastructure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowInfrastructureProvider;

public class InfrastructureHelper {
	
	private static String instanceUser;
	private static String instanceIP;
	private static String instancePort;
	private static String instanceExtraPort;
	
	private static final String INFRA_CRAWLER = "crawler";
	private static final String INFRA_SCHEDULER = "scheduler";
	
	private static final Logger LOGGER = Logger.getLogger(InfrastructureHelper.class);

	protected static void createInfrastrucute(Properties properties,
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
		
		Resource resource = infraProvider.getFogbowResource(infraManager.getRequestID());
		
		instanceUser = resource.getMetadataValue(Resource.METADATA_SSH_USERNAME_ATT);
		instanceIP = resource.getMetadataValue(Resource.METADATA_SSH_HOST);
		instancePort = resource.getMetadataValue(Resource.METADATA_SSH_PORT);
		instanceExtraPort = resource.getMetadataValue(Resource.METADATA_EXTRA_PORTS_ATT);
		
		if(infraType.equals(INFRA_SCHEDULER)) {
			String fogbowRequirements = specs.get(0).getRequirementValue("FogbowRequirements");
			String[] splitRequirements = fogbowRequirements.split("\"");
			String requirement = splitRequirements[splitRequirements.length - 1];
			
			StorageInitializer storageInitializer = new StorageInitializer(
					infraManager.getRequestID(), requirement);
			storageInitializer.init();
		}
		
	}
	
	private static String getRequestId(String requestInformation) {
		String[] requestRes = requestInformation.split(":");
		String[] requestId = requestRes[requestRes.length - 1].split("/");
		return requestId[requestId.length - 1];
	}
	
	protected static void writeInstanceDataFile(String infraType)
			throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("infra_" + infraType + ".txt",
				"UTF-8");
		writer.println(instanceUser);
		writer.println(instanceIP);
		writer.println(instancePort);
		writer.println(instanceExtraPort);

		writer.close();
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
