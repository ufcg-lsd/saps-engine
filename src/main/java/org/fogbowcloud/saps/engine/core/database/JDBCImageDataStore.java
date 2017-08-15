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
import org.fogbowcloud.saps.engine.core.model.ImageData;
import org.fogbowcloud.saps.engine.core.model.ImageState;
import org.fogbowcloud.saps.engine.core.model.SebalUser;
import org.fogbowcloud.saps.notifier.Ward;

public class JDBCImageDataStore implements ImageDataStore {

	private static final Logger LOGGER = Logger
			.getLogger(JDBCImageDataStore.class);
	protected static final String IMAGE_TABLE_NAME = "NASA_IMAGES";
	protected static final String STATES_TABLE_NAME = "STATES_TIMESTAMPS";
	private static final String IMAGE_NAME_COL = "image_name";
	private static final String DOWNLOAD_LINK_COL = "download_link";
	private static final String PRIORITY_COL = "priority";
	private static final String FEDERATION_MEMBER_COL = "federation_member";
	private static final String STATE_COL = "state";
	private static final String STATION_ID_COL = "station_id";
	private static final String SEBAL_VERSION_COL = "sebal_version";
	private static final String SEBAL_TAG_COL = "sebal_tag";
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
	private static final String JOB_ID_COL = "job_id";

	private static final String DEPLOY_CONFIG_TABLE_NAME = "deploy_config";
	private static final String NFS_IP_COL = "nfs_ip";
	private static final String NFS_PORT_COL = "nfs_port";

	private Map<String, Connection> lockedImages = new ConcurrentHashMap<String, Connection>();
	private BasicDataSource connectionPool;

	public JDBCImageDataStore(Properties properties) throws SQLException {

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}

		String imageStoreIP = properties.getProperty(DATASTORE_IP);
		String imageStorePort = properties.getProperty(DATASTORE_PORT);
		String imageStoreURLPrefix = properties
				.getProperty(DATASTORE_URL_PREFIX);
		String dbUserName = properties.getProperty(DATASTORE_USERNAME);
		String dbUserPass = properties.getProperty(DATASTORE_PASSWORD);
		String dbDrive = properties.getProperty(DATASTORE_DRIVER);
		String dbName = properties.getProperty(DATASTORE_NAME);

