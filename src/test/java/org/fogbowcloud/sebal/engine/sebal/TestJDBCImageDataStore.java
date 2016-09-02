package org.fogbowcloud.sebal.engine.sebal;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestJDBCImageDataStore {
	
	private String IMAGE_TABLE_NAME = "FAKE_NASA_IMAGES";
	private static String STATES_TABLE_NAME = "STATES_TIMESTAMPS";
	private String fakeImageStoreIP = "fake-IP";
	private String fakeImageStorePort = "fake-Port";
	private Properties properties;
	private JDBCImageDataStore imageDataStore;
	
	private String INSERT_IMAGE_SQL = "INSERT INTO " + IMAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = new Properties();
		imageDataStore = mock(JDBCImageDataStore.class);
	}
	
	@Test
	public void testJDBCDataStoreNullProperties() throws SQLException {
		exception.expect(Exception.class);
		
		imageDataStore = new JDBCImageDataStore(null);
	}
	
	@Test
	public void testGetConnection() throws SQLException {		
		Connection connection = mock(Connection.class);
		
		doReturn(connection).when(imageDataStore).getConnection();
	}
	
	@Test
	public void testClose() {
		Statement statement = mock(Statement.class);
		Connection connection = mock(Connection.class);
		
		doNothing().when(imageDataStore).close(eq(statement), eq(connection));
	}
	
	@Test
	public void testAddImage() throws SQLException {		
		String fakeImageName = "fake-image-name";
		String fakeDownloadLink = "fake-download-link";
		int fakePriority = 0;
		String fakeStationId = "fake-station-id";
		String fakeSebalVersion = "fake-sebal-version";
		long fakeCreationTime = 0;
		long fakeUpdatedTime = 0;
		
		Connection connection = mock(Connection.class);
		PreparedStatement preparedStatement = mock(PreparedStatement.class);
		
		doReturn(connection).when(imageDataStore).getConnection();
		
		doReturn(preparedStatement).when(connection).prepareStatement(eq(INSERT_IMAGE_SQL));
		doNothing().when(preparedStatement).setString(eq(1), eq(fakeImageName));
		doNothing().when(preparedStatement).setString(eq(2), eq(fakeDownloadLink));
		doNothing().when(preparedStatement).setString(eq(3), eq(ImageState.NOT_DOWNLOADED.getValue()));
		doNothing().when(preparedStatement).setString(eq(4), eq(ImageDataStore.NONE));
		doNothing().when(preparedStatement).setInt(eq(5), eq(fakePriority));
		doNothing().when(preparedStatement).setString(eq(6), eq(fakeStationId));
		doNothing().when(preparedStatement).setString(eq(7), eq(fakeSebalVersion));
		doNothing().when(preparedStatement).setString(eq(8), eq(String.valueOf(fakeCreationTime)));
		doNothing().when(preparedStatement).setString(eq(9), eq(String.valueOf(fakeUpdatedTime)));
		
		doReturn(true).when(preparedStatement).execute();
		
		doNothing().when(imageDataStore).close(preparedStatement, connection);
	}
	
	private static final String INSERT_STATE_TIMESTAMP_SQL = "INSERT INTO " + STATES_TABLE_NAME
			+ " VALUES(?, ?, ?)";
	
	@Test
	public void testAddStateStamp() throws SQLException {
		String fakeImageName = "fake-image-name";
		Date fakeUpdatedTime = mock(Date.class);
		
		Connection connection = mock(Connection.class);
		PreparedStatement insertStatement = mock(PreparedStatement.class);
		
		doReturn(connection).when(imageDataStore).getConnection();
		
		doReturn(insertStatement).when(connection).prepareStatement(eq(INSERT_STATE_TIMESTAMP_SQL));
		doNothing().when(insertStatement).setString(eq(1), eq(fakeImageName));
		doNothing().when(insertStatement).setString(eq(2), eq(ImageState.DOWNLOADING.getValue()));
		doNothing().when(insertStatement).setDate(eq(3), eq(fakeUpdatedTime));
		
		doReturn(true).when(insertStatement).execute();
		
		doNothing().when(imageDataStore).close(insertStatement, connection);
	}
	
	private static final String REMOVE_STATE_TIMESTAMP_SQL = "DELETE FROM " + STATES_TABLE_NAME
			+ " WHERE image_name = ? AND state = ? AND utime = ?";
	
	@Test
	public void testRemoveStateStamp() throws SQLException {		
		String fakeImageName = "fake-image-name";
		Date fakeUpdatedTime = mock(Date.class);
		
		Connection connection = mock(Connection.class);
		PreparedStatement insertStatement = mock(PreparedStatement.class);
		PreparedStatement removeStatement = mock(PreparedStatement.class);
		
		doReturn(connection).when(imageDataStore).getConnection();
		
		doReturn(insertStatement).when(connection).prepareStatement(eq(INSERT_STATE_TIMESTAMP_SQL));
		doNothing().when(insertStatement).setString(eq(1), eq(fakeImageName));
		doNothing().when(insertStatement).setString(eq(2), eq(ImageState.DOWNLOADING.getValue()));
		doNothing().when(insertStatement).setDate(eq(3), eq(fakeUpdatedTime));
		
		doReturn(true).when(insertStatement).execute();
		
		doNothing().when(imageDataStore).close(insertStatement, connection);
		
		doReturn(connection).when(imageDataStore).getConnection();
		
		doReturn(removeStatement).when(connection).prepareStatement(eq(REMOVE_STATE_TIMESTAMP_SQL));
		doNothing().when(removeStatement).setString(eq(1), eq(fakeImageName));
		doNothing().when(removeStatement).setString(eq(2), eq(ImageState.DOWNLOADING.getValue()));
		doNothing().when(removeStatement).setDate(eq(3), eq(fakeUpdatedTime));
		
		doReturn(true).when(removeStatement).execute();
		
		doNothing().when(imageDataStore).close(removeStatement, connection);
	}
	
	@Test
	public void testTimeStamp() {
		
	}
}