package org.fogbowcloud.sebal.bootstrap;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestDBUtilsImpl {
	
	private Properties properties;
	private ImageDataStore imageStore;
	private DBUtilsImpl dbUtilsImpl;
	private String imageStoreIPMock = "fake-store-IP";
	private String imageStorePortMock = "fake-store-Port";
	private String imageStoreUserNameMock = "fake-store-User";
	private String imageStoreUserPassMock = "fake-store-pass";
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = new Properties();
		imageStore = mock(ImageDataStore.class);
		dbUtilsImpl = mock(DBUtilsImpl.class);
	}
	
	@Test
	public void testImageStoreNullProperties() {
		exception.expect(Exception.class);
		
		ImageDataStore imageStore = new JDBCImageDataStore(null, imageStoreIPMock, imageStorePortMock);
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
		
		doReturn(Long.valueOf(fakeImageDataDay1).longValue()).when(dbUtilsImpl)
				.convertMilliToDays(
						eq(Long.valueOf(fakeImageDataMillis1).longValue()));
		doReturn(Long.valueOf(fakeImageDataDay2).longValue()).when(dbUtilsImpl)
				.convertMilliToDays(
						eq(Long.valueOf(fakeImageDataMillis2).longValue()));
		
		doReturn(true).when(dbUtilsImpl).isBeforeDay(eq(fakeDay), eq(Integer.valueOf(fakeImageDataDay1)));
		doReturn(true).when(dbUtilsImpl).isBeforeDay(eq(fakeDay), eq(Integer.valueOf(fakeImageDataDay2)));
		
		doNothing().when(imageDataMock).setImageStatus(eq(ImageData.PURGED));
		doNothing().when(imageDataMock).setUpdateTime(eq(String.valueOf(System.currentTimeMillis())));
	}

}
