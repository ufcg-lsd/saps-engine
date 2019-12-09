package org.fogbowcloud.saps.engine.core.dispatcher.notifier;

import org.fogbowcloud.saps.engine.core.model.SapsImage;

public interface Warden {
	
	/**
	 * Sends an e-mail to notify the user about the change of a task state
	 * @param email
	 * @param submissionId
	 * @param context
	 */
	boolean doNotify(String email, String submissionId, SapsImage context);
	
}
