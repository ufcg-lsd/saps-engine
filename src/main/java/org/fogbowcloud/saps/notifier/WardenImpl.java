package org.fogbowcloud.saps.notifier;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.SubmissionDispatcherImpl;
import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class WardenImpl implements Warden {

	private Properties properties;
	private SubmissionDispatcherImpl dbUtilsImpl;

	public static final Logger LOGGER = Logger.getLogger(WardenImpl.class);

	private static final String CONF_PATH = "config/sebal.conf";
	private static final String NOREPLY_EMAIL = "noreply_email";
	private static final String NOREPLY_PASSWORD = "noreply_password";
	private static final String DEFAULT_SLEEP_TIME = "default_sleep_time";

	public WardenImpl() {
		try {
			properties = new Properties();
			FileInputStream input = new FileInputStream(CONF_PATH);
			properties.load(input);

			dbUtilsImpl = new SubmissionDispatcherImpl(properties);
		} catch (IOException e) {
			LOGGER.error("Error while getting properties", e);
		} catch (SQLException e) {
			LOGGER.error("Error while initializing DBUtilsImpl", e);
		}
	}

	// For test only
	protected WardenImpl(Properties properties, SubmissionDispatcherImpl dbUtilsImpl) {
		this.properties = properties;
		this.dbUtilsImpl = dbUtilsImpl;
	}

	public void init() {
		while (true) {
			Collection<Ward> notified = new LinkedList<Ward>();
			for (Ward ward : getPending()) {
				ImageTask imageTask = getImageTask(ward.getTaskId());
				if (imageTask == null) {
					LOGGER.debug("Image task " + ward.getTaskId()
							+ " does not exist in main database anymore");
					removeNonExistentWard(ward);
				} else {
					if (reached(ward, imageTask)) {
						try {
							if (doNotify(ward.getEmail(), ward.getSubmissionId(), imageTask)) {
								notified.add(ward);
							}
						} catch (Throwable e) {
							LOGGER.error("Could not notify the user on: " + ward.getEmail()
									+ " about " + ward, e);
						}
					}
				}
			}

			removeNotified(notified);
			try {
				Thread.sleep(Long.valueOf(properties.getProperty(DEFAULT_SLEEP_TIME)));
			} catch (InterruptedException e) {
				LOGGER.error("Thread error while sleep", e);
			}
		}
	}

	@Override
	public boolean doNotify(String email, String submissionId, ImageTask context) {
		String subject = "TASK " + context.getTaskId() + " WITH SUBMISSION_ID " + submissionId
				+ " ARCHIVED";

		String message = "The task " + context.getTaskId() + " was ARCHIVED into swift.\n"
				+ context.toString();

		try {
			GoogleMail.Send(properties.getProperty(NOREPLY_EMAIL),
					properties.getProperty(NOREPLY_PASSWORD), email, subject, message);
			return true;
		} catch (AddressException e) {
			LOGGER.error("Error while sending email to " + email, e);
		} catch (MessagingException e) {
			LOGGER.error("Error while sending email to " + email, e);
		}

		return false;
	}

	private void removeNonExistentWard(Ward ward) {
		try {
			dbUtilsImpl.removeUserNotification(ward.getSubmissionId(), ward.getTaskId(),
					ward.getEmail());
		} catch (SQLException e) {
			LOGGER.error("Error while accessing database", e);
		} catch (NullPointerException e) {
			LOGGER.error("Ward is null", e);
		}
	}

	protected void removeNotified(Collection<Ward> notified) {
		try {
			for (Ward ward : notified) {
				dbUtilsImpl.removeUserNotification(ward.getSubmissionId(), ward.getTaskId(),
						ward.getEmail());
			}
		} catch (SQLException e) {
			LOGGER.error("Error while accessing database", e);
		} catch (NullPointerException e) {
			LOGGER.error("Ward list is null", e);
		}
	}

	protected ImageTask getImageTask(String taskId) {
		try {
			return dbUtilsImpl.getTaskInDB(taskId);
		} catch (SQLException e) {
			LOGGER.error("Error while accessing database", e);
		}

		return null;
	}

	protected List<Ward> getPending() {
		List<Ward> wards = new ArrayList<Ward>();

		try {
			wards = dbUtilsImpl.getUsersToNotify();
		} catch (SQLException e) {
			LOGGER.error("Error while accessing database", e);
		}

		return wards;
	}

	protected boolean reached(Ward ward, ImageTask imageData) {
		return (imageData.getState().ordinal() == ward.getTargetState().ordinal());
	}
}
