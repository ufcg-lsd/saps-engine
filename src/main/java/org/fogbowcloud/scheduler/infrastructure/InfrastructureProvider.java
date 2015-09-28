package org.fogbowcloud.scheduler.infrastructure;

import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.infrastructure.exceptions.RequestResourceException;

public interface InfrastructureProvider {

	/**
	 * Creates new Request for resource and return the Request ID
	 * @param requirements
	 * @return Request's ID
	 */
	public String requestResource(Specification requirements) throws RequestResourceException;
	
	public Resource getResource(String requestID) throws RequestResourceException;
	
	public void deleteResource(Resource resource) throws Exception;
}
