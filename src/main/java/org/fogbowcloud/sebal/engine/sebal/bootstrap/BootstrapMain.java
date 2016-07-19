package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageState;

public class BootstrapMain {

	static String SELECT_ALL_IMAGES_SQL = "SELECT * FROM nasa_images ORDER BY priority, image_name";

	private static BasicDataSource connectionPool;

	public static void main(String[] args) throws IOException, SQLException {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String sqlIP = args[1];
		String sqlPort = args[2];
		//SELECT
		 Connection c = null;

		connectionPool = new BasicDataSource();
		connectionPool.setUsername(properties.getProperty("datastore_username"));
		connectionPool.setPassword(properties.getProperty("datastore_password"));
		connectionPool.setDriverClassName(properties.getProperty("datastore_driver"));
		connectionPool.setUrl(properties.getProperty("datastore_url_prefix")
				+ sqlIP + ":" + sqlPort);
		connectionPool.setInitialSize(1);
		
		c = getConnection();
	       try {
	     		PreparedStatement selectStatement = null;
	     		selectStatement = c.prepareStatement(SELECT_ALL_IMAGES_SQL);
	     		
	     	ResultSet rs = selectStatement.executeQuery();
	         
			while (rs.next()) {
				System.out.println(new ImageData(rs.getString("image_name"), rs
						.getString("download_link"), ImageState
						.getStateFromStr(rs.getString("state")), rs
						.getString("federation_member"), rs.getInt("priority"),
						rs.getString("station_id"), rs
								.getString("sebal_version"), rs
								.getString("sebal_engine_version"), rs
								.getString("blowout_version"), rs
								.getDate("ctime"), rs.getDate("utime"), rs
								.getString("error_msg")));
			}
	         
	       } catch ( Exception e ) {
	         System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	         e.printStackTrace();
	         System.exit(0);
	       }
	       System.out.println("Operation done successfully");
	}
	
	private static final String UPDATE_STATE_SQL = "UPDATE nasa_images SET state = ? WHERE image_name = ?";
	
	public static Connection getConnection() throws SQLException {
		try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}
}
