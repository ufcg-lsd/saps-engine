package org.fogbowcloud.scheduler.core.model;

import org.fogbowcloud.scheduler.core.Scheduler;

public class Order{

	public static enum OrderState{
		OPEN,ORDERED,FULFILLED
	}
	
	private Scheduler scheduler;
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Order other = (Order) obj;
		if (requestId == null) {
			if (other.requestId != null)
				return false;
		} else if (!requestId.equals(other.requestId))
			return false;
		return true;
	}

	
	
}
