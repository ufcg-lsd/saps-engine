package org.fogbowcloud.scheduler.infrastructure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.DataStore;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Order;
import org.fogbowcloud.scheduler.core.model.Order.OrderState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.util.AppUtil;
import org.fogbowcloud.scheduler.core.util.DateUtils;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;

import com.google.gson.Gson;

public class InfrastructureManager {

	private static final Logger LOGGER = Logger.getLogger(InfrastructureManager.class);

	private ManagerTimer orderTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private OrderService orderService = new OrderService();
	private ManagerTimer resourceTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private ResourceService resourceService = new ResourceService();
	private final ExecutorService resourceConnectivityMonitor = Executors.newCachedThreadPool();

	private InfrastructureProvider infraProvider;
	private boolean isStatic;
	private Properties properties;
	private DataStore ds;

	private List<Order> orders = new ArrayList<Order>();
	private Map<Resource, Order> allocatedResources = new ConcurrentHashMap<Resource, Order>();
	private Map<Resource, Long> idleResources = new ConcurrentHashMap<Resource, Long>();
	//Map to hold state of inicial spec for resources (true if resource is ready)
//	private Map<Specification, Boolean> initialSpecs = new ConcurrentHashMap<Specification, Boolean>();

	private DateUtils dateUtils = new DateUtils();

