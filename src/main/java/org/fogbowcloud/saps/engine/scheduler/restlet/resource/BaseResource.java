package org.fogbowcloud.saps.engine.scheduler.restlet.resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.scheduler.restlet.DatabaseApplication;
import org.restlet.data.Form;
import org.restlet.resource.ServerResource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BaseResource extends ServerResource {
	protected DatabaseApplication application;

	public BaseResource() {
		application = (DatabaseApplication) getApplication();
	}

	protected boolean authenticateUser(String userEmail, String userPass) {
		return authenticateUser(userEmail, userPass, false);
	}

	protected boolean authenticateUser(String userEmail, String userPass, boolean mustBeAdmin) {
		if (userEmail == null || userEmail.isEmpty() || userPass == null || userPass.isEmpty()) {
			return false;
		}

		SapsUser user = application.getUser(userEmail);
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

	String extractCoordinate(Form form, String name, int index) {
		String data[] = form.getValuesArray(name + "[]");
		return data[index];
	}

	Date extractDate(Form form, String name) throws ParseException {
		String data = form.getFirstValue(name);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.parse(data);
	}
}
