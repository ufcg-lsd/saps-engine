package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.CatalogException;
import org.fogbowcloud.saps.engine.core.model.SapsUser;

public class GetUser implements CatalogRetry<SapsUser> {

	private Catalog imageStore;
	private String userEmail;

	public GetUser(Catalog imageStore, String userEmail) {
		this.imageStore = imageStore;
		this.userEmail = userEmail;
	}

	@Override
	public SapsUser run() throws SQLException {
		try {
			return imageStore.getUserByEmail(userEmail);
		} catch (CatalogException e) {
			// nothing
		}
		return null;
	}

}
