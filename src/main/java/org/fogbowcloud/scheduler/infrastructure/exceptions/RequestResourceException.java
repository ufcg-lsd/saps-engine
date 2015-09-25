package org.fogbowcloud.scheduler.infrastructure.exceptions;

public class RequestResourceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5588125728287316546L;

	public RequestResourceException(String errorMsg){
		super(errorMsg);
	}
	
	public RequestResourceException(String errorMsg, Exception ex){
		super(errorMsg, ex);
	}
	
}
