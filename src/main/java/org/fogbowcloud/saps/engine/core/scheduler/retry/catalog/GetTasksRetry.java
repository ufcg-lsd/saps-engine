package org.fogbowcloud.saps.engine.core.scheduler.retry.catalog;

import java.sql.SQLException;
import java.util.List;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;

public class GetTasksRetry implements CatalogRetry<List<ImageTask>>{

	private ImageDataStore imageStore;
	private ImageTaskState state;
	private Integer limit;
	
	public GetTasksRetry(ImageDataStore imageStore, ImageTaskState state, Integer limit) {
		this.imageStore = imageStore;
		this.state = state;
		this.limit = limit;
	}
	
	@Override
	public List<ImageTask> run() throws SQLException {
		return imageStore.getIn(state, limit);
	}

}
