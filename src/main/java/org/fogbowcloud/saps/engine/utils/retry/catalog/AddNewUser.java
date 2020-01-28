package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.database.Catalog;

public class AddNewUser implements CatalogRetry<Void> {

	private Catalog imageStore;
	private String userEmail;
	private String userName;
	private String userPass;
	private boolean userState;
	private boolean userNotify;
	private boolean adminRole;

	public AddNewUser(Catalog imageStore, String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) {
		this.imageStore = imageStore;
		this.userEmail = userEmail;
		this.userName = userName;
		this.userPass = userPass;
		this.userState = userState;
		this.userNotify = userNotify;
		this.adminRole = adminRole;
	}

	@Override
	public Void run() throws SQLException {
		imageStore.addUser(userEmail, userName, userPass, userState, userNotify, adminRole);
		return null;
	}

}
