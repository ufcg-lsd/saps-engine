package org.fogbowcloud.scheduler.infrastructure.fogbow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.occi.order.OrderType;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.TestResourceHelper;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
		String userData = "scripts/lvl-user-data.sh";

		requirements = new HashMap<String, String>();
		spec = new Specification(image, userName, publicKey, privateKey, userData);
		suitableResource = mock(Resource.class);
		doReturn("resquest_01").when(suitableResource).getId();
		when(suitableResource.match(Mockito.any(Specification.class))).thenCallRealMethod();
	}

	@After
	public void setDown() throws Exception {
		requirements.clear();
		requirements = null;
		spec.getAllRequirements().clear();
		spec = null;
		suitableResource.getAllMetadata().clear();
		suitableResource = null;
		properties.clear();
		properties = null;
	}

	@Test
	public void validateFogbowRequirementsSyntaxSucessTest() {

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
	public void validateFogbowRequirementsSyntaxFailTest() {

		String fogbowRequirementA = "X (r =x) 1 && w = y";
		assertFalse(FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementA));

	}

	@Test
	public void matchesResourceSucessA(){
		
		String fogbowRequirements = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		
		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "9898";
		String userName = "user";
		String extraPorts = "";
		String cpuSize = "1";
		String menSize = "1024";
		String diskSize = "30";
		String location = "servers.your.domain";
		
		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);

		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirements));
		
	}

	@Test
	public void matchesResourceSucessB() {

		String fogbowRequirements = "Glue2vCPU >= 2 && Glue2RAM >= 2048 && Glue2disk >= 20 && Glue2CloudComputeManagerID ==\"servers.your.domain\"";

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "2";
		String menSize = "8192";
		String diskSize = "30";
		String location = "\"servers.your.domain\"";

		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);

		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirements));

	}

	@Test
	public void matchesResourceSucessC(){
		
		String fogbowRequirements = " ";
		
		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "9898";
		String userName = "user";
		String extraPorts = "";
		String cpuSize = "1";
		String menSize = "1024";
		String diskSize = "30";
		String location = "servers.your.domain";
		
		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);

		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);
		
		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirements));
		
	}
	
	@Test
	public void matchesResourceSucessVCpuOrMenSize() {

		String fogbowRequirementsB = "Glue2vCPU >= 2 || Glue2RAM >= 2048";

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "1";
		String menSize = "8192";
		String diskSize = "";
		String location = "";

		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertTrue(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));
	}

	@Test
	public void matchesResourceVcpuFail() {

		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "1";
		String menSize = "8192";
		String diskSize = "";
		String location = "";
		
		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}

	@Test
	public void matchesResourceVcpuFailOutOfRange() {

		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2vCPU <= 4 && Glue2RAM >= 2048";

		FogbowRequirementsHelper.validateFogbowRequirementsSyntax(fogbowRequirementsB);

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "5";// Out of range
		String menSize = "8192";
		String diskSize = "";
		String location = "";
		
		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}

	@Test
	public void matchesResourceMenSizeFail() {

		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048";

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "2";
		// Test low boundary value.
		String menSize = "2047";
		String diskSize = "";
		String location = "";
		
		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}

	@Test
	public void matchesResourceMenSizeFailOutOfRange() {

		String fogbowRequirementsB = "Glue2vCPU >= 2 && Glue2RAM >= 2048 && Glue2RAM <= 8192";

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "2";
		// Out of range - Test boundary value.
		String menSize = "8193";
		String diskSize = "";
		String location = "";
		
		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirementsB));

	}
	
	@Test
	public void matchesResourceDiskSizeFail() {

		String fogbowRequirements = "Glue2vCPU >= 2 && Glue2RAM >= 2048 && Glue2disk >= 40 && Glue2CloudComputeManagerID ==\"servers.your.domain\"";

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "2";
		String menSize = "8192";
		String diskSize = "39";
		String location = "servers.your.domain";

		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);

		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirements));

	}
	
	@Test
	public void matchesResourceLocationFail() {

		String fogbowRequirements = "Glue2vCPU >= 2 && Glue2RAM >= 2048 && Glue2disk >= 40 && Glue2CloudComputeManagerID ==\"servers.your.domainB\"";

		String requestId = "FakeRequestID1";
		String host = "100.10.1.1";
		String port = "8008";
		String userName = "userName";
		String extraPorts = "7060,8070";
		String cpuSize = "2";
		String menSize = "8192";
		String diskSize = "40";
		String location = "servers.your.domainA";

		Map<String, String> resourceMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, OrderType.ONE_TIME, spec.getImage(), spec.getPublicKey(), cpuSize, menSize, diskSize,
				location);

		suitableResource = TestResourceHelper.generateMockResource(requestId, resourceMetadata, true);

		assertFalse(FogbowRequirementsHelper.matches(suitableResource, fogbowRequirements));

	}

	private void generateDefaulProperties() {

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
