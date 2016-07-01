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
	private static final String STATION_ID_COL = "station_id";
	private static final String SEBAL_VERSION_COL = "sebal_version";
	private static final String CREATION_TIME_COL = "ctime";
	private static final String UPDATED_TIME_COL = "utime";
	private static final String IMAGE_STATUS_COL = "status";
	private static final String DOWNLOADING_UPDATE_TIME_COL = "utime_downloading";
	private static final String DOWNLOADED_UPDATE_TIME_COL = "utime_downloaded";
	private static final String RUNNING_R_UPDATE_TIME_COL = "utime_running_r";
	private static final String FINISHED_UPDATE_TIME_COL = "utime_finished";
	private static final String FETCHING_UPDATE_TIME_COL = "utime_fetching";
	private static final String FETCHED_UPDATE_TIME_COL = "utime_fetched";
	private static final String CORRUPTED_UPDATE_TIME_COL = "utime_corrupted";
	
	private Map<String, Connection> lockedImages = new ConcurrentHashMap<String, Connection>();
	private BasicDataSource connectionPool;

	public JDBCImageDataStore(Properties properties, String imageStoreIP, String imageStorePort) {
		if (properties == null) {
			throw new IllegalArgumentException("Properties arg must not be null.");
		}

		Statement statement = null;
		Connection connection = null;
		try {
			LOGGER.debug("DatastoreURL: "
					+ properties.getProperty("datastore_url_prefix")
					+ imageStoreIP + ":" + imageStorePort + "/" + properties.getProperty("datastore_name"));
			
			connectionPool = new BasicDataSource();
			connectionPool.setUsername(properties.getProperty("datastore_username"));
			connectionPool.setPassword(properties.getProperty("datastore_password"));
			connectionPool.setDriverClassName(properties.getProperty("datastore_driver"));
			connectionPool.setUrl(properties.getProperty("datastore_url_prefix")
					+ imageStoreIP + ":" + imageStorePort + "/" + properties.getProperty("datastore_name"));
			connectionPool.setInitialSize(1);
			
			connection = getConnection();
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS " + IMAGE_TABLE_NAME
					+ "(" + IMAGE_NAME_COL + " VARCHAR(255) PRIMARY KEY, "
					+ DOWNLOAD_LINK_COL + " VARCHAR(255), " + STATE_COL
					+ " VARCHAR(100), " + FEDERATION_MEMBER_COL
					+ " VARCHAR(255), " + PRIORITY_COL + " INTEGER, "
					+ STATION_ID_COL + " VARCHAR(255), " + SEBAL_VERSION_COL
					+ " VARCHAR(255), " + CREATION_TIME_COL + " VARCHAR(255), "
					+ UPDATED_TIME_COL + " VARCHAR(255), " + IMAGE_STATUS_COL
					+ " VARCHAR(255), " + DOWNLOADING_UPDATE_TIME_COL
					+ " VARCHAR(255), " + DOWNLOADED_UPDATE_TIME_COL
					+ " VARCHAR(255), " + RUNNING_R_UPDATE_TIME_COL
					+ " VARCHAR(255), " + FINISHED_UPDATE_TIME_COL
					+ " VARCHAR(255), " + FETCHING_UPDATE_TIME_COL
					+ " VARCHAR(255), " + FETCHED_UPDATE_TIME_COL
					+ " VARCHAR(255), " + CORRUPTED_UPDATE_TIME_COL
					+ " VARCHAR(255))");
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

	protected void close(Statement statement, Connection conn) {
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
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	@Override
	public void addImage(String imageName, String downloadLink, int priority) throws SQLException {
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
			insertStatement.setString(6, "NE");
			insertStatement.setString(7, "NE");
			insertStatement.setString(8, "NE");
			insertStatement.setString(9, "NE");
			insertStatement.setString(10, ImageData.AVAILABLE);
			insertStatement.setString(11, "NE");
			insertStatement.setString(12, "NE");
			insertStatement.setString(13, "NE");
			insertStatement.setString(14, "NE");
			insertStatement.setString(15, "NE");
			insertStatement.setString(16, "NE");
			insertStatement.setString(17, "NE");

			insertStatement.execute();
		} finally {
			close(insertStatement, connection);
		}
	}

	private static String UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
			+ " SET state = ?, utime = ? WHERE image_name = ?";

	@Override
	public void updateImageState(String imageName, ImageState state) throws SQLException {

		if (imageName == null || imageName.isEmpty() || state == null) {
			LOGGER.error("Invalid image name " + imageName + " or state " + state);
			throw new IllegalArgumentException("Invalid image name " + imageName + " or state "
					+ state);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			imageStateCheck(state);

			updateStatement = connection.prepareStatement(UPDATE_IMAGE_STATE_SQL);
			updateStatement.setString(1, state.getValue());
			
			String epoch = String.valueOf(System.currentTimeMillis());
			updateStatement.setString(2, epoch);
			updateStatement.setString(3, epoch);
			updateStatement.setString(4, imageName);
			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}

	private void imageStateCheck(ImageState state) {
		if(state.getValue().equals(ImageState.DOWNLOADING)) {
			UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
					+ " SET state = ?, utime = ?, utime_downloading = ? WHERE image_name = ?";
		} else if(state.getValue().equals(ImageState.DOWNLOADED)) {
			UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
					+ " SET state = ?, utime = ?, utime_downloaded = ? WHERE image_name = ?";
		} else if(state.getValue().equals(ImageState.RUNNING_R)) {
			UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
					+ " SET state = ?, utime = ?, utime_running_r = ? WHERE image_name = ?";
		} else if(state.getValue().equals(ImageState.FINISHED)) {
			UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
					+ " SET state = ?, utime = ?, utime_finished = ? WHERE image_name = ?";
		} else if(state.getValue().equals(ImageState.FETCHING)) {
			UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
					+ " SET state = ?, utime = ?, utime_fetching = ? WHERE image_name = ?";
		} else if(state.getValue().equals(ImageState.FETCHED)) {
			UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
					+ " SET state = ?, utime = ?, utime_fetched = ? WHERE image_name = ?";
		} else if(state.getValue().equals(ImageState.CORRUPTED)) {
			UPDATE_IMAGE_STATE_SQL = "UPDATE " + IMAGE_TABLE_NAME
					+ " SET state = ?, utime = ?, utime_corrupted = ? WHERE image_name = ?";
		}
	}
	
	private static final String UPDATE_IMAGEDATA_SQL = "UPDATE " + IMAGE_TABLE_NAME + " SET download_link = ?, state = ?, federation_member = ?,"
			+ " priority = ?, station_id = ?, sebal_version = ?, ctime = ?, utime = ?, status = ?, utime_downloading = ?, utime_downloaded = ?,"
			+ " utime_running_r = ?, utime_finished = ?, utime_fetching = ?, utime_fetched = ?, utime_corrupted = ? WHERE image_name = ?";
	
	@Override
	public void updateImage(ImageData imageData) throws SQLException {
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
			updateStatement.setString(5, imageData.getStationId());
			updateStatement.setString(6, imageData.getSebalVersion());
			updateStatement.setString(7, imageData.getCreationTime());
			updateStatement.setString(8, imageData.getUpdateTime());
			updateStatement.setString(9, imageData.getImageStatus());
			updateStatement.setString(10, imageData.getDownloadingUpdateTime());
			updateStatement.setString(11, imageData.getDownloadedUpdateTime());
			updateStatement.setString(12, imageData.getRunningRUpdateTime());
			updateStatement.setString(13, imageData.getFinishedUpdateTime());
			updateStatement.setString(14, imageData.getFetchingUpdateTime());
			updateStatement.setString(15, imageData.getFetchedUpdateTime());
			updateStatement.setString(16, imageData.getCorruptedUpdateTime());
			updateStatement.setString(17, imageData.getName());

			updateStatement.execute();
		} finally {
			close(updateStatement, connection);
		}
	}
	
	private static final String UPDATE_IMAGE_METADATA_SQL = "UPDATE " + IMAGE_TABLE_NAME
			+ " SET station_id = ?, sebal_version = ?, utime = ? WHERE image_name = ?";
	
	@Override
	public void updateImageMetadata(String imageName, String stationId,
			String sebalVersion) throws SQLException {
		if (imageName == null || imageName.isEmpty() || stationId == null
				|| stationId.isEmpty() || sebalVersion == null
				|| sebalVersion.isEmpty()) {
			LOGGER.error("Invalid image name " + imageName + ", station ID " + stationId + " or sebal version " + sebalVersion);
			throw new IllegalArgumentException("Invalid image name " + imageName + ", station ID "
					+ stationId + " or sebal version " + sebalVersion);
		}
		PreparedStatement updateStatement = null;
		Connection connection = null;

		try {
			connection = getConnection();

			updateStatement = connection.prepareStatement(UPDATE_IMAGE_METADATA_SQL);
			updateStatement.setString(1, stationId);
			updateStatement.setString(2, sebalVersion);
			updateStatement.setString(3, String.valueOf(System.currentTimeMillis()));
			updateStatement.setString(4, imageName);
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
	public List<ImageData> getAllImages() throws SQLException {
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

	private static final String SELECT_IMAGES_BY_FILTERS_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_WHERE_SQL = " WHERE ";
	private static final String SELECT_IMAGES_BY_FILTERS_STATE_SQL = " state = ? " + IMAGE_TABLE_NAME;
	private static final String SELECT_IMAGES_BY_FILTERS_NAME_SQL = " name = ? " + IMAGE_TABLE_NAME;
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
		if(state != null){
			if(paramtersCount == 0){
				finalQuery.append(SELECT_IMAGES_BY_FILTERS_WHERE_SQL);
			}
			finalQuery.append(SELECT_IMAGES_BY_FILTERS_STATE_SQL);
			paramtersCount++;
		}
		
		if(name != null && !name.trim().isEmpty()){
			if(paramtersCount == 0){
				finalQuery.append(SELECT_IMAGES_BY_FILTERS_WHERE_SQL);
			}else{
				finalQuery.append(" AND ");
			}
			finalQuery.append(SELECT_IMAGES_BY_FILTERS_NAME_SQL);
			paramtersCount++;
		}
		
		if(processDateInit > 0 && processDateEnd > 0){
			if(paramtersCount == 0){
				finalQuery.append(SELECT_IMAGES_BY_FILTERS_WHERE_SQL);
			}else{
				finalQuery.append(" AND ");
			}
			finalQuery.append(SELECT_IMAGES_BY_FILTERS_PERIOD);
			paramtersCount++;
			paramtersCount++;//Has tow parameters (processDateInit and processDateEnd)
		}
		
		try {
			connection = getConnection();
			
			selectStatement = connection.prepareStatement(finalQuery.toString());
			
			if(state != null){
				selectStatement.setString(++paramtersInsertCount, state.getValue());
			}
			
			if(name != null && !name.trim().isEmpty()){
				selectStatement.setString(++paramtersInsertCount, name);
			}
			
			if(processDateInit > 0 && processDateEnd > 0){
				selectStatement.setLong(++paramtersInsertCount, processDateInit);
				selectStatement.setLong(++paramtersInsertCount, processDateEnd);
			}
			
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
	
	private static final String SELECT_PURGED_IMAGES_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE status = ? ORDER BY priority, image_name";
	
	@Override
	public List<ImageData> getPurgedImages() throws SQLException {
		PreparedStatement selectStatement = null;
		Connection connection = null;
		
		try {
			connection = getConnection();
			
			selectStatement = connection.prepareStatement(SELECT_PURGED_IMAGES_SQL);
			selectStatement.setString(1, ImageData.PURGED);
			selectStatement.execute();
			
			ResultSet rs = selectStatement.getResultSet();
			List<ImageData> imageDatas = extractImageDataFrom(rs);
			rs.close();
			return imageDatas;
		} finally {
			close(selectStatement, connection);
		}
			
	}
	
	private static final String SELECT_AND_LOCK_LIMITED_IMAGES_TO_DOWNLOAD = "UPDATE " + IMAGE_TABLE_NAME + " it SET " + STATE_COL + " = ?, " 
			+ FEDERATION_MEMBER_COL + " = ?, " + UPDATED_TIME_COL + " = ?, " + DOWNLOADING_UPDATE_TIME_COL + " = ? FROM (SELECT * FROM " 
			+ IMAGE_TABLE_NAME + " WHERE " + STATE_COL + " = ? AND " + IMAGE_STATUS_COL + " = ? LIMIT ? FOR UPDATE) filter WHERE it." 
			+ IMAGE_NAME_COL + " = filter." + IMAGE_NAME_COL;
	
	private static final String SELECT_DOWNLOADING_IMAGES_BY_FEDERATION_MEMBER = "SELECT * FROM " + IMAGE_TABLE_NAME 
			+ " WHERE " + STATE_COL + " = ? AND " + IMAGE_STATUS_COL + " = ? AND " + FEDERATION_MEMBER_COL + " = ?";
	
	/**
	 * This method selects and locks all images marked as NOT_DOWNLOADED and updates to DOWNLOADING
	 * and changes the federation member to the crawler ID and then selects and returns the updated images
	 * based on the state and federation member. 
	 */
	@Override
	public List<ImageData> getImagesToDownload(String federationMember,
			int limit) throws SQLException {
		/* 
		 * In future versions, if the crawler starts to use a multithread approach
		 * this method needs to be reviewed to avoid concurrency problems between its threads.
		 * As the crawler selects images where the state is DOWNLOADING and federation member 
		 * is equal to its ID, new threads could start to download an image that is already 
		 * been downloaded by the another thread.
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
			
			lockAndUpdateStatement = connection.prepareStatement(SELECT_AND_LOCK_LIMITED_IMAGES_TO_DOWNLOAD);
			lockAndUpdateStatement.setString(1, ImageState.DOWNLOADING.getValue());
			lockAndUpdateStatement.setString(2, federationMember);			
			String epoch = String.valueOf(System.currentTimeMillis());
			lockAndUpdateStatement.setString(3, epoch);
			lockAndUpdateStatement.setString(4, epoch);
			lockAndUpdateStatement.setString(5, ImageState.NOT_DOWNLOADED.getValue());
			lockAndUpdateStatement.setString(6, ImageData.AVAILABLE);
			lockAndUpdateStatement.setInt(6, limit);
			lockAndUpdateStatement.execute();

			selectStatement = connection.prepareStatement(SELECT_DOWNLOADING_IMAGES_BY_FEDERATION_MEMBER);
			selectStatement.setString(1, ImageState.DOWNLOADING.getValue());
			selectStatement.setString(2, ImageData.AVAILABLE);
			selectStatement.setString(3, federationMember);
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
	
	private static List<ImageData> extractImageDataFrom(ResultSet rs) throws SQLException {
		List<ImageData> imageDatas = new ArrayList<ImageData>();
		while (rs.next()) {
			imageDatas.add(new ImageData(rs.getString(IMAGE_NAME_COL), rs
					.getString(DOWNLOAD_LINK_COL), ImageState
					.getStateFromStr(rs.getString(STATE_COL)), rs
					.getString(FEDERATION_MEMBER_COL), rs.getInt(PRIORITY_COL),
					rs.getString(STATION_ID_COL), rs
							.getString(SEBAL_VERSION_COL), rs
							.getString(CREATION_TIME_COL), rs
							.getString(UPDATED_TIME_COL), rs
							.getString(DOWNLOADING_UPDATE_TIME_COL), rs
							.getString(DOWNLOADED_UPDATE_TIME_COL), rs
							.getString(RUNNING_R_UPDATE_TIME_COL), rs
							.getString(FINISHED_UPDATE_TIME_COL), rs
							.getString(FETCHING_UPDATE_TIME_COL), rs
							.getString(FETCHED_UPDATE_TIME_COL), rs
							.getString(CORRUPTED_UPDATE_TIME_COL)));
		}
		return imageDatas;
	}

	private static final String SELECT_IMAGE_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME
			+ " WHERE image_name = ?";
	
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
