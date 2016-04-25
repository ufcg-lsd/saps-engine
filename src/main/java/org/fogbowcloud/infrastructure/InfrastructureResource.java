package org.fogbowcloud.infrastructure;

import org.fogbowcloud.scheduler.core.model.Resource;

public interface InfrastructureResource {
	
	public void schedulerResourceReady(final Resource resource);
	
	public void crawlerResourceReady(final Resource resource);
	
	public void fetcherResourceReady(final Resource resource);
}
