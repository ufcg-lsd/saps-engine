package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.util.List;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public class GetProcessingTasksRetry implements CatalogRetry<List<SapsImage>>{

	private Catalog imageStore;

	public GetProcessingTasksRetry(Catalog imageStore) {
		this.imageStore = imageStore;
	}
	
	@Override
	public List<SapsImage> run(){
		ImageTaskState[] states = {ImageTaskState.DOWNLOADING, ImageTaskState.PREPROCESSING, ImageTaskState.RUNNING};
		return imageStore.getTasksByState(states);
	}

}
