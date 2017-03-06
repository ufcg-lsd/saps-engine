package org.fogbowcloud.sebal.engine.scheduler.core.exception;

public class SebalException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2520888793776997437L;
	
	public SebalException (String msg){
		super(msg);
	}
	
	public SebalException (String msg, Exception e){
		super(msg, e);
	}
}