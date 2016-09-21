package org.fogbowcloud.sebal.engine.scheduler.restlet.resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.fogbowcloud.sebal.engine.scheduler.restlet.DatabaseApplication;
import org.fogbowcloud.sebal.engine.sebal.model.SebalUser;
import org.restlet.resource.ServerResource;

public class BaseResource extends ServerResource {
	protected DatabaseApplication application;
	
	public BaseResource() {
		application = (DatabaseApplication) getApplication();
	}
	
	protected boolean authenticateUser(String userEmail, String userPass) {
		return authenticateUser(userEmail, userPass, false);
	}
	
	protected boolean authenticateUser(String userEmail, String userPass, boolean mustBeAdmin) {
		if (userEmail == null || userEmail.isEmpty() || userPass == null
				|| userPass.isEmpty()) {
			return false;
		}
		
		SebalUser user = application.getUser(userEmail);
		String md5Pass = DigestUtils.md5Hex(userPass);
		if (user != null && user.getUserPassword().equals(md5Pass) && user.getActive()) {
			if (mustBeAdmin && !user.getAdminRole()) {
				// the user must be an admin and the logged user is not
				return false;
			}
			return true;
		}
		return false;
	}
	
}
