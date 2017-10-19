package org.fogbowcloud.saps.engine.scheduler.core;


import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Properties;

import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.SchedulerInterface;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.fogbowcloud.blowout.infrastructure.manager.InfrastructureManager;
import org.fogbowcloud.blowout.infrastructure.monitor.ResourceMonitor;
import org.fogbowcloud.blowout.infrastructure.provider.InfrastructureProvider;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
	
	// FIXME: fix test
	@Test
	public void testSebalControllerStartNotNull() throws Exception {
		// set up		
		SapsController sebalController = mock(SapsController.class);
		
		doReturn(properties).when(sebalController).getProperties();
		doReturn(blowoutPool).when(sebalController).getBlowoutPool();
		doReturn(infraProvider).when(sebalController).getInfraProvider();
		doReturn(resourceMonitor).when(sebalController).getResourceMonitor();
		doReturn(infraManager).when(sebalController).getInfraManager();
		doReturn(schedulerInterface).when(sebalController).getSchedulerInterface();
		
		// exercise
		sebalController.start(true);
		
		// expect		
		Assert.assertNotNull(sebalController.getProperties());
		Assert.assertNotNull(sebalController.getBlowoutPool());
		Assert.assertNotNull(sebalController.getInfraProvider());
		Assert.assertNotNull(sebalController.getResourceMonitor());
		Assert.assertNotNull(sebalController.getInfraManager());
		Assert.assertNotNull(sebalController.getSchedulerInterface());
	}
	
	// FIXME: fix test
	@Test
	public void testSebalControllerBlowoutPoolAccess() throws Exception {
		// set up
		SapsController sebalController = mock(SapsController.class);
		TaskImpl taskImpl = mock(TaskImpl.class);
		
		doReturn(properties).when(sebalController).getProperties();
		doReturn(blowoutPool).when(sebalController).getBlowoutPool();
		doReturn(infraProvider).when(sebalController).getInfraProvider();
		doReturn(resourceMonitor).when(sebalController).getResourceMonitor();
		doReturn(infraManager).when(sebalController).getInfraManager();
		doReturn(schedulerInterface).when(sebalController).getSchedulerInterface();
		
		// exercise
		sebalController.start(true);
		
		// expect		
		verify(sebalController.getBlowoutPool()).getTaskById(taskImpl.getId());
	}
}