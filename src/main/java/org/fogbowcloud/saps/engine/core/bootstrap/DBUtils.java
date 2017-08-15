package org.fogbowcloud.saps.engine.core.bootstrap;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.fogbowcloud.saps.engine.core.model.SebalUser;
import org.fogbowcloud.saps.notifier.Ward;

public interface DBUtils {

	void listImagesInDB() throws SQLException, ParseException;

	void listCorruptedImages() throws ParseException;	
	
	void addUserInDB(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException;
	
	void addUserInNotifyDB(String jobId, String imageName, String userEmail) throws SQLException;

	List<String> fillDB(int firstYear, int lastYear, List<String> regions, String dataSet,
			String sebalVersion, String sebalTag) throws IOException;

	List<Ward> getUsersToNotify() throws SQLException;
	
	SebalUser getUser(String userEmail);
	
	void setImageForPhase2(String imageName, String sebalVersion, String sebalTag) throws SQLException;
			
	void setImagesToPurge(String day, boolean forceRemoveNonFetched) throws SQLException, ParseException;
	
	void removeUserNotify(String jobId, String imageName, String userEmail) throws SQLException;

	void updateUserState(String userEmail, boolean userState) throws SQLException;	

	boolean isUserNotifiable(String userEmail) throws SQLException;
}