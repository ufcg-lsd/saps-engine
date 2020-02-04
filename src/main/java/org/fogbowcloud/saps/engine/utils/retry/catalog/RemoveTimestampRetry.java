package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.database.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class RemoveTimestampRetry implements CatalogRetry<Void> {
	private Catalog imageStore;
	private SapsImage task;

	public RemoveTimestampRetry(Catalog imageStore, SapsImage task) {
		this.imageStore = imageStore;
		this.task = task;
	}

	@Override
	public Void run() throws SQLException {
		imageStore.removeStateChangeTime(task.getTaskId(), task.getState(), task.getUpdateTime());
		return null;
	}
}
