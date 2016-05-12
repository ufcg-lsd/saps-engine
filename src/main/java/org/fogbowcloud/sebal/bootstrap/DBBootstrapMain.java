package org.fogbowcloud.sebal.bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
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

public class DBBootstrapMain {

	static String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM nasa_images WHERE state = ? ORDER BY priority, image_name LIMIT ? ";
	static String SELECT_ALL_IMAGES_SQL = "SELECT * FROM nasa_images ORDER BY priority, image_name";

	static String SELECT_LOCK_IMAGE_SQL_SQL = "SELECT pg_try_advisory_lock(?) FROM nasa_images WHERE image_name = ?";
	static String SELECT_UNLOCK_IMAGE_SQL_SQL = "SELECT pg_advisory_unlock(?)";

	private static BasicDataSource connectionPool;

	private static final Logger LOGGER = Logger
			.getLogger(DBBootstrapMain.class);

	public static void main(String[] args) throws IOException, SQLException {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		String sqlIP = args[1];
		String sqlPort = args[2];
		String dbUserName = args[3];
		String dbUserPass = args[4];
		String dbUseType = args[5];
		String firstYear = args[6];
		String lastYear = args[7];
		String regionsFilePath = args[8];
		String specificRegion = args[9];

		if (dbUseType.equals("add")) {
			addImages(properties, sqlIP, sqlPort, dbUserName, dbUserPass,
					firstYear, lastYear, regionsFilePath);
		} else if (dbUseType.equals("list")) {
			listImagesInDB(properties, sqlIP, sqlPort);
		} else if (dbUseType.equals("list-corrupted")) {
			listCorruptedImages(properties, sqlIP, sqlPort);
		} else if (dbUseType.equals("get")) {
			getRegionImages(properties, sqlIP, sqlPort, specificRegion,
					firstYear, lastYear);
		}

		System.out.println("Operation done successfully");

	}

	private static void listCorruptedImages(Properties properties,
			String imageStoreIP, String imageStorePort) {
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

	private static final String UPDATE_STATE_SQL = "UPDATE nasa_images SET state = ? WHERE image_name = ?";

	public static void preparingStatement(Connection c) throws SQLException {
		PreparedStatement selectStatement = null;

		selectStatement = c.prepareStatement(SELECT_ALL_IMAGES_SQL);

		ResultSet rs = selectStatement.executeQuery();

		while (rs.next()) {
			System.out.println(new ImageData(rs.getString("image_name"), rs
					.getString("download_link"), ImageState.getStateFromStr(rs
					.getString("state")), rs.getString("federation_member"), rs
					.getInt("priority")));
		}
	}

	public static void updateState(String imageName, ImageState state)
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

	public static void addImages(Properties properties, String sqlIP,
			String sqlPort, String dbUserName, String dbUserPass,
			String firstYear, String lastYear, String regionsFilePath)
			throws SQLException {

		LOGGER.debug("Establishing connection to database...");
		Connection c = null;

		connectionPool = new BasicDataSource();
		connectionPool.setUsername(dbUserName);
		connectionPool.setPassword(dbUserPass);
		connectionPool.setDriverClassName(properties
				.getProperty("datastore_driver"));
		connectionPool.setUrl(properties.getProperty("datastore_url_prefix")
				+ sqlIP + ":" + sqlPort + "/"
				+ properties.getProperty("datastore_name"));
		connectionPool.setInitialSize(1);

		c = getConnection();

		try {
			LOGGER.debug("Filling DB...");
			DBBootstrap dbBootstrap = new DBBootstrap(properties, sqlIP,
					sqlPort);
			dbBootstrap.fillDB(firstYear, lastYear, regionsFilePath);

			preparingStatement(c);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}

		LOGGER.debug("Images added to " + dbUserName + " database");
	}

	private static void listImagesInDB(Properties properties,
			String imageStoreIP, String imageStorePort) throws SQLException {
		ImageDataStore imageStore = new JDBCImageDataStore(properties,
				imageStoreIP, imageStorePort);

		List<ImageData> allImageData = imageStore.getAllImages();
		for (int i = 0; i < allImageData.size(); i++) {
			System.out.println(allImageData.get(i).toString());
		}
	}

	public static void getRegionImages(Properties properties,
			String imageStoreIP, String imageStorePort, String region,
			String firstYear, String lastYear) throws SQLException {
		ImageDataStore imageStore = new JDBCImageDataStore(properties,
				imageStoreIP, imageStorePort);

		for (int year = Integer.parseInt(firstYear); year <= Integer
				.parseInt(lastYear); year++) {
			List<String> imageList = new ArrayList<String>();

			for (int day = 1; day < 366; day++) {
				NumberFormat formatter = new DecimalFormat("000");
				String imageName = "LT5" + region + year
						+ formatter.format(day) + "CUB00";
				imageList.add(imageName);
				if (imageStore.getImage(imageName) != null) {
					imageStore.getImage(imageName).toString();
				}
			}
		}

	}

	public static Connection getConnection() throws SQLException {
		try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

}
