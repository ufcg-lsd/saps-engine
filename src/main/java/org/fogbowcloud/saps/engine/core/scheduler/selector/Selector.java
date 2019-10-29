package org.fogbowcloud.saps.engine.core.scheduler.selector;

import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public interface Selector {

	/**
	 * This function select tasks for schedule up to count.
	 * 
	 * @param count slots number in Arrebol queue
	 * @param tasks user map by tasks
	 * @return list of selected tasks
	 */
	public List<ImageTask> select(int count, Map<String, List<ImageTask>> tasks);
	
	/**
	 * This function returns selector version information
	 * @return selector version
	 */
	public String version();
}
