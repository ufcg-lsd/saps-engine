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

public class BootstrapMain {

	static String SELECT_LIMITED_IMAGES_IN_STATE_SQL = "SELECT * FROM nasa_images WHERE state = ? ORDER BY priority, image_name LIMIT ? ";
	static String SELECT_ALL_IMAGES_SQL = "SELECT * FROM nasa_images ORDER BY priority, image_name";

	static String SELECT_LOCK_IMAGE_SQL_SQL  = "SELECT pg_try_advisory_lock(?) FROM nasa_images WHERE image_name = ?";
	static String SELECT_UNLOCK_IMAGE_SQL_SQL  = "SELECT pg_advisory_unlock(?)";

	private static BasicDataSource connectionPool;

//	static String SELECT_LOCK_IMAGE_SQL_SQL  = "SELECT pg_try_advisory_xact_lock(1) FROM nasa_images WHERE image_name = ?";
//	static String SELECT_UNLOCK_IMAGE_SQL_SQL  = "SELECT pg_advisory_unlock(1)";
	
	public static void main(String[] args) throws IOException, SQLException {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
//		
//		DataBaseBootstrap dbBootstrap = new DataBaseBootstrap(properties);
//		
//		dbBootstrap.fillDB();
//			
		//SELECT 
		 Connection c = null;
//	       Statement stmt = null;
				
		connectionPool = new BasicDataSource();
		connectionPool.setUsername(properties.getProperty("datastore_username"));
		connectionPool.setPassword(properties.getProperty("datastore_password"));
		connectionPool.setDriverClassName(properties.getProperty("datastore_driver"));
		connectionPool.setUrl(properties.getProperty("datastore_url"));
		connectionPool.setInitialSize(1);
		
		c = getConnection();
	       try {
//	       Class.forName("org.postgresql.Driver");
//	         c = DriverManager
//	            .getConnection("jdbc:postgresql://localhost:9899/imagestest", "sebal",
//						"S3B4L");
//	         c.setAutoCommit(false);
//	         System.out.println("Opened database successfully");

//	    	   updateState("LT52160651994139CUB00", ImageState.DOWNLOADED);
//	    	   
	     		PreparedStatement selectStatement = null;
		     			
//   				selectStatement = c.prepareStatement(SELECT_LIMITED_IMAGES_IN_STATE_SQL);
//	     		selectStatement.setString(1, "not_downloaded");
//	     		selectStatement.setInt(2, 20);
	     		selectStatement = c.prepareStatement(SELECT_ALL_IMAGES_SQL);
	     		
	     	ResultSet rs = selectStatement.executeQuery();
	         
			while (rs.next()) {
				System.out.println(new ImageData(rs.getString("image_name"), rs
						.getString("download_link"), ImageState.getStateFromStr(rs
						.getString("state")), rs.getString("federation_member"), rs
						.getInt("priority")));
			}
	         
//			ImageDataStore imageStore = new JDBCImageDataStore(properties);
//
//			final List<ImageData> imageDataList = imageStore.getIn(ImageState.NOT_DOWNLOADED,
//					1);
//			System.out.println(imageDataList);
//			
//			selectStatement = c.prepareStatement(SELECT_LOCK_IMAGE_SQL_SQL);
//     		final int hashCode1 = imageDataList.get(0).getName().hashCode();
//			selectStatement.setInt(1, hashCode1);
//     		selectStatement.setString(2, imageDataList.get(0).getName());
//     		ResultSet rs = selectStatement.executeQuery();
//     		
//     		rs.next();
//     		System.out.println("LOCK 1: " + rs.getBoolean(1));
//     		
//     		
//     		c.close();
//
//     		String imageName = imageDataList.get(0).getName();
//
//     		Connection firstConn = getConnection();
//     		selectStatement = firstConn.prepareStatement(SELECT_LOCK_IMAGE_SQL_SQL);						
//			System.out.println("ImageName - 1: " + imageName);
//			System.out.println(imageStore.get(imageDataList.get(0).getName()));
//
//			selectStatement.setInt(1, hashCode1);
//     		selectStatement.setString(2, imageDataList.get(0).getName());
//     		rs = selectStatement.executeQuery();
//     		    		
//     		while (rs.next()) {
//     			System.out.println("LOCK 1: " + rs.getBoolean(1));
//     		}
//     		
////     		firstConn.prepareStatement(SELECT_UNLOCK_IMAGE_SQL_SQL);
////     		selectStatement.setInt(1, hashCode1);
////     		rs = selectStatement.executeQuery();
////     		
////     		while (rs.next()) {
////     			System.out.println("UNLOCK 1: " + rs.getBoolean(1));
////     		}
//     		
////     		c = getConnection();
////     		selectStatement = c.prepareStatement(SELECT_LOCK_IMAGE_SQL_SQL);
////			int hashCode2 = "LT52150661990249CUB00".hashCode();
////			selectStatement.setInt(1, hashCode2 );
////     		selectStatement.setString(2, "LT52150661990249CUB00");
////     		rs = selectStatement.executeQuery();
////
////     		while (rs.next()) {
////     			System.out.println("LOCK 1 - IMAGE2: " + rs.getBoolean(1));
////     		}
//
//     		Runnable s = new Runnable() {
//				
//     			private BasicDataSource connectionPool;
//
//				@Override
//				public void run() {
//					Connection c = null;
//
//					PreparedStatement selectStatement;
//					try {
//						
//						
//						connectionPool = new BasicDataSource();
//						connectionPool.setUsername(properties.getProperty("datastore_username"));
//						connectionPool.setPassword(properties.getProperty("datastore_password"));
//						connectionPool.setDriverClassName(properties.getProperty("datastore_driver"));
//						connectionPool.setUrl(properties.getProperty("datastore_url"));
//						connectionPool.setInitialSize(1);
//						
//						c = connectionPool.getConnection();
////						 Class.forName("org.postgresql.Driver");
////				         c = DriverManager
////				            .getConnection("jdbc:postgresql://localhost:9899/imagestest", "sebal",
////									"S3B4L");
////				         c.setAutoCommit(false);
////				         System.out.println("Opened database successfully");
////						c = getConnection();
//						ImageDataStore imageStore = new JDBCImageDataStore(properties);
//
//						selectStatement = c.prepareStatement(SELECT_LOCK_IMAGE_SQL_SQL);
//						selectStatement.setInt(1, hashCode1);
//			     		selectStatement.setString(2, imageDataList.get(0).getName());
//
//						ResultSet rs = selectStatement.executeQuery();
//
//						System.out.println("ImageName - 2: " + imageDataList.get(0).getName());
////						imageStore.updateState(imageDataList.get(0).getName(), ImageState.DOWNLOADED);
//												
//						System.out.println(imageStore.get(imageDataList.get(0).getName()));
//						while (rs.next()) {
//							System.out.println("LOCK 2: " + rs.getBoolean(1));
//						}
//
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//
//				}
//			};
//     		
//			s.run();
//			c = getConnection();
//			
//			System.out.println("ImageName - 3: " + imageDataList.get(0).getName());
//			System.out.println(imageStore.get(imageDataList.get(0).getName()));
//
////			imageStore.updateState(imageDataList.get(0).getName(), ImageState.DOWNLOADED);									
////			System.out.println(imageStore.get(imageDataList.get(0).getName()));
//			
//     		selectStatement = c.prepareStatement(SELECT_UNLOCK_IMAGE_SQL_SQL);
//     		selectStatement.setInt(1, hashCode1);
//     		rs = selectStatement.executeQuery();
//     		
//     		while (rs.next()) {
//     			System.out.println("UNLOCK 1: " + rs.getBoolean(1));
//     		}
//     		
//     		selectStatement = firstConn.prepareStatement(SELECT_UNLOCK_IMAGE_SQL_SQL);
//     		selectStatement.setInt(1, hashCode1);
//     		rs = selectStatement.executeQuery();
//     		
//     		rs.next();
//     		System.out.println("UNLOCK 1: " + rs.getBoolean(1));
//     		
//
//			
//			s.run();
//			
//	         rs.close();
//	         selectStatement.close();
//	         c.close();
	       } catch ( Exception e ) {
	         System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	         e.printStackTrace();
	         System.exit(0);
	       }
	       System.out.println("Operation done successfully");
	}
	
	private static final String UPDATE_STATE_SQL = "UPDATE nasa_images SET state = ? WHERE image_name = ?";

	
	public static void updateState(String imageName, ImageState state) throws SQLException {

		if (imageName == null || imageName.isEmpty() || state == null) {
//			LOGGER.error("Invalid image name " + imageName + " or state " + state);
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
