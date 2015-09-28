package org.fogbowcloud.scheduler.core.model;

import org.fogbowcloud.scheduler.core.Scheduler;

public class Order {
	
	public static enum OrderState{
		OPEN,ORDERED,FULFILLED
	}
	
	private Scheduler scheduler;
	//TODO How to represent requeriments?
	private Specification specification;
	private OrderState state;
	private String requestId;
	
	public Order(Scheduler scheduler, Specification specification) {
		this.scheduler = scheduler;
		this.specification = specification;
		this.state = OrderState.OPEN;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public Specification getSpecification() {
		return specification;
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
