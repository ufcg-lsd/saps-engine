package org.fogbowcloud.scheduler.infrastructure.answer;

import org.fogbowcloud.scheduler.core.model.Resource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * This class simulate the Scheduler Resources Hold's structure
 * @author gustavorag
 *
 */
public class ResourceReadyAnswer implements Answer<Resource>{

	private Resource resourceReady = null;
	
	@Override
	public Resource answer(InvocationOnMock invocation) throws Throwable {
		
		resourceReady = (Resource) invocation.getArguments()[0];
		
		return null;
	}

	public Resource getResourceReady() {
		return resourceReady;
	}
	
}
