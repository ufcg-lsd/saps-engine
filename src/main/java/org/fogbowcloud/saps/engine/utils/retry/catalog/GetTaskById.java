package org.fogbowcloud.saps.engine.utils.retry.catalog;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class GetTaskById implements CatalogRetry<SapsImage> {

	private Catalog imageStore;
	private String taskId;

	public GetTaskById(Catalog imageStore, String taskId) {
		this.imageStore = imageStore;
		this.taskId = taskId;
	}

	@Override
	public SapsImage run(){
		return imageStore.getTaskById(taskId);
	}

}
