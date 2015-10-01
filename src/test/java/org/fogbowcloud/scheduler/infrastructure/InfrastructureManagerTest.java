package org.fogbowcloud.scheduler.infrastructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.scheduler.core.DataStore;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Order.OrderState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.util.DateUtils;
import org.fogbowcloud.scheduler.infrastructure.answer.ResourceReadyAnswer;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;


public class InfrastructureManagerTest {

	private final String SEBAL_SCHEDULER_PROPERTIES = "src/test/resources/sebal-scheduler.properties";
	private final String DATASTORE_PATH = "src/test/resources/persistance/";
	private Scheduler schedulerMock;
	private InfrastructureProvider infrastructureProviderMock;
	private InfrastructureManager infrastructureManager;
	private Properties properties;
	private DataStore ds;
	
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	

	//TODO
	/*
	 *  1 - Testar remoção de recurso por time out e por expiração de data
	 *  2 - Testar mover recursos pra IDLE e valor da data de expiração
	 *  3 - Testar validação de properties (inclusive teste de falha)
	 *  
	 */
	
	@Before
	public void setUp() throws Exception {
		
		//Initiating properties file.
		properties = new Properties();
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);
		properties.load(input);
		properties.put("accounting_datastore_url", "jdbc:h2:mem:"
				+ new File(DATASTORE_PATH).getAbsolutePath() + "orders");
		
		ds = mock(DataStore.class);
		schedulerMock = mock(Scheduler.class);
		infrastructureProviderMock = mock(InfrastructureProvider.class);
		
		doReturn(true).when(ds).update(Mockito.anyList());
		
