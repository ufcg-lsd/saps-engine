package org.fogbowcloud.scheduler.infrastructure.fogbow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.junit.Test;

public class FogbowRequirementsHelperTest {

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
		
		//Creating mocks
		String image = "ubunto01";
		String publicKey = "key01";
		String fogbowRequirements = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		
		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirements);
		
		Specification spec = new Specification(image,publicKey);
		spec.putAllRequirements(requirements);
		
		Resource suitableResource = new Resource("Intance_A");
		suitableResource.setFogbowRequestId("RequestId");
		suitableResource.setSpecification(spec);
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "1");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "1024");
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirements));

		
	}
	
	@Test
	public void matchesResourceSucessB(){
		
		//Creating mocks
		String image = "ubunto01";
		String publicKey = "key01";
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";
		
		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		
		Specification spec = new Specification(image,publicKey);
		spec.putAllRequirements(requirements);
		
		Resource suitableResource = new Resource("Intance_A");
		suitableResource.setFogbowRequestId("RequestId");
		suitableResource.setSpecification(spec);
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "3");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

		
	}
	
	@Test
	public void matchesResourceSucessVCpuOrMenSize(){
		
		//Creating mocks
		String image = "ubunto01";
		String publicKey = "key01";
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 || Glue2RAM >= 2048";
		
		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		
		Specification spec = new Specification(image,publicKey);
		spec.putAllRequirements(requirements);
		
		Resource suitableResource = new Resource("Intance_A");
		suitableResource.setFogbowRequestId("RequestId");
		suitableResource.setSpecification(spec);
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "1");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceVcpuFail(){
		
		//Creating mocks
		String image = "ubunto01";
		String publicKey = "key01";
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";
		
		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		
		Specification spec = new Specification(image,publicKey);
		spec.putAllRequirements(requirements);
		
		Resource suitableResource = new Resource("Intance_A");
		suitableResource.setFogbowRequestId("RequestId");
		suitableResource.setSpecification(spec);
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "1");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceVcpuFailOutOfRange(){
		
		//Creating mocks
		String image = "ubunto01";
		String publicKey = "key01";
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2vCPU <= 4 Glue2RAM >= 2048";
		
		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		
		Specification spec = new Specification(image,publicKey);
		spec.putAllRequirements(requirements);
		
		Resource suitableResource = new Resource("Intance_A");
		suitableResource.setFogbowRequestId("RequestId");
		suitableResource.setSpecification(spec);
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "5"); //Out of range
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8192");
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceMenSizeFail(){
		
		//Creating mocks
		String image = "ubunto01";
		String publicKey = "key01";
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";
		
		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		
		Specification spec = new Specification(image,publicKey);
		spec.putAllRequirements(requirements);
		
		Resource suitableResource = new Resource("Intance_A");
		suitableResource.setFogbowRequestId("RequestId");
		suitableResource.setSpecification(spec);
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "2");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "2047"); //Test low boundary value. 
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceMenSizeFailOutOfRange(){
		
		//Creating mocks
		String image = "ubunto01";
		String publicKey = "key01";
		String fogbowRequirementsA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048 && Glue2RAM <= 8192";
		
		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementsA);
		
		Specification spec = new Specification(image,publicKey);
		spec.putAllRequirements(requirements);
		
		Resource suitableResource = new Resource("Intance_A");
		suitableResource.setFogbowRequestId("RequestId");
		suitableResource.setSpecification(spec);
		suitableResource.putMetadata(Resource.METADATA_SSH_HOST, "10.10.1.1");
		suitableResource.putMetadata(Resource.METADATA_SSH_PORT, "8008");
		suitableResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "userName");
		suitableResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "7060,8070");				
		suitableResource.putMetadata(Resource.METADATA_VCPU, "2");
		suitableResource.putMetadata(Resource.METADATA_MEN_SIZE, "8193"); //Out of range - Test boundary value. 
		
		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
}
