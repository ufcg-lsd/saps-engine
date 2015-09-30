package org.fogbowcloud.scheduler.infrastructure;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Order.OrderState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
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


public class InfrastructureManagerTest {

	private final String SEBAL_SCHEDULER_PROPERTIES = "src/test/resources/sebal-scheduler.properties";
	private Scheduler schedulerMock;
	private InfrastructureProvider infrastructureProviderMock;
	private InfrastructureManager infrastructureManager;
	private Properties properties;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	

	@Before
	public void setUp() throws IOException, FileNotFoundException {
		
		//Initiating properties file.
		properties = new Properties();
		FileInputStream input;
		input = new FileInputStream(SEBAL_SCHEDULER_PROPERTIES);
		properties.load(input);
		
		schedulerMock = mock(Scheduler.class);
		infrastructureProviderMock = mock(InfrastructureProvider.class);
		
		try {
			infrastructureManager = new InfrastructureManager(properties);
			infrastructureManager.cancelOrderTimer();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
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
	public void orderResourceTestSucess(){
		
		try {
			
			String fakeRequestId = "requestId";
			String fakeFogbowInstanceId = "instanceId";

			Specification specs = new Specification("imageMock", "publicKeyMock");
			Resource fakeResource = new Resource(fakeFogbowInstanceId, specs);

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

		} catch (RequestResourceException e) {
			e.printStackTrace();
			fail();
		}
		
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
				Resource fakeResource = spy(new Resource(fogbowInstanceId + (++count), specs));

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
				doReturn(true).when(fakeResource).checkConnectivity();
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

		// IntaceId from the Last resource ready.
		Resource firstResourceOrdered = rrAnswer.getResourceReady(); 
		infrastructureManager.releaseResource(firstResourceOrdered);

		assertEquals(0, infrastructureManager.getAllocatedResources().size());
		assertEquals(1, infrastructureManager.getIdleResources().size());
		assertEquals(0, infrastructureManager.getOrders().size());

		infrastructureManager.orderResource(specs, schedulerMock);
		// resolving new Open Orders
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
	public void orderResourceTestNotReuseResource(){
		
		try {
			
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
			doReturn(fakeRequestIdA).when(infrastructureProviderMock).requestResource(Mockito.eq(specsA)); //Return for new Instance's request
			doReturn(fakeRequestIdB).when(infrastructureProviderMock).requestResource(Mockito.eq(specsB)); //Return for new Instance's request
			
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
					
					Resource fakeResource = spy(new Resource(fogbowInstanceId+(++count),spec));
					
					fakeResource.putMetadata(Resource.METADATA_SSH_HOST, "100.10.1.1"+count);
					fakeResource.putMetadata(Resource.METADATA_SSH_PORT, "9898");
					fakeResource.putMetadata(Resource.METADATA_SSH_USERNAME_ATT, "user");
					fakeResource.putMetadata(Resource.METADATA_EXTRA_PORTS_ATT, "");				
					fakeResource.putMetadata(Resource.METADATA_VCPU, vpcu);
					fakeResource.putMetadata(Resource.METADATA_MEN_SIZE, menSize);
					//fakeResource.putMetadata(Resource.METADATA_DISK_SIZE, instanceAttributes.get(INSTANCE_ATTRIBUTE_DISKSIZE)); //TODO Descomentar quando o fogbow estiver retornando este atributo
					//fakeResource.putMetadata(Resource.METADATA_LOCATION, instanceAttributes.get(INSTANCE_ATTRIBUTE_MEMBER_ID)); //TODO Descomentar quando o fogbow estiver retornando este atributo
					doReturn(true).when(fakeResource).checkConnectivity();
					
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
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
	}

	@Test
	public void releaseResourceSucess(){
		
		try{

			String fakeRequestId = "requestId";
			String fakeFogbowInstanceId = "instanceId";
			int timeSleep = 5000+Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME));

			Specification specs = new Specification("imageMock", "publicKeyMock");
			Resource fakeResource = spy(new Resource(fakeFogbowInstanceId,specs));

			//Creating mocks behaviors
			doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.any(Specification.class)); //Return for new Instance's request 
			doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
			doReturn(true).when(fakeResource).checkConnectivity();
			
			infrastructureManager.setInfraProvider(infrastructureProviderMock);
			
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

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void releaseResourceFailTestSSH(){
		
		try{

			String fakeRequestId = "requestId";
			String fakeFogbowInstanceId = "instanceId";
			int timeSleep = 8000+Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME));

			Specification specs = new Specification("imageMock", "publicKeyMock");
			Resource fakeResource = spy(new Resource(fakeFogbowInstanceId,specs));

			//Creating mocks behaviors
			doReturn(fakeRequestId).when(infrastructureProviderMock).requestResource(Mockito.eq(specs)); //Return for new Instance's request 
			doReturn(fakeResource).when(infrastructureProviderMock).getResource(Mockito.eq(fakeRequestId));
			doReturn(false).when(fakeResource).checkConnectivity();
			infrastructureManager.setInfraProvider(infrastructureProviderMock);
			
			infrastructureManager.orderResource(specs, schedulerMock);

			//Waits InfrastructureManager process order.
			infrastructureManager.getOrderService().run(); // resolving Open Orders (setting to Ordered)
			infrastructureManager.getOrderService().run(); // resolving Ordered Orders (setting to Fulfilled)

			assertEquals(1, infrastructureManager.getAllocatedResources().size());
			assertEquals(1, infrastructureManager.getOrders().size());
			
			infrastructureManager.releaseResource(fakeResource);
			
			assertEquals(0, infrastructureManager.getAllocatedResources().size());
			assertEquals(0, infrastructureManager.getIdleResources().size());
			assertEquals(0, infrastructureManager.getOrders().size());
		
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	private void sleep(int time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
