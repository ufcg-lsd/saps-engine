package org.fogbowcloud.saps.engine.core.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.notifier.Ward;

public interface ImageDataStore {

	String NONE = "None";
	int UNLIMITED = -1;

	String DATASTORE_USERNAME = "datastore_username";
	String DATASTORE_PASSWORD = "datastore_password";
	String DATASTORE_DRIVER = "datastore_driver";
	String DATASTORE_URL_PREFIX = "datastore_url_prefix";
	String DATASTORE_NAME = "datastore_name";
	String DATASTORE_IP = "datastore_ip";
	String DATASTORE_PORT = "datastore_port";

	void addImageTask(String taskId, String imageName, String downloadLink, int priority,
			String sebalVersion, String sebalTag, String collectionTierImageName)
			throws SQLException;

	void addStateStamp(String imageName, ImageTaskState state, Timestamp timestamp) throws SQLException;

	void addUser(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException;

	void addUserNotification(String submissionId, String taskId, String imageName, String userEmail) throws SQLException;

	void addDeployConfig(String nfsIP, String nfsSshPort, String nfsPort, String federationMember) throws SQLException;

	List<Ward> getUsersToNotify() throws SQLException;

	Map<String, String> getFederationNFSConfig(String federationMember) throws SQLException;

	void updateUserState(String userEmail, boolean state) throws SQLException;

	void updateImageTask(ImageTask imageData) throws SQLException;

	void updateTaskState(String imageName, ImageTaskState state) throws SQLException;

	void updateImageMetadata(String imageName, String stationId, String sebalVersion)
			throws SQLException;

	boolean isUserNotifiable(String userEmail) throws SQLException;

	boolean deployConfigExists(String federationMember) throws SQLException;

	boolean imageExist(String imageName) throws SQLException;

	List<ImageTask> getAllTasks() throws SQLException;

	List<ImageTask> getIn(ImageTaskState state) throws SQLException;

	List<ImageTask> getIn(ImageTaskState state, int limit) throws SQLException;

	List<ImageTask> getPurgedTasks() throws SQLException;

	List<ImageTask> getImagesToDownload(String federationMember, int limit) throws SQLException;

	ImageTask getTask(String imageName) throws SQLException;

	SapsUser getUser(String userEmail) throws SQLException;

	String getNFSServerIP(String federationMember) throws SQLException;

	void dispose();

	boolean lockTask(String imageName) throws SQLException;

	boolean unlockTask(String imageName) throws SQLException;

	void removeNotification(String submissionId, String taskId, String imageName, String userEmail) throws SQLException;

	void removeStateStamp(String imageName, ImageTaskState state, Timestamp timestamp)
			throws SQLException;

	void removeDeployConfig(String federationMember) throws SQLException;

	List<ImageTask> getTasksByFilter(ImageTaskState state, String name, long processDateInit,
			long processDateEnd) throws SQLException;
}
