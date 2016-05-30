package org.fogbowcloud.sebal.fetcher;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.Properties;

import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mapdb.DB;
import org.mapdb.HTreeMap;

public class TestFetcher {
	
	private Properties properties;
	private DB pendingImageFetchDBMock;
	private ImageDataStore imageStoreMock;
	private String imageStoreIPMock = "fake-store-IP";
	private String imageStorePortMock = "fake-store-Port";
	private HTreeMap<String, ImageData> pendingImageFetchMapMock;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = new Properties();
		pendingImageFetchDBMock = mock(DB.class);
		pendingImageFetchMapMock = mock(HTreeMap.class);
	}
	
	@Test
	public void testImageStoreNullProperties() {
		exception.expect(Exception.class);
		
		imageStoreMock = spy(new JDBCImageDataStore(null, imageStoreIPMock,
				imageStorePortMock));
	}

	@Test
	public void testImageStoreNotNull() {
		imageStoreMock = spy(new JDBCImageDataStore(properties, imageStoreIPMock,
				imageStorePortMock));
		
		Assert.assertNotNull(imageStoreMock);
	}
	
	@Test
	public void testCleanUnfinishedFetchedData() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageData imageData1 = mock(ImageData.class);
		ImageData imageData2 = mock(ImageData.class);
		
		doNothing().when(pendingImageFetchMapMock).put(eq("image-data-1"), eq(imageData1));
		doNothing().when(pendingImageFetchMapMock).put(eq("image-data-2"), eq(imageData2));
		doNothing().when(pendingImageFetchDBMock).commit();

		imageData1.setState(ImageState.FINISHED);
		imageStoreMock.updateImage(imageData1);
		verify(pendingImageFetchMapMock, times(4)).remove(imageData1);
		doNothing().when(pendingImageFetchDBMock).commit();
		
		imageData2.setState(ImageState.FINISHED);
		imageStoreMock.updateImage(imageData2);
		verify(pendingImageFetchMapMock, times(4)).remove(imageData2);
		doNothing().when(pendingImageFetchDBMock).commit();
	}
	
	@Test
	public void testPrepareFetch() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);

		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		doReturn(true).when(imageStoreMock).lockImage(imageDataMock.getName());
		doNothing().when(imageDataMock).setState(ImageState.FETCHING);
		doNothing().when(imageDataMock).setFederationMember("fake-federation-member-1");
		doReturn(imageDataMock).when(pendingImageFetchMapMock).put(imageDataMock.getName(), imageDataMock);
		doNothing().when(pendingImageFetchDBMock).commit();

		imageStoreMock.updateImage(imageDataMock);
		imageStoreMock.unlockImage(imageDataMock.getName());
		
		doReturn(true).when(imageStoreMock).lockImage(imageDataMock2.getName());
		doNothing().when(imageDataMock2).setState(ImageState.FETCHING);
		doNothing().when(imageDataMock2).setFederationMember("fake-federation-member-2");
		doReturn(imageDataMock2).when(pendingImageFetchMapMock).put(imageDataMock2.getName(), imageDataMock2);
		doNothing().when(pendingImageFetchDBMock).commit();
		
		imageStoreMock.updateImage(imageDataMock2);
		imageStoreMock.unlockImage(imageDataMock2.getName());
	}
	
	@Test
	public void testFinishFetch() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		doNothing().when(imageDataMock).setState(ImageState.FETCHED);
		doNothing().when(imageStoreMock).updateImage(imageDataMock);
		doReturn(imageDataMock).when(pendingImageFetchMapMock).remove(imageDataMock.getName(), imageDataMock);
		doNothing().when(pendingImageFetchDBMock).commit();
		
		doNothing().when(imageDataMock2).setState(ImageState.FETCHED);
		doNothing().when(imageStoreMock).updateImage(imageDataMock2);
		doReturn(imageDataMock2).when(pendingImageFetchMapMock).remove(imageDataMock2.getName(), imageDataMock2);
		doNothing().when(pendingImageFetchDBMock).commit();
	}
	
	@Test
	public void testRollBackFetch() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		doReturn(imageDataMock).when(pendingImageFetchMapMock).remove(imageDataMock.getName(), imageDataMock);
		doNothing().when(pendingImageFetchDBMock).commit();
		doNothing().when(imageDataMock).setState(ImageState.FINISHED);
		doNothing().when(imageStoreMock).updateImage(imageDataMock);
		
		doReturn(imageDataMock2).when(pendingImageFetchMapMock).remove(imageDataMock2.getName(), imageDataMock2);
		doNothing().when(pendingImageFetchDBMock).commit();
		doNothing().when(imageDataMock2).setState(ImageState.FINISHED);
		doNothing().when(imageStoreMock).updateImage(imageDataMock2);
	}
}
