package org.fogbowcloud.saps.notifier;

import org.fogbowcloud.saps.engine.core.model.ImageData;

public interface Warden {
	
	/**
	 * Sends an e-mail to notify the user about the change of an image state
	 * @param email
	 * @param jobId
	 * @param context
	 */
	boolean doNotify(String email, String jobId, ImageData context);
	
}
