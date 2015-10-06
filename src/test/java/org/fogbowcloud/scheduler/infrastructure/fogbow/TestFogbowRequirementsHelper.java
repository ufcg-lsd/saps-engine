package org.fogbowcloud.scheduler.infrastructure.fogbow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.occi.request.RequestType;
import org.fogbowcloud.scheduler.core.http.HttpWrapper;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestFogbowRequirementsHelper {
	
	private Properties properties;
	private Map<String, String> requirements;
	private Specification spec;
	private Resource suitableResource;
	
	@Before
	public void setUp() throws Exception {

		this.generateDefaulProperties();
		
		String image = "ubunto01";
		String userName = "userName";
		String publicKey = "key01";
		String privateKey = "privKey01";
		
		requirements = new HashMap<String, String>();
		spec = new Specification(image,userName,publicKey,privateKey);
		suitableResource = new Resource("Intance_A", properties);
	} 
	
	@After
	public void setDown() throws Exception {
		requirements.clear();
		requirements = null;
		spec.getAllRequirements().clear();
		spec = null;
		suitableResource.getAllMetadata().clear();
		suitableResource = null;
		properties = null;
	}

	@Test
	public void validateFogbowRequirementsSyntaxSucessTest(){
		
		String fogbowRequirementA = "Glue2vCPU >= 1 && Glue2RAM >= 1024 ";
		String fogbowRequirementB = "Glue2vCPU >= 1 && Glue2RAM >= 1024 && Glue2disk >= 20 ";
		String fogbowRequirementC = "Glue2vCPU >= 1 && Glue2RAM >= 1024 && Glue2disk >= 20 && Glue2CloudComputeManagerID ==\"servers.your.domain\"";
		String fogbowRequirementD = "Glue2vCPU >= 1 && Glue2RAM == 1024 || Glue2RAM == 2048 && Glue2disk >= 20 && Glue2CloudComputeManagerID ==\"servers.your.domain\"";
		String fogbowRequirementE = "";
		String fogbowRequirementF = null;
		
		assertTrue(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementA));
		assertTrue(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementB));
		assertTrue(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementC));
		assertTrue(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementD));
		assertTrue(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementE));
		assertTrue(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementF));
		
	}
	
	@Test
	public void validateFogbowRequirementsSyntaxFailTest(){
		
		String fogbowRequirementA = "X (r =x) 1 && w = y";
		assertFalse(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementA));
		
	}
	
	@Test
	public void matchesResourceSucessA(){
		
		String fogbowRequirements = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirements);
		spec.putAllRequirements(requirements);
		
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "1");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "1024");
		suitableResource.putMetadata(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		suitableResource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
		suitableResource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirements));
		
	}
	
	@Test
	public void matchesResourceSucessB(){
		
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";
		
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		spec.putAllRequirements(requirements);
		
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "3");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		suitableResource.putMetadata(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		suitableResource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
		suitableResource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceSucessVCpuOrMenSize(){
		
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 || Glue2RAM >= 2048";
		
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		spec.putAllRequirements(requirements);
		
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "1");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		suitableResource.putMetadata(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		suitableResource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
		suitableResource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));
	}
	
	@Test
	public void matchesResourceVcpuFail(){
		
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";
		
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		spec.putAllRequirements(requirements);
		
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "1");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		suitableResource.putMetadata(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		suitableResource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
		suitableResource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceVcpuFailOutOfRange(){
		
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2vCPU <= 4 && Glue2RAM >= 2048";
		
		FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementsB);
		
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		spec.putAllRequirements(requirements);
		
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "5"); //Out of range
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		suitableResource.putMetadata(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		suitableResource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
		suitableResource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceMenSizeFail(){
		
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";
		
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		spec.putAllRequirements(requirements);
		
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "2");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "2047"); //Test low boundary value. 
		suitableResource.putMetadata(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		suitableResource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
		suitableResource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceMenSizeFailOutOfRange(){
		
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048 && Glue2RAM <= 8192";
		
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		spec.putAllRequirements(requirements);
		
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "2");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8193"); //Out of range - Test boundary value. 
		suitableResource.putMetadata(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		suitableResource.putMetadata(Resource.METADATA_IMAGE, spec.getImage());
		suitableResource.putMetadata(Resource.METADATA_PUBLIC_KEY, spec.getPublicKey());
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

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
		properties.setProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING, "false");
		properties.setProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL, "100_02_01_01:8098");
		properties.setProperty(AppPropertiesConstants.INFRA_FOGBOW_TOKEN_PUBLIC_KEY_FILEPATH,
				"src/test/resources/publickey_file");
		
	}
	
}
