package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.util.List;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public class GetTasksRetry implements CatalogRetry<List<SapsImage>>{

	private Catalog imageStore;
	private ImageTaskState state;
	private Integer limit;
	
	public GetTasksRetry(Catalog imageStore, ImageTaskState state, Integer limit) {
		this.imageStore = imageStore;
		this.state = state;
		this.limit = limit;
	}
	
	@Override
	public List<SapsImage> run() {
		ImageTaskState[] states = {state};
		return imageStore.getTasksByState(limit, states);
	}

}
