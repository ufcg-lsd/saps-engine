package org.fogbowcloud.sebal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;

public class JDBCImageDataStore implements ImageDataStore {

	private static final Logger LOGGER = Logger.getLogger(JDBCImageDataStore.class);
	protected static final String IMAGE_TABLE_NAME = "NASA_IMAGES";
	private static final String IMAGE_NAME_COL = "image_name";
	private static final String DOWNLOAD_LINK_COL = "download_link";
	private static final String PRIORITY_COL = "priority";
	private static final String FEDERATION_MEMBER_COL = "federation_member";
	private static final String STATE_COL = "state";
	private Map<String, Connection> lockedImages = new ConcurrentHashMap<String, Connection>();
	private BasicDataSource connectionPool;

	public JDBCImageDataStore(Properties properties) {
		if (properties == null) {
			throw new IllegalArgumentException("Properties arg must not be null.");
		}

		Statement statement = null;
		Connection connection = null;
		try {
			LOGGER.debug("DatastoreURL: " + properties.getProperty("datastore_url"));
			
			connectionPool = new BasicDataSource();
			connectionPool.setUsername(properties.getProperty("datastore_username"));
			connectionPool.setPassword(properties.getProperty("datastore_password"));
			connectionPool.setDriverClassName(properties.getProperty("datastore_driver"));
			connectionPool.setUrl(properties.getProperty("datastore_url"));
			connectionPool.setInitialSize(1);
			
			connection = getConnection();
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS " + IMAGE_TABLE_NAME + "("
					+ IMAGE_NAME_COL + " VARCHAR(255) PRIMARY KEY, " + DOWNLOAD_LINK_COL
					+ " VARCHAR(255), " + STATE_COL + " VARCHAR(100), " + FEDERATION_MEMBER_COL
					+ " VARCHAR(255), " + PRIORITY_COL + " INTEGER)");
			statement.close();

		} catch (Exception e) {
			LOGGER.error("Error while initializing the DataStore.", e);
		} finally {
			close(statement, connection);
		}
	}

	public Connection getConnection() throws SQLException {
		try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			LOGGER.error("Error while getting a new connection from the connection pool.", e);
			throw e;
		}
	}

	private void close(Statement statement, Connection conn) {
		close(statement);

		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Couldn't close connection");
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
				LOGGER.error("Couldn't close statement");
			}
		}
	}

	private static final String INSERT_IMAGE_SQL = "INSERT INTO " + IMAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?)";

	@Override
	public void add(String imageName, String downloadLink, int priority) throws SQLException {
		LOGGER.info("Adding image " + imageName + " with download link " + downloadLink
				+ " and priority " + priority);
		if (imageName == null || imageName.isEmpty() || downloadLink == null
				|| downloadLink.isEmpty()) {
			LOGGER.error("Invalid image name " + imageName);
			throw new IllegalArgumentException("Invalid image name " + imageName);
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

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static final String UPDATE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
			+ " SET state = ? WHERE image_name = ?";

	@Override
	public void updateState(String imageName, ImageState state) throws SQLException {

		if (imageName == null || imageName.isEmpty() || state == null) {
			LOGGER.error("Invalid image name " + imageName + " or state " + state);
			throw new IllegalArgumentException("Invalid image name " + imageName + " or state "
					+ state);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_STATE_SQL);
			updateStatement.setString(1, state.getValue());
			updateStatement.setString(2, imageName);
			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}
	
	private static final String UPDATE_IMAGEDATA_SQL = "UPDATE " + IMAGE_TABLE_NAME + " "
			+ "SET download_link = ?, state = ?, federation_member = ? "
			+ ", priority = ? WHERE image_name = ?";
	
	@Override
	public void update(ImageData imageData) throws SQLException {
		if (imageData == null) {
			LOGGER.error("Invalid image " + imageData);
			throw new IllegalArgumentException("Invalid image data " + imageData);
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
			updateStatement.setString(5, imageData.getName());

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
			LOGGER.error("Error wile closing ConnectionPool.", e);
		}
	}

	private static final String SELECT_ALL_IMAGES_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME;

	@Override
	public List<ImageData> getAll() throws SQLException {
		LOGGER.debug("Getting all images.");

		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();

			statement.execute(SELECT_ALL_IMAGES_SQL);
			ResultSet rs = statement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			LOGGER.debug("Current images on data base: " + imageDatas);			
			return imageDatas;
		} finally {
			close(statement, conn);
		}
	}

	private static final String SELECT_IMAGES_IN_STATE_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE state = ? ORDER BY priority, image_name";
	
	private static final String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE state = ? ORDER BY priority, image_name LIMIT ?";

	@Override
	public List<ImageData> getIn(ImageState state, int limit) throws SQLException {
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
				selectStatement.execute();
			} else {
				selectStatement = connection.prepareStatement(SELECT_LIMITED_IMAGES_IN_STATE_SQL);
				selectStatement.setString(1, state.getValue());
				selectStatement.setInt(2, limit);
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
	
	@Override
	public List<ImageData> getIn(ImageState state) throws SQLException {
		return getIn(state, UNLIMITED);
	}

	private static List<ImageData> extractImageDataFrom(ResultSet rs) throws SQLException {
		List<ImageData> imageDatas = new ArrayList<ImageData>();
		while (rs.next()) {
			imageDatas.add(new ImageData(rs.getString(IMAGE_NAME_COL), rs
					.getString(DOWNLOAD_LINK_COL), ImageState.getStateFromStr(rs
					.getString(STATE_COL)), rs.getString(FEDERATION_MEMBER_COL), rs
					.getInt(PRIORITY_COL)));
		}
		return imageDatas;
	}

	private static final String SELECT_IMAGE_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE image_name = ?";
	
	@Override
	public ImageData get(String imageName) throws SQLException {
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
			selectStatement.execute();

			ResultSet rs = selectStatement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			rs.close();
			return imageDatas.get(0);
		} finally {
			close(selectStatement, connection);
		}
	}

	private final String LOCK_IMAGE_SQL = "SELECT pg_try_advisory_lock(?) FROM "
			+ IMAGE_TABLE_NAME + " WHERE image_name = ?";

	@Override
	public boolean lock(String imageName) throws SQLException {
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

	private final String UNLOCK_IMAGE_SQL  = "SELECT pg_advisory_unlock(?)";
	
	@Override
	public boolean unlock(String imageName) throws SQLException {
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
}
