package org.fogbowcloud.saps.engine.core.database;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.Submission;
import org.fogbowcloud.saps.engine.core.dispatcher.Task;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.notifier.Ward;

public class JDBCImageDataStore implements ImageDataStore {

	private static final Logger LOGGER = Logger.getLogger(JDBCImageDataStore.class);

	private static final String IMAGE_TABLE_NAME = "NASA_IMAGES";
	private static final String STATES_TABLE_NAME = "STATES_TIMESTAMPS";
	private static final String TASK_ID_COL = "task_id";
	private static final String DATASET_COL = "dataset";
	private static final String REGION_COL = "region";
	private static final String IMAGE_DATE_COL = "image_date";
	private static final String DOWNLOAD_LINK_COL = "download_link";
	private static final String PRIORITY_COL = "priority";
	private static final String FEDERATION_MEMBER_COL = "federation_member";
	private static final String STATE_COL = "state";
	private static final String STATION_ID_COL = "station_id";
	private static final String INPUT_GATHERING_TAG = "input_gathering_tag";
	private static final String INPUT_PREPROCESSING_TAG = "input_preprocessing_tag";
	private static final String ALGORITHM_EXECUTION_TAG = "algorithm_execution_tag";
	private static final String ARCHIVER_VERSION_COL = "archiver_version";
	private static final String BLOWOUT_VERSION_COL = "blowout_version";
	private static final String CREATION_TIME_COL = "ctime";
	private static final String UPDATED_TIME_COL = "utime";
	private static final String IMAGE_STATUS_COL = "status";
	private static final String ERROR_MSG_COL = "error_msg";

	private static final String USERS_TABLE_NAME = "sebal_users";
	private static final String USER_EMAIL_COL = "user_email";
	private static final String USER_NAME_COL = "user_name";
	private static final String USER_PASSWORD_COL = "user_password";
	private static final String USER_STATE_COL = "active";
	private static final String USER_NOTIFY_COL = "user_notify";
	private static final String ADMIN_ROLE_COL = "admin_role";

	private static final String USERS_NOTIFY_TABLE_NAME = "sebal_notify";
	private static final String SUBMISSION_ID_COL = "submission_id";

	private static final String DEPLOY_CONFIG_TABLE_NAME = "deploy_config";
	private static final String NFS_SERVER_IP_COL = "nfs_ip";
	private static final String NFS_SERVER_SSH_PORT_COL = "nfs_ssh_port";
	private static final String NFS_SERVER_PORT_COL = "nfs_port";

	// Insert queries
	private static final String INSERT_FULL_IMAGE_TASK_SQL = "INSERT INTO " + IMAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private Map<String, Connection> lockedImages = new ConcurrentHashMap<>();
	private BasicDataSource connectionPool;

