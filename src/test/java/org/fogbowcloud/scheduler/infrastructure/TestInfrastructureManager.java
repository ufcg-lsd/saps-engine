package org.fogbowcloud.scheduler.infrastructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.fogbowcloud.scheduler.core.model.Order;
import org.fogbowcloud.scheduler.core.model.Order.OrderState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.TestResourceHelper;
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

	private String DATASTORE_PATH = "src/test/resources/persistance/";
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

		// Initiating properties file.
		generateDefaulProperties();

		ds = mock(DataStore.class);
		schedulerMock = mock(Scheduler.class);
		infrastructureProviderMock = mock(InfrastructureProvider.class);
		sshMock = mock(SshClientWrapper.class);

		doReturn(true).when(ds).updateInfrastructureState(Mockito.anyList(), Mockito.anyList());

		executorService = new CurrentThreadExecutorService();

		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true,
				infrastructureProviderMock, properties, executorService);
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
	public void propertiesEmptyTest() throws Exception {

		exception.expect(Exception.class);

		properties = new Properties();
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true,
				infrastructureProviderMock, properties, executorService);

	}

	@Test
	public void propertiesWrongConnTimeoutTest() throws Exception {

		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true,
				infrastructureProviderMock, properties, executorService);

	}

	@Test
	public void propertiesWrongIdleLifetimeTest() throws Exception {

		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true,
				infrastructureProviderMock, properties, executorService);

	}

	@Test
	public void propertiesWrongOrderServiceTimeTest() throws Exception {

		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true,
				infrastructureProviderMock, properties, executorService);

	}

	@Test
	public void propertiesWrongResourceServiceTimeTest() throws Exception {

		exception.expect(Exception.class);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME, "AB");
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), true,
				infrastructureProviderMock, properties, executorService);
	}

	@Test
	public void createWithNoInitialSpecsAndNonElastic() throws Exception {

		exception.expect(Exception.class);
		infrastructureManager = new InfrastructureManager(new ArrayList<Specification>(), false,
				infrastructureProviderMock, properties, executorService);
	}

	@Test
	public void startsInfraManagerWithInitialSpec() throws Exception {

		String initialSpecFile = "src/test/resources/Specs_Json";
		String requestIdFake1 = "FakeRequestID1";
		String requestIdFake2 = "FakeRequestID2";
		String host = "100.10.1.1";
		String port = "9898";
		String userName = "user";
		String extraPorts = "";
		String cpuSize = "1";
		String menSize = "1024";
		String diskSize = "";
		String location = "";

		BufferedReader br = new BufferedReader(new FileReader(initialSpecFile));
		Gson gson = new Gson();
		List<Specification> specifications = Arrays.asList(gson.fromJson(br, Specification[].class));

		// This test demands two initial specifications.
		if (specifications.size() < 2) {
			fail();
		}

		Map<String, String> resourceMetadataA = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, RequestType.ONE_TIME, specifications.get(0).getImage(),
				specifications.get(0).getPublicKey(), cpuSize, menSize, diskSize, location);
		Resource fakeResourceA = TestResourceHelper.generateMockResource(requestIdFake1, resourceMetadataA, true);

		Map<String, String> resourceMetadataB = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, RequestType.ONE_TIME, specifications.get(1).getImage(),
				specifications.get(1).getPublicKey(), cpuSize, menSize, diskSize, location);
		Resource fakeResourceB = TestResourceHelper.generateMockResource(requestIdFake2, resourceMetadataB, true);

		doReturn(requestIdFake1).when(infrastructureProviderMock).requestResource(Mockito.eq(specifications.get(0)));
		doReturn(requestIdFake2).when(infrastructureProviderMock).requestResource(Mockito.eq(specifications.get(1)));

		doReturn(fakeResourceA).when(infrastructureProviderMock).getResource(Mockito.eq(requestIdFake1));
		doReturn(fakeResourceB).when(infrastructureProviderMock).getResource(Mockito.eq(requestIdFake2));

		infrastructureManager = new InfrastructureManager(specifications, false, infrastructureProviderMock, properties,
				executorService);
		infrastructureManager.setDataStore(ds);
		infrastructureManager.start(true);
		infrastructureManager.cancelOrderTimer();
		infrastructureManager.cancelResourceTimer();
		assertEquals(specifications.size(), infrastructureManager.getIdleResources().size());

		infrastructureManager.stop();
		br.close();
	}

	@Test
	public void orderResourceTestSucess() throws Exception {

		String fakeRequestId = "requestId";

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		Resource fakeResource = mock(Resource.class);

		// Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.eq(specs));
		doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
		infrastructureManager.setInfraProvider(infrastructureProviderMock);

		validateOrderRequested(specs);
		assertEquals(OrderState.ORDERED, infrastructureManager.getOrders().get(0).getState());

		// resolving Ordered Orders (setting to Fulfilled)
		infrastructureManager.getOrderService().run(); 

		// Test allocated resource
		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());
		assertEquals(1, infrastructureManager.getOrders().size());

	}

	@Test
	public void releaseResourceSucess() throws Exception {

		DateUtils dateUtilsMock = mock(DateUtils.class);

		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Resource fakeResource = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.ONE_TIME.getValue());
		doReturn(resourceAMetadata).when(fakeResource).getAllMetadata();
		doReturn(resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource)
				.getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));

		// Creating mocks behaviors
		doReturn(dateMock).when(dateUtilsMock).currentTimeMillis();

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		validateReleaseResource(fakeResource);

		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock + lifetime), expirationTime);

	}

	@Test
	public void movePersistentResourceToIdle() throws Exception {

		DateUtils dateUtilsMock = mock(DateUtils.class);

		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Resource fakeResource = mock(Resource.class);
		Map<String, String> resourceAMetadata = new HashMap<String, String>();
		resourceAMetadata.put(Resource.METADATA_REQUEST_TYPE, RequestType.PERSISTENT.getValue());
		doReturn(resourceAMetadata).when(fakeResource).getAllMetadata();
		doReturn(resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource)
				.getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));

		// Creating mocks behaviors
		doReturn(dateMock).when(dateUtilsMock).currentTimeMillis();

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		infrastructureManager.moveResourceToIdle(fakeResource);

		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(new Long(0), expirationTime);

	}

	@Test()
	public void orderResourceTestReuseResource() throws Exception {

		ResourceReadyAnswer rrAnswer = new ResourceReadyAnswer();

		String fakeRequestId = "requestId";
		String fogbowRequirement = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String host = "100.10.1.1";
		String port = "9898";
		String userName = "user";
		String extraPorts = "";
		String cpuSize = "1";
		String menSize = "1024";
		String diskSize = "";
		String location = "";

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specs.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);

		Map<String, String> resourceAMetadata = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, RequestType.PERSISTENT, specs.getImage(), specs.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		Resource fakeResourceA = TestResourceHelper.generateMockResource(fakeRequestId, resourceAMetadata, false);
		// doReturn(true).when(fakeResourceA).match(specs);
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
		String hostA = "100.10.1.1";
		String hostB = "100.10.1.10";
		String port = "9898";
		String userName = "user";
		String extraPorts = "";
		String cpuSize = "1";
		String menSize = "1024";
		String diskSize = "";
		String location = "";

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specs.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);

		Map<String, String> resourceAMetadata = TestResourceHelper.generateResourceMetadata(hostA, port, userName,
				extraPorts, RequestType.PERSISTENT, specs.getImage(), specs.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		Resource fakeResourceA = TestResourceHelper.generateMockResource(fakeRequestId, resourceAMetadata, false);
		// doReturn(true).when(fakeResourceA).match(Mockito.eq(specs));

		Map<String, String> resourceBMetadata = TestResourceHelper.generateResourceMetadata(hostB, port, userName,
				extraPorts, RequestType.PERSISTENT, specs.getImage(), specs.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		Resource fakeResourceB = TestResourceHelper.generateMockResource(fakeRequestId, resourceBMetadata, true);

		// doReturn(true).when(fakeResourceB).match(Mockito.eq(specs));
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

		String fakeRequestIdA = "requestIdA";
		String fakeRequestIdB = "requestIdB";
		String fogbowRequirementA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementB = "Glue2vCPU >= 1 && Glue2RAM >= 2048";
		String host = "100.10.1.1";
		String port = "9898";
		String userName = "user";
		String extraPorts = "";
		String cpuSize = "1";
		String menSize = "1024";
		String diskSize = "";
		String location = "";

		Specification specsA = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specsA.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementA);

		Specification specsB = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");
		specsB.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementB);

		Map<String, String> resourceMetadataA = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, RequestType.ONE_TIME, specsA.getImage(), specsA.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		Resource fakeResourceA = TestResourceHelper.generateMockResource(fakeRequestIdA, resourceMetadataA, true);

		Map<String, String> resourceMetadataB = TestResourceHelper.generateResourceMetadata(host, port, userName,
				extraPorts, RequestType.ONE_TIME, specsB.getImage(), specsB.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		Resource fakeResourceB = TestResourceHelper.generateMockResource(fakeRequestIdB, resourceMetadataB, true);

		// Creating mocks behaviors
		doReturn(fakeRequestIdA).when(infrastructureProviderMock).requestResource(Mockito.eq(specsA));
		doReturn(fakeRequestIdB).when(infrastructureProviderMock).requestResource(Mockito.eq(specsB));

		doReturn(fakeResourceA).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestIdA));
		doReturn(fakeResourceB).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestIdB));

		doAnswer(rrAnswer).when(schedulerMock).resourceReady(Mockito.any(Resource.class));

		infrastructureManager.setInfraProvider(infrastructureProviderMock);

		validateOrderRequested(specsA);

		// Waits InfrastructureManager process order.
		// resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run();
		// resolving Ordered Orders (setting to Fulfilled)
		infrastructureManager.getOrderService().run();

		// Test allocated resource
		assertEquals(1, infrastructureManager.getAllocatedResources().size());

		// IntaceId from the Last resource ready.
		Resource firstResourceOrdered = rrAnswer.getResourceReady();
		validateReleaseResource(firstResourceOrdered);

		infrastructureManager.orderResource(specsB, schedulerMock);
		// resolving new Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run();
		// resolving Ordered Orders (setting to Fulfilled)
		infrastructureManager.getOrderService().run();

		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());

		// Last resource read must be the old resource.
		Resource secondResourceOrdered = rrAnswer.getResourceReady();
		assertNotEquals(firstResourceOrdered.getId(), secondResourceOrdered.getId());

	}

	@Test
	public void retryPersistentResourceFiled() throws Exception {

		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";

		String hostA = "100.10.1.1";
		String hostB = "100.10.1.10";
		String port = "9898";
		String userName = "user";
		String extraPorts = "";
		String cpuSize = "1";
		String menSize = "1024";
		String diskSize = "";
		String location = "";

		Specification specs = new Specification("imageMock", "UserName", "publicKeyMock", "privateKeyMock");

		Map<String, String> resourceAMetadata = TestResourceHelper.generateResourceMetadata(hostA, port, userName,
				extraPorts, RequestType.PERSISTENT, specs.getImage(), specs.getPublicKey(), cpuSize, menSize, diskSize,
				location);
		Resource fakeResourceA = TestResourceHelper.generateMockResource(fakeRequestId, resourceAMetadata, false);

		Map<String, String> resourceBMetadata = TestResourceHelper.generateResourceMetadata(hostB, port, userName,
				extraPorts, RequestType.PERSISTENT, specs.getImage(), specs.getPublicKey(), cpuSize, menSize, diskSize,
				location);

		Resource fakeResourceB = TestResourceHelper.generateMockResource(fakeRequestId, resourceBMetadata, true);

		doNothing().when(fakeResourceA).copyInformations(fakeResourceB);
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class));
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
		doReturn(resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource)
				.getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));

		// Creating mocks behaviors
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
		assertEquals(Long.valueOf(dateMock + lifetime), Long.valueOf(expirationTime));

		// "advancing time to simulate future monitor of the idle resource.
		doReturn(dateMock + (lifetime * 2)).when(dateUtilsMock).currentTimeMillis();
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
		doReturn(resourceAMetadata.get(Resource.METADATA_REQUEST_TYPE)).when(fakeResource)
				.getMetadataValue(Mockito.eq(Resource.METADATA_REQUEST_TYPE));

		// Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class));
		doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
		doReturn(dateMock).when(dateUtilsMock).currentTimeMillis();

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		infrastructureManager.orderResource(specs, schedulerMock);

		// Waits InfrastructureManager process order.
		// resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run();
		// resolving Ordered Orders (setting to Fulfilled)
		infrastructureManager.getOrderService().run();

		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());

		validateReleaseResource(fakeResource);

		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock + lifetime), Long.valueOf(expirationTime));

		doReturn(false).when(fakeResource).checkConnectivity(Mockito.anyInt());
		infrastructureManager.getInfraIntegrityService().run();

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());

	}

	@Test
	public void removePreviousResourcesTest() throws Exception {

		String requestIdFake1 = "FakeRequestID1";
		String requestIdFake2 = "FakeRequestID2";

		List<String> requestIdsPrevious = new ArrayList<String>();
		requestIdsPrevious.add(requestIdFake1);
		requestIdsPrevious.add(requestIdFake2);

		doReturn(requestIdsPrevious).when(ds).getRequesId();
		infrastructureManager.start(true);

		verify(infrastructureProviderMock).deleteResource(requestIdFake1);
		verify(infrastructureProviderMock).deleteResource(requestIdFake2);

		infrastructureManager.stop();

	}

	@Test
	public void stopInfrastructureManagenTest() throws Exception {

		String requestId1 = "requestId01";
		String requestId2 = "requestId02";
		String requestId3 = "requestId03";
		String requestId4 = "requestId04";
		String requestId5 = "requestId05";
		String requestId6 = "requestId06";
		
		Order o1 = mock(Order.class);
		doReturn(Order.OrderState.ORDERED).when(o1).getState();
		doReturn(requestId1).when(o1).getRequestId();
		Order o2 = mock(Order.class);
		doReturn(Order.OrderState.OPEN).when(o2).getState();
		doReturn(requestId2).when(o2).getRequestId();
		Order o3 = mock(Order.class);
		doReturn(Order.OrderState.FULFILLED).when(o3).getState();
		doReturn(requestId3).when(o3).getRequestId();
		Order o4 = mock(Order.class);
		doReturn(Order.OrderState.ORDERED).when(o4).getState();
		doReturn(requestId4).when(o4).getRequestId();
		
		infrastructureManager.getOrders().add(o1);
		infrastructureManager.getOrders().add(o2);
		infrastructureManager.getOrders().add(o3);
		infrastructureManager.getOrders().add(o4);
		
		Resource r1 = TestResourceHelper.generateMockResource(requestId5, new HashMap<String, String>(), true);
		Resource r2 = TestResourceHelper.generateMockResource(requestId6, new HashMap<String, String>(), true);

		infrastructureManager.getAllocatedResourcesMap().put(r1,o3);
		infrastructureManager.getIdleResourcesMap().put(r2, new Long(0));
		
		infrastructureManager.stop();
		verify(infrastructureProviderMock, times(1)).deleteResource(requestId1);
		verify(infrastructureProviderMock, times(1)).deleteResource(requestId4);
		verify(infrastructureProviderMock, times(1)).deleteResource(requestId5);
		verify(infrastructureProviderMock, times(1)).deleteResource(requestId6);
		assertEquals(0, infrastructureManager.getOrders().size());
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());

	}

	private void validateOrderRequested(Specification specs) {
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

	private void generateDefaulProperties() {

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
