package org.fogbowcloud.scheduler.infrastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.infrastructure.ResourceNotifier;
import org.fogbowcloud.manager.occi.order.OrderType;
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
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;

public class InfrastructureManager {

	private static final Logger LOGGER = Logger.getLogger(InfrastructureManager.class);

	private ManagerTimer orderTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private OrderService orderService = new OrderService();
	private ManagerTimer resourceTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private InfraIntegrityService infraIntegrityService = new InfraIntegrityService();

	private InfrastructureProvider infraProvider;
	private boolean isElastic;
	private Properties properties;
	private DataStore ds;
	private List<Specification> initialSpec;

	private int maxResourceReuses = Integer.MAX_VALUE;

	private List<Order> orders = new ArrayList<Order>();

	private Map<Resource, Order> allocatedResources = new ConcurrentHashMap<Resource, Order>();
	private Map<Resource, Long> idleResources = new ConcurrentHashMap<Resource, Long>();

	private DateUtils dateUtils = new DateUtils();

	private Long noExpirationDate = new Long(0);

	public InfrastructureManager(List<Specification> initialSpec, boolean isElastic,
			InfrastructureProvider infraProvider, Properties properties) throws InfrastructureException {
		this(initialSpec, isElastic, infraProvider, properties, Executors.newCachedThreadPool());
	}

	public InfrastructureManager(List<Specification> initialSpec, boolean isElastic,
			InfrastructureProvider infraProvider, Properties properties, ExecutorService resourceConnectivityMonitor)
					throws InfrastructureException {

		this.properties = properties;
		this.initialSpec = initialSpec;
		this.infraProvider = infraProvider;

		String resourceReuseTimesStr = this.properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_REUSE_TIMES);

		if (resourceReuseTimesStr != null && !resourceReuseTimesStr.isEmpty()){
			this.maxResourceReuses = Integer.parseInt(resourceReuseTimesStr);
		}

		this.validateProperties();

		if (!isElastic && (initialSpec == null || initialSpec.isEmpty())) {
			throw new IllegalArgumentException(
					"No resource may be created with isElastic=" + isElastic + " and initialSpec=" + initialSpec + ".");
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
		if (blockWhileInitializing && initialSpec != null) {
			while (idleResources.size() != initialSpec.size()) {
				Thread.sleep(2000);
			}
		}
		LOGGER.info("Starting Infrastructure Manager finished");
	}

	public void stop() throws Exception {
		LOGGER.info("Stoping Infrastructure Manager");

		cancelOrderTimer();
		cancelResourceTimer();

		for (Order o : getOrdersByState(OrderState.ORDERED)) {
			infraProvider.deleteResource(o.getRequestId());
		}

		for (Resource r : getAllResources()) {
			infraProvider.deleteResource(r.getId());
		}

		orders.clear();
		allocatedResources.clear();
		idleResources.clear();
		ds.dispose();
		LOGGER.info("Stoping Infrastructure Manager finished");

	}

	private void removePreviousResources() {
		LOGGER.info("Removing previous resources...");

		List<String> recoveredRequests = ds.getRequesId();
		for (String requestId : recoveredRequests) {
			try {
				infraProvider.deleteResource(requestId);
			} catch (Exception e) {
				LOGGER.error("Error while trying to delete Resource with request id [" + requestId + "]", e);
			}
		}
		LOGGER.info("Removing previous resources finished");
	}

