package org.fogbowcloud.saps.engine.core.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.dispatcher.Submission;
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

	ImageTask addImageTask(
			String taskId,
			String dataset,
			String region,
			Date date,
			String downloadLink,
			int priority,
			String inputGathering,
			String inputPreprocessing,
			String algorithmExecution) throws SQLException;

	void addImageTask(ImageTask imageTask) throws SQLException;

	void addStateStamp(String taskId, ImageTaskState state, Timestamp timestamp)
			throws SQLException;

	void addUser(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException;

	void addUserNotification(String submissionId, String taskId, String userEmail)
			throws SQLException;

	void addDeployConfig(String nfsIP, String nfsSshPort, String nfsPort, String federationMember)
			throws SQLException;
	
	void dispatchMetadataInfo(String taskId) throws SQLException;
	
	void updateMetadataInfo(String metadataFilePath, String operatingSystem,
			String kernelVersion, String componentType, String taskId) throws SQLException;

	List<Ward> getUsersToNotify() throws SQLException;

	Map<String, String> getFederationNFSConfig(String federationMember) throws SQLException;

	void updateUserState(String userEmail, boolean state) throws SQLException;

	void updateImageTask(ImageTask imageTask) throws SQLException;

	void updateTaskError(String taskId, String errorMsg) throws SQLException;

	void updateTaskState(String taskId, ImageTaskState state) throws SQLException;

	void updateImageMetadata(String taskId, String stationId) throws SQLException;

	boolean isUserNotifiable(String userEmail) throws SQLException;

	boolean deployConfigExists(String federationMember) throws SQLException;

	boolean taskExist(String taskId) throws SQLException;

	List<Submission> getAllSubmissions() throws SQLException;

	List<ImageTask> getAllTasks() throws SQLException;

	List<ImageTask> getIn(ImageTaskState state) throws SQLException;

	List<ImageTask> getIn(ImageTaskState state, int limit) throws SQLException;

	List<ImageTask> getPurgedTasks() throws SQLException;

	List<ImageTask> getImagesToDownload(String federationMember, int limit) throws SQLException;

	Submission getSubmission(String submissionId) throws SQLException;

	ImageTask getTask(String taskId) throws SQLException;

	SapsUser getUser(String userEmail) throws SQLException;

	String getNFSServerIP(String federationMember) throws SQLException;

	String getNFSServerSshPort(String federationMember) throws SQLException;
	
	String getMetadataInfo(String taskId, String componentType, String infoType)
			throws SQLException;

	void dispose();

	boolean lockTask(String taskId) throws SQLException;

	boolean unlockTask(String taskId) throws SQLException;

	void removeNotification(String submissionId, String taskId, String userEmail)
			throws SQLException;

	void removeStateStamp(String taskId, ImageTaskState state, Timestamp timestamp)
			throws SQLException;

	void removeDeployConfig(String federationMember) throws SQLException;

	List<ImageTask> getTasksByFilter(ImageTaskState state, String taskId, long processDateInit,
			long processDateEnd) throws SQLException;
}
