package org.fogbowcloud.infrastructure.plugin;

import java.util.concurrent.TimeUnit;

import org.fogbowcloud.manager.occi.model.Token;

public interface TokenUpdatePluginInterface {
	
	public Token generateToken();
	
	public int getUpdateTime();
	
	public TimeUnit getUpdateTimeUnits();

}
