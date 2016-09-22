package org.fogbowcloud.sebal.notifier;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.bootstrap.DBUtilsImpl;

public class WardenImpl implements Warden {

	private Properties properties;
	private DBUtilsImpl dbUtilsImpl;

	public static final Logger LOGGER = Logger.getLogger(WardenImpl.class);

	private static final String CONF_PATH = "config/sebal.conf";
	private static final String NOREPLY_EMAIL = "noreply_email";
	private static final String NOREPLY_PASSWORD = "noreply_password";
	private static final String DEFAULT_EMAIL_TITLE = "SEBAL IMAGE FETCHED";

	public void init() throws IOException, SQLException {
		properties = new Properties();
		FileInputStream input = new FileInputStream(CONF_PATH);
		properties.load(input);

		dbUtilsImpl = new DBUtilsImpl(properties);

		while (true) {
			Collection<Ward> notified = new LinkedList<Ward>();
			for (Ward ward : getPending()) {
				if (reached(ward)) {
					ImageData imageData = getImageData(ward.getImageName());
					try {
						if (doNotify(ward.getEmail(), ward.getJobId(),
								imageData)) {
							notified.add(ward);
						}
					} catch (Throwable e) {
						LOGGER.error(
								"Could not notify the user on: "
										+ ward.getEmail() + " about " + ward, e);
					}
				}
			}
			removeNotified(notified);
		}
	}

	@Override
	public boolean doNotify(String email, String jobId, ImageData context) {

		String message = "The image " + context.getName()
				+ " was FETCHED into swift.";

		try {
			GoogleMail.Send(properties.getProperty(NOREPLY_EMAIL),
					properties.getProperty(NOREPLY_PASSWORD), email,
					DEFAULT_EMAIL_TITLE, message);
			return true;
		} catch (AddressException e) {
			LOGGER.error("Error while sending email to " + email, e);
		} catch (MessagingException e) {
			LOGGER.error("Error while sending email to " + email, e);
		}

		return false;
	}

	private void removeNotified(Collection<Ward> notified) throws SQLException {

		for (Ward ward : notified) {
			dbUtilsImpl.removeUserNotify(ward.getImageName(), ward.getEmail());
		}
	}

	private ImageData getImageData(String imageName) throws SQLException {

		return dbUtilsImpl.getImageInDB(imageName);
	}

	private List<Ward> getPending() throws SQLException {

		Map<String, String> mapUsersImages = dbUtilsImpl.getUsersToNotify();
		List<Ward> wards = new ArrayList<Ward>();

		for (Map.Entry<String, String> entry : mapUsersImages.entrySet()) {
			Ward ward = new Ward(entry.getKey(), ImageState.FETCHED, UUID
					.randomUUID().toString(), entry.getValue());
			wards.add(ward);
		}

		return wards;
	}

	private boolean reached(Ward ward) throws SQLException {

		if (dbUtilsImpl.getImageInDB(ward.getImageName()).getState()
				.equals(ward.getTargetState())) {
			return true;
		}

		return false;
	}

	private class Ward {

		private final String imageName;
		private final ImageState targetState;
		private final String jobId;
		private final String email;

		public Ward(String imageName, ImageState targetState, String jobId,
				String email) {
			this.imageName = imageName;
			this.targetState = targetState;
			this.email = email;
			this.jobId = jobId;
		}

		@Override
		public String toString() {
			return "Ward [imageName=" + imageName + ", targetState="
					+ targetState + ", jobId=" + jobId + ", email=" + email
					+ "]";
		}

		public String getJobId() {
			return jobId;
		}

		public String getEmail() {
			return email;
		}

		public String getImageName() {
			return imageName;
		}

		public ImageState getTargetState() {
			return targetState;
		}

	}
}