	public void orderResource(Specification specification, ResourceNotifier resourceNotifier, int resourceNumber) {

		List<Order> existingOrdersMatches = this.getOrdersByStateAndSchedulerAndSpec(resourceNotifier, specification, OrderState.OPEN);
		existingOrdersMatches.addAll(this.getOrdersByStateAndSchedulerAndSpec(resourceNotifier, specification, OrderState.ORDERED));

		int qtyOpenOrOrdered = existingOrdersMatches.size();
		// int qtyIdle = existingIdlesMatches.size();

		int qtyToRequest = resourceNumber - qtyOpenOrOrdered;

		/*
		 * If scheduler need less resources than existings Orders and Idles
		 * resources that matches with required specifications, then the surplus
		 * orders related to this scheduler with this specs will be removed
		 */
		if (qtyToRequest < 0) {

			qtyToRequest = qtyToRequest * (-1);
			for (int count = 0; count < qtyToRequest; count++) {
				Order toDispose = existingOrdersMatches.get(count);
				try {
					orders.remove(toDispose);
					disposeRequest(existingOrdersMatches.get(count).getRequestId());
				} catch (Exception e) {
					LOGGER.error("ERROR While trying to dispose order [RequestID: " + toDispose.getRequestId(), e);
				}
			}
		} else if (qtyToRequest == 0) {
			/*
			 * If already have quantity of open or ordered orders that matches
			 * this specifications then do nothing
			 */
			return;
		} else {
			for (int count = 0; count < qtyToRequest; count++) {
				Order order = new Order(resourceNotifier, specification);
				orders.add(order);
				resolveOpenOrder(order);
			}
		}

		this.updateInfrastuctureState();
	}

