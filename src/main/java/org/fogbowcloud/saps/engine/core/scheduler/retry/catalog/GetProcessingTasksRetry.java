package org.fogbowcloud.saps.engine.core.scheduler.retry.catalog;

import java.sql.SQLException;
import java.util.List;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class GetProcessingTasksRetry implements CatalogRetry<List<ImageTask>>{

	private ImageDataStore imageStore;

	public GetProcessingTasksRetry(ImageDataStore imageStore) {
		this.imageStore = imageStore;
	}
	
	@Override
	public List<ImageTask> run() throws SQLException {
		return imageStore.getTasksInProcessingState();
	}

}
