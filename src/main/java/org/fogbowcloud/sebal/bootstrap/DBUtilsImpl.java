package org.fogbowcloud.sebal.bootstrap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;

public class DBUtilsImpl implements DBUtils {

	private static Properties properties;
	private static String imageStoreIP;
	private static String imageStorePort;
	private static String dbUserName;
	private static String dbUserPass;
	private static String firstYear;
	private static String lastYear;
	private static String regionsFilePath;
	private static String specificRegion;
	private static BasicDataSource connectionPool;

	static String SELECT_ALL_IMAGES_SQL = "SELECT * FROM nasa_images ORDER BY priority, image_name";
	static String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM nasa_images WHERE state = ? ORDER BY priority, image_name LIMIT ? ";

	static String SELECT_UNLOCK_IMAGE_SQL_SQL = "SELECT pg_advisory_unlock(?)";
	static String SELECT_LOCK_IMAGE_SQL_SQL = "SELECT pg_try_advisory_lock(?) FROM nasa_images WHERE image_name = ?";

	private static final String UPDATE_STATE_SQL = "UPDATE nasa_images SET state = ? WHERE image_name = ?";

	private static final Logger LOGGER = Logger.getLogger(DBUtilsImpl.class);

	public DBUtilsImpl(Properties properties, String imageStoreIP,
			String imageStorePort, String dbUserName, String dbUserPass,
			String firstYear, String lastYear, String regionsFilePath,
			String specificRegion) {
		DBUtilsImpl.properties = properties;
		DBUtilsImpl.imageStoreIP = imageStoreIP;
		DBUtilsImpl.imageStorePort = imageStorePort;
		DBUtilsImpl.dbUserName = dbUserName;
		DBUtilsImpl.dbUserPass = dbUserPass;
		DBUtilsImpl.firstYear = firstYear;
		DBUtilsImpl.lastYear = lastYear;
		DBUtilsImpl.regionsFilePath = regionsFilePath;
		DBUtilsImpl.specificRegion = specificRegion;
	}

	@Override
	public Connection getConnection() throws SQLException {
		try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public void preparingStatement(Connection c) throws SQLException {
		PreparedStatement selectStatement = null;

		selectStatement = c.prepareStatement(SELECT_ALL_IMAGES_SQL);

		ResultSet rs = selectStatement.executeQuery();

		while (rs.next()) {
			System.out.println(new ImageData(rs.getString("image_name"), rs
					.getString("download_link"), ImageState.getStateFromStr(rs
					.getString("state")), rs.getString("federation_member"), rs
					.getInt("priority"), rs.getString("station_id"), rs.getString("sebal_version")));
		}
	}

	@Override
	public void updateState(String imageName, ImageState state)
			throws SQLException {
		if (imageName == null || imageName.isEmpty() || state == null) {
			throw new IllegalArgumentException("Invalid image name "
					+ imageName + " or state " + state);
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
			if (updateStatement != null) {
				try {
					if (!updateStatement.isClosed()) {
						updateStatement.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			connection.close();
		}
	}

	@Override
	public void addImages() throws SQLException {
		LOGGER.debug("Establishing connection to database...");
		Connection c = null;

		connectionPool = new BasicDataSource();
		connectionPool.setUsername(dbUserName);
		connectionPool.setPassword(dbUserPass);
		connectionPool.setDriverClassName(properties
				.getProperty("datastore_driver"));
		connectionPool.setUrl(properties.getProperty("datastore_url_prefix")
				+ imageStoreIP + ":" + imageStorePort + "/"
				+ properties.getProperty("datastore_name"));
		connectionPool.setInitialSize(1);

		c = getConnection();

		try {
			LOGGER.debug("Filling DB...");
			DBBootstrap dbBootstrap = new DBBootstrap(properties, imageStoreIP,
					imageStorePort);
			dbBootstrap.fillDB(firstYear, lastYear, regionsFilePath);

			preparingStatement(c);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}

		LOGGER.debug("Images added to " + dbUserName + " database");
	}

	@Override
	public void listImagesInDB() throws SQLException {
		ImageDataStore imageStore = new JDBCImageDataStore(properties,
				imageStoreIP, imageStorePort);

		List<ImageData> allImageData = imageStore.getAllImages();
		for (int i = 0; i < allImageData.size(); i++) {
			System.out.println(allImageData.get(i).toString());
		}
	}

	@Override
	public void listCorruptedImages() {
		ImageDataStore imageStore = new JDBCImageDataStore(properties,
				imageStoreIP, imageStorePort);

		List<ImageData> allImageData;
		try {
			allImageData = imageStore.getIn(ImageState.CORRUPTED);
			for (int i = 0; i < allImageData.size(); i++) {
				System.out.println(allImageData.get(i).toString());
			}
		} catch (SQLException e) {
			LOGGER.error(e);
		}
	}

	@Override
	public void getRegionImages() throws SQLException {
		ImageDataStore imageStore = new JDBCImageDataStore(properties,
				imageStoreIP, imageStorePort);

		for (int year = Integer.parseInt(firstYear); year <= Integer
				.parseInt(lastYear); year++) {
			List<String> imageList = new ArrayList<String>();

			for (int day = 1; day < 366; day++) {
				NumberFormat formatter = new DecimalFormat("000");
				String imageName = "LT5" + specificRegion + year
						+ formatter.format(day) + "CUB00";
				imageList.add(imageName);
				if (imageStore.getImage(imageName) != null) {
					imageStore.getImage(imageName).toString();
				}
			}
		}
	}

}
