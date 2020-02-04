package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;
import java.util.List;

import org.fogbowcloud.saps.engine.core.database.Catalog;
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
	public List<SapsImage> run() throws SQLException {
		return imageStore.getTasksByState(state, limit);
	}

}
