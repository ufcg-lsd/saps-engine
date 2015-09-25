package org.fogbowcloud.scheduler.infrastructure.exceptions;

public class InfrastructureException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5588125728287316546L;

	public InfrastructureException(String errorMsg){
		super(errorMsg);
	}
	
	public InfrastructureException(String errorMsg, Exception ex){
		super(errorMsg, ex);
	}
	
}
