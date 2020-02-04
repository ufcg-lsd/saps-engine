package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class AddTimestampRetry implements CatalogRetry<Void>{

	private Catalog imageStore;
	private SapsImage task;

	public AddTimestampRetry(Catalog imageStore, SapsImage task) {
		this.imageStore = imageStore;
		this.task = task;
	}
	
	@Override
	public Void run() throws SQLException {
		task.setUpdateTime(imageStore.getTaskById(task.getTaskId()).getUpdateTime());
		imageStore.addStateStamp(task.getTaskId(), task.getState(), task.getUpdateTime());
		return null;
	}
	

}
