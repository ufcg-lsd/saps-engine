package org.fogbowcloud.sebal;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestJDBCImageDataStore {
	
	private String IMAGE_TABLE_NAME = "FAKE_NASA_IMAGES";
	private String IMAGE_NAME_COL = "fake_image_name";
	private String DOWNLOAD_LINK_COL = "fake-download_link";
	private String PRIORITY_COL = "fake-priority";
	private String FEDERATION_MEMBER_COL = "fake-federation_member";
	private String STATE_COL = "fake-state";
	private String STATION_ID_COL = "fake-station_id";
	private String SEBAL_VERSION_COL = "fake-sebal_version";
	private String CREATED_COL = "fake-created";
	private String LAST_UPDATED_COL = "fake-last_updated";
	private String fakeImageStoreIP = "fake-IP";
	private String fakeImageStorePort = "fake-Port";
	private Properties properties;
	private Map<String, Connection> lockedImages;
	private BasicDataSource connectionPool;
	private JDBCImageDataStore imageDataStore;
	
	private String INSERT_IMAGE_SQL = "INSERT INTO " + IMAGE_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = new Properties();
		lockedImages = mock(Map.class);
		imageDataStore = mock(JDBCImageDataStore.class);
	}
	
	@Test
	public void testCreateJDBCDataStore() {
		imageDataStore = new JDBCImageDataStore(properties, fakeImageStoreIP, fakeImageStorePort);
		
		Assert.assertNotNull(imageDataStore);
	}
	
	@Test
	public void testJDBCDataStoreNullProperties() {
		exception.expect(Exception.class);
		
		imageDataStore = new JDBCImageDataStore(null, fakeImageStoreIP, fakeImageStorePort);
	}
	
	@Test
	public void testGetConnection() throws SQLException {		
		Connection connection = mock(Connection.class);		
		connectionPool = mock(BasicDataSource.class);
		
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
		
		doReturn(true).when(preparedStatement).execute();
		
		doNothing().when(imageDataStore).close(preparedStatement, connection);
	}

}