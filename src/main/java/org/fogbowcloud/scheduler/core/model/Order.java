package org.fogbowcloud.scheduler.core.model;

import org.fogbowcloud.infrastructure.ResourceNotifier;

public class Order{

	public static enum OrderState{
		OPEN,ORDERED,FULFILLED
	}
	
	private ResourceNotifier resourceNotifier;
	private Specification specification;
	private OrderState state;
	private String requestId;
	
	
	public Order(ResourceNotifier resourceNotifier, Specification specification) {
		this.resourceNotifier = resourceNotifier;
		this.specification = specification;
		this.state = OrderState.OPEN;
	}
	
	public ResourceNotifier getResourceNotifier() {
		return resourceNotifier;
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
