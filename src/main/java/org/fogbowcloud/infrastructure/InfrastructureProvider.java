package org.fogbowcloud.infrastructure;

import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;

public interface InfrastructureProvider {
	/**
	 * Creates new Request for resource and return the Request ID
	 * @param specification
	 * @return Request's ID
	 */
	public String requestResource(Specification specification) throws RequestResourceException;
	
	public Resource getResource(String requestID);
	
	public void deleteResource(String resourceId) throws Exception;
}
