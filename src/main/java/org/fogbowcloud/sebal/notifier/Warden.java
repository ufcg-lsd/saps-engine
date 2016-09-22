package org.fogbowcloud.sebal.notifier;

import org.fogbowcloud.sebal.engine.sebal.ImageData;

public interface Warden {
	
	/**
	 * Sends an e-mail to notify the user about the change of an image state
	 * @param email
	 * @param jobId
	 * @param context
	 */
	void doNotify(String email, String jobId, ImageData context);
	
}
