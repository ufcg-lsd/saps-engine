package org.fogbowcloud.infrastructure;

import org.fogbowcloud.scheduler.core.model.Resource;

public interface ResourceNotifier {
	
	public void resourceReady(final Resource resource);

}
