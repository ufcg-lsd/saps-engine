package org.fogbowcloud.scheduler.infrastructure;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.occi.request.RequestType;
import org.fogbowcloud.scheduler.core.CurrentThreadExecutorService;
import org.fogbowcloud.scheduler.core.DataStore;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Order.OrderState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.ssh.SshClientWrapper;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.util.DateUtils;
import org.fogbowcloud.scheduler.infrastructure.answer.ResourceReadyAnswer;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.google.gson.Gson;


public class TestInfrastructureManager {

	private final String DATASTORE_PATH = "src/test/resources/persistance/";
	private Scheduler schedulerMock;
	private InfrastructureProvider infrastructureProviderMock;
	private InfrastructureManager infrastructureManager;
	private Properties properties;
	private DataStore ds;
	private CurrentThreadExecutorService executorService;
	private SshClientWrapper sshMock;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	
	@Before
	public void setUp() throws Exception {
		
		//Initiating properties file.
		generateDefaulProperties();
		
		ds = mock(DataStore.class);
		schedulerMock = mock(Scheduler.class);
		infrastructureProviderMock = mock(InfrastructureProvider.class);
		sshMock = mock(SshClientWrapper.class);
		
		doReturn(true).when(ds).updateInfrastructureState(Mockito.anyList(), Mockito.anyList());
		
		executorService = new CurrentThreadExecutorService();
		
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true, infrastructureProviderMock, properties, executorService);
		infrastructureManager.cancelOrderTimer();
		infrastructureManager.cancelResourceTimer();
		infrastructureManager.setDataStore(ds);
		infrastructureManager.setInfraProvider(infrastructureProviderMock);
	}    
	

	@After
	public void setDown() throws Exception {
		
		infrastructureManager.stop();
		infrastructureManager = null;
		properties = null;
		schedulerMock = null;
		infrastructureProviderMock = null;
		executorService.shutdown();
		executorService = null;
	}
	
	@Test
	public void propertiesEmptyTest() throws Exception{
		
		exception.expect(Exception.class);
		
		properties = new Properties();
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true, infrastructureProviderMock, properties, executorService);

	}
	
	@Test
	public void propertiesWrongConnTimeoutTest() throws Exception{
		
		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true, infrastructureProviderMock, properties, executorService);

	}
	
	@Test
	public void propertiesWrongIdleLifetimeTest() throws Exception{
		
		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true, infrastructureProviderMock, properties, executorService);

	}
	
	@Test
	public void propertiesWrongOrderServiceTimeTest() throws Exception{
		
		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true, infrastructureProviderMock, properties, executorService);

	}

	@Test
	public void propertiesWrongResourceServiceTimeTest() throws Exception{
		
		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true, infrastructureProviderMock, properties, executorService);
	}
	
	//@Test
	public void startsInfraManagerWithInitialSpec() throws Exception{
		
		String initialSpecFile = "src/test/resources/Specs_Json";
		final String requestIdFake1 = "FakeRequestID1";
		final String requestIdFake2 = "FakeRequestID2";
		
		BufferedReader br = new BufferedReader(new FileReader(initialSpecFile));
		Gson gson = new Gson();
		final List<Specification> specifications = Arrays.asList(gson.fromJson(br, Specification[].class));

		//This test demands two initial specifications.
		if(specifications.size() < 2){
			fail();
		}

		Resource fakeResourceA = this.mountFakeResourceFromRequestId(requestIdFake1, specifications.get(0), true, RequestType.ONE_TIME);
		Resource fakeResourceB = this.mountFakeResourceFromRequestId(requestIdFake2, specifications.get(1), true, RequestType.ONE_TIME);
		
		doReturn(requestIdFake1).when(infrastructureProviderMock).requestResource(Mockito.eq(specifications.get(0)));
		doReturn(requestIdFake2).when(infrastructureProviderMock).requestResource(Mockito.eq(specifications.get(1))); 
		
		doReturn(fakeResourceA).when(infrastructureProviderMock).getResource(Mockito.eq(requestIdFake1));
		doReturn(fakeResourceB).when(infrastructureProviderMock).getResource(Mockito.eq(requestIdFake2));
		
		infrastructureManager = new InfrastructureManager(specifications, false, infrastructureProviderMock, properties, executorService);
		infrastructureManager.setDataStore(ds);
		infrastructureManager.start(true);
		infrastructureManager.cancelOrderTimer();
		infrastructureManager.cancelResourceTimer();
		assertEquals(specifications.size(), infrastructureManager.getIdleResources().size());
		
		infrastructureManager.stop();
		br.close();
	}

	@Test
	public void orderResourceTestSucess() throws Exception{

		String fakeRequestId = "requestId";

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		Resource fakeResource = mock(Resource.class);

		// Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.eq(specs));
		doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
		infrastructureManager.setInfraProvider(infrastructureProviderMock);

		validateOrderRequested(specs);
		assertEquals(OrderState.OPEN, infrastructureManager.getOrders().get(0).getState());

		infrastructureManager.getOrderService().run(); // resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run(); // resolving Ordered Orders (setting to Fulfilled)

		// Test allocated resource
		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());
		assertEquals(1, infrastructureManager.getOrders().size());

	}
	
	@Test
	public void releaseResourceSucess() throws Exception {
		
		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";
		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Resource fakeResource = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		doReturn(resourceAMetadata).when(fakeResource).getAllMetadata();
		doReturn( resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));

		//Creating mocks behaviors
		doReturn(dateMock).when(dateUtilsMock).currentTimeMillis();

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		validateReleaseResource(fakeResource);
		
		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock+lifetime), Long.valueOf(expirationTime));

	}
	
	@Test()
	public void orderResourceTestReuseResource() throws Exception {

		ResourceReadyAnswer rrAnswer = new ResourceReadyAnswer();
		
		String fakeRequestId = "requestId";
		String fogbowRequirement = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String IP_RESOURCE_A = "100.10.1.1";

		final Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specs.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);

		Resource fakeResourceA = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.PERSISTENT.getValue());
		resourceAMetadata.put(Resource.METADATA_SSH_HOST, IP_RESOURCE_A);
		resourceAMetadata.put(Resource.METADATA_IMAGE, specs.getImage());
		resourceAMetadata.put(Resource.METADATA_PUBLIC_KEY, specs.getPublicKey());
		resourceAMetadata.put(Resource.METADATA_VCPU, "1");
		resourceAMetadata.put(Resource.METADATA_MEN_SIZE, "1024");
		doReturn(false).when(fakeResourceA).checkConnectivity(Mockito.anyInt());
		doReturn(resourceAMetadata).when(fakeResourceA).getAllMetadata();
		doReturn(fakeRequestId).when(fakeResourceA).getId();
		doReturn(true).when(fakeResourceA).match(specs);
		doReturn( resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));
		doReturn( resourceAMetadata.get(Resource.METADATA_SSH_HOST)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_SSH_HOST));
		doReturn( resourceAMetadata.get(Resource.METADATA_IMAGE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_IMAGE));
		doReturn( resourceAMetadata.get(Resource.METADATA_PUBLIC_KEY)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_PUBLIC_KEY));
		doReturn( resourceAMetadata.get(Resource.METADATA_VCPU)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_VCPU));
		doReturn( resourceAMetadata.get(Resource.METADATA_MEN_SIZE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_MEN_SIZE));
		
		doReturn(true).when(fakeResourceA).checkConnectivity(Mockito.anyInt());
		doAnswer(rrAnswer).when(schedulerMock).resourceReady(Mockito.any(Resource.class));
		
		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.getIdleResourcesMap().put(fakeResourceA, new Long(0));

		infrastructureManager.orderResource(specs, schedulerMock);
			// resolving new Open Orders
		infrastructureManager.getOrderService().run();
		
		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());
		// Last resource read must be the old resource.
		Resource secondResourceOrdered = rrAnswer.getResourceReady(); 
		assertEquals(fakeResourceA.getId(), secondResourceOrdered.getId());

	}
	
	@Test()
	public void orderResourceTestReuseResourcePersistent() throws Exception {

		String fakeRequestId = "requestId";
		String fogbowRequirement = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String IP_RESOURCE_A = "100.10.1.1";
		String IP_RESOURCE_B = "100.10.1.10";
		String cpuSizeA = "1";
		String menSizeA = "1024";

		final Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specs.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);

		Resource fakeResourceA = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.PERSISTENT.getValue());
		resourceAMetadata.put(Resource.METADATA_SSH_HOST, IP_RESOURCE_A);
		resourceAMetadata.put(Resource.METADATA_IMAGE, specs.getImage());
		resourceAMetadata.put(Resource.METADATA_PUBLIC_KEY, specs.getPublicKey());
		resourceAMetadata.put(Resource.METADATA_VCPU, cpuSizeA);
		resourceAMetadata.put(Resource.METADATA_MEN_SIZE, menSizeA);
		doReturn(false).when(fakeResourceA).checkConnectivity(Mockito.anyInt());
		doReturn(resourceAMetadata).when(fakeResourceA).getAllMetadata();
		doReturn(fakeRequestId).when(fakeResourceA).getId();
		doReturn( resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));
		doReturn( resourceAMetadata.get(Resource.METADATA_SSH_HOST)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_SSH_HOST));
		doReturn( resourceAMetadata.get(Resource.METADATA_IMAGE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_IMAGE));
		doReturn( resourceAMetadata.get(Resource.METADATA_PUBLIC_KEY)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_PUBLIC_KEY));
		doReturn( resourceAMetadata.get(Resource.METADATA_VCPU)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_VCPU));
		doReturn( resourceAMetadata.get(Resource.METADATA_MEN_SIZE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_MEN_SIZE));
		doReturn(true).when(fakeResourceA).match(Mockito.eq(specs));
		
		final Resource fakeResourceB = mock(Resource.class);
		Map<String, String> resourceBMetadata = new HashMap<String, String>();
		resourceBMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.PERSISTENT.getValue());
		resourceBMetadata.put(Resource.METADATA_SSH_HOST, IP_RESOURCE_B);
		resourceBMetadata.put(Resource.METADATA_IMAGE, specs.getImage());
		resourceBMetadata.put(Resource.METADATA_PUBLIC_KEY, specs.getPublicKey());
		resourceBMetadata.put(Resource.METADATA_VCPU, cpuSizeA);
		resourceBMetadata.put(Resource.METADATA_MEN_SIZE, menSizeA);
		doReturn(true).when(fakeResourceB).checkConnectivity(Mockito.anyInt());
		doReturn(resourceBMetadata).when(fakeResourceB).getAllMetadata();
		doReturn(fakeRequestId).when(fakeResourceB).getId();
		doReturn( resourceBMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));
		doReturn( resourceBMetadata.get(Resource.METADATA_SSH_HOST)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_SSH_HOST));
		doReturn( resourceBMetadata.get(Resource.METADATA_IMAGE)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_IMAGE));
		doReturn( resourceBMetadata.get(Resource.METADATA_PUBLIC_KEY)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_PUBLIC_KEY));
		doReturn( resourceBMetadata.get(Resource.METADATA_VCPU)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_VCPU));
		doReturn( resourceBMetadata.get(Resource.METADATA_MEN_SIZE)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_MEN_SIZE));
		doReturn(true).when(fakeResourceB).match(Mockito.eq(specs));
		doNothing().when(fakeResourceA).copyInformations(fakeResourceB);
		doReturn(fakeResourceB).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		
		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.getIdleResourcesMap().put(fakeResourceA, new Long(0));

		infrastructureManager.orderResource(specs, schedulerMock);
			// resolving new Open Orders
		infrastructureManager.getOrderService().run();
		
		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());
		// Last resource read must be the old resource.
		
		verify(infrastructureProviderMock).getResource(fakeRequestId);
		verify(fakeResourceA).copyInformations(fakeResourceB);
	}

	@Test
	public void orderResourceTestNotReuseResource() throws Exception {

		ResourceReadyAnswer rrAnswer = new ResourceReadyAnswer();

		final String fakeRequestIdA = "requestIdA";
		final String fakeRequestIdB = "requestIdB";
		String fogbowRequirementA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementB = "Glue2vCPU >= 1 && Glue2RAM >= 2048";

		final Specification specsA = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specsA.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementA);

		final Specification specsB = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specsB.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementB);
		
		Resource fakeResourceA = this.mountFakeResourceFromRequestId(fakeRequestIdA, specsA, true, RequestType.ONE_TIME);
		Resource fakeResourceB = this.mountFakeResourceFromRequestId(fakeRequestIdB, specsB, true, RequestType.ONE_TIME);

		//Creating mocks behaviors
		doReturn(fakeRequestIdA).when(infrastructureProviderMock).requestResource(Mockito.eq(specsA));
		doReturn(fakeRequestIdB).when(infrastructureProviderMock).requestResource(Mockito.eq(specsB));

		doReturn(fakeResourceA).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestIdA));
		doReturn(fakeResourceB).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestIdB));

		doAnswer(rrAnswer).when(schedulerMock).resourceReady(Mockito.any(Resource.class));

		infrastructureManager.setInfraProvider(infrastructureProviderMock);

		validateOrderRequested(specsA);

		//Waits InfrastructureManager process order.
		infrastructureManager.getOrderService().run(); // resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run(); // resolving Ordered Orders (setting to Fulfilled)

		//Test allocated resource
		assertEquals(1, infrastructureManager.getAllocatedResources().size());

		Resource firstResourceOrdered = rrAnswer.getResourceReady(); //IntaceId from the Last resource ready.
		validateReleaseResource(firstResourceOrdered);

		infrastructureManager.orderResource(specsB, schedulerMock);
		infrastructureManager.getOrderService().run(); // resolving new Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run(); // resolving Ordered Orders (setting to Fulfilled)

		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());

		Resource secondResourceOrdered = rrAnswer.getResourceReady(); //Last resource read must be the old resource.
		assertNotEquals(firstResourceOrdered.getId(), secondResourceOrdered.getId());


	}
	
	@Test
	public void retryPersistentResourceFiled() throws Exception {
		
		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";

		String IP_RESOURCE_A = "100.10.1.1";
		String IP_RESOURCE_B = "100.10.1.10";
		String cpuSizeA = "1";
		String menSizeA = "1024";

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		
		Resource fakeResourceA = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.PERSISTENT.getValue());
		resourceAMetadata.put(Resource.METADATA_SSH_HOST, IP_RESOURCE_A);
		resourceAMetadata.put(Resource.METADATA_IMAGE, specs.getImage());
		resourceAMetadata.put(Resource.METADATA_PUBLIC_KEY, specs.getPublicKey());
		resourceAMetadata.put(Resource.METADATA_VCPU, cpuSizeA);
		resourceAMetadata.put(Resource.METADATA_MEN_SIZE, menSizeA);
		doReturn(false).when(fakeResourceA).checkConnectivity(Mockito.anyInt());
		doReturn(resourceAMetadata).when(fakeResourceA).getAllMetadata();
		doReturn(fakeRequestId).when(fakeResourceA).getId();
		doReturn( resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));
		doReturn( resourceAMetadata.get(Resource.METADATA_SSH_HOST)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_SSH_HOST));
		doReturn( resourceAMetadata.get(Resource.METADATA_IMAGE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_IMAGE));
		doReturn( resourceAMetadata.get(Resource.METADATA_PUBLIC_KEY)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_PUBLIC_KEY));
		doReturn( resourceAMetadata.get(Resource.METADATA_VCPU)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_VCPU));
		doReturn( resourceAMetadata.get(Resource.METADATA_MEN_SIZE)).when(fakeResourceA).getMetadataValue(Mockito.eq(Resource.METADATA_MEN_SIZE));
		
		final Resource fakeResourceB = mock(Resource.class);
		Map<String, String> resourceBMetadata = new HashMap<String, String>();
		resourceBMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.PERSISTENT.getValue());
		resourceBMetadata.put(Resource.METADATA_SSH_HOST, IP_RESOURCE_B);
		resourceBMetadata.put(Resource.METADATA_IMAGE, specs.getImage());
		resourceBMetadata.put(Resource.METADATA_PUBLIC_KEY, specs.getPublicKey());
		resourceBMetadata.put(Resource.METADATA_VCPU, cpuSizeA);
		resourceBMetadata.put(Resource.METADATA_MEN_SIZE, menSizeA);
		doReturn(true).when(fakeResourceB).checkConnectivity(Mockito.anyInt());
		doReturn(resourceBMetadata).when(fakeResourceB).getAllMetadata();
		doReturn(fakeRequestId).when(fakeResourceB).getId();
		doReturn( resourceBMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));
		doReturn( resourceBMetadata.get(Resource.METADATA_SSH_HOST)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_SSH_HOST));
		doReturn( resourceBMetadata.get(Resource.METADATA_IMAGE)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_IMAGE));
		doReturn( resourceBMetadata.get(Resource.METADATA_PUBLIC_KEY)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_PUBLIC_KEY));
		doReturn( resourceBMetadata.get(Resource.METADATA_VCPU)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_VCPU));
		doReturn( resourceBMetadata.get(Resource.METADATA_MEN_SIZE)).when(fakeResourceB).getMetadataValue(Mockito.eq(Resource.METADATA_MEN_SIZE));
		
		doNothing().when(fakeResourceA).copyInformations(fakeResourceB);
		//Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class)); //Return for new Instance's request 
		doReturn(fakeResourceB).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		infrastructureManager.getIdleResourcesMap().put(fakeResourceA, new Long(0));

		infrastructureManager.getInfraIntegrityService().run();
		
		verify(infrastructureProviderMock).getResource(fakeResourceA.getId());
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		verify(fakeResourceA).copyInformations(fakeResourceB);
	}
	

	@Test
	public void deleteResourceDueExpirationTime() throws Exception {
		
		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";
		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		
		Resource fakeResource = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		doReturn(resourceAMetadata).when(fakeResource).getAllMetadata();
		doReturn( resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));

		//Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class));  
		doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
		doReturn(dateMock).when(dateUtilsMock).currentTimeMillis();

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		infrastructureManager.orderResource(specs, schedulerMock);

		// resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run();
		// resolving Ordered Orders (setting to Fulfilled)
		infrastructureManager.getOrderService().run();

		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());

		validateReleaseResource(fakeResource);
		
		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock+lifetime), Long.valueOf(expirationTime));
		
		//"advancing time to simulate future monitor of the idle resource.
		doReturn(dateMock+(lifetime*2)).when(dateUtilsMock).currentTimeMillis();
		infrastructureManager.getInfraIntegrityService().run();
		
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());

	}
	
	@Test
	public void deleteResourceDueConectionFailed() throws Exception {
		
		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";
		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		Resource fakeResource = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		doReturn(resourceAMetadata).when(fakeResource).getAllMetadata();
		doReturn( resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource).getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));

		//Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class)); //Return for new Instance's request 
		doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
		doReturn(dateMock).when(dateUtilsMock).currentTimeMillis();

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		infrastructureManager.orderResource(specs, schedulerMock);

		//Waits InfrastructureManager process order.
		 // resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run();
		// resolving Ordered Orders (setting to Fulfilled)
		infrastructureManager.getOrderService().run(); 

		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());

		validateReleaseResource(fakeResource);
		
		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock+lifetime), Long.valueOf(expirationTime));
		
		doReturn(false).when(fakeResource).checkConnectivity(Mockito.anyInt());
		infrastructureManager.getInfraIntegrityService().run();
		
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());

	}
	
	@Test
	public void removePreviousResourcesTest() throws Exception{
	
		final String requestIdFake1 = "FakeRequestID1";
		final String requestIdFake2 = "FakeRequestID2";
		
		List<String> requestIdsPrevious = new ArrayList<String>();
		requestIdsPrevious.add(requestIdFake1);
		requestIdsPrevious.add(requestIdFake2);
		
		doReturn(requestIdsPrevious).when(ds).getRequesId();
		infrastructureManager.start(true);
		
		verify(infrastructureProviderMock).deleteResource(requestIdFake1);
		verify(infrastructureProviderMock).deleteResource(requestIdFake2);
		
		infrastructureManager.stop();
		
	}
	
	private void validateOrderRequested(final Specification specs) {
		assertEquals(0, infrastructureManager.getOrders().size());
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());
		infrastructureManager.orderResource(specs, schedulerMock);
		assertEquals(1, infrastructureManager.getOrders().size());
	}
	
	private void validateReleaseResource(Resource firstResourceOrdered) {
		infrastructureManager.releaseResource(firstResourceOrdered);

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(0, infrastructureManager.getOrders().size());
	}
	
	private Resource mountFakeResourceFromRequestId(final String requestId,
			final Specification specification, boolean connectivity, RequestType requestType) {
		
		Resource fakeResource = spy(new Resource(requestId, properties));
		
		fakeResource.putMetadata(Resource.METADATA_SSH_HOST, "100.10.1.1");
		fakeResource.putMetadata(Resource.METADATA_SSH_PORT, "9898");
		fakeResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "user");
		fakeResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "");
		fakeResource.putMetadata(Resource.METADATA_REQUEST_TYPE, requestType.getValue());
		fakeResource.putMetadata(Resource.METADATA_IMAGE, specification.getImage());
		fakeResource.putMetadata(Resource.METADATA_PUBLIC_KEY, specification.getPublicKey());
		fakeResource.putMetadata(Resource.METADATA_VCPU, "1");
		fakeResource.putMetadata(Resource.METADATA_MEN_SIZE, "1024");
		// fakeResource.putMetadata(Resource.METADATA_DISK_SIZE,
		// instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE)); //TODO
		// Descomentar quando o fogbow estiver retornando este atributo
		// fakeResource.putMetadata(Resource.METADATA_LOCATION,
		// instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMBER_ID)); //TODO
		// Descomentar quando o fogbow estiver retornando este atributo
		doReturn(connectivity).when(fakeResource).checkConnectivity(Mockito.anyInt());
		return fakeResource;
	}
	
	private void generateDefaulProperties(){
	
		properties = new Properties();
		
		properties.setProperty(AppPropertiesConstants.INFRA_IS_STATIC, "false");
		properties.setProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME,
				"org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowInfrastructureProvider");
		properties.setProperty(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME, "2000");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME, "2000");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "10000");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "300000");
		properties.setProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH, "src/test/resources/Specs_Json");
		properties.setProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING, "false");
		properties.setProperty(AppPropertiesConstants.INFRA_FOGBOW_MANAGER_BASE_URL, "100_02_01_01:8098");
		properties.setProperty(AppPropertiesConstants.INFRA_FOGBOW_TOKEN_PUBLIC_KEY_FILEPATH,
				"src/test/resources/publickey_file");
		properties.put("accounting_datastore_url",
				"jdbc:h2:mem:" + new File(DATASTORE_PATH).getAbsolutePath() + "orders");
		
	}
}
