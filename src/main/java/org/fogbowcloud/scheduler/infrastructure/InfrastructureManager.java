package org.fogbowcloud.scheduler.infrastructure;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
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
import org.h2.mvstore.DataUtils;

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

	private List<Order> orders = new ArrayList<Order>();
	private Map<Resource, Order> allocatedResources = new ConcurrentHashMap<Resource, Order>();
	private Map<Resource, Long> idleResources = new ConcurrentHashMap<Resource, Long>();
	
	private DateUtils dateUtils = new DateUtils();

	public InfrastructureManager(Properties properties) throws Exception {

		this.validateProperties(properties);

		this.properties = properties;
		// Iniciar o provider OK
		// Se tiver inicialSpecs, criar orders
		// Recuperar do properties se é infra estatica OK

		infraProvider = (InfrastructureProvider) AppUtil
				.instantiateClass(properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME));
		isStatic = new Boolean(properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();
		// Start order service to monitor and resolve orders.
		triggerOrderTimer();
		// Start resource service to monitor and resolve idle Resources.
		triggerResourceTimer();
	}

	protected void triggerOrderTimer() {
		LOGGER.debug("Initiating Order Service");
		int orderPeriod = Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME));
		orderTimer.scheduleAtFixedRate(orderService, 0, orderPeriod);
	}

	public void cancelOrderTimer() {
		LOGGER.debug("Stoping Order Service");
		orderTimer.cancel();
	}
	
	protected void triggerResourceTimer() {
		LOGGER.debug("Initiating Resource Service");
		int resourcePeriod = Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME));
		resourceTimer.scheduleAtFixedRate(resourceService, 0, resourcePeriod);
	}

	public void cancelResourceTimer() {
		LOGGER.debug("Stoping Resource Service");
		resourceTimer.cancel();
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

	private void resolveOpenOrder(Order order) {

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
			
			//Async call to avoid wating time from test connectivity with resource
			this.ildeResourceToOrder(resource, order);
			
		} else { // Else, requests a new resource from provider.

			try {
				String requestId = infraProvider.requestResource(order.getSpecification());
				order.setRequestId(requestId);
				order.setState(OrderState.ORDERED);
				LOGGER.debug("Order [" + order.getRequestId() + "] update to Ordered with request [" + requestId + "]");

			} catch (RequestResourceException e) {
				LOGGER.error("Error while resolving Order [" + order.getRequestId() + "]", e);
				order.setState(OrderState.OPEN);
			}

		}

	}
	
	private void resolveOrderedOrder(Order order) {
		
		LOGGER.debug("Resolving Ordered Order [" + order.getRequestId() + "]");
		
		Resource newResource = infraProvider.getResource(order.getRequestId());
		if (newResource != null) {

			order.setState(OrderState.FULFILLED);
			order.getScheduler().resourceReady(newResource);

			allocatedResources.put(newResource, order);
			LOGGER.debug("Order [" + order.getRequestId() + "] resolved to Fulfilled with Resource ["+newResource.getId()+"]");
		}
	}

	private void ildeResourceToOrder(final Resource idleResource, final Order order){

		resourceConnectivityMonitor.submit(new Runnable() {

            @Override
            public void run() {
                
            	if(isResourceAlive(idleResource)){
            		LOGGER.debug("Idle Resource founded for Order [" + order.getRequestId() + "] - Specifications"
            				+ order.getSpecification().toString());
            		order.setState(OrderState.FULFILLED);
            		// Allocate recource on Scheduler.
            		order.getScheduler().resourceReady(idleResource);
            		idleResources.remove(idleResource);
            		allocatedResources.put(idleResource, order);
            	}
            }
        });
		
	}
	
	private void moveResourceToIdle(Resource resource){
		
		int idleLifeTime = Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));
		Long expirationDate = Long.valueOf(dateUtils.currentTimeMillis()+idleLifeTime);
		idleResources.put(resource, expirationDate);
	}
	
	private boolean isResourceAlive(Resource resource){
		
		int timeout = Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT));
		return resource.checkConnectivity(timeout);
	}

	private void validateProperties(Properties properties) throws InfrastructureException {

		try {
			String providerClassName = properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);
			if(AppUtil.isStringEmpty(providerClassName)){
				throw new Exception("Provider Class Name canot be empty");

			}
			Object clazz = AppUtil.instantiateClass(providerClassName);
			if(!(clazz instanceof InfrastructureProvider)){
				throw new Exception("Provider Class Name is not a InfrastructureProvider implementation");
			}
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

	public List<Resource> getAllocatedResources() {
		return new ArrayList<Resource>(allocatedResources.keySet());
	}

	public List<Resource> getIdleResources() {
		return new ArrayList<Resource>(idleResources.keySet());
	}

	public InfrastructureProvider getInfraProvider() {
		return infraProvider;
	}

	public void setInfraProvider(InfrastructureProvider infraProvider) {
		this.infraProvider = infraProvider;
	}

	public List<Order> getOrders() {
		return orders;
	}

	protected OrderService getOrderService() {
		return orderService;
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
				 * Obs: Como tratar falha de recursos. Como tratar os recursos IDLE
				 */
				Resource r = entry.getKey();
				
				if(!isResourceAlive(r)){
					resourcesToRemove.add(r);
					continue;
				}
				
				Date expirationDate = new Date(entry.getValue().longValue());
				Date actualDate = new Date(dateUtils.currentTimeMillis());
				
				if(expirationDate.before(actualDate)){
					resourcesToRemove.add(r);
					continue;
				}
			}
			
			for(Resource r : resourcesToRemove){
				idleResources.remove(r);
			}
		}
	}

}
