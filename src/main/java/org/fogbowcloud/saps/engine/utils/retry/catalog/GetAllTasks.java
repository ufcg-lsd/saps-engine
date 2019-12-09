package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;
import java.util.List;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class GetAllTasks implements CatalogRetry<List<SapsImage>> {

	private ImageDataStore imageStore;

	public GetAllTasks(ImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	@Override
	public List<SapsImage> run() throws SQLException {
		return imageStore.getAllTasks();
	}

}
