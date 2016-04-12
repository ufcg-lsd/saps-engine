package org.fogbowcloud.scheduler.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.scheduler.core.ExecutionCommandHelper;
import org.fogbowcloud.scheduler.core.TaskExecutionResult;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestResource {
	
	Resource resource;
	
	private Properties properties;
	private ExecutionCommandHelper executionCommandHelperMock;
	
	@Before
	public void setUp() throws Exception {
		
		this.generateDefaulProperties();
		
		executionCommandHelperMock = mock(ExecutionCommandHelper.class);
		
		resource = spy(new Resource("resource_01",properties));
		resource.setExecutionCommandHelper(executionCommandHelperMock);
	}    

	@After
	public void setDown() throws Exception {
		
		executionCommandHelperMock = null;
		resource.getAllMetadata().clear();
		resource = null;
		properties.clear();
		properties = null;
		
	}
	
	@Test
	public void matchTest() {
		
		String image = "image";
		String userName = "userName";
		String publicKey = "publicKey";
		String privateKey = "privateKey";
		String fogbowRequirement = "Glue2vCPU >= 1 && Glue2RAM >= 1024 ";
		
		String coreSize = "1";
		String menSize = "1024";
		String diskSize = "20";
		String location = "edu.ufcg.lsd.cloud_1s";
		
		Specification spec = new Specification(image, userName, publicKey, privateKey);
		spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);
		
		resource.putMetadata(Resource.METADATA_IMAGE, image);
		resource.putMetadata(Resource.METADATA_PUBLIC_KEY, publicKey);
		resource.putMetadata(Resource.METADATA_VCPU, coreSize);
		resource.putMetadata(Resource.METADATA_MEN_SIZE, menSize);
		resource.putMetadata(Resource.METADATA_DISK_SIZE, diskSize);
		resource.putMetadata(Resource.METADATA_LOCATION, location);
		
		assertTrue(resource.match(spec));
		
		spec.getAllRequirements().clear();
		spec = null;
		
	}
	
	@Test
	public void matchTestRequirementNotMach() {
		
		String image = "image";
		String userName = "userName";
		String publicKey = "publicKey";
		String privateKey = "privateKey";
		String fogbowRequirement = "Glue2vCPU > 1 || Glue2RAM = 1024 ";
		
		String coreSize = "1";
		String menSize = "2048";
		String diskSize = "20";
		String location = "edu.ufcg.lsd.cloud_1s";
		
		Specification spec = new Specification(image, userName, publicKey, privateKey);
		spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);
		
		resource.putMetadata(Resource.METADATA_IMAGE, image);
		resource.putMetadata(Resource.METADATA_PUBLIC_KEY, publicKey);
		resource.putMetadata(Resource.METADATA_VCPU, coreSize);
		resource.putMetadata(Resource.METADATA_MEN_SIZE, menSize);
		resource.putMetadata(Resource.METADATA_DISK_SIZE, diskSize);
		resource.putMetadata(Resource.METADATA_LOCATION, location);
		
		assertFalse(resource.match(spec));
		
		spec.getAllRequirements().clear();
		spec = null;
	}
	
	@Test
	public void matchTestImageNotMatch() {
		
		String imageA = "imageA";
		String imageB = "imageB";
		String userName = "userName";
		String publicKey = "publicKey";
		String privateKey = "privateKey";
		String fogbowRequirement = "Glue2vCPU >= 1 && Glue2RAM >= 1024 ";
		
		String coreSize = "1";
		String menSize = "1024";
		String diskSize = "20";
		String location = "edu.ufcg.lsd.cloud_1s";
		
		Specification spec = new Specification(imageB, userName, publicKey, privateKey);
		spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);
		
		resource.putMetadata(Resource.METADATA_IMAGE, imageA);
		resource.putMetadata(Resource.METADATA_PUBLIC_KEY, publicKey);
		resource.putMetadata(Resource.METADATA_VCPU, coreSize);
		resource.putMetadata(Resource.METADATA_MEN_SIZE, menSize);
		resource.putMetadata(Resource.METADATA_DISK_SIZE, diskSize);
		resource.putMetadata(Resource.METADATA_LOCATION, location);
		
		assertFalse(resource.match(spec));
	
		spec.getAllRequirements().clear();
		spec = null;
	}
	
	@Test
	public void matchTestPublicKeyNotMatch() {
		
		String image = "image";
		String userName = "userName";
		String publicKeyA = "publicKeyA";
		String publicKeyB = "publicKeyB";
		String privateKey = "privateKey";
		String fogbowRequirement = "Glue2vCPU >= 1 && Glue2RAM >= 1024 ";

		String coreSize = "1";
		String menSize = "1024";
		String diskSize = "20";
		String location = "edu.ufcg.lsd.cloud_1s";

		Specification spec = new Specification(image, userName, publicKeyB, privateKey);
		spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);

		resource.putMetadata(Resource.METADATA_IMAGE, image);
		resource.putMetadata(Resource.METADATA_PUBLIC_KEY, publicKeyA);
		resource.putMetadata(Resource.METADATA_VCPU, coreSize);
		resource.putMetadata(Resource.METADATA_MEN_SIZE, menSize);
		resource.putMetadata(Resource.METADATA_DISK_SIZE, diskSize);
		resource.putMetadata(Resource.METADATA_LOCATION, location);

		assertFalse(resource.match(spec));

		spec.getAllRequirements().clear();
		spec = null;
	}

	
	@Test
	public void testExecuteTask(){
		
		List<Command> commandsPrologue = new ArrayList<Command>();
		List<Command> commandsRemote = new ArrayList<Command>();
		List<Command> commandsEpilogue = new ArrayList<Command>();
		Map<String, String> envVariables = new HashMap<String, String>();

		String image = "image";
		String userName = "userName";
		String publicKey = "publicKey";
		String privateKey = "privateKey";
		String host = "10.100.0.1";
		String port = "1091";
		
		Task task = prepareMockCommandsToExecute(commandsPrologue, commandsRemote, commandsEpilogue, envVariables,
				image, userName, publicKey, privateKey, host, port);
		
		doReturn(TaskExecutionResult.OK).when(executionCommandHelperMock).execLocalCommands(Mockito.eq(commandsPrologue),
				Mockito.eq(envVariables));
		doReturn(TaskExecutionResult.OK).when(executionCommandHelperMock).execRemoteCommands(Mockito.eq(host),
				Mockito.eq(Integer.parseInt(port)), Mockito.eq(userName), Mockito.eq(privateKey), Mockito.eq(commandsRemote));
		doReturn(TaskExecutionResult.OK).when(executionCommandHelperMock).execLocalCommands(Mockito.eq(commandsEpilogue),
				Mockito.eq(envVariables));
		
		resource.executeTask(task);
		
		verify(executionCommandHelperMock, times(1)).execLocalCommands(Mockito.eq(commandsPrologue), Mockito.eq(envVariables));
		verify(executionCommandHelperMock, times(1)).execRemoteCommands(Mockito.eq(host),
				Mockito.eq(Integer.parseInt(port)), Mockito.eq(userName), Mockito.eq(privateKey), Mockito.eq(commandsRemote));
		verify(executionCommandHelperMock, times(1)).execLocalCommands(Mockito.eq(commandsEpilogue), Mockito.eq(envVariables));
		assertEquals(TaskExecutionResult.OK, resource.getTaskExecutionResult().getExitValue());

		commandsPrologue.clear();
		commandsPrologue = null;
		commandsRemote.clear();
		commandsRemote = null;
		commandsEpilogue.clear();
		commandsEpilogue = null;
		envVariables.clear();
		envVariables = null;
	}
	
	@Test
	public void testExecuteTaskFail(){

		List<Command> commandsPrologue = new ArrayList<Command>();
		List<Command> commandsRemote = new ArrayList<Command>();
		List<Command> commandsEpilogue = new ArrayList<Command>();
		Map<String, String> envVariables = new HashMap<String, String>();
		
		String image = "image";
		String userName = "userName";
		String publicKey = "publicKey";
		String privateKey = "privateKey";
		String host = "10.100.0.1";
		String port = "1091";
		
		Task task = prepareMockCommandsToExecute(commandsPrologue, commandsRemote, commandsEpilogue, envVariables,
				image, userName, publicKey, privateKey, host, port);
		
		doReturn(TaskExecutionResult.OK).when(executionCommandHelperMock).execLocalCommands(Mockito.eq(commandsPrologue),
				Mockito.eq(envVariables));
		doReturn(TaskExecutionResult.NOK).when(executionCommandHelperMock).execRemoteCommands(Mockito.eq(host),
				Mockito.eq(Integer.parseInt(port)), Mockito.eq(userName), Mockito.eq(privateKey), Mockito.eq(commandsRemote));
		doReturn(TaskExecutionResult.OK).when(executionCommandHelperMock).execLocalCommands(Mockito.eq(commandsEpilogue),
				Mockito.eq(envVariables));
		
		resource.executeTask(task);
		
		verify(executionCommandHelperMock, times(1)).execLocalCommands(Mockito.eq(commandsPrologue), Mockito.eq(envVariables));
		verify(executionCommandHelperMock, times(1)).execRemoteCommands(Mockito.eq(host),
				Mockito.eq(Integer.parseInt(port)), Mockito.eq(userName), Mockito.eq(privateKey), Mockito.eq(commandsRemote));
		assertEquals(TaskExecutionResult.NOK, resource.getTaskExecutionResult().getExitValue());
		
		commandsPrologue.clear();
		commandsPrologue = null;
		commandsRemote.clear();
		commandsRemote = null;
		commandsEpilogue.clear();
		commandsEpilogue = null;
		envVariables.clear();
		envVariables = null;
		
	}

	private Task prepareMockCommandsToExecute(List<Command> commandsPrologue, List<Command> commandsRemote,
			List<Command> commandsEpilogue, Map<String, String> envVariables, String image, String userName,
			String publicKey, String privateKey, String host, String port) {
		
		Specification spec = new Specification(image, userName, publicKey, privateKey);
		
		resource.putMetadata(Resource.METADATA_IMAGE, "image");
		resource.putMetadata(Resource.METADATA_PUBLIC_KEY, "publicKey");
		resource.putMetadata(Resource.METADATA_SSH_HOST, host);
		resource.putMetadata(Resource.METADATA_SSH_PORT, port);
		
		Command c1 = new Command("command_01", Command.Type.PROLOGUE);
		Command c2 = new Command("command_02", Command.Type.REMOTE);
		Command c3 = new Command("command_03", Command.Type.EPILOGUE);

		commandsPrologue.add(c1);
		commandsRemote.add(c2);
		commandsEpilogue.add(c3);
		
		envVariables.put(Resource.ENV_HOST, host);
		envVariables.put(Resource.ENV_SSH_PORT, port);
		envVariables.put(Resource.ENV_SSH_USER, userName);
		envVariables.put(Resource.ENV_PRIVATE_KEY_FILE, privateKey);
		
		Task task = mock(Task.class);
		doReturn("Task_01").when(task).getId();
		doReturn(spec).when(task).getSpecification();
		
		doReturn(commandsPrologue).when(task).getCommandsByType(Command.Type.PROLOGUE);
		doReturn(commandsRemote).when(task).getCommandsByType(Command.Type.REMOTE);
		doReturn(commandsEpilogue).when(task).getCommandsByType(Command.Type.EPILOGUE);
		return task;
	}
	

	private void generateDefaulProperties(){
		
		properties = new Properties();
		
		properties.setProperty(AppPropertiesConstants.INFRA_IS_STATIC, "false");
		properties.setProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME,
				"org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowInfrastructureProvider");
		properties.setProperty(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME, "2000");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME, "3000");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "10000");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "300000");
		properties.setProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH, "src/test/resources/Specs_Json");
		properties.setProperty(AppPropertiesConstants.INFRA_SPECS_BLOCK_CREATING, "false");
		properties.setProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL, "100_02_01_01:8098");
		properties.setProperty(AppPropertiesConstants.INFRA_FOGBOW_TOKEN_PUBLIC_KEY_FILEPATH,
				"src/test/resources/publickey_file");
		
	}
}
