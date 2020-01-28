package org.fogbowcloud.saps.engine.core.dispatcher.notifier;

import org.fogbowcloud.saps.engine.exceptions.SapsException;

public class WardenMain {
	
	public static void main(String[] args) throws SapsException {
		WardenImpl wardenImpl = new WardenImpl();
		
		wardenImpl.init();
	}
}
