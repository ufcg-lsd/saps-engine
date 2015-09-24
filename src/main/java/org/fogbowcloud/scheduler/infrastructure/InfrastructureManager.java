package org.fogbowcloud.scheduler.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Order;
import org.fogbowcloud.scheduler.core.model.Resource;

public class InfrastructureManager {
	
	private ManagerTimer orderTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private InfrastructureProvider infraProvider;
	private List<Order> orders;
	private boolean isStatic;
	private List<Resource> allocatedResources = new ArrayList<Resource>(); //TODO Check concurrent concerns.
	private List<Resource> idleResources = new ArrayList<Resource>(); //TODO Check concurrent concerns.
	
	Properties properties;
	
	public InfrastructureManager(Properties properties) {
		// TODO Auto-generated constructor stub		
		this.properties = properties;
				
		//Iniciar o provider
		//Se tiver inicialSpecs, criar orders
		//Recuperar do properties se é infra estatica
		triggerOrderTimer();
	}

	private void triggerOrderTimer() {
		
		int orderPeriod = Integer.parseInt(properties.getProperty("order_period"));
		
		orderTimer.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				// TODO Desenvolver a logica do metodo:
				/* - Buscar por Open Orders
				 * 	- Se existir open order, para cada uma:
				 * 		1 - Verificar se existe recurso IDLE (idleResources) que atende os requisitos - Alterar Order para Fulfilled e muda recurso de lista
				 * 		2 - Se não existir, solicita novo recurso ao provider. Altera o Order para Ordered.
				 * 
				 * - Busca por Ordered Orders
				 * 	-Se existir, para cada uma:
				 * 		1 - Verifica status da requisição junto ao Provider.
				 * 		2 - Se recurso estiver pronto, coloca Order para fulfilled, adciona o recurso nos alocados, e repassa o recurso para o schedule do Order (order.schedule.resourceReady()).
				 * 
				 * Obs: Como tratar falha de recursos.
				 */
				
			}
		}, 0, orderPeriod);		
	}
	
	public void orderResource(String requirements, Scheduler scheduler){
		orders.add(new Order(scheduler, requirements));
	}

	//TODO Verificar sync. (Requisições concorrentes)
	public void releaseResource(Resource resource){
		
		//TODO check if resource is OK
		allocatedResources.remove(resource);
		idleResources.add(resource);
		
	}
}
