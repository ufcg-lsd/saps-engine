package org.fogbowcloud.scheduler.infrastructure.fogbow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestState;
import org.fogbowcloud.scheduler.core.http.HttpWrapper;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;


public class TestFogbowInfrastructureProvider {
	
	//150.65.15.81 8182 - URL fogbow
	
	private final String FILE_RESPONSE_NO_INSTANCE_ID = "src/test/resources/requestInfoWithoutInstanceId";
	private final String FILE_RESPONSE_INSTANCE_ID = "src/test/resources/requestInfoWithInstanceId";
	private final String FILE_RESPONSE_NO_SSH = "src/test/resources/instanceInfoWithoutSshInfo";
	private final String FILE_RESPONSE_SSH = "src/test/resources/instanceInfoWithSshInfo";
	private final String FILE_RESPONSE_REQUEST_INSTANCE = "src/test/resources/requestId";
	

	private static final String PRIVATE_KEY = "src/test/resources/privatekey";
	private static final String PUBLIC_KEY = "src/test/resources/publickey";

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	private FogbowInfrastructureProvider fogbowInfrastructureProvider; 
	private HttpWrapper httpWrapperMock;
	private Properties properties;
	
	@Before
	public void setUp() throws Exception {
		
		//Initiating properties file.
		this.generateDefaulProperties();
		
		httpWrapperMock = mock(HttpWrapper.class);
		Date date = new Date(System.currentTimeMillis() + (long)Math.pow(10,9));
		fogbowInfrastructureProvider = spy(new FogbowInfrastructureProvider(properties));
	}    

	@After
	public void setDown() throws Exception {
		httpWrapperMock = null;
		fogbowInfrastructureProvider = null;
	}

	@Test
	public void requestResourceGetRequestIdTestSucess(){
		
		String requestIdMokc = "request01";
		
		try {

			//Create Mock behavior for httpWrapperMock
			createDefaultRequestResponse(requestIdMokc);

			Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");

			fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);

			String requestIdReturned = fogbowInfrastructureProvider.requestResource(specs);
			assertEquals(requestIdMokc, requestIdReturned);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

	@Test
	public void getResourceTestSucess() throws Exception{
		
		//Attributes
		String requestIdMock = "request01";
		
		Resource resource = mock(Resource.class);
		doReturn(requestIdMock).when(resource).getId();

		//To avoid SSH Connection Erro when tries to test connection to a FAKE host.
		doReturn(resource).when(fogbowInfrastructureProvider).getFogbowResource(Mockito.eq(requestIdMock));
		doReturn(true).when(resource).checkConnectivity(); 

		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);

		Resource newResource = fogbowInfrastructureProvider.getResource(requestIdMock);

		assertNotNull(newResource);
		assertEquals(requestIdMock, newResource.getId());

	}

	@Test
	public void getFogbowResourceTestSucess() throws Exception{
		
		//Attributes
		String requestIdMock = "request01";
		String instanceIdMock = "instance01";
		String memSizeMock = "1.0";
		String menSizeFormated = String.valueOf(Float.parseFloat(memSizeMock)*1024);
		String coreSizeMock = "1";
		String hostMock = "10.0.1.10";
		String portMock = "8989";
		String memberIdMock = "member01";

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");

		//Create Mock behavior for httpWrapperMock
		//Creating response for request for resource.
		createDefaultRequestResponse(requestIdMock);
		//Creating response for request for Instance ID
		createDefaulInstanceIdResponse(requestIdMock, instanceIdMock, memberIdMock, RequestState.FULFILLED);
		//Creating response for request for Instance Attributes
		createDefaulInstanceAttributesResponse(requestIdMock, instanceIdMock, memSizeMock, coreSizeMock, hostMock, portMock);
		
		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);
		fogbowInfrastructureProvider.requestResource(specs);

		Resource newResource = fogbowInfrastructureProvider.getFogbowResource(requestIdMock);

