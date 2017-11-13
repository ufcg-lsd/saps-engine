package org.fogbowcloud.saps.engine.scheduler.core;


import static org.mockito.Mockito.mock;

import java.util.Properties;

import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.SchedulerInterface;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.fogbowcloud.blowout.infrastructure.manager.InfrastructureManager;
import org.fogbowcloud.blowout.infrastructure.monitor.ResourceMonitor;
import org.fogbowcloud.blowout.infrastructure.provider.InfrastructureProvider;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class TestSapsController {
	
	public Properties properties;
	public BlowoutPool blowoutPool;
	public ImageDataStore imageStore;
	public ManagerTimer sebalExecutionTimer;
	public InfrastructureProvider infraProvider;
	public InfrastructureManager infraManager;
	public ResourceMonitor resourceMonitor;
	public SchedulerInterface schedulerInterface;
	public BlowoutController blowoutController;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp()  {
		properties = mock(Properties.class);
		blowoutPool = mock(BlowoutPool.class);
		imageStore = mock(ImageDataStore.class);
		sebalExecutionTimer = mock(ManagerTimer.class);
		infraProvider = mock(InfrastructureProvider.class);
		resourceMonitor = mock(ResourceMonitor.class);
		infraManager = mock(InfrastructureManager.class);
		schedulerInterface = mock(SchedulerInterface.class);
		blowoutController = mock(BlowoutController.class);
	}
	
	// TODO implements tests
	
}