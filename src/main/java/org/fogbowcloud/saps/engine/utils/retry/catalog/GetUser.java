package org.fogbowcloud.saps.engine.utils.retry.catalog;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.CatalogException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.UserNotFoundException;
import org.fogbowcloud.saps.engine.core.model.SapsUser;

public class GetUser implements CatalogRetry<SapsUser> {

	private Catalog imageStore;
	private String userEmail;
	public static final Logger LOGGER = Logger.getLogger(GetUser.class);

	public GetUser(Catalog imageStore, String userEmail) {
		this.imageStore = imageStore;
		this.userEmail = userEmail;
	}

	@Override
	public SapsUser run() {
		try {
			return imageStore.getUserByEmail(userEmail);
		} catch (CatalogException | UserNotFoundException e) {
			LOGGER.error("Error while gets user by email.", e);
		}
		return null;
	}

}
