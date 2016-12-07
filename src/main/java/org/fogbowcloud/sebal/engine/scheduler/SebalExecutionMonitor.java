package org.fogbowcloud.sebal.engine.scheduler;

import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.monitor.TaskMonitor;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;

public class SebalExecutionMonitor extends TaskMonitor{

	private static final Logger LOGGER = Logger.getLogger(SebalExecutionMonitor.class);
	
	private ImageDataStore imageStore;
	
	public SebalExecutionMonitor(BlowoutPool blowoutPool, ExecutorService service,ImageDataStore imageStore) {
		super(blowoutPool, 10000);
		this.imageStore = imageStore;
	}
	
	@Override
	public void procMon() {
		
	}
	
	public String getImageFromTaskProcess(TaskProcess tp) {
		return getScheduler().getTaskFromTaskProcess(tp).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
	}
	
	

}
