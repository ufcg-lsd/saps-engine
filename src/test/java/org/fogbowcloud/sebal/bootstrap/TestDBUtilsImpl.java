package org.fogbowcloud.sebal.bootstrap;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestDBUtilsImpl {
	
	private DBUtilsImpl dbUtilsImpl;
	private JDBCImageDataStore imageStore;
	private BasicDataSource connectionPool;
	private String imageStoreIPMock = "fake-store-IP";
	private String imageStorePortMock = "fake-store-Port";
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		dbUtilsImpl = mock(DBUtilsImpl.class);
		imageStore = mock(JDBCImageDataStore.class);
		connectionPool = mock(BasicDataSource.class);
	}
	
	@Test
	public void testImageStoreNullProperties() {
		exception.expect(Exception.class);
		
		ImageDataStore imageStore = new JDBCImageDataStore(null, imageStoreIPMock, imageStorePortMock);
	}
	
	@Test
	public void testGetConnection() throws SQLException {		
		Connection connection = mock(Connection.class);
		connectionPool = mock(BasicDataSource.class);
		
		doReturn(connection).when(imageStore).getConnection();
	}
	
	@Test
	public void preparingStatement() throws SQLException {
		PreparedStatement selectStatementMock = mock(PreparedStatement.class);
		ResultSet rsMock = mock(ResultSet.class);
		
		doReturn(rsMock).when(selectStatementMock).executeQuery();
		
		ImageData imageData = new ImageData(rsMock.getString("image_name"), rsMock
				.getString("download_link"), ImageState.getStateFromStr(rsMock
				.getString("state")), rsMock.getString("federation_member"), rsMock
				.getInt("priority"), rsMock.getString("station_id"), rsMock
				.getString("sebal_version"), rsMock.getDate("ctime"), rsMock
				.getDate("utime"));
	}
	
	private static final String UPDATE_STATE_SQL = "UPDATE nasa_images SET state = ? WHERE image_name = ?";
	
	@Test
	public void testUpdateState() throws SQLException {
		Connection connectionMock = mock(Connection.class);
		PreparedStatement updateStatementMock = mock(PreparedStatement.class);
		String fakeState = "fake-state";
		String fakeImageName = "fake-image-name";
		
		doReturn(connectionMock).when(dbUtilsImpl).getConnection();
		
		doReturn(updateStatementMock).when(connectionMock).prepareStatement(eq(UPDATE_STATE_SQL));
		doNothing().when(updateStatementMock).setString(eq(1), eq(fakeState));
		doNothing().when(updateStatementMock).setString(eq(2), eq(fakeImageName));
		doReturn(true).when(updateStatementMock).execute();
	}

	@Test
	public void testSetImagesToPurge() throws SQLException {
		String fakeDay = "152376";
		String fakeImageDataMillis1 = "13345678214504";
		String fakeImageDataMillis2 = "12238643677504";
		String fakeImageDataDay1 = "151463";
		String fakeImageDataDay2 = "141650";
		
		List<ImageData> imagesToPurgeMock = mock(ArrayList.class);
		
		doReturn(imagesToPurgeMock).when(imageStore).getAllImages();
		doReturn(imagesToPurgeMock).when(imageStore).getIn(eq(ImageState.FETCHED));
		
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		doReturn(fakeImageDataMillis1).when(imageDataMock).getUpdateTime();
		doReturn(fakeImageDataMillis2).when(imageDataMock2).getUpdateTime();
		
		// FIXME: update test
/*		doReturn(Long.valueOf(fakeImageDataDay1).longValue()).when(dbUtilsImpl)
				.convertMilliToDays(
						eq(Long.valueOf(fakeImageDataMillis1).longValue()));
		doReturn(Long.valueOf(fakeImageDataDay2).longValue()).when(dbUtilsImpl)
				.convertMilliToDays(
						eq(Long.valueOf(fakeImageDataMillis2).longValue()));
		
		doReturn(true).when(dbUtilsImpl).isBeforeDay(eq(fakeDay), eq(Integer.valueOf(fakeImageDataDay1)));
		doReturn(true).when(dbUtilsImpl).isBeforeDay(eq(fakeDay), eq(Integer.valueOf(fakeImageDataDay2)));*/
		
		doNothing().when(imageDataMock).setImageStatus(eq(ImageData.PURGED));
		doNothing().when(imageDataMock).setUpdateTime(eq(new Date(Calendar.getInstance().getTimeInMillis())));
	}

}
