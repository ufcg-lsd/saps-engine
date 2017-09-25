package org.fogbowcloud.saps.engine.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
	protected static final String IMAGE_TABLE_NAME = "NASA_IMAGES";
	protected static final String STATES_TABLE_NAME = "STATES_TIMESTAMPS";
	private static final String TASK_ID_COL = "task_id";
	private static final String IMAGE_NAME_COL = "image_name";
	private static final String DOWNLOAD_LINK_COL = "download_link";
	private static final String PRIORITY_COL = "priority";
	private static final String FEDERATION_MEMBER_COL = "federation_member";
	private static final String STATE_COL = "state";
	private static final String STATION_ID_COL = "station_id";
	private static final String CONTAINER_REPOSITORY_COL = "container_repository";
	private static final String CONTAINER_TAG_COL = "container_tag";
	private static final String CRAWLER_VERSION_COL = "crawler_version";
	private static final String FETCHER_VERSION_COL = "fetcher_version";
	private static final String BLOWOUT_VERSION_COL = "blowout_version";
	private static final String FMASK_VERSION_COL = "fmask_version";
	private static final String CREATION_TIME_COL = "ctime";
	private static final String UPDATED_TIME_COL = "utime";
	private static final String IMAGE_STATUS_COL = "status";
	private static final String ERROR_MSG_COL = "error_msg";
	private static final String COLLECTION_TIER_IMAGE_NAME_COL = "tier_collection_image_name";

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

	private Map<String, Connection> lockedImages = new ConcurrentHashMap<String, Connection>();
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
					+ " VARCHAR(255) PRIMARY KEY, " + IMAGE_NAME_COL + " VARCHAR(255), "
					+ DOWNLOAD_LINK_COL + " VARCHAR(255), " + STATE_COL + " VARCHAR(100), "
					+ FEDERATION_MEMBER_COL + " VARCHAR(255), " + PRIORITY_COL + " INTEGER, "
					+ STATION_ID_COL + " VARCHAR(255), " + CONTAINER_REPOSITORY_COL
					+ " VARCHAR(255), " + CONTAINER_TAG_COL + " VARCHAR(255), "
					+ CRAWLER_VERSION_COL + " VARCHAR(255), " + FETCHER_VERSION_COL
					+ " VARCHAR(255), " + BLOWOUT_VERSION_COL + " VARCHAR(255), "
					+ FMASK_VERSION_COL + " VARCHAR(255), " + CREATION_TIME_COL + " TIMESTAMP, "
					+ UPDATED_TIME_COL + " TIMESTAMP, " + IMAGE_STATUS_COL + " VARCHAR(255), "
					+ ERROR_MSG_COL + " VARCHAR(255), " + COLLECTION_TIER_IMAGE_NAME_COL
					+ " VARCHAR(255))");

			statement.execute("CREATE TABLE IF NOT EXISTS " + STATES_TABLE_NAME + "(" + TASK_ID_COL
					+ " VARCHAR(255), " + IMAGE_NAME_COL + " VARCHAR(255), " + STATE_COL
					+ " VARCHAR(100), " + UPDATED_TIME_COL + " TIMESTAMP, " + ERROR_MSG_COL
					+ " VARCHAR(255))");

			statement.execute("CREATE TABLE IF NOT EXISTS " + USERS_TABLE_NAME + "("
					+ USER_EMAIL_COL + " VARCHAR(255) PRIMARY KEY, " + USER_NAME_COL
					+ " VARCHAR(255), " + USER_PASSWORD_COL + " VARCHAR(100), " + USER_STATE_COL
					+ " BOOLEAN, " + USER_NOTIFY_COL + " BOOLEAN, " + ADMIN_ROLE_COL + " BOOLEAN)");

			statement.execute("CREATE TABLE IF NOT EXISTS " + USERS_NOTIFY_TABLE_NAME + "("
					+ SUBMISSION_ID_COL + " VARCHAR(255), " + TASK_ID_COL + " VARCHAR(255), "
					+ IMAGE_NAME_COL + " VARCHAR(255), " + USER_EMAIL_COL + " VARCHAR(255), "
					+ " PRIMARY KEY(" + SUBMISSION_ID_COL + ", " + TASK_ID_COL + ", "
					+ IMAGE_NAME_COL + ", " + USER_EMAIL_COL + "))");

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

	private static final String INSERT_IMAGE_TASK_SQL = "INSERT INTO " + IMAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), ?, ?, ?)";

	@Override
	public void addImageTask(String taskId, String imageName, String downloadLink, int priority,
			String containerRepository, String containerTag, String collectionTierImageName)
			throws SQLException {
		LOGGER.info("Adding image task " + taskId + " with name " + imageName + ", download link "
				+ downloadLink + " and priority " + priority);
		if (taskId == null || taskId.isEmpty() || imageName == null || imageName.isEmpty()
				|| downloadLink == null || downloadLink.isEmpty()
				|| collectionTierImageName == null || collectionTierImageName.isEmpty()) {
			LOGGER.error("Invalid image task " + taskId);
			throw new IllegalArgumentException("Invalid image task " + taskId);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_IMAGE_TASK_SQL);
			insertStatement.setString(1, taskId);
			insertStatement.setString(2, imageName);
			insertStatement.setString(3, downloadLink);
			insertStatement.setString(4, ImageTaskState.CREATED.getValue());
			insertStatement.setString(5, ImageDataStore.NONE);
			insertStatement.setInt(6, priority);
			insertStatement.setString(7, "NE");
			insertStatement.setString(8, containerRepository);
			insertStatement.setString(9, containerTag);
			insertStatement.setString(10, "NE");
			insertStatement.setString(11, "NE");
			insertStatement.setString(12, "NE");
			insertStatement.setString(13, "NE");
			insertStatement.setString(14, ImageTask.AVAILABLE);
			insertStatement.setString(15, "no_errors");
			insertStatement.setString(16, collectionTierImageName);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}
	
	private static final String INSERT_FULL_IMAGE_TASK_SQL = "INSERT INTO " + IMAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public void addImageTask(ImageTask imageTask) throws SQLException {
		LOGGER.info("Adding image task " + imageTask.getTaskId() + " with name "
				+ imageTask.getName() + ", download link " + imageTask.getDownloadLink()
				+ " and priority " + imageTask.getPriority());
		if (imageTask.getTaskId() == null || imageTask.getTaskId().isEmpty()
				|| imageTask.getName() == null || imageTask.getName().isEmpty()
				|| imageTask.getDownloadLink() == null || imageTask.getDownloadLink().isEmpty()
				|| imageTask.getCollectionTierName() == null
				|| imageTask.getCollectionTierName().isEmpty()) {
			LOGGER.error("Invalid image task " + imageTask.getTaskId());
			throw new IllegalArgumentException("Invalid image task " + imageTask.getTaskId());
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_FULL_IMAGE_TASK_SQL);
			insertStatement.setString(1, imageTask.getTaskId());
			insertStatement.setString(2, imageTask.getName());
			insertStatement.setString(3, imageTask.getDownloadLink());
			insertStatement.setString(4, imageTask.getState().getValue());
			insertStatement.setString(5, imageTask.getFederationMember());
			insertStatement.setInt(6, imageTask.getPriority());
			insertStatement.setString(7, imageTask.getStationId());
			insertStatement.setString(8, imageTask.getContainerRepository());
			insertStatement.setString(9, imageTask.getContainerTag());
			insertStatement.setString(10, imageTask.getCrawlerVersion());
			insertStatement.setString(11, imageTask.getFetcherVersion());
			insertStatement.setString(12, imageTask.getBlowoutVersion());
			insertStatement.setString(13, imageTask.getFmaskVersion());
			insertStatement.setTimestamp(14, imageTask.getCreationTime());
			insertStatement.setTimestamp(15, imageTask.getUpdateTime());
			insertStatement.setString(16, imageTask.getImageStatus());
			insertStatement.setString(17, imageTask.getImageError());
			insertStatement.setString(18, imageTask.getCollectionTierName());
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_USER_NOTIFICATION_SQL = "INSERT INTO "
			+ USERS_NOTIFY_TABLE_NAME + " VALUES(?, ?, ?, ?)";

	@Override
	public void addUserNotification(String submissionId, String taskId, String imageName,
			String userEmail) throws SQLException {
		LOGGER.info("Adding image task " + taskId + " from submission " + submissionId
				+ " notification for " + userEmail);
		if (taskId == null || taskId.isEmpty() || imageName == null || imageName.isEmpty()
				|| userEmail == null || userEmail.isEmpty()) {
			throw new IllegalArgumentException("Invalid image name " + imageName + " or user "
					+ userEmail);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_USER_NOTIFICATION_SQL);
			insertStatement.setString(1, submissionId);
			insertStatement.setString(2, taskId);
			insertStatement.setString(3, imageName);
			insertStatement.setString(4, userEmail);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_DEPLOY_CONFIG_SQL = "INSERT INTO "
			+ DEPLOY_CONFIG_TABLE_NAME + " VALUES(?, ?, ?, ?)";

	@Override
	public void addDeployConfig(String nfsIP, String nfsSshPort, String nfsPort,
			String federationMember) throws SQLException {
		LOGGER.info("Adding NFS IP " + nfsIP + " and port " + nfsPort + " from " + federationMember
				+ " in DB");
		if (nfsIP == null || nfsIP.isEmpty() || nfsSshPort == null || nfsSshPort.isEmpty()
				|| nfsPort == null || nfsPort.isEmpty() || federationMember == null
				|| federationMember.isEmpty()) {
			throw new IllegalArgumentException("Invalid NFS IP " + nfsIP + ", ssh port "
					+ nfsSshPort + ", port " + nfsPort + " or federation member "
					+ federationMember);
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

		List<Ward> wards = new ArrayList<Ward>();

		while (rs.next()) {
			wards.add(new Ward(rs.getString(IMAGE_NAME_COL), ImageTaskState.ARCHIVED, rs
					.getString(SUBMISSION_ID_COL), rs.getString(TASK_ID_COL), rs
					.getString(USER_EMAIL_COL)));
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
		LOGGER.debug("Verifying if a deploy config for " + federationMember + " exists in database");

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
	public boolean imageExist(String collectionTierImageName) throws SQLException {
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
			+ " WHERE " + SUBMISSION_ID_COL + " = ?, " + TASK_ID_COL + " = ? AND " + IMAGE_NAME_COL
			+ " = ? AND " + USER_EMAIL_COL + " = ?";

	@Override
	public void removeNotification(String submissionId, String taskId, String imageName,
			String userEmail) throws SQLException {
		LOGGER.debug("Removing image " + imageName + " notification for " + userEmail);
		if (submissionId == null || submissionId.isEmpty() || taskId == null || taskId.isEmpty()
				|| imageName == null || imageName.isEmpty() || userEmail == null
				|| userEmail.isEmpty()) {
			throw new IllegalArgumentException("Invalid submissionId " + submissionId + ", taskId "
					+ taskId + ", imageName " + imageName + " or user " + userEmail);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(REMOVE_USER_NOTIFY_SQL);
			insertStatement.setString(1, submissionId);
			insertStatement.setString(2, taskId);
			insertStatement.setString(3, imageName);
			insertStatement.setString(4, userEmail);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String REMOVE_DEPLOY_CONFIG_SQL = "DELETE FROM "
			+ DEPLOY_CONFIG_TABLE_NAME + " WHERE " + FEDERATION_MEMBER_COL + " = ?";

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
	public void addStateStamp(String imageName, ImageTaskState state, Timestamp timestamp)
			throws SQLException {
		LOGGER.info("Adding image " + imageName + " state " + state.getValue() + " with timestamp "
				+ timestamp + " into DB");
		if (imageName == null || imageName.isEmpty() || state == null) {
			LOGGER.error("Invalid image " + imageName + " or state " + state.getValue());
			throw new IllegalArgumentException("Invalid image " + imageName);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_NEW_STATE_TIMESTAMP_SQL);
			insertStatement.setString(1, imageName);
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

	private static String UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
			+ " SET state = ?, utime = now() WHERE image_name = ?";

	@Override
	public void updateTaskState(String taskId, ImageTaskState state) throws SQLException {

		if (taskId == null || taskId.isEmpty() || state == null) {
			LOGGER.error("Invalid image task " + taskId + " or state " + state);
			throw new IllegalArgumentException("Invalid image task " + taskId + " or state "
					+ state);
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

	private static final String UPDATE_IMAGEDATA_SQL = "UPDATE "
			+ IMAGE_TABLE_NAME
			+ " SET name = ?, download_link = ?, state = ?, federation_member = ?,"
			+ " priority = ?, station_id = ?, container_repository = ?, container_tag = ?, crawler_version = ?, fetcher_version = ?,"
			+ " blowout_version = ?, fmask_version = ?," + " utime = now(), status = ?,"
			+ " error_msg = ?, tier_collection_image_name = ? WHERE task_id = ?";

	@Override
	public void updateImageTask(ImageTask imagetask) throws SQLException {
		if (imagetask == null) {
			LOGGER.error("Invalid image " + imagetask);
			throw new IllegalArgumentException("Invalid image data " + imagetask);
		}

		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_IMAGEDATA_SQL);
			updateStatement.setString(1, imagetask.getName());
			updateStatement.setString(2, imagetask.getDownloadLink());
			updateStatement.setString(3, imagetask.getState().getValue());
			updateStatement.setString(4, imagetask.getFederationMember());
			updateStatement.setInt(5, imagetask.getPriority());
			updateStatement.setString(6, imagetask.getStationId());
			updateStatement.setString(7, imagetask.getContainerRepository());
			updateStatement.setString(8, imagetask.getContainerTag());
			updateStatement.setString(9, imagetask.getCrawlerVersion());
			updateStatement.setString(10, imagetask.getFetcherVersion());
			updateStatement.setString(11, imagetask.getBlowoutVersion());
			updateStatement.setString(12, imagetask.getFmaskVersion());
			updateStatement.setString(13, imagetask.getImageStatus());
			updateStatement.setString(14, imagetask.getImageError());
			updateStatement.setString(15, imagetask.getCollectionTierName());
			updateStatement.setString(16, imagetask.getTaskId());
			updateStatement.setQueryTimeout(300);

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private static final String UPDATE_IMAGE_METADATA_SQL = "UPDATE " + IMAGE_TABLE_NAME
			+ " SET station_id = ?, container_repository = ?, utime = now() WHERE task_id = ?";

	@Override
	public void updateImageMetadata(String taskId, String stationId, String containerRepository)
			throws SQLException {
		if (taskId == null || taskId.isEmpty() || stationId == null || stationId.isEmpty()
				|| containerRepository == null || containerRepository.isEmpty()) {
			LOGGER.error("Invalid image task " + taskId + ", station ID " + stationId
					+ " or sebal version " + containerRepository);
			throw new IllegalArgumentException("Invalid image task " + taskId + ", station ID "
					+ stationId + " or container repository " + containerRepository);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_IMAGE_METADATA_SQL);
			updateStatement.setString(1, stationId);
			updateStatement.setString(2, containerRepository);
			updateStatement.setString(3, taskId);
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
			List<ImageTask> imageDatas = extractImageTaskFrom(rs);
			return imageDatas;
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
			+ " WHERE state = ? ORDER BY priority ASC";

	private static final String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME + " WHERE state = ? ORDER BY priority ASC LIMIT ?";

	@Override
	public List<ImageTask> getIn(ImageTaskState state, int limit) throws SQLException {
		if (state == null) {
			LOGGER.error("Invalid state " + state);
			throw new IllegalArgumentException("Invalid state " + state);
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
	private static final String SELECT_IMAGES_BY_FILTERS_NAME_SQL = " name = ? " + IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_PERIOD = " ctime BETWEEN ? AND ? ";

	@Override
	public List<ImageTask> getTasksByFilter(ImageTaskState state, String name,
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

		if (name != null && !name.trim().isEmpty()) {
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

			if (name != null && !name.trim().isEmpty()) {
				selectStatement.setString(++paramtersInsertCount, name);
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
			+ " WHERE status = ? ORDER BY priority, image_name";

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

	private static final String SELECT_AND_LOCK_LIMITED_IMAGES_TO_DOWNLOAD = "UPDATE "
			+ IMAGE_TABLE_NAME + " SET " + STATE_COL + " = ?, " + FEDERATION_MEMBER_COL + " = ?, "
			+ UPDATED_TIME_COL + " = now() WHERE " + STATE_COL + " = ? AND " + IMAGE_STATUS_COL
			+ " = ? LIMIT ?";

	private static final String SELECT_DOWNLOADING_IMAGES_BY_FEDERATION_MEMBER = "SELECT * FROM "
			+ IMAGE_TABLE_NAME + " WHERE " + STATE_COL + " = ? AND " + IMAGE_STATUS_COL
			+ " = ? AND " + FEDERATION_MEMBER_COL + " = ? LIMIT ?";

	/**
	 * This method selects and locks all images marked as CREATED and updates to
	 * DOWNLOADING and changes the federation member to the crawler ID and then
	 * selects and returns the updated images based on the state and federation
	 * member.
	 */
	@Override
	public List<ImageTask> getImagesToDownload(String federationMember, int limit)
			throws SQLException {
		/*
		 * In future versions, if the crawler starts to use a multithread
		 * approach this method needs to be reviewed to avoid concurrency
		 * problems between its threads. As the crawler selects images where the
		 * state is DOWNLOADING and federation member is equal to its ID, new
		 * threads could start to download an image that is already been
		 * downloaded by the another thread.
		 */

		if (federationMember == null) {
			LOGGER.error("Invalid federation member " + federationMember);
			throw new IllegalArgumentException("Invalid federation member " + federationMember);
		}

		PreparedStatement lockAndUpdateStatement = null;
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			lockAndUpdateStatement = connection
					.prepareStatement(SELECT_AND_LOCK_LIMITED_IMAGES_TO_DOWNLOAD);
			lockAndUpdateStatement.setString(1, ImageTaskState.DOWNLOADING.getValue());
			lockAndUpdateStatement.setString(2, federationMember);
			lockAndUpdateStatement.setString(3, ImageTaskState.CREATED.getValue());
			lockAndUpdateStatement.setString(4, ImageTask.AVAILABLE);
			lockAndUpdateStatement.setInt(5, limit);
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

			ResultSet rs = selectStatement.getResultSet();
			List<ImageTask> imageDatas = extractImageTaskFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, null);
			close(lockAndUpdateStatement, connection);
		}
	}

	private static SapsUser extractSapsUserFrom(ResultSet rs) throws SQLException {
		SapsUser sebalUser = new SapsUser(rs.getString(USER_EMAIL_COL),
				rs.getString(USER_NAME_COL), rs.getString(USER_PASSWORD_COL),
				rs.getBoolean(USER_STATE_COL), rs.getBoolean(USER_NOTIFY_COL),
				rs.getBoolean(ADMIN_ROLE_COL));

		return sebalUser;
	}

	private static List<ImageTask> extractImageTaskFrom(ResultSet rs) throws SQLException {
		List<ImageTask> imageDatas = new ArrayList<ImageTask>();
		while (rs.next()) {
			imageDatas.add(new ImageTask(rs.getString(TASK_ID_COL), rs.getString(IMAGE_NAME_COL),
					rs.getString(DOWNLOAD_LINK_COL), ImageTaskState.getStateFromStr(rs
							.getString(STATE_COL)), rs.getString(FEDERATION_MEMBER_COL), rs
							.getInt(PRIORITY_COL), rs.getString(STATION_ID_COL), rs
							.getString(CONTAINER_REPOSITORY_COL), rs.getString(CONTAINER_TAG_COL),
					rs.getString(CRAWLER_VERSION_COL), rs.getString(FETCHER_VERSION_COL), rs
							.getString(BLOWOUT_VERSION_COL), rs.getString(FMASK_VERSION_COL), rs
							.getTimestamp(CREATION_TIME_COL), rs.getTimestamp(UPDATED_TIME_COL), rs
							.getString(IMAGE_STATUS_COL), rs.getString(ERROR_MSG_COL), rs
							.getString(COLLECTION_TIER_IMAGE_NAME_COL)));
		}
		return imageDatas;
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
			+ " WHERE image_name = ?";

	@Override
	public boolean lockTask(String imageName) throws SQLException {
		if (imageName == null) {
			LOGGER.error("Invalid imageName " + imageName);
			throw new IllegalArgumentException("Invalid state " + imageName);
		}
		PreparedStatement lockImageStatement = null;
		Connection connection = null;

		boolean locked = false;
		try {
			connection = getConnection();
			lockImageStatement = connection.prepareStatement(LOCK_IMAGE_SQL);
			final int imageHashCode = imageName.hashCode();
			lockImageStatement.setInt(1, imageHashCode);
			lockImageStatement.setString(2, imageName);
			lockImageStatement.setQueryTimeout(300);

			ResultSet rs = lockImageStatement.executeQuery();
			if (rs.next()) {
				locked = rs.getBoolean(1);
			}

			if (locked) {
				lockedImages.put(imageName, connection);
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
	public boolean unlockTask(String imageName) throws SQLException {
		if (imageName == null) {
			LOGGER.error("Invalid imageName " + imageName);
			throw new IllegalArgumentException("Invalid state " + imageName);
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		boolean unlocked = false;
		if (lockedImages.containsKey(imageName)) {
			connection = lockedImages.get(imageName);
			try {
				selectStatement = connection.prepareStatement(UNLOCK_IMAGE_SQL);
				selectStatement.setInt(1, imageName.hashCode());
				selectStatement.setQueryTimeout(300);

				ResultSet rs = selectStatement.executeQuery();

				if (rs.next()) {
					unlocked = rs.getBoolean(1);
				}

				lockedImages.remove(imageName);
			} finally {
				close(selectStatement, connection);
			}
		}
		return unlocked;
	}

	private static final String REMOVE_STATE_SQL = "DELETE FROM " + STATES_TABLE_NAME
			+ " WHERE image_name = ? AND state = ? AND utime = ?";

	@Override
	public void removeStateStamp(String imageName, ImageTaskState state, Timestamp timestamp)
			throws SQLException {
		LOGGER.info("Removing image " + imageName + " state " + state.getValue()
				+ " with timestamp " + timestamp);
		if (imageName == null || imageName.isEmpty() || state == null) {
			LOGGER.error("Invalid image " + imageName + " or state " + state.getValue());
			throw new IllegalArgumentException("Invalid image " + imageName);
		}

		PreparedStatement removeStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			removeStatement = connection.prepareStatement(REMOVE_STATE_SQL);
			removeStatement.setString(1, imageName);
			removeStatement.setString(2, state.getValue());
			removeStatement.setTimestamp(3, timestamp);
			removeStatement.setQueryTimeout(300);

			removeStatement.execute();
		} finally {
			close(removeStatement, connection);
		}
	}
}
