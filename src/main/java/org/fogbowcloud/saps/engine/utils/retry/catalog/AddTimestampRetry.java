package org.fogbowcloud.saps.engine.utils.retry.catalog;

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
	public Void run(){
		task.setUpdateTime(imageStore.getTaskById(task.getTaskId()).getUpdateTime());
		imageStore.addStateChangeTime(task.getTaskId(), task.getState(), task.getUpdateTime());
		return null;
	}
	

}
