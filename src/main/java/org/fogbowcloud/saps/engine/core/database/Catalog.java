package org.fogbowcloud.saps.engine.core.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.dispatcher.notifier.Ward;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public interface Catalog {

	String NONE = "None";
	int UNLIMITED = -1;

	String DATASTORE_USERNAME = "datastore_username";
	String DATASTORE_PASSWORD = "datastore_password";
	String DATASTORE_DRIVER = "datastore_driver";
	String DATASTORE_URL_PREFIX = "datastore_url_prefix";
	String DATASTORE_NAME = "datastore_name";
	String DATASTORE_IP = "datastore_ip";
	String DATASTORE_PORT = "datastore_port";

	SapsImage addImageTask(String taskId, String dataset, String region, Date date, int priority, String user,
			String inputdownloadingPhaseTag, String digestInputdownloading, String preprocessingPhaseTag,
			String digestPreprocessing, String processingPhaseTag, String digestProcessing) throws SQLException;

	void addImageTask(SapsImage imageTask) throws SQLException;

	void addStateStamp(String taskId, ImageTaskState state, Timestamp timestamp) throws SQLException;

	void addUser(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
			boolean adminRole) throws SQLException;

	void addUserNotification(String submissionId, String taskId, String userEmail) throws SQLException;

	void addDeployConfig(String nfsIP, String nfsSshPort, String nfsPort, String federationMember) throws SQLException;

	void updateMetadataInfo(String metadataFilePath, String operatingSystem, String kernelVersion, String componentType,
			String taskId) throws SQLException;

	List<Ward> getUsersToNotify() throws SQLException;

	Map<String, String> getFederationNFSConfig(String federationMember) throws SQLException;

	void updateUserState(String userEmail, boolean state) throws SQLException;

	void updateImageTask(SapsImage imageTask) throws SQLException;

	void updateTaskError(String taskId, String errorMsg) throws SQLException;

	void updateTaskState(String taskId, ImageTaskState state) throws SQLException;

	void updateTaskStatus(String taskId, String status) throws SQLException;

	boolean isUserNotifiable(String userEmail) throws SQLException;

	boolean deployConfigExists(String federationMember) throws SQLException;

	boolean metadataRegisterExist(String taskId) throws SQLException;

	List<SapsImage> getAllTasks() throws SQLException;

	List<SapsImage> getTasksInProcessingState() throws SQLException;

	List<SapsImage> getIn(ImageTaskState state) throws SQLException;

	List<SapsImage> getIn(ImageTaskState state, int limit) throws SQLException;

	List<SapsImage> getImagesToDownload(String federationMember, int limit) throws SQLException;

	SapsImage getTask(String taskId) throws SQLException;

	SapsUser getUser(String userEmail) throws SQLException;

	void removeNotification(String submissionId, String taskId, String userEmail) throws SQLException;

	void removeStateStamp(String taskId, ImageTaskState state, Timestamp timestamp) throws SQLException;

	List<SapsImage> getProcessedImages(String region, Date initDate, Date endDate, String inputGathering,
			String inputPreprocessing, String algorithmExecution) throws SQLException;
}
