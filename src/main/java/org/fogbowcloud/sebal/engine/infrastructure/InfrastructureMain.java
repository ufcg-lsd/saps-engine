package org.fogbowcloud.sebal.engine.infrastructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.infrastructure.ResourceNotifier;
import org.fogbowcloud.blowout.scheduler.core.model.Resource;
import org.fogbowcloud.blowout.scheduler.core.model.Specification;
import org.fogbowcloud.blowout.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.blowout.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.blowout.scheduler.infrastructure.InfrastructureProvider;

public class InfrastructureMain implements ResourceNotifier {
	
	//Commands
	private static final String COMPUTE = "compute";
	private static final String TEST_COMPUTE = "test-compute";
	private static final String STORAGE = "storage";
	private static final String TEST_STORAGE = "test-storage";
	private static final String STORAGE_ATTACHMENT = "attachment";
	
	//Constants
	private static final String EXTRA_PORT = "EXTRA_PORT=";
	private static final String SSH_PORT = "SSH_PORT=";
	private static final String SSH_HOST = "SSH_HOST=";
	private static final String USER_NAME = "USER_NAME=";
	private static final String INSTANCE_ID = "INSTANCE_ID=";
	private static final String STORAGE_ID = "STORAGE_ID=";
	private static final String ATTACHMENT_ID = "ATTACHMENT_ID=";
	private static final String STORAGE_STATUS = "STORAGE_STATUS=";
	private static final String DELIMITER = ";";
	private static final String CLEAN = "true";

	private Resource resource;
	
	//Global variables:
	private static String command;
	private static String confgFilePath;
	private static String specsFilePath;
	private static String cleanCommand;
	private static String storageSize;
	private static String instanceId;
	private static String storageId;
	
	
	private static final Logger LOGGER = Logger.getLogger(InfrastructureMain.class);

	//TODO colocar o primeiro parametro como opção de criação de VM, Storage ou LinkStorage
	public static void main(String[] args) throws Exception {
		
//		if(true){
//			try {
//				AccountConfig config = new AccountConfig();
//				config.setUsername("fogbow");
//				config.setPassword("nc3SRPS2");
//				config.setTenantName("Fogbow");
//				config.setAuthUrl("http://10.5.0.14:5000/v2.0/tokens");
//				config.setAuthenticationMethod(AuthenticationMethod.KEYSTONE);
//				Account account = new AccountFactory(config).createAccount();
//
//				Collection<Container> containers = account.list();
//				for (Container currentContainer : containers) {
//					System.out.println(currentContainer.getName());
//					StoredObject storedObject = currentContainer.getObject("images/id001/teste_path2.txt");
//					storedObject.uploadObject(new File("sebal.conf"));
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//			
//			System.exit(0);
//		}
		
		//FIXME: mover para o objeto?
		LOGGER.debug("Starting infrastructure creation process...");

		try {
			validateArgs(args);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			printUsage();
			System.exit(1);
		}
		
		//FIXME: block 1 parse args
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(confgFilePath);
		properties.load(input);
		
		boolean blockWhileInitializing = new Boolean(
				properties
						.getProperty(AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING))
				.booleanValue();
		
		//TODO: create manager
		InfrastructureProvider infraProvider = createInfraProviderInstance(properties);

		InfrastructureManager infraManager = new InfrastructureManager(null, true,
				infraProvider, properties);
		
		if(CLEAN.equals(cleanCommand)) {
			infraManager.start(blockWhileInitializing, true);
		} else {
			infraManager.start(blockWhileInitializing, false);
		}
		
		InfrastructureMain infraMain = new InfrastructureMain();
		
		//TODO Verificar qual o tipo de comando e seguir fluxos especificos por comando.
		if(COMPUTE.equals(command)){
		
			List<Specification> specs = getSpecs(properties, specsFilePath);
			
			infraManager.orderResource(specs.get(0), infraMain, 1);
			
			while (infraMain.resource == null) {
				Thread.sleep(2000);
			}
			
			String globalResourceId = infraManager.getResourceComputeId(infraMain.resource);
			//String[] splitResourceId = globalResourceId.split("@");
			
			String resourceStr = resourceAsString(globalResourceId, infraMain.resource);
			System.out.println(resourceStr);
			
			infraManager.stop(false);
			
		}else if(TEST_COMPUTE.equals(command)){
			
			StorageInitializer storageInitializer = new StorageInitializer(properties);
			
			List<Specification> specs = getSpecs(properties, specsFilePath);
			String fogbowRequirements = specs.get(0).getRequirementValue(
					"FogbowRequirements");
			String[] splitRequirements = fogbowRequirements.split("&&");
			String requirement = splitRequirements[splitRequirements.length - 1];
			requirement = requirement.substring(1);
			
			String storageId = storageInitializer.orderStorage(Integer.parseInt(storageSize), requirement);
			System.out.println(STORAGE_ID+storageId);
			
		}else if(STORAGE.equals(command)){
			
			StorageInitializer storageInitializer = new StorageInitializer(properties);
			
			List<Specification> specs = getSpecs(properties, specsFilePath);
			String fogbowRequirements = specs.get(0).getRequirementValue(
					"FogbowRequirements");
			String[] splitRequirements = fogbowRequirements.split("&&");
			String requirement = splitRequirements[splitRequirements.length - 1];
			requirement = requirement.substring(1);
			
			String globalStorageId = storageInitializer.orderStorage(Integer.parseInt(storageSize), requirement);
			//String[] splitStorageId = globalStorageId.split("@");
			
			System.out.println(STORAGE_ID+globalStorageId);
			
		}else if(TEST_STORAGE.equals(command)){
			
			StorageInitializer storageInitializer = new StorageInitializer(properties);
			
			String status = storageInitializer.testStorage(storageId.trim());
			System.out.println(STORAGE_STATUS+status);
			
		}else if(STORAGE_ATTACHMENT.equals(command)){
			
			StorageInitializer storageInitializer = new StorageInitializer(properties);
			String attachmentId = storageInitializer.attachStorage(instanceId, storageId);
			
			System.out.println(ATTACHMENT_ID+attachmentId);
			
		}

		LOGGER.debug("Infrastructure created.");

		//FIXME: parece que alguma thread to Infra estah pendurada. 
		System.exit(0);
	}
	
