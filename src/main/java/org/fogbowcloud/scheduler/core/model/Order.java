package org.fogbowcloud.scheduler.core.model;

import org.fogbowcloud.scheduler.core.Scheduler;

public class Order {
	
	public static enum OrderState{
		OPEN,ORDERED,FULFILLED
	}
	
	private Scheduler scheduler;
	//TODO How to represent requeriments?
	private String requirements;
	private OrderState state;
	private String requestId;
	
	public Order(Scheduler scheduler, String requirements) {
		this.scheduler = scheduler;
		this.requirements = requirements;
		this.state = OrderState.OPEN;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public String getRequirements() {
		return requirements;
	}

	public OrderState getState() {
		return state;
	}

	public void setState(OrderState state) {
		this.state = state;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	
}
