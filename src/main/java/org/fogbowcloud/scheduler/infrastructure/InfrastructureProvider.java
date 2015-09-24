package org.fogbowcloud.scheduler.infrastructure;

import org.fogbowcloud.scheduler.core.model.Resource;

public interface InfrastructureProvider {

	/**
	 * Creates new Request for resource and return the Request ID
	 * @param requirements
	 * @return Request's ID
	 */
	public String requestResource(String requirements);
	
	public Resource getResource(String requestID);
	
	public void deleteResource(Resource resource);
}
