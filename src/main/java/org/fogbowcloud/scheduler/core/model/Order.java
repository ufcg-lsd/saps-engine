package org.fogbowcloud.scheduler.core.model;

import org.fogbowcloud.infrastructure.ResourceNotifier;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.sebal.crawler.Crawler;
import org.fogbowcloud.sebal.fetcher.Fetcher;

public class Order{

	public static enum OrderState{
		OPEN,ORDERED,FULFILLED
	}
	
	private ResourceNotifier resourceNotifier;
	private Crawler crawler;
	private Scheduler scheduler;
	private Fetcher fetcher;
	private Specification specification;
	private OrderState state;
	private String requestId;
	
	public Order(Crawler crawler, Specification specification) {
		this.crawler = crawler;
		this.specification = specification;
		this.state = OrderState.OPEN;
	}
	
	public Order(Scheduler scheduler, Specification specification) {
		this.scheduler = scheduler;
		this.specification = specification;
		this.state = OrderState.OPEN;
	}
	
	public Order(Fetcher fetcher, Specification specification) {
		this.fetcher = fetcher;
		this.specification = specification;
		this.state = OrderState.OPEN;
	}
	
	public Order(ResourceNotifier resourceNotifier, Specification specification) {
		this.resourceNotifier = resourceNotifier;
		this.specification = specification;
		this.state = OrderState.OPEN;
	}
	
	public ResourceNotifier getResourceNotifier() {
		return resourceNotifier;
	}
	
	public Crawler getCrawler() {
		return crawler;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}
	
	public Fetcher getFetcher() {
		return fetcher;
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
