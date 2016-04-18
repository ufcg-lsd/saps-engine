package org.fogbowcloud.infrastructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;

public class InfrastructureMain {
	
	private static final String INFRA_CRAWLER = "crawler";
	private static final String INFRA_SCHEDULER = "scheduler";
	private static final String INFRA_FETCHER = "fetcher";
	private static final Logger LOGGER = Logger.getLogger(InfrastructureMain.class);

	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		createInfrastrucute(properties, INFRA_CRAWLER);
		createInfrastrucute(properties, INFRA_SCHEDULER);
		createInfrastrucute(properties, INFRA_FETCHER);
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

		List<Specification> initialSpecs = getSpecs(properties, infraType);

		InfrastructureProvider infraProvider = createInfraProviderInstance(properties);

		InfrastructureManager infraManager = new InfrastructureManager(
				initialSpecs, isElastic, infraProvider, properties, infraType);
		infraManager.start(blockWhileInitializing);
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