	public InfrastructureManager(Properties properties) throws Exception {

		this.properties = properties;

		this.validateProperties();

		// Iniciar o provider OK
		// Se tiver inicialSpecs, criar orders
		// Recuperar do properties se é infra estatica OK

		ds = new DataStore(properties);
		infraProvider = createInfraProvaiderInstance();
		isStatic = new Boolean(properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();
		
		this.resolveInitialSpecifications();
		
		// Start order service to monitor and resolve orders.
		triggerOrderTimer();
		// Start resource service to monitor and resolve idle Resources.
		triggerResourceTimer();
	}
	
	// --------- PUBLIC METHODS --------- //

	public void orderResource(Specification specification, Scheduler scheduler) {
		orders.add(new Order(scheduler, specification));
	}

	public void releaseResource(Resource resource) {

		LOGGER.debug("Releasing Resource [" + resource.getId() + "]");
		Order orderToRemove = allocatedResources.get(resource);
		orders.remove(orderToRemove);
		allocatedResources.remove(resource);
		moveResourceToIdle(resource);

	}
	
	public void cancelOrderTimer() {
		LOGGER.debug("Stoping Order Service");
		orderTimer.cancel();
	}

	public void cancelResourceTimer() {
		LOGGER.debug("Stoping Resource Service");
		resourceTimer.cancel();
	}
	
	// --------- PRIVATE OR PROTECTED METHODS --------- //

	private InfrastructureProvider createInfraProvaiderInstance() throws Exception {

		String providerClassName = properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class)
				.newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception("Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;

	}
	
	private void resolveInitialSpecifications() throws Exception{
		
		String initialSpecsPathFile = properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH);

		if(initialSpecsPathFile != null && new File(initialSpecsPathFile).exists()){

			boolean blockCreating = new Boolean(properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING)).booleanValue();

			LOGGER.info("Creating Initial Infrastructure "+ (blockCreating ? "with " : "without ") + "Blocking Creation!");
			
			BufferedReader br = new BufferedReader(new FileReader(initialSpecsPathFile));

			Gson gson = new Gson();
			List<Specification> specifications = Arrays.asList(gson.fromJson(br, Specification[].class));
			//Map to hold request status of an initial specification
			Map<String, Boolean> specsRequestsMap = new ConcurrentHashMap<String, Boolean>();

			for(Specification spec : specifications){
				submitRequestForInitialResource(spec, specsRequestsMap);
			}
			if(blockCreating){

				boolean waitingRequest;
				do{
					waitingRequest = false;
					for(Entry<String,Boolean> entry : specsRequestsMap.entrySet()){
						if(!entry.getValue()){
							waitingRequest = true;
						}
					}
				}while(waitingRequest);
				
				LOGGER.debug("Success creationg of Initial Infrastructure with Blocking Creation!");
			}
		}
	}

	protected void triggerOrderTimer() {
		LOGGER.debug("Initiating Order Service");
		int orderPeriod = Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME));
		orderTimer.scheduleAtFixedRate(orderService, 0, orderPeriod);
	}


	protected void triggerResourceTimer() {
		LOGGER.debug("Initiating Resource Service");
		int resourcePeriod = Integer
				.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME));
		resourceTimer.scheduleAtFixedRate(resourceService, 0, resourcePeriod);
	}

	

	protected void resolveOpenOrder(Order order) {

		LOGGER.debug("Resolving Open Order [" + order.getRequestId() + "]");
		Resource resource = null;
		// Find resource that matches with order's specification (if exists)
		if (idleResources != null && !idleResources.isEmpty()) {
			for (Resource idleResource : idleResources.keySet()) {
				if (idleResource.match(order.getSpecification())) {
					resource = idleResource;
					break;
				}
			}
		}

		// If a resource that matches specification was founded:
		if (resource != null) {

			// Async call to avoid wating time from test connectivity with
			// resource
			this.ildeResourceToOrder(resource, order);

		} else { // Else, requests a new resource from provider.

			try {
				String requestId = infraProvider.requestResource(order.getSpecification());
				order.setRequestId(requestId);
				order.setState(OrderState.ORDERED);
				ds.update(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED));
				LOGGER.debug("Order [" + order.getRequestId() + "] update to Ordered with request [" + requestId + "]");

			} catch (RequestResourceException e) {
				LOGGER.error("Error while resolving Order [" + order.getRequestId() + "]", e);
				order.setState(OrderState.OPEN);
			}

		}

	}

	protected void resolveOrderedOrder(Order order) {

		LOGGER.debug("Resolving Ordered Order [" + order.getRequestId() + "]");

		Resource newResource = infraProvider.getResource(order.getRequestId());
		if (newResource != null) {

			order.setState(OrderState.FULFILLED);
			ds.update(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED));
			order.getScheduler().resourceReady(newResource);

			allocatedResources.put(newResource, order);
			LOGGER.debug("Order [" + order.getRequestId() + "] resolved to Fulfilled with Resource ["
					+ newResource.getId() + "]");
		}
	}
	
	protected void submitRequestForInitialResource(final Specification spec, final Map<String, Boolean> specsRequestsMap) {

		resourceConnectivityMonitor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					
					String requestId = infraProvider.requestResource(spec);
					specsRequestsMap.put(requestId, new Boolean(false));
					
					Resource resource = null;
					do{
						resource = infraProvider.getResource(requestId);
						Thread.sleep(5000);
					}while(resource == null);
					
					specsRequestsMap.put(requestId, new Boolean(true));
					moveResourceToIdle(resource);
				} catch (Exception e) {
					//TODO - What to do when a creation of initial resource fails
				}
			}
		});

	}
	
	protected void ildeResourceToOrder(final Resource idleResource, final Order order) {

		resourceConnectivityMonitor.submit(new Runnable() {

			@Override
			public void run() {

				if (isResourceAlive(idleResource)) {
					LOGGER.debug("Idle Resource founded for Order [" + order.getRequestId() + "] - Specifications"
							+ order.getSpecification().toString());
					order.setState(OrderState.FULFILLED);
					ds.update(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED));
					order.getScheduler().resourceReady(idleResource);
					idleResources.remove(idleResource);
					allocatedResources.put(idleResource, order);
				}
			}
		});

	}
	
	protected void moveResourceToIdle(Resource resource) {

		int idleLifeTime = Integer
				.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));
		Long expirationDate = Long.valueOf(dateUtils.currentTimeMillis() + idleLifeTime);
		idleResources.put(resource, expirationDate);
		LOGGER.debug("Resource [" + resource.getId() + "] moved to Idle - Expiration Date: ["
				+ dateUtils.getStringDateFromMiliFormat(expirationDate, DateUtils.DATE_FORMAT_YYYY_MM_DD_HOUR));
	}

	protected boolean isResourceAlive(Resource resource) {

		int timeout = Integer
				.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT));
		return resource.checkConnectivity(timeout);
	}

	protected void disposeResource(Resource r) throws Exception {
		infraProvider.deleteResource(r);
		idleResources.remove(r);
	}

	private void validateProperties() throws InfrastructureException {

		try {
			String providerClassName = properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);
			if (AppUtil.isStringEmpty(providerClassName)) {
				throw new Exception("Provider Class Name canot be empty");
			}
			createInfraProvaiderInstance();

		} catch (Exception e) {
			LOGGER.debug("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + "]", e);
			throw new InfrastructureException("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + "]", e);
		}

		try {
			Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT));
		} catch (Exception e) {
			LOGGER.debug("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT + "]", e);
			throw new InfrastructureException("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT + "]", e);
		}

		try {
			Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));
		} catch (Exception e) {
			LOGGER.debug("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME + "]", e);
			throw new InfrastructureException("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME + "]", e);
		}

		try {
			Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME));
		} catch (Exception e) {
			LOGGER.debug("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME + "]", e);
			throw new InfrastructureException("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME + "]", e);
		}
		try {
			Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME));
		} catch (Exception e) {
			LOGGER.debug("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME + "]", e);
			throw new InfrastructureException("App Properties are not correctly configured: ["
					+ AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME + "]", e);
		}

	}

	// ----- GETTERS AND SETTERS ----- //
	protected List<Order> getOrdersByState(OrderState... states){
		
		List<Order> filtredOrders = new ArrayList<Order>();
		List<OrderState> filters = Arrays.asList(states);
		
		for(Order o : orders){
			if(filters.contains(o.getState())){
				filtredOrders.add(o);
			}
		}
		
		return filtredOrders;
	}

	protected List<Resource> getAllocatedResources() {
		return new ArrayList<Resource>(allocatedResources.keySet());
	}
	
	protected Map<Resource, Order> getAllocatedResourcesMap() {
		return allocatedResources;
	}

	protected List<Resource> getIdleResources() {
		return new ArrayList<Resource>(idleResources.keySet());
	}
	
	protected Map<Resource, Long> getIdleResourcesMap() {
		return idleResources;
	}
	
	protected InfrastructureProvider getInfraProvider() {
		return infraProvider;
	}

	protected void setInfraProvider(InfrastructureProvider infraProvider) {
		this.infraProvider = infraProvider;
	}

	protected List<Order> getOrders() {
		return orders;
	}

	protected OrderService getOrderService() {
		return orderService;
	}
	
	protected ResourceService getResourceService() {
		return resourceService;
	}
	
	protected DateUtils getDateUtils() {
		return dateUtils;
	}

	protected void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}

	protected void setDataStore(DataStore ds){
		this.ds = ds;
	}

	protected class OrderService implements Runnable {
		@Override
		public void run() {
			for (Order order : orders) {

				switch (order.getState()) {
				case OPEN:
					resolveOpenOrder(order);
					break;
				case ORDERED:
					resolveOrderedOrder(order);
					break;
				default:
					break;
				}
			}
		}
	}

	protected class ResourceService implements Runnable {
		@Override
		public void run() {

			List<Resource> resourcesToRemove = new ArrayList<Resource>();

			for (Entry<Resource, Long> entry : idleResources.entrySet()) {
				// TODO Desenvolver a logica do metodo:
				/*
				 * Check if should exists more rules to dispose resources.
				 */
				Resource r = entry.getKey();

				if (!isResourceAlive(r)) {
					resourcesToRemove.add(r);
					LOGGER.info("Resource: [" + r.getId() + "] to be disposed due fails connection");
					continue;
				}

				Date expirationDate = new Date(entry.getValue().longValue());
				Date actualDate = new Date(dateUtils.currentTimeMillis());

				if (expirationDate.before(actualDate)) {
					resourcesToRemove.add(r);
					LOGGER.info("Resource: [" + r.getId() + "] to be disposed due lifetime's expiration");
					continue;
				}
			}

			for (Resource r : resourcesToRemove) {
				try {
					disposeResource(r);
				} catch (Exception e) {
					LOGGER.error("Error while disposing resource: [" + r.getId() + "]");
				}
			}
		}
	}

}
