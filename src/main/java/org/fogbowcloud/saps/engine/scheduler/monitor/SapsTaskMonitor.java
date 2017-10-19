package org.fogbowcloud.saps.engine.scheduler.monitor;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.core.monitor.TaskMonitor;
import org.fogbowcloud.blowout.infrastructure.model.ResourceState;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;

public class SapsTaskMonitor extends TaskMonitor {

	private ImageDataStore imageStore;
	private static long timeout = 10000;

	private static final Logger LOGGER = Logger.getLogger(SapsTaskMonitor.class);

	public SapsTaskMonitor(BlowoutPool blowoutpool, ImageDataStore imageStore) {
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

	protected void imageToRunning(TaskProcess tp) {
		try {
			updateImageToRunning(tp);
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void imageToFinished(TaskProcess tp) {
		try {
			updateImageToFinished(tp);
			Task task = getTaskById(tp.getTaskId());
			task.finish();
			getRunningTasks().remove(task);
			if (tp.getResource() != null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void imageToTimedout(TaskProcess tp) {
		try {
			updateImageToQueued(tp);
			getRunningTasks().remove(getTaskById(tp.getTaskId()));
			if (tp.getResource() != null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void imageToFailed(TaskProcess tp) {
		try {
			updateImageToError(tp);
			getRunningTasks().remove(getTaskById(tp.getTaskId()));
			if (tp.getResource() != null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void updateImageToRunning(TaskProcess tp) throws SQLException {
		ImageTask imageData = this.imageStore.getTask(getImageFromTaskProcess(tp));
		if (!imageData.getState().equals(ImageTaskState.RUNNING)) {
			imageData.setState(ImageTaskState.RUNNING);
			imageStore.updateImageTask(imageData);

			// Inserting update time into stateStamps table in DB
			imageData.setUpdateTime(imageStore.getTask(imageData.getName()).getUpdateTime());
			imageStore.addStateStamp(imageData.getName(), imageData.getState(),
					imageData.getUpdateTime());
		}
	}

	protected void updateImageToFinished(TaskProcess tp) throws SQLException {
		ImageTask imageData = this.imageStore.getTask(getImageFromTaskProcess(tp));
		imageData.setState(ImageTaskState.FINISHED);
		imageStore.updateImageTask(imageData);

		// Inserting update time into stateStamps table in DB
		imageData.setUpdateTime(imageStore.getTask(imageData.getName()).getUpdateTime());
		imageStore.addStateStamp(imageData.getName(), imageData.getState(),
				imageData.getUpdateTime());
	}

	protected void updateImageToError(TaskProcess tp) throws SQLException {
		ImageTask imageData = this.imageStore.getTask(getImageFromTaskProcess(tp));
		imageData.setState(ImageTaskState.FAILED);
		imageData.setImageError("Image " + getImageFromTaskProcess(tp) + " process failed");
		imageStore.updateImageTask(imageData);

		// Inserting update time into stateStamps table in DB
		imageData.setUpdateTime(imageStore.getTask(imageData.getName()).getUpdateTime());
		imageStore.addStateStamp(imageData.getName(), imageData.getState(),
				imageData.getUpdateTime());
	}

	protected void updateImageToQueued(TaskProcess tp) throws SQLException {
		ImageTask imageData = this.imageStore.getTask(getImageFromTaskProcess(tp));
		imageData.setState(ImageTaskState.READY);
		imageStore.updateImageTask(imageData);

		// Inserting update time into stateStamps table in DB
		imageData.setUpdateTime(imageStore.getTask(imageData.getName()).getUpdateTime());
		imageStore.addStateStamp(imageData.getName(), imageData.getState(),
				imageData.getUpdateTime());
	}

	public String getImageFromTaskProcess(TaskProcess tp) {
		return getBlowoutPool().getTaskById(tp.getTaskId()).getMetadata(SapsTask.METADATA_TASK_ID);
	}
}