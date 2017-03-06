package org.fogbowcloud.sebal.engine.scheduler;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.core.monitor.TaskMonitor;
import org.fogbowcloud.blowout.infrastructure.model.ResourceState;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;

public class SebalTaskMonitor extends TaskMonitor {

	private static final Logger LOGGER = Logger.getLogger(SebalTaskMonitor.class);
	
	private ImageDataStore imageStore;
	
	private static long timeout = 10000;
	
	public SebalTaskMonitor(BlowoutPool blowoutpool, ImageDataStore imageStore) {
		super(blowoutpool, timeout);
		this.imageStore = imageStore;
	}
	
	@Override
	public void procMon() {
		for (TaskProcess tp : getRunningProcesses()) {
			if (tp.getStatus().equals(TaskState.RUNNING)) {
				imageToRunning(tp);
			}
			if (tp.getStatus().equals(TaskState.FINNISHED)) {
				imageToFinished(tp);
			}
			if (tp.getStatus().equals(TaskState.TIMEDOUT)) {
				imageToTimedout(tp);
			}
			if (tp.getStatus().equals(TaskState.FAILED)) {
				imageToFailed(tp);
			}
		}
	}
	
	private void imageToRunning(TaskProcess tp) {
		ImageData imageData;
		try {
			imageData = this.imageStore.getImage(getImageFromTaskProcess(tp));
			imageData.setState(ImageState.RUNNING);
			imageStore.updateImage(imageData);
			
			// Inserting update time into stateStamps table in DB
			imageData.setUpdateTime(imageStore.getImage(imageData.getName())
					.getUpdateTime());
			imageStore.addStateStamp(imageData.getName(), imageData.getState(),
					imageData.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}

		getRunningTasks().remove(getTaskById(tp.getTaskId()));
		if (tp.getResource()!= null) {
			getBlowoutPool().updateResource(tp.getResource(), ResourceState.FAILED);
		}
	}
	
	private void imageToFinished(TaskProcess tp) {
		ImageData imageData;
		try {
			imageData = this.imageStore.getImage(getImageFromTaskProcess(tp));
			imageData.setState(ImageState.FINISHED);
			imageStore.updateImage(imageData);
			
			// Inserting update time into stateStamps table in DB
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
			Task task = getTaskById(tp.getTaskId());
			task.finish();
			getRunningTasks().remove(task);
			if (tp.getResource()!= null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}
	
	private void imageToTimedout(TaskProcess tp) {		
		ImageData imageData;
		try {
			imageData = this.imageStore.getImage(getImageFromTaskProcess(tp));
			imageData.setState(ImageState.ERROR);
			imageData.setImageError("Image " +  getImageFromTaskProcess(tp) + " process timedout");
			imageStore.updateImage(imageData);
			
			// Inserting update time into stateStamps table in DB
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
			
			getRunningTasks().remove(getTaskById(tp.getTaskId()));
			if (tp.getResource()!= null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.FAILED);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}
	
	private void imageToFailed(TaskProcess tp) {
		ImageData imageData;
		try {
			imageData = this.imageStore.getImage(getImageFromTaskProcess(tp));
			imageData.setState(ImageState.QUEUED);
			imageStore.updateImage(imageData);
			
			// Inserting update time into stateStamps table in DB
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
			getRunningTasks().remove(getTaskById(tp.getTaskId()));
			if (tp.getResource()!= null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.FAILED);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	public String getImageFromTaskProcess(TaskProcess tp) {
		return getBlowoutPool().getTaskById(tp.getTaskId()).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
	}
}