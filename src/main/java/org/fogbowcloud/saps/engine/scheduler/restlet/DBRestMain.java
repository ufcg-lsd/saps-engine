package org.fogbowcloud.saps.engine.scheduler.restlet;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.SubmissionDispatcherImpl;
import org.fogbowcloud.saps.engine.core.model.SebalUser;

public class DBRestMain {

	private static final String ADMIN_EMAIL = "admin_email";
	private static final String ADMIN_USER = "admin_user";
	private static final String ADMIN_PASSWORD = "admin_password";
	private static final Logger LOGGER = Logger.getLogger(DBRestMain.class);

	public static void main(String[] args) throws Exception {

		String confPath = args[0];

		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(confPath);
		properties.load(input);

		SubmissionDispatcherImpl dbUtilsImpl = new SubmissionDispatcherImpl(properties);

		DatabaseApplication databaseApplication = new DatabaseApplication(
				dbUtilsImpl);
		databaseApplication.startServer();

		String userEmail = properties.getProperty(ADMIN_EMAIL);
		SebalUser user = databaseApplication.getUser(userEmail);
		if (user == null) {
			String userName = properties.getProperty(ADMIN_USER);
			String userPass = DigestUtils.md5Hex(properties
					.getProperty(ADMIN_PASSWORD));

			try {
				databaseApplication.createUser(userEmail, userName, userPass,
						true, false, true);
			} catch (Exception e) {
				LOGGER.error("Error while creating user", e);
			}
		}
	}

}
