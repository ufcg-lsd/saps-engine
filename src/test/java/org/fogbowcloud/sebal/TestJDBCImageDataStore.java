package org.fogbowcloud.sebal;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestJDBCImageDataStore {
	
	private String IMAGE_TABLE_NAME = "FAKE_NASA_IMAGES";
	private String IMAGE_NAME_COL = "fake_image_name";
	private String DOWNLOAD_LINK_COL = "fake-download_link";
	private String PRIORITY_COL = "fake-priority";
	private String FEDERATION_MEMBER_COL = "fake-federation_member";
	private String STATE_COL = "fake-state";
	private String fakeImageStoreIP = "fake-IP";
	private String fakeImageStorePort = "fake-Port";
	private Properties properties;
	private Map<String, Connection> lockedImages;
	private BasicDataSource connectionPool;
	private JDBCImageDataStore imageDataStore;

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
		exception.expect(Exception.class);
		Connection connection = mock(Connection.class);
		
		connectionPool = mock(BasicDataSource.class);
		
		doReturn(connection).when(connectionPool).getConnection();
		verify(connectionPool, times(1)).getConnection();
	}
	
	@Test
	public void testClose() {
		Statement statement = mock(Statement.class);
		Connection connection = mock(Connection.class);
		
		doNothing().when(imageDataStore).close(eq(statement), eq(connection));
	}
	
	@Test
	public void testAddImage() {
		
	}

}
