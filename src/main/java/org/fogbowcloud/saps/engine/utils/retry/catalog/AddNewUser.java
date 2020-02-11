package org.fogbowcloud.saps.engine.utils.retry.catalog;

import org.fogbowcloud.saps.engine.core.catalog.Catalog;

public class AddNewUser implements CatalogRetry<Void> {

	private Catalog imageStore;
	private String userEmail;
	private String userName;
	private String userPass;
	private boolean isEnable;
	private boolean userNotify;
	private boolean adminRole;

	public AddNewUser(Catalog imageStore, String userEmail, String userName, String userPass, boolean isEnable,
			boolean userNotify, boolean adminRole) {
		this.imageStore = imageStore;
		this.userEmail = userEmail;
		this.userName = userName;
		this.userPass = userPass;
		this.isEnable = isEnable;
		this.userNotify = userNotify;
		this.adminRole = adminRole;
	}

	@Override
	public Void run(){
		imageStore.addUser(userEmail, userName, userPass, isEnable, userNotify, adminRole);
		return null;
	}

}