	public void releaseResource(Resource resource) {
		resource.incReuse();
		LOGGER.debug("Releasing Resource [" + resource.getId() + "]");
		Order orderToRemove = allocatedResources.get(resource);
		if (orderToRemove != null) {
			orders.remove(orderToRemove);
			allocatedResources.remove(resource);
		}
		if (resource.getReusedTimes() <= getMaxResourceReuses()){
			if (isResourceAlive(resource)) {
				// Anticipating resource to Scheduler if it is needed
				for (Order order : this.getOrdersByState(OrderState.OPEN, OrderState.ORDERED)) {

					if (order.getResourceNotifier() != null) {
						if (resource.match(order.getSpecification())) {
							try {
								this.disposeRequest(order.getRequestId());
								order.setRequestId(resource.getId());
								order.setState(OrderState.FULFILLED);
								allocatedResources.put(resource, order);
								this.updateInfrastuctureState();
								order.getResourceNotifier().resourceReady(resource);
								return;
							} catch (Exception e) {
								LOGGER.error("Error while trying to relate Idle Resource to Ordered request [RequestID: "
										+ order.getRequestId() + "]", e);
							}
						}
					}
				}
			}

			moveResourceToIdle(resource);
		} else {
			try {
				disposeResource(resource);
			} catch (Exception e) {
				LOGGER.error("Error when disposing of resource for excessive reuse", e);
			}
		}
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
		if(initialSpec!=null){
			LOGGER.info("Creating orders to initial specs: \n" + initialSpec);

			for (Specification spec : initialSpec) {
				// Initial specs must be Persistent
				spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUEST_TYPE,
						OrderType.PERSISTENT.getValue());
				orderResource(spec, null, 1);
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
		if (idleResources != null && !idleResources.isEmpty() && order.getResourceNotifier() != null) {
			for (Resource idleResource : idleResources.keySet()) {
				if (idleResource.match(order.getSpecification())) {
					resource = idleResource;
					break;
				}
			}
		}

		boolean resolvedWithIdle = false;

		if (resource != null) {

			// Async call to avoid wating time from test connectivity with
			// resource
			resolvedWithIdle = this.relateResourceToOrder(resource, order, true);

			// Else, requests a new resource from provider.
		}
		if (!resolvedWithIdle && (isElastic || order.getResourceNotifier() == null)) {

			try {
				String requestId = infraProvider.requestResource(order.getSpecification());
				order.setRequestId(requestId);
				order.setState(OrderState.ORDERED);
				updateInfrastuctureState();
				LOGGER.debug("Order [" + order.getRequestId() + "] update to Ordered with request [" + requestId + "]");

			} catch (RequestResourceException e) {
				LOGGER.error("Error while resolving Order", e);
				order.setState(OrderState.OPEN);
			}
		} else {
			LOGGER.debug("There is not idelResource available for order " + order
					+ " and it may not request new resource to infra provider.");
		}
	}

	protected void resolveOrderedOrder(Order order) {

		LOGGER.debug("Resolving Ordered Order [" + order.getRequestId() + "]");

		Resource resource = null;

		// First verify if any idle resource can be resolve this order.
		if (idleResources != null && !idleResources.isEmpty() && order.getResourceNotifier() != null) {
			for (Resource idleResource : idleResources.keySet()) {
				if (idleResource.match(order.getSpecification())) {
					resource = idleResource;
					break;
				}
			}
		}

		boolean resolvedWithIdle = false;

		if (resource != null) {

			try {
				String oldRequest = order.getRequestId();
				resolvedWithIdle = relateResourceToOrder(resource, order, true);
				if(resolvedWithIdle){
					this.disposeRequest(oldRequest);
				}
			} catch (Exception e) {
				LOGGER.error("Error while trying to relate Idle Resource to Ordered request [RequestID: "
						+ order.getRequestId() + "]");
			}

		}
		if (!resolvedWithIdle){
			/*
			 * Attempt to get resource from this order, even when a idle resource
			 * was founded. If a new resource is returned and don't exists any task
			 * to execute, this resource is move to idle by scheduler.
			 */
			Resource newResource = infraProvider.getResource(order.getRequestId());
			if (newResource != null) {

				// if order is not related to initial spec
				if (order.getResourceNotifier() != null) {

					this.relateResourceToOrder(newResource, order, false);

				} else {

					orders.remove(order);
					moveResourceToIdle(newResource);
				}

				updateInfrastuctureState();
			}
		}
	}

	protected boolean relateResourceToOrder(Resource resource, Order order, boolean isIdle) {

		if (isIdle) {
			idleResources.remove(resource);
		}
		allocatedResources.put(resource, order);

		String requestType = resource.getMetadataValue(Resource.METADATA_REQUEST_TYPE);
		boolean resourceOK = true;

		if (!isResourceAlive(resource)) {
			resourceOK = false;
			// If is a persistent resource, tries to recover it.
			if (OrderType.PERSISTENT.getValue().equals(requestType)) {
				Resource retryResource = infraProvider.getResource(resource.getId());
				if (retryResource != null && isResourceAlive(retryResource)) {
					resource.copyInformations(retryResource);
					resourceOK = true;
					retryResource = null;
				}
			}
		}
		if (resourceOK) {
			LOGGER.debug("Resource related Order with Specifications: " + order.getSpecification().toString());
			order.setRequestId(resource.getId());
			order.setState(OrderState.FULFILLED);
			updateInfrastuctureState();
			order.getResourceNotifier().resourceReady(resource);

		} else {
			allocatedResources.remove(resource);
			if (isIdle) {
				moveResourceToIdle(resource);
			}
		}

		return resourceOK;
	}

	private void updateInfrastuctureState() {
		ds.updateInfrastructureState(getOrdersByState(OrderState.ORDERED, OrderState.FULFILLED), getIdleResources());
	}

	protected void moveResourceToIdle(Resource resource) {

		Long expirationDate = noExpirationDate;

		if (OrderType.ONE_TIME.getValue().equals(resource.getMetadataValue(Resource.METADATA_REQUEST_TYPE))) {
			int idleLifeTime = Integer
					.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));

			expirationDate = Long.valueOf(+idleLifeTime);
			Calendar c = Calendar.getInstance();
			c.setTime(new Date(dateUtils.currentTimeMillis()));
			c.add(Calendar.MILLISECOND, idleLifeTime);
			expirationDate = c.getTimeInMillis();
		}
		idleResources.put(resource, expirationDate);
		updateInfrastuctureState();
		LOGGER.debug("Resource [" + resource.getId() + "] moved to Idle - Expiration Date: ["
				+ DateUtils.getStringDateFromMiliFormat(expirationDate, DateUtils.DATE_FORMAT_YYYY_MM_DD_HOUR) + "]");

	}

	protected boolean isResourceAlive(Resource resource) {

		return resource.checkConnectivity();
	}

	protected void disposeResource(Resource resource) throws Exception {
		infraProvider.deleteResource(resource.getId());
		idleResources.remove(resource);
	}

	protected void disposeRequest(String request) throws Exception {
		infraProvider.deleteResource(request);
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
	protected List<Resource> getIdleResourceBySpec(Specification spec) {
		List<Resource> maches = new ArrayList<Resource>();
		for (Resource r : getIdleResources()) {
			if (r.match(spec)) {
				maches.add(r);
			}
		}
		return maches;
	}

	protected List<Order> getOrdersByState(OrderState... states) {

		List<Order> filtredOrders = new ArrayList<Order>();
		List<OrderState> filters = Arrays.asList(states);

		for (Order o : orders) {
			if (filters.contains(o.getState())) {
				filtredOrders.add(o);
			}
		}

		return filtredOrders;
	}

	protected List<Order> getOrdersByStateAndSchedulerAndSpec(
			ResourceNotifier resourceNotifier, Specification spec,
			OrderState... states) {

		List<Order> filtredOrders = new ArrayList<Order>();
		List<OrderState> filters = Arrays.asList(states);

		for (Order o : orders) {
			if (filters.contains(o.getState())
					&& (resourceNotifier == null || resourceNotifier.equals(o
							.getResourceNotifier()))
					&& (spec == null || spec.equals(o.getSpecification()))) {
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

	protected List<Resource> getAllResources() {
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

	protected DateUtils getDateUtils() {
		return dateUtils;
	}

	protected void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}

	protected void setDataStore(DataStore ds) {
		this.ds = ds;
	}

	protected DataStore getDataStore() {
		return this.ds;
	}
	
	
	public int getMaxResourceReuses(){
		return this.maxResourceReuses;
	}

	protected class OrderService implements Runnable {
		@Override
		public void run() {
			for (Order order : new ArrayList<Order>(orders)) {

				if (order != null && order.getState() != null) {

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
	}

	protected class InfraIntegrityService implements Runnable {
		@Override
		public void run() {

			List<Resource> resourcesToRemove = new ArrayList<Resource>();

			for (Entry<Resource, Long> entry : idleResources.entrySet()) {
				if (entry != null) {
					Resource r = entry.getKey();

					String requestType = r.getMetadataValue(Resource.METADATA_REQUEST_TYPE);
					// Persistent resource can not be removed.
					if (OrderType.ONE_TIME.getValue().equals(requestType)) {

						if (!isResourceAlive(r)) {
							resourcesToRemove.add(r);
							LOGGER.info("Resource: [" + r.getId() + "] to be disposed due connection's fail");
							continue;
						}

						if (isElastic && noExpirationDate.compareTo(entry.getValue()) != 0) {
							Date expirationDate = new Date(entry.getValue().longValue());
							Date currentDate = new Date(dateUtils.currentTimeMillis());

							if (expirationDate.before(currentDate)) {
								resourcesToRemove.add(r);
								LOGGER.info("Resource: [" + r.getId() + "] to be disposed due lifetime's expiration");
								continue;
							}
						}
					} else {
						if (!isResourceAlive(r)) {
							Resource retryResource = infraProvider.getResource(r.getId());
							if (retryResource != null) {
								r.copyInformations(retryResource);
								LOGGER.debug("Resource [ID: " + r.getId() + "] new ssh information: "
										+ retryResource.getMetadataValue(Resource.METADATA_SSH_HOST) + ":"
										+ retryResource.getMetadataValue(Resource.METADATA_SSH_PORT));
								retryResource = null;
							}
							continue;
						}
					}
				}
			}

			for (Resource r : resourcesToRemove) {
				if (r != null) {
					try {
						disposeResource(r);

					} catch (Exception e) {
						LOGGER.error("Error while disposing resource: [" + r.getId() + "]", e);
					}
				}
			}
		}
	}

}
