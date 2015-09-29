package org.fogbowcloud.scheduler.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Order;
import org.fogbowcloud.scheduler.core.model.Order.OrderState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;

public class InfrastructureManager {
	
	private ManagerTimer orderTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private OrderService orderService = new OrderService();
	
	private InfrastructureProvider infraProvider;
	private List<Order> orders = new ArrayList<Order>();
	private boolean isStatic;
	private Map<Resource, Order> allocatedResources = new ConcurrentHashMap<Resource, Order>();
	private List<Resource> idleResources = new ArrayList<Resource>(); //TODO Check concurrent concerns.
	private Properties properties;
	
	public InfrastructureManager(Properties properties) throws InfrastructureException {
		
		this.validateProperties(properties);
		
		this.properties = properties;
		//Iniciar o provider
		//Se tiver inicialSpecs, criar orders
		//Recuperar do properties se é infra estatica
		triggerOrderTimer();
	}

	protected void triggerOrderTimer() {
		
		int orderPeriod = Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOLVE_ORDER_TIME));
		orderTimer.scheduleAtFixedRate(orderService, 0, orderPeriod);		
	}
	
	public void cancelOrderTimer(){
		orderTimer.cancel();
	}
	
	public void orderResource(Specification specification, Scheduler scheduler){
		orders.add(new Order(scheduler, specification));
	}

	//TODO Verificar sync. (Requisições concorrentes)
	public void releaseResource(Resource resource){

		Order orderToRemove = allocatedResources.get(resource);
		orders.remove(orderToRemove);
		allocatedResources.remove(resource);
		idleResources.add(resource);
		
	}
	
	private void resolveOpenOrder(Order order){
		
		Resource resource = null;
		//Find resource that matches with order's specification (if exists)
		if(idleResources != null && !idleResources.isEmpty()){
			for(Resource idleResource : idleResources){
				if(idleResource.match(order.getSpecification()) && idleResource.checkConnectivity()){
					resource = idleResource;
					break;
				}
			}
		}
		
		//If a resource that matches specification was founded:
		if(resource != null){
			order.setState(OrderState.FULFILLED);
			order.getScheduler().resourceReady(resource); //Allocate recource on Scheduler.
			idleResources.remove(resource);
			allocatedResources.put(resource,order);
			
		}else{ //Else, requests a new resource from provider.
			
			try {
				String requestId = infraProvider.requestResource(order.getSpecification());
				order.setRequestId(requestId);
				order.setState(OrderState.ORDERED);
				
			} catch (RequestResourceException e) {
				e.printStackTrace();
				order.setState(OrderState.OPEN);
			}
			
		}
		
	}
	
	private void resolveOrderedOrder(Order order){
		Resource newResource = infraProvider.getResource(order.getRequestId());
		if(newResource != null){

			order.setState(OrderState.FULFILLED);
			order.getScheduler().resourceReady(newResource);

			allocatedResources.put(newResource, order);
		}
	}
	
	private void validateProperties(Properties properties) throws InfrastructureException{
		try{
			Integer.parseInt(properties.getProperty(AppPropertiesConstants.INFRA_RESOLVE_ORDER_TIME));
		}catch(Exception e){
			throw new InfrastructureException("App Properties are not correctly configured: ["+AppPropertiesConstants.INFRA_RESOLVE_ORDER_TIME+"]");
		}
	}

	// ----- GETTERS AND SETTERS ----- //

	public List<Resource> getAllocatedResources() {
		return new ArrayList<Resource>(allocatedResources.keySet());
	}

	public List<Resource> getIdleResources() {
		return idleResources;
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
	
	protected OrderService getOrderService(){
		return orderService;
	}
	
	protected class OrderService implements Runnable{
		@Override
		public void run() {
			// TODO Desenvolver a logica do metodo:
			/* 
			 * Obs: Como tratar falha de recursos. Como tratar os recursos IDLE
			 */
			for(Order order : orders){
				
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