		infrastructureManager = new InfrastructureManager(properties);
		infrastructureManager.cancelOrderTimer();
		infrastructureManager.cancelResourceTimer();
		infrastructureManager.setDataStore(ds);
	}    
	

	@After
	public void setDown() {
		infrastructureManager.cancelOrderTimer();
		infrastructureManager = null;
		properties = null;
		schedulerMock = null;
		infrastructureProviderMock = null;
	}
	
	@Test
	public void propertiesEmptyTest() throws Exception{
		
		exception.expect(Exception.class);
		
		properties = new Properties();
		infrastructureManager = new InfrastructureManager(properties);

	}
	
	@Test
	public void propertiesWrongProviderTest() throws Exception{
		
		exception.expect(Exception.class);
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);

		properties = new Properties();
		properties.load(input);
		properties.put(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME, "org.fogbowcloud.scheduler.core.model.Specification");
		infrastructureManager = new InfrastructureManager(properties);

	}
	
	@Test
	public void propertiesWrongConnTimeoutTest() throws Exception{
		
		exception.expect(Exception.class);
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);

		properties = new Properties();
		properties.load(input);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "AB");
		infrastructureManager = new InfrastructureManager(properties);

	}
	
	@Test
	public void propertiesWrongIdleLifetimeTest() throws Exception{
		
		exception.expect(Exception.class);
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);

		properties = new Properties();
		properties.load(input);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "AB");
		infrastructureManager = new InfrastructureManager(properties);

	}
	
	@Test
	public void propertiesWrongOrderServiceTimeTest() throws Exception{
		
		exception.expect(Exception.class);
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);

		properties = new Properties();
		properties.load(input);
		properties.put(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME, "AB");
		infrastructureManager = new InfrastructureManager(properties);

	}

	@Test
	public void propertiesWrongResourceServiceTimeTest() throws Exception{
		
		exception.expect(Exception.class);
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);
		
		properties = new Properties();
		properties.load(input);
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME, "AB");
		infrastructureManager = new InfrastructureManager(properties);
	}
	
	@Test
	public void initiateInfraManagerWithInitialSpec() throws Exception{
		
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);
		
		String initialSpecFile = "src/test/resources/Specs_Json";
		
		properties = new Properties();
		properties.load(input);
		properties.put(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING, "true");
		properties.put(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH, initialSpecFile);
		
		BufferedReader br = new BufferedReader(new FileReader(initialSpecFile));
		Gson gson = new Gson();
		List<Specification> specifications = Arrays.asList(gson.fromJson(br, Specification[].class));

		doReturn("FakeRequestID").when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class)); 
		doAnswer(new Answer<Resource>() {

			String fogbowInstanceId = "instanceId";
			int count = 0;

			@Override
			public Resource answer(InvocationOnMock invocation) throws Throwable {
				Resource fakeResource = spy(new Resource(fogbowInstanceId + (++count), new Specification("Image1", "Key1"), properties));

				fakeResource.putMetadata(Resource.METADATA_SSH_HOST, "100.10.1.1" + count);
				fakeResource.putMetadata(Resource.METADATA_SSH_PORT, "9898");
				fakeResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "user");
				fakeResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "");
				fakeResource.putMetadata(Resource.METADATA_VCPU, "1");
				fakeResource.putMetadata(Resource.METADATA_MEN_SIZE, "1024");
				// fakeResource.putMetadata(Resource.METADATA_DISK_SIZE,
				// instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE)); //TODO
				// Descomentar quando o fogbow estiver retornando este atributo
				// fakeResource.putMetadata(Resource.METADATA_LOCATION,
				// instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMBER_ID)); //TODO
				// Descomentar quando o fogbow estiver retornando este atributo
				doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
				return fakeResource;
			}

		}).when(infrastructureProviderMock).getResource(Mockito.any(String.class));
		
		infrastructureManager = new InfrastructureManager(properties);
		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.cancelOrderTimer();
		infrastructureManager.cancelResourceTimer();
		//assertEquals(specifications.size(), infrastructureManager.getIdleResources().size());
		
	}

	@Test
	public void orderResourceTestSucess() throws Exception{

		String fakeRequestId = "requestId";
		String fakeFogbowInstanceId = "instanceId";

		Specification specs = new Specification("imageMock", "publicKeyMock");
		Resource fakeResource = new Resource(fakeFogbowInstanceId, specs, properties);

		// Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.eq(specs));
		doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		infrastructureManager.setInfraProvider(infrastructureProviderMock);

		assertEquals(0, infrastructureManager.getOrders().size());
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());
		infrastructureManager.orderResource(specs, schedulerMock);
		assertEquals(1, infrastructureManager.getOrders().size());
		assertEquals(OrderState.OPEN, infrastructureManager.getOrders().get(0).getState());

		infrastructureManager.getOrderService().run(); // resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run(); // resolving Ordered Orders (setting to Fulfilled)

		// Test allocated resource
		assertNotNull(fakeResource.getId());
		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(specs, fakeResource.getSpecification());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());
		assertEquals(1, infrastructureManager.getOrders().size());

	}
	
	@Test
	public void releaseResourceSucess() throws Exception {
		
		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";
		String fakeFogbowInstanceId = "instanceId";
		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Specification specs = new Specification("imageMock", "publicKeyMock");
		Resource fakeResource = spy(new Resource(fakeFogbowInstanceId, specs, properties));

		//Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class)); //Return for new Instance's request 
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

		infrastructureManager.releaseResource(fakeResource);

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(0, infrastructureManager.getOrders().size());
		
		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock+lifetime), Long.valueOf(expirationTime));

	}
	
	@Test
	public void orderResourceTestReuseResource() throws Exception {

		ResourceReadyAnswer rrAnswer = new ResourceReadyAnswer();

		String fakeRequestId = "requestId";
		String fogbowRequirement = "Glue2vCPU >= 1 && Glue2RAM >= 1024";

		final Specification specs = new Specification("imageMock", "publicKeyMock");
		specs.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirement);

		// Creating mocks behaviors
		// Return for new Instance's request
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.eq(specs)); 
		doAnswer(new Answer<Resource>() {

			String fogbowInstanceId = "instanceId";
			int count = 0;

			@Override
			public Resource answer(InvocationOnMock invocation) throws Throwable {
				Resource fakeResource = spy(new Resource(fogbowInstanceId + (++count), specs, properties));

				fakeResource.putMetadata(Resource.METADATA_SSH_HOST, "100.10.1.1" + count);
				fakeResource.putMetadata(Resource.METADATA_SSH_PORT, "9898");
				fakeResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "user");
				fakeResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "");
				fakeResource.putMetadata(Resource.METADATA_VCPU, "1");
				fakeResource.putMetadata(Resource.METADATA_MEN_SIZE, "1024");
				// fakeResource.putMetadata(Resource.METADATA_DISK_SIZE,
				// instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE)); //TODO
				// Descomentar quando o fogbow estiver retornando este atributo
				// fakeResource.putMetadata(Resource.METADATA_LOCATION,
				// instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMBER_ID)); //TODO
				// Descomentar quando o fogbow estiver retornando este atributo
				doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
				return fakeResource;
			}

		}).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));

		doAnswer(rrAnswer).when(schedulerMock).resourceReady(Mockito.any(Resource.class));

		infrastructureManager.setInfraProvider(infrastructureProviderMock);

		assertEquals(0, infrastructureManager.getOrders().size());
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());
		infrastructureManager.orderResource(specs, schedulerMock);
		assertEquals(1, infrastructureManager.getOrders().size());

		// resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run(); 
		// resolving Ordered Orders (setting to Fulfilled)
		infrastructureManager.getOrderService().run(); 

		// Test allocated resource
		assertEquals(1, infrastructureManager.getAllocatedResources().size());

		// Last resource ready. - rrAnsewr always holds the last resource ready
		// (when scheduler.resourceReady() is called).
		Resource firstResourceOrdered = rrAnswer.getResourceReady(); 
		infrastructureManager.releaseResource(firstResourceOrdered);

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(0, infrastructureManager.getOrders().size());

		infrastructureManager.orderResource(specs, schedulerMock);
		// resolving new Open Orders
		infrastructureManager.getOrderService().run();
		// resolving Ilde Resource founded
		infrastructureManager.getOrderService().run();

		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());
		assertEquals(OrderState.FULFILLED, infrastructureManager.getOrders().get(0).getState());
		// Last resource read must be the old resource.
		Resource secondResourceOrdered = rrAnswer.getResourceReady(); 
		assertEquals(firstResourceOrdered.getId(), secondResourceOrdered.getId());

	}
	
	@Test
	public void orderResourceTestNotReuseResource() throws Exception {

		ResourceReadyAnswer rrAnswer = new ResourceReadyAnswer();

		final String fakeRequestIdA = "requestIdA";
		final String fakeRequestIdB = "requestIdB";
		String fogbowRequirementA = "Glue2vCPU >= 1 && Glue2RAM >= 1024";
		String fogbowRequirementB = "Glue2vCPU >= 1 && Glue2RAM >= 2048";

		final Specification specsA = new Specification("imageMock", "publicKeyMock");
		specsA.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementA);

		final Specification specsB = new Specification("imageMock", "publicKeyMock");
		specsB.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, fogbowRequirementB);

		//Creating mocks behaviors
		doReturn(fakeRequestIdA).when(infrastructureProviderMock).requestResource(Mockito.eq(specsA));
		doReturn(fakeRequestIdB).when(infrastructureProviderMock).requestResource(Mockito.eq(specsB));

		doAnswer(new Answer<Resource>() {

			String fogbowInstanceId = "instanceId";
			int count=0; 
			@Override
			public Resource answer(InvocationOnMock invocation) throws Throwable {

				String requestId = (String) invocation.getArguments()[0];
				String vpcu = "1";
				String menSize = "1024";
				Specification spec = specsA;
				if(fakeRequestIdB.equals(requestId)){
					vpcu = "2";
					menSize = "2048";
					spec = specsB;
				}

				Resource fakeResource = spy(new Resource(fogbowInstanceId+(++count),spec, properties));

				fakeResource.putMetadata(Resource.METADATA_SSH_HOST, "100.10.1.1"+count);
				fakeResource.putMetadata(Resource.METADATA_SSH_PORT, "9898");
				fakeResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "user");
				fakeResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "");				
				fakeResource.putMetadata(Resource.METADATA_VCPU, vpcu);
				fakeResource.putMetadata(Resource.METADATA_MEN_SIZE, menSize);
				//TODO Descomentar quando o fogbow estiver retornando este atributo
				//fakeResource.putMetadata(Resource.METADATA_DISK_SIZE, instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE)); 
				//TODO Descomentar quando o fogbow estiver retornando este atributo
				//fakeResource.putMetadata(Resource.METADATA_LOCATION, instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMBER_ID)); 
				doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());

				return fakeResource;
			}

		}).when(infrastructureProviderMock).getResource(Mockito.any(String.class));

		doAnswer(rrAnswer).when(schedulerMock).resourceReady(Mockito.any(Resource.class));


		infrastructureManager.setInfraProvider(infrastructureProviderMock);

		assertEquals(0, infrastructureManager.getOrders().size());
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());
		infrastructureManager.orderResource(specsA, schedulerMock);
		assertEquals(1, infrastructureManager.getOrders().size());

		//Waits InfrastructureManager process order.
		infrastructureManager.getOrderService().run(); // resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run(); // resolving Ordered Orders (setting to Fulfilled)

		//Test allocated resource
		assertEquals(1, infrastructureManager.getAllocatedResources().size());

		Resource firstResourceOrdered = rrAnswer.getResourceReady(); //IntaceId from the Last resource ready.
		infrastructureManager.releaseResource(firstResourceOrdered);

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(0, infrastructureManager.getOrders().size());

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
	public void deleteResourceDueExpirationTime() throws Exception {
		
		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";
		String fakeFogbowInstanceId = "instanceId";
		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Specification specs = new Specification("imageMock", "publicKeyMock");
		Resource fakeResource = spy(new Resource(fakeFogbowInstanceId,specs, properties));

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

		infrastructureManager.releaseResource(fakeResource);

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(0, infrastructureManager.getOrders().size());
		
		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock+lifetime), Long.valueOf(expirationTime));
		
		//"advancing time to simulate future monitor of the idle resource.
		doReturn(dateMock+(lifetime*2)).when(dateUtilsMock).currentTimeMillis();
		infrastructureManager.getResourceService().run();
		
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());

	}
	
	@Test
	public void deleteResourceDueConectionFailed() throws Exception {
		
		DateUtils dateUtilsMock = mock(DateUtils.class);

		String fakeRequestId = "requestId";
		String fakeFogbowInstanceId = "instanceId";
		Long dateMock = System.currentTimeMillis();
		Long lifetime = Long.valueOf(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

		Specification specs = new Specification("imageMock", "publicKeyMock");
		Resource fakeResource = spy(new Resource(fakeFogbowInstanceId, specs, properties));

		//Creating mocks behaviors
		doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class)); //Return for new Instance's request 
		doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
		doReturn(true).when(fakeResource).checkConnectivity(Mockito.anyInt());
		doReturn(dateMock).when(dateUtilsMock).currentTimeMillis();

		infrastructureManager.setInfraProvider(infrastructureProviderMock);
		infrastructureManager.setDateUtils(dateUtilsMock);

		infrastructureManager.orderResource(specs, schedulerMock);

		//Waits InfrastructureManager process order.
		infrastructureManager.getOrderService().run(); // resolving Open Orders (setting to Ordered)
		infrastructureManager.getOrderService().run(); // resolving Ordered Orders (setting to Fulfilled)

		assertEquals(1, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getOrders().size());

		infrastructureManager.releaseResource(fakeResource);

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(0, infrastructureManager.getOrders().size());
		
		Long expirationTime = infrastructureManager.getIdleResourcesMap().get(fakeResource);
		assertNotNull(expirationTime);
		assertEquals(Long.valueOf(dateMock+lifetime), Long.valueOf(expirationTime));
		
		doReturn(false).when(fakeResource).checkConnectivity(Mockito.anyInt());
		infrastructureManager.getResourceService().run();
		
		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(0, infrastructureManager.getIdleResources().size());

	}
	
	@Test
	public void TestDBConsistencyNoResources(){
	}
	
}
