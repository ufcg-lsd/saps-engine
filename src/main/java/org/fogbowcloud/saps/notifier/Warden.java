package org.fogbowcloud.saps.notifier;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public interface Warden {
	
	/**
	 * Sends an e-mail to notify the user about the change of a task state
	 * @param email
	 * @param submissionId
	 * @param context
	 */
	boolean doNotify(String email, String submissionId, ImageTask context);
	
}