		assertEquals(requestIdMock, newResource.getId());
		assertEquals(menSizeFormated, newResource.getMetadataValue(Resource.METADATA_MEN_SIZE));
		assertEquals(coreSizeMock, newResource.getMetadataValue(Resource.METADATA_VCPU));
		assertEquals(hostMock, newResource.getMetadataValue(Resource.METADATA_SSH_HOST));
		assertEquals(portMock, newResource.getMetadataValue(Resource.METADATA_SSH_PORT));
		
		newResource = null;
	}
	
	@Test
	public void getFogbowResourceTestSucessB() throws Exception{
		
		//Attributes
		String requestIdMock = "request01";
		String instanceIdMock = "instance01";
		String memSizeMock = "3.0";
		String menSizeFormated = String.valueOf(Float.parseFloat(memSizeMock)*1024);
		String coreSizeMock = "1";
		String hostMock = "10.0.1.10";
		String portMock = "8989";
		String memberIdMock = "member01";
		
		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");

		//Create Mock behavior for httpWrapperMock
		//Creating response for request for resource.
		createDefaultRequestResponse(requestIdMock);
		//Creating response for request for Instance ID
		createDefaulInstanceIdResponse(requestIdMock, instanceIdMock, memberIdMock, RequestState.FULFILLED);
		//Creating response for request for Instance Attributes
		createDefaulInstanceAttributesResponse(requestIdMock, instanceIdMock, memSizeMock, coreSizeMock, hostMock, portMock);
		
		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);
		fogbowInfrastructureProvider.requestResource(specs);

		Resource newResource = fogbowInfrastructureProvider.getFogbowResource(requestIdMock);

		assertEquals("\""+memberIdMock+"\"", newResource.getMetadataValue(Resource.METADATA_LOCATION));
		assertEquals(menSizeFormated, newResource.getMetadataValue(Resource.METADATA_MEN_SIZE));
		assertEquals(coreSizeMock, newResource.getMetadataValue(Resource.METADATA_VCPU));
		assertEquals(hostMock, newResource.getMetadataValue(Resource.METADATA_SSH_HOST));
		assertEquals(portMock, newResource.getMetadataValue(Resource.METADATA_SSH_PORT));
	}
	
	@Test
	public void getResourceTestNoInstanceId() throws Exception{
		
		//Attributes
		String requestIdMock = "request01";
		String instanceIdMock = "instance01";
		String memSizeMock = "1.0";
		String coreSizeMock = "1";
		String hostMock = "10.0.1.10";
		String portMock = "8989";
		String memberIdMock = "member01";


		//Create Mock behavior for httpWrapperMock
		//Creating response for request for resource.
		createDefaultRequestResponse(requestIdMock);
		//Creating response for request for Instance ID
		createDefaulInstanceIdResponse(requestIdMock, instanceIdMock, memberIdMock, RequestState.FAILED);
		
		
		
		createDefaulInstanceAttributesResponseNoShh(requestIdMock, instanceIdMock, memSizeMock, coreSizeMock);

		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);

		Resource newResource = fogbowInfrastructureProvider.getFogbowResource(requestIdMock);

		assertNull(newResource);
			
	}
	
	@Test
	public void getResourceTestNotFulfiled() throws Exception{
		
		//Attributes
		String requestIdMock = "request01";
		String instanceIdMock = "instance01";
		String memSizeMock = "1.0";
		String coreSizeMock = "1";
		String hostMock = "10.0.1.10";
		String portMock = "8989";

		//Create Mock behavior for httpWrapperMock
		//Creating response for request for resource.
		createDefaultRequestResponse(requestIdMock);
		//Creating response for request for Instance ID
		createDefaulRequestInstanceIdResponseNoId(requestIdMock);
		//Creating response for request for Instance Attributes
		createDefaulInstanceAttributesResponse(requestIdMock, instanceIdMock, memSizeMock, coreSizeMock, hostMock, portMock);

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");

		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);
		fogbowInfrastructureProvider.requestResource(specs);

		Resource newResource = fogbowInfrastructureProvider.getFogbowResource(requestIdMock);

		assertNull(newResource);
			
	}
	
	@Test
	public void getResourceTestNoSShInformation() throws Exception{
		
		//Attributes
		String requestIdMock = "request01";
		String instanceIdMock = "instance01";
		String memSizeMock = "1.0";
		String coreSizeMock = "1";
		String memberIdMock = "member01";

		//Create Mock behavior for httpWrapperMock
		//Creating response for request for resource.
		createDefaultRequestResponse(requestIdMock);
		//Creating response for request for Instance ID
		createDefaulInstanceIdResponse(requestIdMock, instanceIdMock, memberIdMock, RequestState.FULFILLED);
		//Creating response for request for Instance Attributes
		createDefaulInstanceAttributesResponseNoShh(requestIdMock, instanceIdMock, memSizeMock, coreSizeMock);

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");

		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);
		fogbowInfrastructureProvider.requestResource(specs);

		Resource newResource = fogbowInfrastructureProvider.getFogbowResource(requestIdMock);

		assertNull(newResource);
			
	}
	

	@Test
	public void deleteResourceTestSucess() throws Exception{
		
		String requestIdMock = "requestId";
		String instanceIdMock = "instance01";
		String memberIdMock = "member01";
		String urlEndpointInstanceDelete = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL)
				+ "/compute/" + instanceIdMock;
		
		Resource resource = mock(Resource.class);
		doReturn(requestIdMock).when(resource).getId();
		createDefaulInstanceIdResponse(requestIdMock, instanceIdMock, memberIdMock, RequestState.FULFILLED);

		doReturn("OK").when(httpWrapperMock).doRequest(Mockito.eq("delete"), Mockito.eq(urlEndpointInstanceDelete), 
				Mockito.any(String.class), Mockito.any(List.class));

		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);
		fogbowInfrastructureProvider.deleteResource(resource.getId());
			
	}
	
	@Test
	public void deleteResourceTestFail() throws Exception {
		
		exception.expect(InfrastructureException.class);
		
		String requestIdMock = "requestId";
		String instanceIdMock = "instance01";
		String memberIdMock = "member01";
		String urlEndpointInstanceDelete = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL)
				+ "/compute/" + instanceIdMock;
		
		Resource resource = mock(Resource.class);
		doReturn(requestIdMock).when(resource).getId();
		createDefaulInstanceIdResponse(requestIdMock, instanceIdMock, memberIdMock, RequestState.FULFILLED);

		doThrow(new Exception("Erro on request.")).when(httpWrapperMock).doRequest(Mockito.eq("delete"), Mockito.eq(urlEndpointInstanceDelete), 
				Mockito.any(String.class), Mockito.any(List.class));

		fogbowInfrastructureProvider.setHttpWrapper(httpWrapperMock);
		fogbowInfrastructureProvider.deleteResource(resource.getId());
			
	}
	
	// ---- HELPER METHODS ---- //
	
	private void createDefaultRequestResponse(String requestIdMokc) 
			throws FileNotFoundException, IOException, Exception {
		
		String urlEndpointNewInstance = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL)
				+ "/" + RequestConstants.TERM;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(FogbowInfrastructureTestUtils.REQUEST_ID_TAG, requestIdMokc);
		String fogbowResponse = FogbowInfrastructureTestUtils.createHttpWrapperResponseFromFile(FILE_RESPONSE_REQUEST_INSTANCE, params);

		doReturn(fogbowResponse).when(httpWrapperMock).doRequest(Mockito.any(String.class), Mockito.eq(urlEndpointNewInstance), 
				Mockito.any(String.class), Mockito.any(List.class));
	}
	
	private void createDefaulInstanceIdResponse(String requestIdMock, String instanceIdMock, String location, RequestState requestState) 
			throws FileNotFoundException, IOException, Exception {
		
		String urlEndpointRequestInformations = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL)
				+ "/" + RequestConstants.TERM + "/"+ requestIdMock;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(FogbowInfrastructureTestUtils.REQUEST_ID_TAG, requestIdMock);
		params.put(FogbowInfrastructureTestUtils.INSTANCE_TAG, instanceIdMock);
		params.put(FogbowInfrastructureTestUtils.PROVIDER_MEMBER_TAG, location);
		params.put(FogbowInfrastructureTestUtils.STATE_TAG, requestState.getValue());
		String fogbowResponse = FogbowInfrastructureTestUtils.createHttpWrapperResponseFromFile(FILE_RESPONSE_INSTANCE_ID, params);
		
		doReturn(fogbowResponse).when(httpWrapperMock).doRequest(Mockito.any(String.class), Mockito.eq(urlEndpointRequestInformations), 
				Mockito.any(String.class), Mockito.any(List.class));
	}
	
	private void createDefaulRequestInstanceIdResponseNoId(String requestIdMock) 
			throws FileNotFoundException, IOException, Exception {
		
		String urlEndpointRequestInformations = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL)
				+ "/" + RequestConstants.TERM + "/"+ requestIdMock;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(FogbowInfrastructureTestUtils.REQUEST_ID_TAG, requestIdMock);
		String fogbowResponse = FogbowInfrastructureTestUtils.createHttpWrapperResponseFromFile(FILE_RESPONSE_NO_INSTANCE_ID, params);
		
		doReturn(fogbowResponse).when(httpWrapperMock).doRequest(Mockito.any(String.class), Mockito.eq(urlEndpointRequestInformations), 
				Mockito.any(String.class), Mockito.any(List.class));
	}
	
	private void createDefaulInstanceAttributesResponse(String requestIdMock, String instanceIdMock,
			String memSizeMock, String coreSizeMock, String hostMock, String portMock)
					throws FileNotFoundException, IOException, Exception {
		
		String urlEndpointInstanceAttributes = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL)
				+ "/compute/" + instanceIdMock;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(FogbowInfrastructureTestUtils.REQUEST_ID_TAG, requestIdMock);
		params.put(FogbowInfrastructureTestUtils.INSTANCE_TAG, instanceIdMock);
		params.put(FogbowInfrastructureTestUtils.MEN_SIZE_TAG, memSizeMock);
		params.put(FogbowInfrastructureTestUtils.CORE_SIZE_TAG, coreSizeMock);
		params.put(FogbowInfrastructureTestUtils.HOST_TAG, hostMock);
		params.put(FogbowInfrastructureTestUtils.PORT_TAG, portMock);
		
		String fogbowResponse = FogbowInfrastructureTestUtils.createHttpWrapperResponseFromFile(FILE_RESPONSE_SSH, params);
		
		doReturn(fogbowResponse).when(httpWrapperMock).doRequest(Mockito.any(String.class), Mockito.eq(urlEndpointInstanceAttributes), 
				Mockito.any(String.class), Mockito.any(List.class));
	}
	
	private void createDefaulInstanceAttributesResponseNoShh(String requestIdMock, String instanceIdMock,
			String memSizeMock, String coreSizeMock)
					throws FileNotFoundException, IOException, Exception {
		
		String urlEndpointInstanceAttributes = properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL)
				+ "/compute/" + instanceIdMock;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(FogbowInfrastructureTestUtils.REQUEST_ID_TAG, requestIdMock);
		params.put(FogbowInfrastructureTestUtils.INSTANCE_TAG, instanceIdMock);
		params.put(FogbowInfrastructureTestUtils.MEN_SIZE_TAG, memSizeMock);
		params.put(FogbowInfrastructureTestUtils.CORE_SIZE_TAG, coreSizeMock);
		
		String fogbowResponse = FogbowInfrastructureTestUtils.createHttpWrapperResponseFromFile(FILE_RESPONSE_NO_SSH, params);
		
		doReturn(fogbowResponse).when(httpWrapperMock).doRequest(Mockito.any(String.class), Mockito.eq(urlEndpointInstanceAttributes), 
				Mockito.any(String.class), Mockito.any(List.class));
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