	public JDBCImageDataStore(Properties properties) throws SQLException {

		if (properties == null) {
			throw new IllegalArgumentException("Properties arg must not be null.");
		}

		String imageStoreIP = properties.getProperty(DATASTORE_IP);
		String imageStorePort = properties.getProperty(DATASTORE_PORT);
		String imageStoreURLPrefix = properties.getProperty(DATASTORE_URL_PREFIX);
		String dbUserName = properties.getProperty(DATASTORE_USERNAME);
		String dbUserPass = properties.getProperty(DATASTORE_PASSWORD);
		String dbDrive = properties.getProperty(DATASTORE_DRIVER);
		String dbName = properties.getProperty(DATASTORE_NAME);

		LOGGER.info("Imagestore " + imageStoreIP + ":" + imageStorePort);
		init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName, dbUserPass, dbDrive,
				dbName);
	}

	public JDBCImageDataStore(String imageStoreURLPrefix, String imageStoreIP,
			String imageStorePort, String dbUserName, String dbUserPass, String dbDrive,
			String dbName) throws SQLException {

		init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName, dbUserPass, dbDrive,
				dbName);
	}

	private void init(String imageStoreIP, String imageStorePort, String imageStoreURLPrefix,
			String dbUserName, String dbUserPass, String dbDrive, String dbName)
			throws SQLException {
		connectionPool = createConnectionPool(imageStoreURLPrefix, imageStoreIP, imageStorePort,
				dbUserName, dbUserPass, dbDrive, dbName);
		createTable();
	}

	private void createTable() throws SQLException {

		Connection connection = null;
		Statement statement = null;

		try {
			connection = getConnection();
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS " + IMAGE_TABLE_NAME + "(" + TASK_ID_COL
					+ " VARCHAR(255) PRIMARY KEY, " + DATASET_COL + " VARCHAR(100), " + REGION_COL
					+ " VARCHAR(100), " + IMAGE_DATE_COL + " DATE, " + DOWNLOAD_LINK_COL
					+ " VARCHAR(255), " + STATE_COL + " VARCHAR(100), " + FEDERATION_MEMBER_COL
					+ " VARCHAR(255), " + PRIORITY_COL + " INTEGER, " + STATION_ID_COL
					+ " VARCHAR(255), " + INPUT_GATHERING_TAG + " VARCHAR(100), "
					+ INPUT_PREPROCESSING_TAG + " VARCHAR(100), " + ALGORITHM_EXECUTION_TAG
					+ " VARCHAR(100), " + ARCHIVER_VERSION_COL + " VARCHAR(255), "
					+ BLOWOUT_VERSION_COL + " VARCHAR(255), " + CREATION_TIME_COL + " TIMESTAMP, "
					+ UPDATED_TIME_COL + " TIMESTAMP, " + IMAGE_STATUS_COL + " VARCHAR(255), "
					+ ERROR_MSG_COL + " VARCHAR(255)" + ")");

			statement.execute("CREATE TABLE IF NOT EXISTS " + STATES_TABLE_NAME + "(" + TASK_ID_COL
					+ " VARCHAR(255), " + STATE_COL + " VARCHAR(100), " + UPDATED_TIME_COL
					+ " TIMESTAMP" + ")");

			statement.execute("CREATE TABLE IF NOT EXISTS " + USERS_TABLE_NAME + "("
					+ USER_EMAIL_COL + " VARCHAR(255) PRIMARY KEY, " + USER_NAME_COL
					+ " VARCHAR(255), " + USER_PASSWORD_COL + " VARCHAR(100), " + USER_STATE_COL
					+ " BOOLEAN, " + USER_NOTIFY_COL + " BOOLEAN, " + ADMIN_ROLE_COL + " BOOLEAN)");

			statement.execute("CREATE TABLE IF NOT EXISTS " + USERS_NOTIFY_TABLE_NAME + "("
					+ SUBMISSION_ID_COL + " VARCHAR(255), " + TASK_ID_COL + " VARCHAR(255), "
					+ USER_EMAIL_COL + " VARCHAR(255), " + " PRIMARY KEY(" + SUBMISSION_ID_COL
					+ ", " + TASK_ID_COL + ", " + USER_EMAIL_COL + "))");

			statement.execute("CREATE TABLE IF NOT EXISTS " + DEPLOY_CONFIG_TABLE_NAME + "("
					+ NFS_SERVER_IP_COL + " VARCHAR(100), " + NFS_SERVER_SSH_PORT_COL
					+ " VARCHAR(100), " + NFS_SERVER_PORT_COL + " VARCHAR(100), "
					+ FEDERATION_MEMBER_COL + " VARCHAR(255), " + " PRIMARY KEY("
					+ NFS_SERVER_IP_COL + ", " + NFS_SERVER_SSH_PORT_COL + ", "
					+ NFS_SERVER_PORT_COL + ", " + FEDERATION_MEMBER_COL + "))");

			statement.close();
		} catch (SQLException e) {
			LOGGER.error("Error while initializing DataStore", e);
			throw e;
		} finally {
			close(statement, connection);
		}
	}

	private BasicDataSource createConnectionPool(String imageStoreURLPrefix, String imageStoreIP,
			String imageStorePort, String dbUserName, String dbUserPass, String dbDriver,
			String dbName) {

		String url = imageStoreURLPrefix + imageStoreIP + ":" + imageStorePort + "/" + dbName;

		LOGGER.debug("DatastoreURL: " + url);

		BasicDataSource pool = new BasicDataSource();
		pool.setUsername(dbUserName);
		pool.setPassword(dbUserPass);
		pool.setDriverClassName(dbDriver);

		pool.setUrl(url);
		pool.setInitialSize(1);

		return pool;
	}

	public Connection getConnection() throws SQLException {

		try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			LOGGER.error("Error while getting a new connection from the connection pool", e);
			throw e;
		}
	}

	protected void close(Statement statement, Connection conn) {
		close(statement);

		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Couldn't close connection", e);
			}
		}
	}

	private void close(Statement statement) {
		if (statement != null) {
			try {
				if (!statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Couldn't close statement", e);
			}
		}
	}

	@Override
	public ImageTask addImageTask(String taskId, String dataset, String region, Date date,
			String downloadLink, int priority, String inputGathering, String inputPreprocessing,
			String algorithmExecution) throws SQLException {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		ImageTask task = new ImageTask(taskId, dataset, region, date, downloadLink,
				ImageTaskState.CREATED, ImageTask.NON_EXISTENT, priority, ImageTask.NON_EXISTENT,
				inputGathering, inputPreprocessing, algorithmExecution, ImageTask.NON_EXISTENT,
				ImageTask.NON_EXISTENT, now, now, ImageTask.AVAILABLE, ImageTask.NON_EXISTENT);
		addImageTask(task);
		return task;
	}

	@Override
	public void addImageTask(ImageTask imageTask) throws SQLException {
		if (imageTask.getTaskId() == null || imageTask.getTaskId().isEmpty()) {
			LOGGER.error("Task with empty id.");
			throw new IllegalArgumentException("Task with empty id.");
		}
		if (imageTask.getDataset() == null || imageTask.getDataset().isEmpty()) {
			LOGGER.error("Task with empty dataset.");
			throw new IllegalArgumentException("Task with empty dataset.");
		}
		if (imageTask.getImageDate() == null) {
			LOGGER.error("Task must have a date.");
			throw new IllegalArgumentException("Task must have a date.");
		}
		if (imageTask.getDownloadLink() == null || imageTask.getDownloadLink().isEmpty()) {
			LOGGER.error("Task must have a download link.");
			throw new IllegalArgumentException("Task must have a download link.");
		}
		LOGGER.info("Adding image task " + imageTask.getTaskId() + " with download link "
				+ imageTask.getDownloadLink() + " and priority " + imageTask.getPriority());

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_FULL_IMAGE_TASK_SQL);
			insertStatement.setString(1, imageTask.getTaskId());
			insertStatement.setString(2, imageTask.getDataset());
			insertStatement.setString(3, imageTask.getRegion());
			insertStatement.setObject(4, imageTask.getImageDate());
			insertStatement.setString(5, imageTask.getDownloadLink());
			insertStatement.setString(6, imageTask.getState().getValue());
			insertStatement.setString(7, imageTask.getFederationMember());
			insertStatement.setInt(8, imageTask.getPriority());
			insertStatement.setString(9, imageTask.getStationId());
			insertStatement.setString(10, imageTask.getInputGatheringTag());
			insertStatement.setString(11, imageTask.getInputPreprocessingTag());
			insertStatement.setString(12, imageTask.getAlgorithmExecutionTag());
			insertStatement.setString(13, imageTask.getArchiverVersion());
			insertStatement.setString(14, imageTask.getBlowoutVersion());
			insertStatement.setTimestamp(15, imageTask.getCreationTime());
			insertStatement.setTimestamp(16, imageTask.getUpdateTime());
			insertStatement.setString(17, imageTask.getStatus());
			insertStatement.setString(18, imageTask.getError());
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_USER_NOTIFICATION_SQL = "INSERT INTO "
			+ USERS_NOTIFY_TABLE_NAME + " VALUES(?, ?, ?)";

	@Override
	public void addUserNotification(String submissionId, String taskId, String userEmail)
			throws SQLException {
		LOGGER.info("Adding image task " + taskId + " from submission " + submissionId
				+ " notification for " + userEmail);
		if (taskId == null || taskId.isEmpty() || userEmail == null || userEmail.isEmpty()) {
			throw new IllegalArgumentException(
					"Invalid task id " + taskId + " or user " + userEmail);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_USER_NOTIFICATION_SQL);
			insertStatement.setString(1, submissionId);
			insertStatement.setString(2, taskId);
			insertStatement.setString(3, userEmail);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_DEPLOY_CONFIG_SQL = "INSERT INTO " + DEPLOY_CONFIG_TABLE_NAME
			+ " VALUES(?, ?, ?, ?)";

	@Override
	public void addDeployConfig(String nfsIP, String nfsSshPort, String nfsPort,
			String federationMember) throws SQLException {
		LOGGER.info("Adding NFS IP " + nfsIP + " and port " + nfsPort + " from " + federationMember
				+ " in DB");
		if (nfsIP == null || nfsIP.isEmpty() || nfsSshPort == null || nfsSshPort.isEmpty()
				|| nfsPort == null || nfsPort.isEmpty() || federationMember == null
				|| federationMember.isEmpty()) {
			throw new IllegalArgumentException(
					"Invalid NFS IP " + nfsIP + ", ssh port " + nfsSshPort + ", port " + nfsPort
							+ " or federation member " + federationMember);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_DEPLOY_CONFIG_SQL);
			insertStatement.setString(1, nfsIP);
			insertStatement.setString(2, nfsSshPort);
			insertStatement.setString(3, nfsPort);
			insertStatement.setString(4, federationMember);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String SELECT_ALL_USERS_TO_NOTIFY_SQL = "SELECT * FROM "
			+ USERS_NOTIFY_TABLE_NAME;

	@Override
	public List<Ward> getUsersToNotify() throws SQLException {

		LOGGER.debug("Getting all users to notify");

		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();
			statement.setQueryTimeout(300);

			statement.execute(SELECT_ALL_USERS_TO_NOTIFY_SQL);
			ResultSet rs = statement.getResultSet();
			List<Ward> wards = extractUsersToNotifyFrom(rs);
			return wards;
		} finally {
			close(statement, conn);
		}
	}

	private static final String SELECT_NFS_CONFIG_SQL = "SELECT nfs_ip, nfs_port FROM "
			+ DEPLOY_CONFIG_TABLE_NAME + " WHERE federation_member = ?";

	@Override
	public Map<String, String> getFederationNFSConfig(String federationMember) throws SQLException {

		LOGGER.debug("Getting NFS configuration for " + federationMember);

		PreparedStatement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_NFS_CONFIG_SQL);
			statement.setString(1, federationMember);
			statement.setQueryTimeout(300);

			statement.execute();

			ResultSet rs = statement.getResultSet();
			HashMap<String, String> nfsConfig = extractNFSConfigFrom(rs);
			return nfsConfig;
		} finally {
			close(statement, conn);
		}
	}

	private List<Ward> extractUsersToNotifyFrom(ResultSet rs) throws SQLException {

		List<Ward> wards = new ArrayList<>();

		while (rs.next()) {
			wards.add(new Ward(rs.getString(SUBMISSION_ID_COL), rs.getString(TASK_ID_COL),
					ImageTaskState.ARCHIVED, rs.getString(USER_EMAIL_COL)));
		}

		return wards;
	}

	private HashMap<String, String> extractNFSConfigFrom(ResultSet rs) throws SQLException {

		HashMap<String, String> nfsConfig = new HashMap<String, String>();

		while (rs.next()) {
			nfsConfig.put(rs.getString(NFS_SERVER_IP_COL), rs.getString(NFS_SERVER_PORT_COL));
		}

		return nfsConfig;
	}

	private static final String SELECT_USER_NOTIFIABLE_SQL = "SELECT " + USER_NOTIFY_COL + " FROM "
			+ USERS_TABLE_NAME + " WHERE " + USER_EMAIL_COL + " = ?";

	@Override
	public boolean isUserNotifiable(String userEmail) throws SQLException {
		LOGGER.debug("Verifying if user is notifiable");

		PreparedStatement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_USER_NOTIFIABLE_SQL);
			statement.setString(1, userEmail);
			statement.setQueryTimeout(300);

			statement.execute();

			ResultSet rs = statement.getResultSet();
			rs.next();
			return rs.getBoolean(1);
		} finally {
			close(statement, conn);
		}
	}

	private static final String SELECT_CHECK_FEDERATION_EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM "
			+ DEPLOY_CONFIG_TABLE_NAME + " WHERE " + FEDERATION_MEMBER_COL + " = ?)";

	@Override
	public boolean deployConfigExists(String federationMember) throws SQLException {
		LOGGER.debug(
				"Verifying if a deploy config for " + federationMember + " exists in database");

		PreparedStatement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_CHECK_FEDERATION_EXISTS_SQL);
			statement.setString(1, federationMember);
			statement.setQueryTimeout(300);

			statement.execute();

			ResultSet rs = statement.getResultSet();
			return rs.next();
		} finally {
			close(statement, conn);
		}
	}

	// TODO: verify this later
	@Override
	public boolean taskExist(String collectionTierImageName) throws SQLException {
		LOGGER.debug("Verifying if a image " + collectionTierImageName + " exist in database");

		PreparedStatement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_CHECK_FEDERATION_EXISTS_SQL);
			statement.setString(1, collectionTierImageName);
			statement.setQueryTimeout(300);

			statement.execute();

			ResultSet rs = statement.getResultSet();
			return rs.next();
		} finally {
			close(statement, conn);
		}
	}

	private static final String REMOVE_USER_NOTIFY_SQL = "DELETE FROM " + USERS_NOTIFY_TABLE_NAME
			+ " WHERE " + SUBMISSION_ID_COL + " = ? AND " + TASK_ID_COL + " = ? AND "
			+ USER_EMAIL_COL + " = ?";

	@Override
	public void removeNotification(String submissionId, String taskId, String userEmail)
			throws SQLException {
		LOGGER.debug("Removing task " + taskId + " notification for " + userEmail);
		if (submissionId == null || submissionId.isEmpty() || taskId == null || taskId.isEmpty()
				|| userEmail == null || userEmail.isEmpty()) {
			throw new IllegalArgumentException("Invalid submissionId " + submissionId + ", taskId "
					+ taskId + " or user " + userEmail);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(REMOVE_USER_NOTIFY_SQL);
			insertStatement.setString(1, submissionId);
			insertStatement.setString(2, taskId);
			insertStatement.setString(3, userEmail);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String REMOVE_DEPLOY_CONFIG_SQL = "DELETE FROM " + DEPLOY_CONFIG_TABLE_NAME
			+ " WHERE " + FEDERATION_MEMBER_COL + " = ?";

	@Override
	public void removeDeployConfig(String federationMember) throws SQLException {
		LOGGER.debug("Removing register for " + federationMember + " from database");
		if (federationMember == null || federationMember.isEmpty()) {
			throw new IllegalArgumentException("Invalid federationMember " + federationMember);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;
		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(REMOVE_DEPLOY_CONFIG_SQL);
			insertStatement.setString(1, federationMember);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_NEW_STATE_TIMESTAMP_SQL = "INSERT INTO " + STATES_TABLE_NAME
			+ " VALUES(?, ?, ?)";

	@Override
	public void addStateStamp(String taskId, ImageTaskState state, Timestamp timestamp)
			throws SQLException {
		if (taskId == null || taskId.isEmpty() || state == null) {
			LOGGER.error("Task id or state was null.");
			throw new IllegalArgumentException("Task id or state was null.");
		}
		LOGGER.info("Adding task " + taskId + " state " + state.getValue() + " with timestamp "
				+ timestamp + " into Catalogue");

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_NEW_STATE_TIMESTAMP_SQL);
			insertStatement.setString(1, taskId);
			insertStatement.setString(2, state.getValue());
			insertStatement.setTimestamp(3, timestamp);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_NEW_USER_SQL = "INSERT INTO " + USERS_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?)";

	@Override
	public void addUser(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException {

		LOGGER.info("Adding user " + userName + " into DB");
		if (userName == null || userName.isEmpty() || userPass == null || userPass.isEmpty()) {
			throw new IllegalArgumentException("Invalid user " + userName);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_NEW_USER_SQL);
			insertStatement.setString(1, userEmail);
			insertStatement.setString(2, userName);
			insertStatement.setString(3, userPass);
			insertStatement.setBoolean(4, userState);
			insertStatement.setBoolean(5, userNotify);
			insertStatement.setBoolean(6, adminRole);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static String UPDATE_USER_STATE_SQL = "UPDATE " + USERS_TABLE_NAME
			+ " SET active = ? WHERE user_email = ?";

	@Override
	public void updateUserState(String userEmail, boolean userState) throws SQLException {

		LOGGER.info("Updating user " + userEmail + " state to " + userState);
		if (userEmail == null || userEmail.isEmpty()) {
			throw new IllegalArgumentException("Invalid user " + userEmail);
		}

		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_USER_STATE_SQL);
			updateStatement.setBoolean(1, userState);
			updateStatement.setString(2, userEmail);
			updateStatement.setQueryTimeout(300);

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private static String UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME + " SET "
			+ STATE_COL + " = ?, " + UPDATED_TIME_COL + " = now() " + "WHERE " + TASK_ID_COL
			+ " = ?";

	@Override
	public void updateTaskState(String taskId, ImageTaskState state) throws SQLException {

		if (taskId == null || taskId.isEmpty() || state == null) {
			LOGGER.error("Invalid image task " + taskId + " or state " + state);
			throw new IllegalArgumentException(
					"Invalid image task " + taskId + " or state " + state);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_IMAGE_STATE_SQL);
			updateStatement.setString(1, state.getValue());
			updateStatement.setString(2, taskId);
			updateStatement.setQueryTimeout(300);

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private static String UPDATE_ERROR_MESSAGE_SQL = "UPDATE " + IMAGE_TABLE_NAME + " SET "
			+ ERROR_MSG_COL + " = ?, utime = now() WHERE task_id = ?";

	@Override
	public void updateTaskError(String taskId, String errorMsg) throws SQLException {

		if (taskId == null || taskId.isEmpty() || errorMsg == null) {
			LOGGER.error("Invalid image task " + taskId + " or error message: " + errorMsg);
			throw new IllegalArgumentException(
					"Invalid image task " + taskId + " or error message: " + errorMsg);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_ERROR_MESSAGE_SQL);
			updateStatement.setString(1, errorMsg);
			updateStatement.setString(2, taskId);
			updateStatement.setQueryTimeout(300);

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private static final String UPDATE_IMAGEDATA_SQL = "UPDATE " + IMAGE_TABLE_NAME + " SET "
			+ DOWNLOAD_LINK_COL + " = ?, " + STATE_COL + " = ?, " + FEDERATION_MEMBER_COL + " = ?, "
			+ PRIORITY_COL + " = ?, " + STATION_ID_COL + " = ?, " + ALGORITHM_EXECUTION_TAG
			+ " = ?, " + ARCHIVER_VERSION_COL + " = ?, " + BLOWOUT_VERSION_COL + " = ?, "
			+ UPDATED_TIME_COL + " = now(), " + IMAGE_STATUS_COL + " = ?, " + ERROR_MSG_COL
			+ " = ? " + "WHERE " + TASK_ID_COL + " = ?";

	@Override
	public void updateImageTask(ImageTask imagetask) throws SQLException {
		if (imagetask == null) {
			LOGGER.error("Trying to update null image task.");
			throw new IllegalArgumentException("Trying to update null image task.");
		}

		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_IMAGEDATA_SQL);
			updateStatement.setString(1, imagetask.getDownloadLink());
			updateStatement.setString(2, imagetask.getState().getValue());
			updateStatement.setString(3, imagetask.getFederationMember());
			updateStatement.setInt(4, imagetask.getPriority());
			updateStatement.setString(5, imagetask.getStationId());
			updateStatement.setString(6, imagetask.getAlgorithmExecutionTag());
			updateStatement.setString(7, imagetask.getArchiverVersion());
			updateStatement.setString(8, imagetask.getBlowoutVersion());
			updateStatement.setString(9, imagetask.getStatus());
			updateStatement.setString(10, imagetask.getError());
			updateStatement.setString(11, imagetask.getTaskId());
			updateStatement.setQueryTimeout(300);

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private static final String UPDATE_IMAGE_METADATA_SQL = "UPDATE " + IMAGE_TABLE_NAME
			+ " SET station_id = ?, utime = now() WHERE task_id = ?";

	@Override
	public void updateImageMetadata(String taskId, String stationId) throws SQLException {
		if (taskId == null || taskId.isEmpty() || stationId == null || stationId.isEmpty()) {
			LOGGER.error("Invalid image task " + taskId + " or station ID " + stationId);
			throw new IllegalArgumentException(
					"Invalid image task " + taskId + " or station ID " + stationId);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_IMAGE_METADATA_SQL);
			updateStatement.setString(1, stationId);
			updateStatement.setString(2, taskId);
			updateStatement.setQueryTimeout(300);

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	@Override
	public void dispose() {
		try {
			this.connectionPool.close();
		} catch (SQLException e) {
			LOGGER.error("Error wile closing ConnectionPool", e);
		}
	}

	private static final String SELECT_ALL_SUBMISSIONS_SQL = "SELECT " + SUBMISSION_ID_COL
			+ " FROM " + USERS_NOTIFY_TABLE_NAME;

	@Override
	public List<Submission> getAllSubmissions() throws SQLException {
		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();
			statement.setQueryTimeout(300);

			statement.execute(SELECT_ALL_SUBMISSIONS_SQL);
			ResultSet rs = statement.getResultSet();
			List<Submission> allSubmissions = extractSubmissionFrom(rs);
			return allSubmissions;
		} finally {
			close(statement, conn);
		}
	}

	private List<Submission> extractSubmissionFrom(ResultSet rs) throws SQLException {
		List<Submission> allSubmissions = new ArrayList<Submission>();

		Submission previousSubmission = null;
		while (rs.next()) {
			if (previousSubmission == null) {
				previousSubmission = new Submission(rs.getString(SUBMISSION_ID_COL));
				previousSubmission.addTask(new Task(rs.getString(TASK_ID_COL)));
			} else if (previousSubmission.getId().equals(rs.getString(SUBMISSION_ID_COL))) {
				previousSubmission.addTask(new Task(rs.getString(TASK_ID_COL)));
			} else if (!previousSubmission.getId().equals(rs.getString(SUBMISSION_ID_COL))) {
				allSubmissions.add(previousSubmission);
				previousSubmission = new Submission(rs.getString(SUBMISSION_ID_COL));
				previousSubmission.addTask(new Task(rs.getString(TASK_ID_COL)));
			}
		}

		return allSubmissions;
	}

	private static final String SELECT_SUBMISSION_SQL = "SELECT * FROM " + USERS_NOTIFY_TABLE_NAME
			+ " WHERE " + SUBMISSION_ID_COL + " = ?";

	@Override
	public Submission getSubmission(String submissionId) throws SQLException {
		PreparedStatement preparedStatement = null;
		Connection connection = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(SELECT_SUBMISSION_SQL);
			preparedStatement.setString(1, submissionId);
			preparedStatement.setQueryTimeout(300);

			preparedStatement.execute();
			ResultSet rs = preparedStatement.getResultSet();
			Submission submission = null;

			while (rs.next()) {
				if (submission == null) {
					submission = new Submission(rs.getString(SUBMISSION_ID_COL));
				}
				submission.addTask(
						new Task(rs.getString(TASK_ID_COL), getTask(rs.getString(TASK_ID_COL))));
			}
			return submission;
		} finally {
			close(preparedStatement, connection);
		}
	}

	private static final String SELECT_ALL_IMAGES_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME;

	@Override
	public List<ImageTask> getAllTasks() throws SQLException {
		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();
			statement.setQueryTimeout(300);

			statement.execute(SELECT_ALL_IMAGES_SQL);
			ResultSet rs = statement.getResultSet();
			return extractImageTaskFrom(rs);
		} finally {
			close(statement, conn);
		}
	}

	private static final String SELECT_USER_SQL = "SELECT * FROM " + USERS_TABLE_NAME + " WHERE "
			+ USER_EMAIL_COL + " = ?";

	@Override
	public SapsUser getUser(String userEmail) throws SQLException {

		if (userEmail == null || userEmail.isEmpty()) {
			LOGGER.error("Invalid userEmail " + userEmail);
			return null;
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatement = connection.prepareStatement(SELECT_USER_SQL);
			selectStatement.setString(1, userEmail);
			selectStatement.setQueryTimeout(300);

			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			if (rs.next()) {
				SapsUser sebalUser = extractSapsUserFrom(rs);
				return sebalUser;
			}
			rs.close();
			return null;
		} finally {
			close(selectStatement, connection);
		}
	}

	private static final String SELECT_IMAGES_IN_STATE_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE " + STATE_COL + " = ? " + "ORDER BY priority ASC";

	private static final String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME + " WHERE state = ? ORDER BY priority ASC LIMIT ?";

	@Override
	public List<ImageTask> getIn(ImageTaskState state, int limit) throws SQLException {
		if (state == null) {
			LOGGER.error("A state must be given");
			throw new IllegalArgumentException("Can't recover tasks. State was null.");
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			if (limit == UNLIMITED) {
				selectStatement = connection.prepareStatement(SELECT_IMAGES_IN_STATE_SQL);
				selectStatement.setString(1, state.getValue());
				selectStatement.setQueryTimeout(300);

				selectStatement.execute();
			} else {
				selectStatement = connection.prepareStatement(SELECT_LIMITED_IMAGES_IN_STATE_SQL);
				selectStatement.setString(1, state.getValue());
				selectStatement.setInt(2, limit);
				selectStatement.setQueryTimeout(300);

				selectStatement.execute();
			}

			ResultSet rs = selectStatement.getResultSet();
			List<ImageTask> imageDatas = extractImageTaskFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, connection);
		}
	}

	private static final String SELECT_IMAGES_BY_FILTERS_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_WHERE_SQL = " WHERE ";
	private static final String SELECT_IMAGES_BY_FILTERS_STATE_SQL = " state = ? "
			+ IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_NAME_SQL = " task_id = ? "
			+ IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_PERIOD = " ctime BETWEEN ? AND ? ";

	@Override
	public List<ImageTask> getTasksByFilter(ImageTaskState state, String taskId,
			long processDateInit, long processDateEnd) throws SQLException {

		PreparedStatement selectStatement = null;
		Connection connection = null;

		int paramtersCount = 0;
		int paramtersInsertCount = 0;

		StringBuilder finalQuery = new StringBuilder();
		finalQuery.append(SELECT_IMAGES_BY_FILTERS_SQL);
		if (state != null) {
			if (paramtersCount == 0) {
				finalQuery.append(SELECT_IMAGES_BY_FILTERS_WHERE_SQL);
			}
			finalQuery.append(SELECT_IMAGES_BY_FILTERS_STATE_SQL);
			paramtersCount++;
		}

		if (taskId != null && !taskId.trim().isEmpty()) {
			if (paramtersCount == 0) {
				finalQuery.append(SELECT_IMAGES_BY_FILTERS_WHERE_SQL);
			} else {
				finalQuery.append(" AND ");
			}
			finalQuery.append(SELECT_IMAGES_BY_FILTERS_NAME_SQL);
			paramtersCount++;
		}

		if (processDateInit > 0 && processDateEnd > 0) {
			if (paramtersCount == 0) {
				finalQuery.append(SELECT_IMAGES_BY_FILTERS_WHERE_SQL);
			} else {
				finalQuery.append(" AND ");
			}
			finalQuery.append(SELECT_IMAGES_BY_FILTERS_PERIOD);
			paramtersCount++;
			paramtersCount++;// Has tow parameters (processDateInit and
								// processDateEnd)
		}

		try {
			connection = getConnection();

			selectStatement = connection.prepareStatement(finalQuery.toString());

			if (state != null) {
				selectStatement.setString(++paramtersInsertCount, state.getValue());
			}

			if (taskId != null && !taskId.trim().isEmpty()) {
				selectStatement.setString(++paramtersInsertCount, taskId);
			}

			if (processDateInit > 0 && processDateEnd > 0) {
				selectStatement.setLong(++paramtersInsertCount, processDateInit);
				selectStatement.setLong(++paramtersInsertCount, processDateEnd);
			}

			selectStatement.setQueryTimeout(300);
			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageTask> imageDatas = extractImageTaskFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, connection);
		}
	}

	@Override
	public List<ImageTask> getIn(ImageTaskState state) throws SQLException {
		return getIn(state, UNLIMITED);
	}

	private static final String SELECT_PURGED_IMAGES_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE status = ? ORDER BY priority, task_id";

	@Override
	public List<ImageTask> getPurgedTasks() throws SQLException {
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatement = connection.prepareStatement(SELECT_PURGED_IMAGES_SQL);
			selectStatement.setString(1, ImageTask.PURGED);
			selectStatement.setQueryTimeout(300);

			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageTask> imageDatas = extractImageTaskFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, connection);
		}

	}

	private static final String UPDATE_LIMITED_IMAGES_TO_DOWNLOAD = "UPDATE " + IMAGE_TABLE_NAME
			+ " SET " + STATE_COL + " = ?, " + FEDERATION_MEMBER_COL + " = ?, " + UPDATED_TIME_COL
			+ " = now() WHERE " + TASK_ID_COL + " = ?";

	private static final String SELECT_DOWNLOADING_IMAGES_BY_FEDERATION_MEMBER = "SELECT * FROM "
			+ IMAGE_TABLE_NAME + " WHERE " + STATE_COL + " = ? AND " + IMAGE_STATUS_COL
			+ " = ? AND " + FEDERATION_MEMBER_COL + " = ? LIMIT ?";

	private static final String SELECT_CREATED_IMAGE = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE " + STATE_COL + " = ? AND " + IMAGE_STATUS_COL + " = ? LIMIT ?";

	/**
	 * This method selects and locks all images marked as CREATED and updates to
	 * DOWNLOADING and changes the federation member to the crawler ID and then
	 * selects and returns the updated images based on the state and federation
	 * member.
	 */
	@Override
	public List<ImageTask> getImagesToDownload(String federationMember, int limit)
			throws SQLException {
		// TODO: fix this method
		/*
		 * In future versions, if the crawler starts to use a multithread approach this
		 * method needs to be reviewed to avoid concurrency problems between its
		 * threads. As the crawler selects images where the state is DOWNLOADING and
		 * federation member is equal to its ID, new threads could start to download an
		 * image that is already been downloaded by the another thread.
		 */

		if (federationMember == null) {
			LOGGER.error("Invalid federation member " + federationMember);
			throw new IllegalArgumentException("Invalid federation member " + federationMember);
		}

		PreparedStatement lockAndUpdateStatement = null;
		PreparedStatement selectStatement = null;
		PreparedStatement selectStatementLimit = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatementLimit = connection.prepareStatement(SELECT_CREATED_IMAGE);
			selectStatementLimit.setString(1, ImageTaskState.CREATED.getValue());
			selectStatementLimit.setString(2, ImageTask.AVAILABLE);
			selectStatementLimit.setInt(3, limit);
			selectStatementLimit.setQueryTimeout(300);
			selectStatementLimit.execute();

			ResultSet rs = selectStatementLimit.getResultSet();
			List<ImageTask> createdImageTasks = extractImageTaskFrom(rs);
			rs.close();

			lockAndUpdateStatement = connection.prepareStatement(UPDATE_LIMITED_IMAGES_TO_DOWNLOAD);
			lockAndUpdateStatement.setString(1, ImageTaskState.DOWNLOADING.getValue());
			lockAndUpdateStatement.setString(2, federationMember);
			lockAndUpdateStatement.setString(3, createdImageTasks.get(0).getTaskId());
			lockAndUpdateStatement.setQueryTimeout(300);
			lockAndUpdateStatement.execute();

			selectStatement = connection
					.prepareStatement(SELECT_DOWNLOADING_IMAGES_BY_FEDERATION_MEMBER);
			selectStatement.setString(1, ImageTaskState.DOWNLOADING.getValue());
			selectStatement.setString(2, ImageTask.AVAILABLE);
			selectStatement.setString(3, federationMember);
			selectStatement.setInt(4, limit);
			selectStatement.setQueryTimeout(300);
			selectStatement.execute();

			rs = selectStatement.getResultSet();
			List<ImageTask> downloadingImageTasks = extractImageTaskFrom(rs);
			rs.close();
			return downloadingImageTasks;
		} finally {
			close(selectStatement, null);
			close(lockAndUpdateStatement, connection);
		}
	}

	private static SapsUser extractSapsUserFrom(ResultSet rs) throws SQLException {
		SapsUser sebalUser = new SapsUser(rs.getString(USER_EMAIL_COL), rs.getString(USER_NAME_COL),
				rs.getString(USER_PASSWORD_COL), rs.getBoolean(USER_STATE_COL),
				rs.getBoolean(USER_NOTIFY_COL), rs.getBoolean(ADMIN_ROLE_COL));

		return sebalUser;
	}

	private static List<ImageTask> extractImageTaskFrom(ResultSet rs) throws SQLException {
		List<ImageTask> imageTasks = new ArrayList<>();
		while (rs.next()) {
			imageTasks.add(new ImageTask(rs.getString(TASK_ID_COL), rs.getString(DATASET_COL),
					rs.getString(REGION_COL), rs.getDate(IMAGE_DATE_COL),
					rs.getString(DOWNLOAD_LINK_COL),
					ImageTaskState.getStateFromStr(rs.getString(STATE_COL)),
					rs.getString(FEDERATION_MEMBER_COL), rs.getInt(PRIORITY_COL),
					rs.getString(STATION_ID_COL), rs.getString(INPUT_GATHERING_TAG),
					rs.getString(INPUT_PREPROCESSING_TAG), rs.getString(ALGORITHM_EXECUTION_TAG),
					rs.getString(ARCHIVER_VERSION_COL), rs.getString(BLOWOUT_VERSION_COL),
					rs.getTimestamp(CREATION_TIME_COL), rs.getTimestamp(UPDATED_TIME_COL),
					rs.getString(IMAGE_STATUS_COL), rs.getString(ERROR_MSG_COL)));
		}
		return imageTasks;
	}

	private static final String SELECT_TASK_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE task_id = ?";

	@Override
	public ImageTask getTask(String taskId) throws SQLException {
		if (taskId == null) {
			LOGGER.error("Invalid image task " + taskId);
			throw new IllegalArgumentException("Invalid image task " + taskId);
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatement = connection.prepareStatement(SELECT_TASK_SQL);
			selectStatement.setString(1, taskId);
			selectStatement.setQueryTimeout(300);

			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageTask> imageDatas = extractImageTaskFrom(rs);
			rs.close();
			return imageDatas.get(0);
		} finally {
			close(selectStatement, connection);
		}
	}

	private static final String SELECT_NFS_SERVER_IP_SQL = "SELECT nfs_ip FROM "
			+ DEPLOY_CONFIG_TABLE_NAME + " WHERE federation_member = ?";

	@Override
	public String getNFSServerIP(String federation_member) throws SQLException {
		if (federation_member == null || federation_member.isEmpty()) {
			LOGGER.error("Invalid federationMember " + federation_member);
			throw new IllegalArgumentException("Invalid federationMember " + federation_member);
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatement = connection.prepareStatement(SELECT_NFS_SERVER_IP_SQL);
			selectStatement.setString(1, federation_member);
			selectStatement.setQueryTimeout(300);

			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			if (rs.next()) {
				return rs.getString(NFS_SERVER_IP_COL);
			}
			rs.close();
			return null;
		} finally {
			close(selectStatement, connection);
		}
	}

	private static final String SELECT_NFS_SERVER_SSH_PORT_SQL = "SELECT nfs_ssh_port FROM "
			+ DEPLOY_CONFIG_TABLE_NAME + " WHERE federation_member = ?";

	@Override
	public String getNFSServerSshPort(String federation_member) throws SQLException {
		if (federation_member == null || federation_member.isEmpty()) {
			LOGGER.error("Invalid federationMember " + federation_member);
			throw new IllegalArgumentException("Invalid federationMember " + federation_member);
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatement = connection.prepareStatement(SELECT_NFS_SERVER_SSH_PORT_SQL);
			selectStatement.setString(1, federation_member);
			selectStatement.setQueryTimeout(300);

			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			if (rs.next()) {
				return rs.getString(NFS_SERVER_SSH_PORT_COL);
			}
			rs.close();
			return null;
		} finally {
			close(selectStatement, connection);
		}
	}

	private final String LOCK_IMAGE_SQL = "SELECT pg_try_advisory_lock(?) FROM " + IMAGE_TABLE_NAME
			+ " WHERE task_id = ?";

	@Override
	public boolean lockTask(String taskId) throws SQLException {
		if (taskId == null) {
			LOGGER.error("Invalid taskId " + taskId);
			throw new IllegalArgumentException("Invalid state " + taskId);
		}

		PreparedStatement lockImageStatement = null;
		Connection connection = null;

		boolean locked = false;
		try {
			connection = getConnection();
			lockImageStatement = connection.prepareStatement(LOCK_IMAGE_SQL);
			final int imageHashCode = taskId.hashCode();
			lockImageStatement.setInt(1, imageHashCode);
			lockImageStatement.setString(2, taskId);
			lockImageStatement.setQueryTimeout(300);

			ResultSet rs = lockImageStatement.executeQuery();
			if (rs.next()) {
				locked = rs.getBoolean(1);
			}

			if (locked) {
				lockedImages.put(taskId, connection);
				close(lockImageStatement);
			}
		} finally {
			if (!locked) {
				close(lockImageStatement, connection);
			}
		}
		return locked;
	}

	private final String UNLOCK_IMAGE_SQL = "SELECT pg_advisory_unlock(?)";

	@Override
	public boolean unlockTask(String taskId) throws SQLException {
		if (taskId == null) {
			LOGGER.error("Invalid taskId " + taskId);
			throw new IllegalArgumentException("Invalid state " + taskId);
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		boolean unlocked = false;
		if (lockedImages.containsKey(taskId)) {
			connection = lockedImages.get(taskId);
			try {
				selectStatement = connection.prepareStatement(UNLOCK_IMAGE_SQL);
				selectStatement.setInt(1, taskId.hashCode());
				selectStatement.setQueryTimeout(300);

				ResultSet rs = selectStatement.executeQuery();

				if (rs.next()) {
					unlocked = rs.getBoolean(1);
				}

				lockedImages.remove(taskId);
			} finally {
				close(selectStatement, connection);
			}
		}
		return unlocked;
	}

	private static final String REMOVE_STATE_SQL = "DELETE FROM " + STATES_TABLE_NAME
			+ " WHERE task_id = ? AND state = ? AND utime = ?";

	@Override
	public void removeStateStamp(String taskId, ImageTaskState state, Timestamp timestamp)
			throws SQLException {
		LOGGER.info("Removing task " + taskId + " state " + state.getValue() + " with timestamp "
				+ timestamp);
		if (taskId == null || taskId.isEmpty() || state == null) {
			LOGGER.error("Invalid task " + taskId + " or state " + state.getValue());
			throw new IllegalArgumentException("Invalid task " + taskId);
		}

		PreparedStatement removeStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			removeStatement = connection.prepareStatement(REMOVE_STATE_SQL);
			removeStatement.setString(1, taskId);
			removeStatement.setString(2, state.getValue());
			removeStatement.setTimestamp(3, timestamp);
			removeStatement.setQueryTimeout(300);

			removeStatement.execute();
		} finally {
			close(removeStatement, connection);
		}
	}
}
