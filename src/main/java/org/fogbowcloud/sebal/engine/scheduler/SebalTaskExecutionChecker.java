package org.fogbowcloud.sebal.engine.scheduler;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.util.thread.Scheduler;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.core.monitor.TaskMonitor;
import org.fogbowcloud.blowout.infrastructure.model.ResourceState;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;

public class SebalTaskExecutionChecker extends TaskMonitor {
	private static final Logger LOGGER = Logger.getLogger(SebalTaskExecutionChecker.class);

	String imageName;
	
	ImageDataStore imageStore;

	public SebalTaskExecutionChecker(BlowoutPool blowoutPool, Scheduler scheduler, String imageName, ImageDataStore imageStore) {
		super(blowoutPool, 10000);
		this.imageName = imageName;
		this.imageStore = imageStore;
		try {
			imageToRunning(this.imageName);
		} catch (SQLException e) {
			LOGGER.debug("Could not change image '" + this.imageName + "' state to RUNNING", e);
		}
	}

	private void imageToRunning(String image) throws SQLException {
		ImageData imageData = this.imageStore.getImage(image);
		
		// TODO: test this
		if(!imageData.getState().equals(ImageState.RUNNING)) {
			imageData.setState(ImageState.RUNNING);
			imageStore.updateImage(imageData);

			// Inserting update time into stateStamps table in DB
			imageData.setUpdateTime(imageStore.getImage(imageData.getName())
					.getUpdateTime());
			imageStore.addStateStamp(imageData.getName(), imageData.getState(),
					imageData.getUpdateTime());
		}
	}

	private void imageToFinnished(String imageName) throws SQLException {
		ImageData imageData = this.imageStore.getImage(imageName);
		imageData.setState(ImageState.FINISHED);
		imageStore.updateImage(imageData);
		
		// Inserting update time into stateStamps table in DB
		imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
	}

	private void imageToFailed(String imageName) throws SQLException {
		ImageData imageData = this.imageStore.getImage(imageName);
		imageData.setState(ImageState.QUEUED);
		imageStore.updateImage(imageData);
		
		// Inserting update time into stateStamps table in DB
		imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
	}
	
	@Override
	public void procMon() {
		for (TaskProcess tp : getRunningProcesses()) {
			if (tp.getStatus().equals(TaskState.FAILED)) {
				getRunningTasks().remove(getTaskById(tp.getTaskId()));
				if (tp.getResource()!= null) {
					getBlowoutPool().updateResource(tp.getResource(), ResourceState.FAILED);
				}
				failure(tp);
			}
			if (tp.getStatus().equals(TaskState.FINNISHED)) {
				Task task = getTaskById(tp.getTaskId());
				task.finish();
				getRunningTasks().remove(task);
				if (tp.getResource()!= null) {
					getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
				}
				completion(tp);
			}
		}
	}
	
	public void failure(TaskProcess tp) {
		try {
			imageToFailed(this.imageName);
		} catch (SQLException e) {
			LOGGER.debug("Could not change image '" + this.imageName + "' state to Finnished", e);
		}
	}

	public void completion(TaskProcess tp) {
		try {
			imageToFinnished(this.imageName);
		} catch (SQLException e) {
			LOGGER.debug("Could not change image '" + this.imageName + "' state to Finnished", e);
		}
	}

}
