package org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions;

public class GetJobException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public GetJobException(String s, Exception e) {
        super(s, e);
    }
}