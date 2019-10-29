package org.fogbowcloud.saps.engine.core.scheduler.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class UpdateTaskRetry implements CatalogRetry<Boolean>{

	private ImageDataStore imageStore;
	private ImageTask task;

	public UpdateTaskRetry(ImageDataStore imageStore, ImageTask task) {
		this.imageStore = imageStore;
		this.task = task;
	}
	
	@Override
	public Boolean run() throws SQLException {
		task.setUpdateTime(imageStore.getTask(task.getTaskId()).getUpdateTime());
		imageStore.updateImageTask(task);
		return true;
	}

}
