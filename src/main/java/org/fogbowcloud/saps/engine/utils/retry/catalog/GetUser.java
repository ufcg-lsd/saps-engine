package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsUser;

public class GetUser implements CatalogRetry<SapsUser> {

	private ImageDataStore imageStore;
	private String userEmail;

	public GetUser(ImageDataStore imageStore, String userEmail) {
		this.imageStore = imageStore;
		this.userEmail = userEmail;
	}

	@Override
	public SapsUser run() throws SQLException {
		try {
			return imageStore.getUser(userEmail);
		} catch (SQLException e) {
			// nothing
		}
		return null;
	}

}