	private static List<Specification> getSpecs(Properties properties,
			String specsFilePath) throws IOException {

		LOGGER.info("Getting specs from file " + specsFilePath);
		return Specification.getSpecificationsFromJSonFile(specsFilePath);

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
	
	private static void validateArgs(String [] args) throws IllegalArgumentException{
		if(args == null || args.length < 1){
			throw new IllegalArgumentException("Wrong usage. At least one argument command must be informed.");
		}
		command = args[0];
		if(COMPUTE.equals(command)){
			if(args.length < 3){
				throw new IllegalArgumentException("Wrong usage for compute command. Configuration and specification files must be informed.");
			}
			confgFilePath = args[1];
			specsFilePath = args[2];
			cleanCommand = args[3];
		}else if(TEST_COMPUTE.equals(command)){
			if(args.length < 2){
				throw new IllegalArgumentException("Wrong usage for teste compute command. Instance Id must be informed.");
			}
			instanceId = args[1];
			
		}else if(STORAGE.equals(command)){
			if(args.length < 4){
				throw new IllegalArgumentException("Wrong usage for storage command. Storage size must be informed.");
			}
			storageSize = args[1];
			confgFilePath = args[2];
			specsFilePath = args[3];
			cleanCommand = args[4];
			try {
				Integer.parseInt(storageSize);
			} catch (Exception e) {
				throw new IllegalArgumentException("Illegal storage size format. Storage size must be integer value.");
			}
			
		}else if(TEST_STORAGE.equals(command)){
			if(args.length < 3){
				throw new IllegalArgumentException("Wrong usage for test storage command. Storage Id must be informed.");
			}
			storageId = args[1];
			confgFilePath = args[2];
			cleanCommand = args[3];
			
		}else if(STORAGE_ATTACHMENT.equals(command)){
			if(args.length < 4){
				throw new IllegalArgumentException("Wrong usage for attachment command. Instance id and Storage id must be passed.");
			}
			instanceId = args[1];
			storageId = args[2];
			confgFilePath = args[3];
			cleanCommand = args[4];
			
		}else{
			throw new IllegalArgumentException("Invalid command argument usage.");
		}

	}
	
	private static String resourceAsString(String resourceId, Resource resource) {
		
		StringBuilder sb = new StringBuilder();
		sb.append(INSTANCE_ID).append(resourceId);
		sb.append(DELIMITER);
		sb.append(USER_NAME).append(resource.getMetadataValue(Resource.METADATA_SSH_USERNAME_ATT));
		sb.append(DELIMITER);
		sb.append(SSH_HOST).append(resource.getMetadataValue(Resource.METADATA_SSH_HOST));
		sb.append(DELIMITER);
		sb.append(SSH_PORT).append(resource.getMetadataValue(Resource.METADATA_SSH_PORT));
		sb.append(DELIMITER);
		sb.append(EXTRA_PORT).append(resource.getMetadataValue(Resource.METADATA_EXTRA_PORTS_ATT));
		
		return sb.toString();
	}
	
	private static void printUsage(){
		
		StringBuilder sb = new StringBuilder();
		sb.append("Usage of Infrastrucutre : ");
		sb.append("\nCommand "+COMPUTE+":\n");
		sb.append("[1]"+COMPUTE);
		sb.append(" [2]Config_File_Path (String required) [3]Specs_File_Path (String required)");
		sb.append("\nCommand "+STORAGE+":\n");
		sb.append("[1]"+STORAGE);
		sb.append(" [2]Storage Size (Integer required)");
		sb.append("\nCommand "+ATTACHMENT_ID+":\n");
		sb.append("[1]"+ATTACHMENT_ID);
		sb.append(" [2]Instance Id (String required) [3]Storage Id (String required)");
	}

	@Override
	public void resourceReady(Resource resource) {
		LOGGER.debug("Receiving new assigned resource...");
		
		if(resource == null) {
			LOGGER.error("Received resource is null");
			return;
		}
		
		this.resource = resource;
		
		LOGGER.debug("Process finished.");
	}
	
}