package org.fogbowcloud.saps.engine.core.model;

import org.json.JSONException;
import org.json.JSONObject;

public class SebalUser {
	
	private String userEmail;
	private String userName;
	private String userPassword;
	private boolean active;
	private boolean userNotify;
	private boolean adminRole;
	
	public SebalUser(String userEmail, String userName, String userPassword,
			boolean active, boolean userNotify, boolean adminRole) {
		this.userEmail = userEmail;
		this.userName = userName;
		this.userPassword = userPassword;
		this.active = active;
		this.userNotify = userNotify;
		this.adminRole = adminRole;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public String getUserName() {
		return userName;
	}

	public String getUserPassword() {
		return userPassword;
	}
	
	public boolean getActive() {
		return active;
	}

	public boolean getUserNotify() {
		return userNotify;
	}
	
	public boolean getAdminRole() {
		return adminRole;
	}
	
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setUserNotify(boolean userNotify) {
		this.userNotify = userNotify;
	}

	public void setAdminRole(boolean adminRole) {
		this.adminRole = adminRole;
	}
	
	public String toString() {
		return userEmail + ", " + userName + ", " + userPassword + ", "
				+ active + ", " + userNotify + ", " + adminRole;
	}
	
	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("userEmail", userEmail);
		json.put("userName", userName);
		json.put("userPassword", userPassword);
		json.put("active", active);
		json.put("userNotify", userNotify);
		json.put("adminRole", adminRole);
						
		return json;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ImageData) {
			SebalUser other = (SebalUser) o;
			return getUserEmail().equals(other.getUserEmail())
					&& getUserName().equals(other.getUserName())
					&& getUserPassword().equals(other.getUserPassword())
					&& getActive() == other.getActive()
					&& getUserNotify() == other.getUserNotify()
					&& getAdminRole() == other.getAdminRole();
		}
		return false;
	}
}
