package org.fogbowcloud.saps.engine.scheduler.restlet.resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

public class UserResource extends BaseResource {
	private static final String REQUEST_ATTR_USER_EMAIL = "userEmail";
	private static final String REQUEST_ATTR_USERNAME = "userName";
	private static final String REQUEST_ATTR_USERPASS = "userPass";
	private static final String REQUEST_ATTR_USERPASS_CONFIRM = "userPassConfirm";
	private static final String REQUEST_ATTR_USERNOTIFY = "userNotify";
	private static final String REQUEST_ATTR_USERSTATE = "userState";
	private static final CharSequence CREATE_USER_MESSAGE_OK = "User created successfully";
	private static final CharSequence CREATE_USER_ALREADY_EXISTS = "User already exists";
	private static final CharSequence UPDATE_USER_MESSAGE_OK = "User state updated";

	private static final Logger LOGGER = Logger.getLogger(UserResource.class);
	private static final String ADMIN_USER_EMAIL = "adminEmail";
	private static final String ADMIN_USER_PASSWORD = "adminPass";

	public UserResource() {
		super();
	}

	@Post
	public Representation createUser(Representation entity) throws Exception {

		Form form = new Form(entity);

		String userEmail = form.getFirstValue(REQUEST_ATTR_USER_EMAIL);
		String userName = form.getFirstValue(REQUEST_ATTR_USERNAME);
		String userPass = form.getFirstValue(REQUEST_ATTR_USERPASS);
		String userPassConfirm = form.getFirstValue(REQUEST_ATTR_USERPASS_CONFIRM);
		String userNotify = form.getFirstValue(REQUEST_ATTR_USERNOTIFY);

		checkMandatoryAttributes(userName, userEmail, userPass, userPassConfirm);

		LOGGER.debug("Creating user with userEmail " + userEmail + " and userName " + userName);

		try {
			String md5Pass = DigestUtils.md5Hex(userPass);
			boolean notify = false;
			if (userNotify.equals("yes")) {
				notify = true;
			}
			application.createUser(userEmail, userName, md5Pass, false, notify, false);
		} catch (Exception e) {
			LOGGER.error("Error while creating user", e);
			return new StringRepresentation(CREATE_USER_ALREADY_EXISTS, MediaType.TEXT_PLAIN);
		}

		return new StringRepresentation(CREATE_USER_MESSAGE_OK, MediaType.TEXT_PLAIN);
	}

	private void checkMandatoryAttributes(String userName, String userEmail, String userPass,
			String userPassConfirm) {
		if (userEmail == null || userEmail.isEmpty() || userName == null || userName.isEmpty()
				|| userPass == null || userPass.isEmpty() || !userPass.equals(userPassConfirm)) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
	}

	@Put
	public Representation updateUserState(Representation entity) throws Exception {

		Form form = new Form(entity);

		String adminUserEmail = form.getFirstValue(ADMIN_USER_EMAIL, true);
		String adminUserPass = form.getFirstValue(ADMIN_USER_PASSWORD, true);

		if (!authenticateUser(adminUserEmail, adminUserPass, true)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}

		String userEmail = form.getFirstValue(REQUEST_ATTR_USER_EMAIL);
		String userState = form.getFirstValue(REQUEST_ATTR_USERSTATE);
		LOGGER.debug("Upating user " + userEmail);

		boolean state = false;

		try {
			if (userState.equals("yes")) {
				state = true;
			}

			application.updateUserState(userEmail, state);
		} catch (Exception e) {
			LOGGER.error("Error while updating user state to " + state, e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, e);
		}

		return new StringRepresentation(UPDATE_USER_MESSAGE_OK, MediaType.TEXT_PLAIN);
	}
}
