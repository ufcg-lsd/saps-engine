package org.fogbowcloud.saps.engine.exceptions;

public class SapsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2520888793776997437L;
	
	public SapsException (String msg){
		super(msg);
	}
	
	public SapsException (String msg, Exception e){
		super(msg, e);
	}
}