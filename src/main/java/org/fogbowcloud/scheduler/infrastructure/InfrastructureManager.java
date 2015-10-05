package org.fogbowcloud.scheduler.infrastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.occi.request.RequestType;
import org.fogbowcloud.scheduler.core.DataStore;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Order;
import org.fogbowcloud.scheduler.core.model.Order.OrderState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.util.DateUtils;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;

public class InfrastructureManager {

	private static final Logger LOGGER = Logger.getLogger(InfrastructureManager.class);

	private ManagerTimer orderTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private OrderService orderService = new OrderService();
	private ManagerTimer resourceTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private InfraIntegrityService infraIntegrityService = new InfraIntegrityService();
	private final ExecutorService resourceConnectivityMonitor = Executors.newCachedThreadPool();

	private InfrastructureProvider infraProvider;
	private boolean isElastic;
	private Properties properties;
	private DataStore ds;
	private List<Specification> initialSpec;

	private List<Order> orders = new ArrayList<Order>();
	private Map<Resource, Order> allocatedResources = new ConcurrentHashMap<Resource, Order>();
	private Map<Resource, Long> idleResources = new ConcurrentHashMap<Resource, Long>();

	private DateUtils dateUtils = new DateUtils();

	public InfrastructureManager(List<Specification> initialSpec, boolean isElastic,
			InfrastructureProvider infraProvider, Properties properties)
			throws InfrastructureException {

		this.properties = properties;
		this.initialSpec = initialSpec;
		this.infraProvider = infraProvider;

		this.validateProperties();

		if (!isElastic && (initialSpec == null || initialSpec.isEmpty())) {
			throw new IllegalArgumentException("No resource may be created with isElastic="
					+ isElastic + " and initialSpec=" + initialSpec + ".");
		}

		ds = new DataStore(properties);
		this.isElastic = isElastic;
	}
	
	// --------- PUBLIC METHODS --------- //
	
	public void start(boolean blockWhileInitializing) throws Exception {
		LOGGER.info("Starting Infrastructure Manager");

		removePreviousResources();		
		this.createInitialOrders();			
		// Start order service to monitor and resolve orders.
		triggerOrderTimer();
		// Start resource service to monitor and resolve idle Resources.
		triggerResourceTimer();
		
		LOGGER.info("Block while waiting initial resources? " + blockWhileInitializing);
		if (blockWhileInitializing) {			
			while (idleResources.size() != initialSpec.size()) {
				Thread.sleep(2000);
			}			
		}
	}
	
	public void stop() throws Exception {
		LOGGER.info("Stoping Infrastructure Manager");

		cancelOrderTimer();
		cancelResourceTimer();
		resourceConnectivityMonitor.shutdownNow();
		
		for(Order o : getOrdersByState(OrderState.ORDERED)){
			infraProvider.deleteResource(o.getRequestId());
		}
		
		for(Resource r : getAllResources()){
			infraProvider.deleteResource(r.getId());
		}
		
		orders.clear();
		allocatedResources.clear();
		idleResources.clear();
		ds.dispose();
		
	}

	private void removePreviousResources() {
		LOGGER.info("Removing previous resources...");

		List<String> recoveredRequests = ds.getRequesId();
		for(String requestId : recoveredRequests){
			try {
				infraProvider.deleteResource(requestId);
			} catch (Exception e) {
				LOGGER.error("Error while trying to delete Resource with request id ["+requestId+"]", e);
			}
		}
	}

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
	
	private void createInitialOrders() {
		LOGGER.info("Creating orders to initial specs: \n" + initialSpec);

		for (Specification spec : initialSpec) {
			orderResource(spec, null);
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
		resourceTimer.scheduleAtFixedRate(infraIntegrityService, 0, resourcePeriod);
	}

	

	protected void resolveOpenOrder(Order order) {

		LOGGER.debug("Resolving new Open Order");
		Resource resource = null;
		/*
		 * Find resource that matches with order's specification (if exists) and
		 * ensure idleResources is not empty and order is not a initial spec
		 * (initial spec does not have a scheduler)
		 */
		if (idleResources != null && !idleResources.isEmpty() && order.getScheduler() != null) {
			for (Resource idleResource : idleResources.keySet()) {
				if (idleResource.match(order.getSpecification())) {
					resource = idleResource;
					break;
				}
			}
		}

		if (resource != null) {

			// Async call to avoid wating time from test connectivity with
			// resource
			this.idleResourceToOrder(resource, order);

		} else if (isElastic || order.getScheduler() == null) { // Else, requests a new resource from provider.

			try {
				String requestId = infraProvider.requestResource(order.getSpecification());
				order.setRequestId(requestId);
				order.setState(OrderState.ORDERED);
				ds.updateInfrastructureState(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED), getIdleResources());
				LOGGER.debug("Order [" + order.getRequestId() + "] update to Ordered with request [" + requestId + "]");

			} catch (RequestResourceException e) {
				LOGGER.error("Error while resolving Order [" + order.getRequestId() + "]", e);
				order.setState(OrderState.OPEN);
			}
		} else {
			LOGGER.debug("There is not idelResource available for order " + order
					+ " and it may not request new resource to infra provider.");
		}
	}

