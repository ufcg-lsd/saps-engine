package org.fogbowcloud.sebal.engine.scheduler.core;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Properties;
import java.util.concurrent.Executors;

import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.sebal.engine.scheduler.core.exception.SebalException;
import org.fogbowcloud.sebal.engine.scheduler.monitor.SebalTaskMonitor;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestSebalController {
	
	public Properties properties;

	public BlowoutPool blowoutPool;
	public BlowoutController blowoutController;
	
	public ImageDataStore imageStore;
	public SebalTaskMonitor sebalTaskMonitor;	
	public ManagerTimer sebalExecutionTimer;
	public SebalController sebalController;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp()  {
		properties = mock(Properties.class);
		blowoutPool = mock(BlowoutPool.class);
		imageStore = mock(ImageDataStore.class);
		sebalTaskMonitor = spy(new SebalTaskMonitor(blowoutPool, imageStore));
		sebalExecutionTimer = mock(ManagerTimer.class);
		blowoutController = mock(BlowoutController.class);
	}
	
//	@Test
//	public void testSebalControllerStart() throws SebalException, BlowoutException {		
//		sebalController = new SebalController(properties);
//	}
}