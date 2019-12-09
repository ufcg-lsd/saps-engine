package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class RemoveTimestampRetry implements CatalogRetry<Void> {
	private ImageDataStore imageStore;
	private SapsImage task;

	public RemoveTimestampRetry(ImageDataStore imageStore, SapsImage task) {
		this.imageStore = imageStore;
		this.task = task;
	}

	@Override
	public Void run() throws SQLException {
		imageStore.removeStateStamp(task.getTaskId(), task.getState(), task.getUpdateTime());
		return null;
	}
}