	protected void resolveOrderedOrder(Order order) {

		LOGGER.debug("Resolving Ordered Order [" + order.getRequestId() + "]");

		Resource newResource = infraProvider.getResource(order.getRequestId());
		if (newResource != null) {

			// if order is not realated to initial spec
			if (order.getScheduler() != null) {
				order.setState(OrderState.FULFILLED);
				order.getScheduler().resourceReady(newResource);
				
				allocatedResources.put(newResource, order);
				LOGGER.debug("Order [" + order.getRequestId() + "] resolved to Fulfilled with Resource ["
						+ newResource.getId() + "]");				
			} else {
				
				orders.remove(order);
				moveResourceToIdle(newResource);
			}
			
			ds.updateInfrastructureState(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED), getIdleResources());
		}
	}
	
	protected void idleResourceToOrder(final Resource idleResource, final Order order) {

		resourceConnectivityMonitor.submit(new Runnable() {

			@Override
			public void run() {

				String requestType = idleResource.getMetadataValue(Resource.METADATA_REQUEST_TYPE);
				boolean resourceOK = true;
				
				if (!isResourceAlive(idleResource)) {
					resourceOK = false;
					//If is a persistent resource, tries to recover it.
					if(RequestType.PERSISTENT.getValue().equals(requestType)){
						Resource retryResource = infraProvider.getResource(idleResource.getId());
						if(retryResource != null){
							idleResource.copyInformations(retryResource);
							resourceOK = true;
							retryResource = null;
						}
					}
				}
				if(resourceOK){
					LOGGER.debug("Idle Resource founded for Order [" + order.getRequestId() + "] - Specifications"
							+ order.getSpecification().toString());
					order.setState(OrderState.FULFILLED);
					ds.updateInfrastructureState(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED), getIdleResources());
					order.getScheduler().resourceReady(idleResource);
					idleResources.remove(idleResource);
					allocatedResources.put(idleResource, order);
				}
			}
		});
	}
	
	protected void moveResourceToIdle(Resource resource) {

		Long expirationDate = new Long(0);
		
		if(RequestType.ONE_TIME.getValue().equals(resource.getMetadataValue(Resource.METADATA_REQUEST_TYPE))){
			int idleLifeTime = Integer
					.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));
			expirationDate = Long.valueOf(dateUtils.currentTimeMillis() + idleLifeTime);
		}
		idleResources.put(resource, expirationDate);
		ds.updateInfrastructureState(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED), getIdleResources());
		LOGGER.debug("Resource [" + resource.getId() + "] moved to Idle - Expiration Date: ["
				+ DateUtils.getStringDateFromMiliFormat(expirationDate, DateUtils.DATE_FORMAT_YYYY_MM_DD_HOUR)+"]");
	}

	protected boolean isResourceAlive(Resource resource) {

		int timeout = Integer
				.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT));
		return resource.checkConnectivity(timeout);
	}

	protected void disposeResource(Resource resource) throws Exception {
		infraProvider.deleteResource(resource.getId());
		idleResources.remove(resource);
	}

	private void validateProperties() throws InfrastructureException {

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
	
	protected List<Resource> getAllResources(){
		List<Resource> resources = new ArrayList<Resource>();
		resources.addAll(this.getAllocatedResources());
		resources.addAll(this.getIdleResources());
		return resources;
		
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
	
	protected InfraIntegrityService getInfraIntegrityService() {
		return infraIntegrityService;
	}
	
	protected ExecutorService getResourceConnectivityMonitor(){
		return resourceConnectivityMonitor;
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
	
	protected DataStore getDataStore(){
		return this.ds;
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

	protected class InfraIntegrityService implements Runnable {
		@Override
		public void run() {

			List<Resource> resourcesToRemove = new ArrayList<Resource>();

			for (Entry<Resource, Long> entry : idleResources.entrySet()) {
				Resource r = entry.getKey();

				String requestType = r.getMetadataValue(Resource.METADATA_REQUEST_TYPE);
				// Persistent resource can not be removed.
				if (RequestType.ONE_TIME.getValue().equals(requestType)) {

					if (!isResourceAlive(r)) {
						resourcesToRemove.add(r);
						LOGGER.info("Resource: [" + r.getId() + "] to be disposed due connection's fail");
						continue;
					}

					if (isElastic) {
						Date expirationDate = new Date(entry.getValue().longValue());
						Date currentDate = new Date(dateUtils.currentTimeMillis());

						if (expirationDate.before(currentDate)) {
							resourcesToRemove.add(r);
							LOGGER.info("Resource: [" + r.getId() + "] to be disposed due lifetime's expiration");
							continue;
						}
					}
				}else{
					if (!isResourceAlive(r)) {
						Resource retryResource = infraProvider.getResource(r.getId());
						if(retryResource != null){
							r.copyInformations(retryResource);
							retryResource = null;
						}
						continue;
					}
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
