package org.fogbowcloud.sebal.bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageState;

public class DBBootstrapMain {

	static String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM nasa_images WHERE state = ? ORDER BY priority, image_name LIMIT ? ";
	static String SELECT_ALL_IMAGES_SQL = "SELECT * FROM nasa_images ORDER BY priority, image_name";

	static String SELECT_LOCK_IMAGE_SQL_SQL = "SELECT pg_try_advisory_lock(?) FROM nasa_images WHERE image_name = ?";
	static String SELECT_UNLOCK_IMAGE_SQL_SQL = "SELECT pg_advisory_unlock(?)";

	private static BasicDataSource connectionPool;
	
	public static void main(String[] args) throws IOException, SQLException {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String sqlIP = args[1];
		String sqlPort = args[2];
		String dbUserName = args[3];
		String dbUserPass = args[4];
		String firstYear = args[5];
		String lastYear = args[6];
		String regionsFilePath = args[7];
				
		Connection c = null;
				
		connectionPool = new BasicDataSource();
		connectionPool.setUsername(dbUserName);
		connectionPool.setPassword(dbUserPass);
		connectionPool.setDriverClassName(properties.getProperty("datastore_driver"));
		connectionPool.setUrl(properties.getProperty("datastore_url_prefix")
				+ sqlIP + ":" + sqlPort + "/" + properties.getProperty("datastore_name"));
		connectionPool.setInitialSize(1);
		
		c = getConnection();
				
		try {			
			DBBootstrap dbBootstrap = new DBBootstrap(properties, sqlIP, sqlPort);
			dbBootstrap.fillDB(firstYear, lastYear, regionsFilePath);
			
			listImagesInDB(c);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		
		System.out.println("Operation done successfully");

	}
	
	private static final String UPDATE_STATE_SQL = "UPDATE nasa_images SET state = ? WHERE image_name = ?";
	
	public static void listImagesInDB(Connection c) throws SQLException {
		PreparedStatement selectStatement = null;

		selectStatement = c.prepareStatement(SELECT_ALL_IMAGES_SQL);

		ResultSet rs = selectStatement.executeQuery();

		while (rs.next()) {
			System.out
					.println(new ImageData(rs.getString("image_name"), rs
							.getString("download_link"), ImageState
							.getStateFromStr(rs.getString("state")), rs
							.getString("federation_member"), rs
							.getInt("priority")));
		}
	}
	
	public static void updateState(String imageName, ImageState state) throws SQLException {

		if (imageName == null || imageName.isEmpty() || state == null) {
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
	
	public static Connection getConnection() throws SQLException {
		try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

}
