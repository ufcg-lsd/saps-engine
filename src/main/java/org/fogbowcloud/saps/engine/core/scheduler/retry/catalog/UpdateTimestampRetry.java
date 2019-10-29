package org.fogbowcloud.saps.engine.core.scheduler.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class UpdateTimestampRetry implements CatalogRetry<Void>{

	private ImageDataStore imageStore;
	private ImageTask task;

	public UpdateTimestampRetry(ImageDataStore imageStore, ImageTask task) {
		this.imageStore = imageStore;
		this.task = task;
	}
	
	@Override
	public Void run() throws SQLException {
		task.setUpdateTime(imageStore.getTask(task.getTaskId()).getUpdateTime());
		imageStore.addStateStamp(task.getTaskId(), task.getState(), task.getUpdateTime());
		return null;
	}
	

}