		LOGGER.info("Imagestore " + imageStoreIP + ":" + imageStorePort);
		init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName,
				dbUserPass, dbDrive, dbName);
	}

	public JDBCImageDataStore(String imageStoreURLPrefix, String imageStoreIP,
			String imageStorePort, String dbUserName, String dbUserPass,
			String dbDrive, String dbName) throws SQLException {

		init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName,
				dbUserPass, dbDrive, dbName);
	}

	private void init(String imageStoreIP, String imageStorePort,
			String imageStoreURLPrefix, String dbUserName, String dbUserPass,
			String dbDrive, String dbName) throws SQLException {
		connectionPool = createConnectionPool(imageStoreURLPrefix,
				imageStoreIP, imageStorePort, dbUserName, dbUserPass, dbDrive,
				dbName);
		createTable();
	}

	private void createTable() throws SQLException {

		Connection connection = null;
		Statement statement = null;
		
		try {
			connection = getConnection();
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS " + IMAGE_TABLE_NAME
					+ "(" + IMAGE_NAME_COL + " VARCHAR(255) PRIMARY KEY, "
					+ DOWNLOAD_LINK_COL + " VARCHAR(255), " + STATE_COL
					+ " VARCHAR(100), " + FEDERATION_MEMBER_COL
					+ " VARCHAR(255), " + PRIORITY_COL + " INTEGER, "
					+ STATION_ID_COL + " VARCHAR(255), " + SEBAL_VERSION_COL
					+ " VARCHAR(255), " + SEBAL_TAG_COL + " VARCHAR(255), "
					+ CRAWLER_VERSION_COL + " VARCHAR(255), "
					+ FETCHER_VERSION_COL + " VARCHAR(255), "
					+ BLOWOUT_VERSION_COL + " VARCHAR(255), "
					+ FMASK_VERSION_COL + " VARCHAR(255), " + CREATION_TIME_COL
					+ " TIMESTAMP, " + UPDATED_TIME_COL + " TIMESTAMP, "
					+ IMAGE_STATUS_COL + " VARCHAR(255), " + ERROR_MSG_COL
					+ " VARCHAR(255), " + COLLECTION_TIER_IMAGE_NAME_COL
					+ " VARCHAR(255))");

			statement.execute("CREATE TABLE IF NOT EXISTS " + STATES_TABLE_NAME
					+ "(" + IMAGE_NAME_COL + " VARCHAR(255), " + STATE_COL
					+ " VARCHAR(100), " + UPDATED_TIME_COL + " TIMESTAMP, "
					+ ERROR_MSG_COL + " VARCHAR(255))");

			statement.execute("CREATE TABLE IF NOT EXISTS " + USERS_TABLE_NAME
					+ "(" + USER_EMAIL_COL + " VARCHAR(255) PRIMARY KEY, "
					+ USER_NAME_COL + " VARCHAR(255), " + USER_PASSWORD_COL
					+ " VARCHAR(100), " + USER_STATE_COL + " BOOLEAN, "
					+ USER_NOTIFY_COL + " BOOLEAN, " + ADMIN_ROLE_COL
					+ " BOOLEAN)");

			statement.execute("CREATE TABLE IF NOT EXISTS "
					+ USERS_NOTIFY_TABLE_NAME + "(" + JOB_ID_COL
					+ " VARCHAR(255), " + IMAGE_NAME_COL + " VARCHAR(255), "
					+ USER_EMAIL_COL + " VARCHAR(255), " + " PRIMARY KEY("
					+ JOB_ID_COL + ", " + IMAGE_NAME_COL + ", "
					+ USER_EMAIL_COL + "))");

			statement.execute("CREATE TABLE IF NOT EXISTS "
					+ DEPLOY_CONFIG_TABLE_NAME + "(" + NFS_IP_COL
					+ " VARCHAR(100), " + NFS_PORT_COL + " VARCHAR(100), "
					+ FEDERATION_MEMBER_COL + " VARCHAR(255), "
					+ " PRIMARY KEY(" + NFS_IP_COL + ", " + NFS_PORT_COL + ", "
					+ FEDERATION_MEMBER_COL + "))");

			statement.close();
		} catch (SQLException e) {
			LOGGER.error("Error while initializing DataStore", e);
			throw e;
		} finally {
			close(statement, connection);
		}
	}

	private BasicDataSource createConnectionPool(String imageStoreURLPrefix,
			String imageStoreIP, String imageStorePort, String dbUserName,
			String dbUserPass, String dbDriver, String dbName) {

		String url = imageStoreURLPrefix + imageStoreIP + ":" + imageStorePort
				+ "/" + dbName;

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
			LOGGER.error(
					"Error while getting a new connection from the connection pool",
					e);
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

	private static final String INSERT_IMAGE_SQL = "INSERT INTO "
			+ IMAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), ?, ?, ?)";

	@Override
	public void addImage(String imageName, String downloadLink, int priority,
			String sebalVersion, String sebalTag, String collectionTierImageName) throws SQLException {
		LOGGER.info("Adding image " + imageName + " with download link "
				+ downloadLink + " and priority " + priority);
		if (imageName == null || imageName.isEmpty() || downloadLink == null
				|| downloadLink.isEmpty() || collectionTierImageName == null
				|| collectionTierImageName.isEmpty()) {
			LOGGER.error("Invalid image name " + imageName);
			throw new IllegalArgumentException("Invalid image name "
					+ imageName);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection.prepareStatement(INSERT_IMAGE_SQL);
			insertStatement.setString(1, imageName);
			insertStatement.setString(2, downloadLink);
			insertStatement.setString(3, ImageState.NOT_DOWNLOADED.getValue());
			insertStatement.setString(4, ImageDataStore.NONE);
			insertStatement.setInt(5, priority);
			insertStatement.setString(6, "NE");
			insertStatement.setString(7, sebalVersion);
			insertStatement.setString(8, sebalTag);
			insertStatement.setString(9, "NE");
			insertStatement.setString(10, "NE");
			insertStatement.setString(11, "NE");
			insertStatement.setString(12, "NE");
			insertStatement.setString(13, ImageData.AVAILABLE);
			insertStatement.setString(14, "no_errors");
			insertStatement.setString(15, collectionTierImageName);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_USER_NOTIFICATION_SQL = "INSERT INTO "
			+ USERS_NOTIFY_TABLE_NAME + " VALUES(?, ?, ?)";

	@Override
	public void addUserNotify(String jobId, String imageName, String userEmail)
			throws SQLException {
		LOGGER.info("Adding image " + imageName + " with jobId " + jobId
				+ " notification for " + userEmail);
		if (imageName == null || imageName.isEmpty() || userEmail == null
				|| userEmail.isEmpty()) {
			throw new IllegalArgumentException("Invalid image name "
					+ imageName + " or user " + userEmail);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection
					.prepareStatement(INSERT_USER_NOTIFICATION_SQL);
			insertStatement.setString(1, jobId);
			insertStatement.setString(2, imageName);
			insertStatement.setString(3, userEmail);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_DEPLOY_CONFIG_SQL = "INSERT INTO "
			+ DEPLOY_CONFIG_TABLE_NAME + " VALUES(?, ?, ?)";

	@Override
	public void addDeployConfig(String nfsIP, String nfsPort,
			String federationMember) throws SQLException {
		LOGGER.info("Adding NFS IP " + nfsIP + " and port " + nfsPort
				+ " from " + federationMember + " in DB");
		if (nfsIP == null || nfsIP.isEmpty() || nfsPort == null
				|| nfsPort.isEmpty() || federationMember == null
				|| federationMember.isEmpty()) {
			throw new IllegalArgumentException("Invalid NFS IP " + nfsIP
					+ ", port " + nfsPort + " or federation member "
					+ federationMember);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection
					.prepareStatement(INSERT_DEPLOY_CONFIG_SQL);
			insertStatement.setString(1, nfsIP);
			insertStatement.setString(2, nfsPort);
			insertStatement.setString(3, federationMember);
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

	private List<Ward> extractUsersToNotifyFrom(ResultSet rs)
			throws SQLException {

		List<Ward> wards = new ArrayList<Ward>();

		while (rs.next()) {
			wards.add(new Ward(rs.getString(IMAGE_NAME_COL),
					ImageState.FETCHED, rs.getString(JOB_ID_COL), rs
							.getString(USER_EMAIL_COL)));
		}

		return wards;
	}
	
	private HashMap<String, String> extractNFSConfigFrom(ResultSet rs)
			throws SQLException {

		HashMap<String, String> nfsConfig = new HashMap<String, String>();

		while (rs.next()) {
			nfsConfig.put(rs.getString(NFS_IP_COL), rs.getString(NFS_PORT_COL));
		}

		return nfsConfig;
	}

	private static final String SELECT_USER_NOTIFIABLE_SQL = "SELECT "
			+ USER_NOTIFY_COL + " FROM " + USERS_TABLE_NAME + " WHERE "
			+ USER_EMAIL_COL + " = ?";

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
		LOGGER.debug("Verifying if a deploy config for " + federationMember
				+ " exists in database");

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
	
	private static final String SELECT_CHECK_IMAGE_EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM "
			+ IMAGE_TABLE_NAME + " WHERE " + COLLECTION_TIER_IMAGE_NAME_COL + " = ?)";
	
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

	private static final String REMOVE_USER_NOTIFY_SQL = "DELETE FROM "
			+ USERS_NOTIFY_TABLE_NAME + " WHERE " + JOB_ID_COL + " = ? AND "
			+ IMAGE_NAME_COL + " = ? AND " + USER_EMAIL_COL + " = ?";

	@Override
	public void removeUserNotify(String jobId, String imageName,
			String userEmail) throws SQLException {
		LOGGER.debug("Removing image " + imageName + " notification for "
				+ userEmail);
		if (jobId == null || jobId.isEmpty() || imageName == null
				|| imageName.isEmpty() || userEmail == null
				|| userEmail.isEmpty()) {
			throw new IllegalArgumentException("Invalid jobId " + jobId
					+ ", imageName " + imageName + " or user " + userEmail);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection
					.prepareStatement(REMOVE_USER_NOTIFY_SQL);
			insertStatement.setString(1, jobId);
			insertStatement.setString(2, imageName);
			insertStatement.setString(3, userEmail);
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
		LOGGER.debug("Removing register for " + federationMember
				+ " from database");
		if (federationMember == null || federationMember.isEmpty()) {
			throw new IllegalArgumentException("Invalid federationMember "
					+ federationMember);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;
		try {
			connection = getConnection();

			insertStatement = connection
					.prepareStatement(REMOVE_DEPLOY_CONFIG_SQL);
			insertStatement.setString(1, federationMember);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_NEW_STATE_TIMESTAMP_SQL = "INSERT INTO "
			+ STATES_TABLE_NAME + " VALUES(?, ?, ?)";

	@Override
	public void addStateStamp(String imageName, ImageState state,
			Timestamp timestamp) throws SQLException {
		LOGGER.info("Adding image " + imageName + " state " + state.getValue()
				+ " with timestamp " + timestamp + " into DB");
		if (imageName == null || imageName.isEmpty() || state == null) {
			LOGGER.error("Invalid image " + imageName + " or state "
					+ state.getValue());
			throw new IllegalArgumentException("Invalid image " + imageName);
		}

		PreparedStatement insertStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			insertStatement = connection
					.prepareStatement(INSERT_NEW_STATE_TIMESTAMP_SQL);
			insertStatement.setString(1, imageName);
			insertStatement.setString(2, state.getValue());
			insertStatement.setTimestamp(3, timestamp);
			insertStatement.setQueryTimeout(300);

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String INSERT_NEW_USER_SQL = "INSERT INTO "
			+ USERS_TABLE_NAME + " VALUES(?, ?, ?, ?, ?, ?)";

	@Override
	public void addUser(String userEmail, String userName, String userPass,
			boolean userState, boolean userNotify, boolean adminRole)
			throws SQLException {

		LOGGER.info("Adding user " + userName + " into DB");
		if (userName == null || userName.isEmpty() || userPass == null
				|| userPass.isEmpty()) {
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
	public void updateUserState(String userEmail, boolean userState)
			throws SQLException {

		LOGGER.info("Updating user " + userEmail + " state to " + userState);
		if (userEmail == null || userEmail.isEmpty()) {
			throw new IllegalArgumentException("Invalid user " + userEmail);
		}

		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection
					.prepareStatement(UPDATE_USER_STATE_SQL);
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
	public void updateImageState(String imageName, ImageState state)
			throws SQLException {

		if (imageName == null || imageName.isEmpty() || state == null) {
			LOGGER.error("Invalid image name " + imageName + " or state "
					+ state);
			throw new IllegalArgumentException("Invalid image name "
					+ imageName + " or state " + state);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection
					.prepareStatement(UPDATE_IMAGE_STATE_SQL);
			updateStatement.setString(1, state.getValue());
			updateStatement.setString(2, imageName);
			updateStatement.setQueryTimeout(300);
			
			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private static final String UPDATE_IMAGEDATA_SQL = "UPDATE "
			+ IMAGE_TABLE_NAME
			+ " SET download_link = ?, state = ?, federation_member = ?,"
			+ " priority = ?, station_id = ?, sebal_version = ?, sebal_tag = ?, crawler_version = ?, fetcher_version = ?,"
			+ " blowout_version = ?, fmask_version = ?,"
			+ " utime = now(), status = ?,"
			+ " error_msg = ? WHERE image_name = ?";

	@Override
	public void updateImage(ImageData imageData) throws SQLException {
		if (imageData == null) {
			LOGGER.error("Invalid image " + imageData);
			throw new IllegalArgumentException("Invalid image data "
					+ imageData);
		}

		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_IMAGEDATA_SQL);
			updateStatement.setString(1, imageData.getDownloadLink());
			updateStatement.setString(2, imageData.getState().getValue());
			updateStatement.setString(3, imageData.getFederationMember());
			updateStatement.setInt(4, imageData.getPriority());
			updateStatement.setString(5, imageData.getStationId());
			updateStatement.setString(6, imageData.getSebalVersion());
			updateStatement.setString(7, imageData.getSebalTag());
			updateStatement.setString(8, imageData.getCrawlerVersion());
			updateStatement.setString(9, imageData.getFetcherVersion());
			updateStatement.setString(10, imageData.getBlowoutVersion());
			updateStatement.setString(11, imageData.getFmaskVersion());
			updateStatement.setString(12, imageData.getImageStatus());
			updateStatement.setString(13, imageData.getImageError());
			updateStatement.setString(14, imageData.getName());
			updateStatement.setQueryTimeout(300);

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private static final String UPDATE_IMAGE_METADATA_SQL = "UPDATE "
			+ IMAGE_TABLE_NAME
			+ " SET station_id = ?, sebal_version = ?, utime = now() WHERE image_name = ?";

	@Override
	public void updateImageMetadata(String imageName, String stationId,
			String sebalVersion) throws SQLException {
		if (imageName == null || imageName.isEmpty() || stationId == null
				|| stationId.isEmpty() || sebalVersion == null
				|| sebalVersion.isEmpty()) {
			LOGGER.error("Invalid image name " + imageName + ", station ID "
					+ stationId + " or sebal version " + sebalVersion);
			throw new IllegalArgumentException("Invalid image name "
					+ imageName + ", station ID " + stationId
					+ " or sebal version " + sebalVersion);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection
					.prepareStatement(UPDATE_IMAGE_METADATA_SQL);
			updateStatement.setString(1, stationId);
			updateStatement.setString(2, sebalVersion);
			updateStatement.setString(3, imageName);
			updateStatement.setQueryTimeout(300);
			
			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}
	
	private static final String UPDATE_IMAGE_PHASE2_SQL = "UPDATE "
			+ IMAGE_TABLE_NAME + " SET " + STATE_COL + " = ?, "
			+ SEBAL_VERSION_COL + " = ?, " + SEBAL_TAG_COL
			+ " = ?, utime = now() WHERE image_name = ?";
	
	// TODO: test
	@Override
	public void updateImageForPhase2(String imageName, String sebalVersion,
			String sebalTag) throws SQLException {
		if (imageName == null || imageName.isEmpty() || sebalVersion == null
				|| sebalVersion.isEmpty() || sebalTag == null
				|| sebalTag.isEmpty()) {
			LOGGER.error("Invalid image name " + imageName + ", sebal version "
					+ sebalVersion + " or tag " + sebalTag);
			throw new IllegalArgumentException("Invalid image name "
					+ imageName + ", sebal version " + sebalVersion
					+ " or tag " + sebalTag);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection
					.prepareStatement(UPDATE_IMAGE_PHASE2_SQL);
			updateStatement.setString(1, ImageState.QUEUED.getValue());
			updateStatement.setString(2, sebalVersion);
			updateStatement.setString(3, sebalTag);
			updateStatement.setString(4, imageName);
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

	private static final String SELECT_ALL_IMAGES_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME;

	@Override
	public List<ImageData> getAllImages() throws SQLException {
		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();
			statement.setQueryTimeout(300);

			statement.execute(SELECT_ALL_IMAGES_SQL);
			ResultSet rs = statement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			return imageDatas;
		} finally {
			close(statement, conn);
		}
	}

	private static final String SELECT_USER_SQL = "SELECT * FROM "
			+ USERS_TABLE_NAME + " WHERE " + USER_EMAIL_COL + " = ?";

	@Override
	public SebalUser getUser(String userEmail) throws SQLException {

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
				SebalUser sebalUser = extractSebalUserFrom(rs);
				return sebalUser;
			}
			rs.close();
			return null;
		} finally {
			close(selectStatement, connection);
		}
	}

	private static final String SELECT_IMAGES_IN_STATE_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME
			+ " WHERE state = ? ORDER BY priority ASC";

	private static final String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME
			+ " WHERE state = ? ORDER BY priority ASC LIMIT ?";

	@Override
	public List<ImageData> getIn(ImageState state, int limit)
			throws SQLException {
		if (state == null) {
			LOGGER.error("Invalid state " + state);
			throw new IllegalArgumentException("Invalid state " + state);
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			if (limit == UNLIMITED) {
				selectStatement = connection
						.prepareStatement(SELECT_IMAGES_IN_STATE_SQL);
				selectStatement.setString(1, state.getValue());
				selectStatement.setQueryTimeout(300);
				
				selectStatement.execute();
			} else {
				selectStatement = connection
						.prepareStatement(SELECT_LIMITED_IMAGES_IN_STATE_SQL);
				selectStatement.setString(1, state.getValue());
				selectStatement.setInt(2, limit);
				selectStatement.setQueryTimeout(300);
				
				selectStatement.execute();
			}

			ResultSet rs = selectStatement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, connection);
		}
	}

	private static final String SELECT_IMAGES_BY_FILTERS_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_WHERE_SQL = " WHERE ";
	private static final String SELECT_IMAGES_BY_FILTERS_STATE_SQL = " state = ? "
			+ IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_NAME_SQL = " name = ? "
			+ IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_PERIOD = " ctime BETWEEN ? AND ? ";

	@Override
	public List<ImageData> getImagesByFilter(ImageState state, String name,
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

			selectStatement = connection
					.prepareStatement(finalQuery.toString());			

			if (state != null) {
				selectStatement.setString(++paramtersInsertCount,
						state.getValue());
			}

			if (name != null && !name.trim().isEmpty()) {
				selectStatement.setString(++paramtersInsertCount, name);
			}

			if (processDateInit > 0 && processDateEnd > 0) {
				selectStatement
						.setLong(++paramtersInsertCount, processDateInit);
				selectStatement.setLong(++paramtersInsertCount, processDateEnd);
			}

			selectStatement.setQueryTimeout(300);
			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, connection);
		}
	}

	@Override
	public List<ImageData> getIn(ImageState state) throws SQLException {
		return getIn(state, UNLIMITED);
	}

	private static final String SELECT_PURGED_IMAGES_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME
			+ " WHERE status = ? ORDER BY priority, image_name";

	@Override
	public List<ImageData> getPurgedImages() throws SQLException {
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatement = connection
					.prepareStatement(SELECT_PURGED_IMAGES_SQL);
			selectStatement.setString(1, ImageData.PURGED);
			selectStatement.setQueryTimeout(300);
			
			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, connection);
		}

	}

	private static final String SELECT_AND_LOCK_LIMITED_IMAGES_TO_DOWNLOAD = "UPDATE "
			+ IMAGE_TABLE_NAME
			+ " it SET "
			+ STATE_COL
			+ " = ?, "
			+ FEDERATION_MEMBER_COL
			+ " = ?, "
			+ UPDATED_TIME_COL
			+ " = now() FROM (SELECT * FROM "
			+ IMAGE_TABLE_NAME
			+ " WHERE "
			+ STATE_COL
			+ " = ? AND "
			+ IMAGE_STATUS_COL
			+ " = ? LIMIT ? FOR UPDATE) filter WHERE it."
			+ IMAGE_NAME_COL
			+ " = filter." + IMAGE_NAME_COL;

	private static final String SELECT_DOWNLOADING_IMAGES_BY_FEDERATION_MEMBER = "SELECT * FROM "
			+ IMAGE_TABLE_NAME
			+ " WHERE "
			+ STATE_COL
			+ " = ? AND "
			+ IMAGE_STATUS_COL + " = ? AND " + FEDERATION_MEMBER_COL + " = ? LIMIT ?";

	/**
	 * This method selects and locks all images marked as NOT_DOWNLOADED and
	 * updates to DOWNLOADING and changes the federation member to the crawler
	 * ID and then selects and returns the updated images based on the state and
	 * federation member.
	 */
	@Override
	public List<ImageData> getImagesToDownload(String federationMember,
			int limit) throws SQLException {
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
			throw new IllegalArgumentException("Invalid federation member "
					+ federationMember);
		}				
		
		PreparedStatement lockAndUpdateStatement = null;
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			lockAndUpdateStatement = connection
					.prepareStatement(SELECT_AND_LOCK_LIMITED_IMAGES_TO_DOWNLOAD);
			lockAndUpdateStatement.setString(1, ImageState.SELECTED.getValue());
			lockAndUpdateStatement.setString(2, federationMember);
			lockAndUpdateStatement.setString(3,
					ImageState.NOT_DOWNLOADED.getValue());
			lockAndUpdateStatement.setString(4, ImageData.AVAILABLE);
			lockAndUpdateStatement.setInt(5, limit);
			lockAndUpdateStatement.setQueryTimeout(300);			
			lockAndUpdateStatement.execute();

			selectStatement = connection
					.prepareStatement(SELECT_DOWNLOADING_IMAGES_BY_FEDERATION_MEMBER);
			selectStatement.setString(1, ImageState.SELECTED.getValue());
			selectStatement.setString(2, ImageData.AVAILABLE);
			selectStatement.setString(3, federationMember);
			selectStatement.setInt(4, limit);
			selectStatement.setQueryTimeout(300);
			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, null);
			close(lockAndUpdateStatement, connection);
		}
	}

	private static SebalUser extractSebalUserFrom(ResultSet rs)
			throws SQLException {
		SebalUser sebalUser = new SebalUser(rs.getString(USER_EMAIL_COL),
				rs.getString(USER_NAME_COL), rs.getString(USER_PASSWORD_COL),
				rs.getBoolean(USER_STATE_COL), rs.getBoolean(USER_NOTIFY_COL),
				rs.getBoolean(ADMIN_ROLE_COL));

		return sebalUser;
	}

	private static List<ImageData> extractImageDataFrom(ResultSet rs)
			throws SQLException {
		List<ImageData> imageDatas = new ArrayList<ImageData>();
		while (rs.next()) {
			imageDatas.add(new ImageData(rs.getString(IMAGE_NAME_COL), rs
					.getString(DOWNLOAD_LINK_COL), ImageState
					.getStateFromStr(rs.getString(STATE_COL)), rs
					.getString(FEDERATION_MEMBER_COL), rs.getInt(PRIORITY_COL),
					rs.getString(STATION_ID_COL), rs
							.getString(SEBAL_VERSION_COL), rs
							.getString(SEBAL_TAG_COL), rs
							.getString(CRAWLER_VERSION_COL), rs
							.getString(FETCHER_VERSION_COL), rs
							.getString(BLOWOUT_VERSION_COL), rs
							.getString(FMASK_VERSION_COL), rs
							.getTimestamp(CREATION_TIME_COL), rs
							.getTimestamp(UPDATED_TIME_COL), rs
							.getString(IMAGE_STATUS_COL), rs
							.getString(ERROR_MSG_COL), rs
							.getString(COLLECTION_TIER_IMAGE_NAME_COL)));
		}
		return imageDatas;
	}

	private static final String SELECT_IMAGE_SQL = "SELECT * FROM "
			+ IMAGE_TABLE_NAME + " WHERE image_name = ?";

	@Override
	public ImageData getImage(String imageName) throws SQLException {
		if (imageName == null) {
			LOGGER.error("Invalid imageName " + imageName);
			throw new IllegalArgumentException("Invalid state " + imageName);
		}
		PreparedStatement selectStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			selectStatement = connection.prepareStatement(SELECT_IMAGE_SQL);
			selectStatement.setString(1, imageName);
			selectStatement.setQueryTimeout(300);
			
			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
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
				return rs.getString(NFS_IP_COL);
			}
			rs.close();
			return null;
		} finally {
			close(selectStatement, connection);
		}
	}

	private final String LOCK_IMAGE_SQL = "SELECT pg_try_advisory_lock(?) FROM "
			+ IMAGE_TABLE_NAME + " WHERE image_name = ?";

	@Override
	public boolean lockImage(String imageName) throws SQLException {
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
	public boolean unlockImage(String imageName) throws SQLException {
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

	private static final String REMOVE_STATE_SQL = "DELETE FROM "
			+ STATES_TABLE_NAME
			+ " WHERE image_name = ? AND state = ? AND utime = ?";

	@Override
	public void removeStateStamp(String imageName, ImageState state,
			Timestamp timestamp) throws SQLException {
		LOGGER.info("Removing image " + imageName + " state "
				+ state.getValue() + " with timestamp " + timestamp);
		if (imageName == null || imageName.isEmpty() || state == null) {
			LOGGER.error("Invalid image " + imageName + " or state "
					+ state.getValue());
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