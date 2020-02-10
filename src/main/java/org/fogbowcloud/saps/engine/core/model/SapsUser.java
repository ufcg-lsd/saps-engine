package org.fogbowcloud.saps.engine.core.model;

public class SapsUser {
	
	private String userEmail;
	private String userName;
	private String userPassword;
	private boolean isEnable;
	private boolean userNotify;
	private boolean adminRole;
	
	public SapsUser(String userEmail, String userName, String userPassword,
					boolean isEnable, boolean userNotify, boolean adminRole) {
		this.userEmail = userEmail;
		this.userName = userName;
		this.userPassword = userPassword;
		this.isEnable = isEnable;
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
	
	public boolean isEnable() {
		return isEnable;
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
	
	public String toString() {
		return userEmail + ", " + userName + ", " + userPassword + ", "
				+ isEnable + ", " + userNotify + ", " + adminRole;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof SapsImage) {
			SapsUser other = (SapsUser) o;
			return getUserEmail().equals(other.getUserEmail())
					&& getUserName().equals(other.getUserName())
					&& getUserPassword().equals(other.getUserPassword())
					&& isEnable() == other.isEnable()
					&& getUserNotify() == other.getUserNotify()
					&& getAdminRole() == other.getAdminRole();
		}
		return false;
	}
}
