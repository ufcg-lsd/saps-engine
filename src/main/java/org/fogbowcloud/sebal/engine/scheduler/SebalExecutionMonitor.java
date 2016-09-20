package org.fogbowcloud.sebal.engine.scheduler;

import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.scheduler.core.CommonTaskExecutionChecker;
import org.fogbowcloud.blowout.scheduler.core.ExecutionMonitor;
import org.fogbowcloud.blowout.scheduler.core.Scheduler;
import org.fogbowcloud.blowout.scheduler.core.model.Job;
import org.fogbowcloud.blowout.scheduler.core.model.TaskProcess;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;

public class SebalExecutionMonitor extends ExecutionMonitor{

	private static final Logger LOGGER = Logger.getLogger(SebalExecutionMonitor.class);
	
	private ImageDataStore imageStore;
	
	public SebalExecutionMonitor(Scheduler scheduler, ExecutorService service,ImageDataStore imageStore) {
		super(scheduler, service);
		this.imageStore = imageStore;
	}
	
	@Override
	public void run() {
		LOGGER.debug("Submitting monitoring tasks");
		for (TaskProcess tp : getScheduler().getRunningProcs()) {
			getService().submit(new SebalTaskExecutionChecker(tp, getScheduler(), getImageFromTaskProcess(tp), imageStore));
		}
	}
	
	public String getImageFromTaskProcess(TaskProcess tp) {
		return getScheduler().getTaskFromTaskProcess(tp).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
	}
	
	

}
