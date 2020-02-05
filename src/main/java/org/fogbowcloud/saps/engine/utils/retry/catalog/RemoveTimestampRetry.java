package org.fogbowcloud.saps.engine.utils.retry.catalog;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class RemoveTimestampRetry implements CatalogRetry<Void> {
	private Catalog imageStore;
	private SapsImage task;

	public RemoveTimestampRetry(Catalog imageStore, SapsImage task) {
		this.imageStore = imageStore;
		this.task = task;
	}

	@Override
	public Void run(){
		imageStore.removeStateChangeTime(task.getTaskId(), task.getState(), task.getUpdateTime());
		return null;
	}
}
