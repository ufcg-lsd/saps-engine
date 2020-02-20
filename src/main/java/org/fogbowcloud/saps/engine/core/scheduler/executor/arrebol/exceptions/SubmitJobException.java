package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.exceptions;

public class SubmitJobException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SubmitJobException(String s, Exception e) {
        super(s, e);
    }
}
