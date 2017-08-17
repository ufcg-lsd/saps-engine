package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.notifier.Ward;

public interface SubmissionDispatcher {

	void listImagesInDB() throws SQLException, ParseException;

	void listCorruptedImages() throws ParseException;

	void addUserInDB(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException;

	void addTaskNotificationIntoDB(String submissionId, String taskId, String imageName,
			String userEmail) throws SQLException;

	List<Task> fillDB(int firstYear, int lastYear, List<String> regions, String dataSet,
			String sebalVersion, String sebalTag) throws IOException;

	List<Ward> getUsersToNotify() throws SQLException;

	SapsUser getUser(String userEmail);

	void setTasksToPurge(String day, boolean forceRemoveNonFetched) throws SQLException,
			ParseException;

	void removeUserNotification(String submissionId, String taskId, String imageName,
			String userEmail) throws SQLException;

	void updateUserState(String userEmail, boolean userState) throws SQLException;

	boolean isUserNotifiable(String userEmail) throws SQLException;
}
