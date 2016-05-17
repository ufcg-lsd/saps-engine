package org.fogbowcloud.sebal.fetcher;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

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
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.spy;

public class TestFetcher {
	
	private Properties properties;
	private DB pendingImageFetchDBMock;
	private ImageDataStore imageStoreMock;
	private String imageStoreIPMock = "fake-store-IP";
	private String imageStorePortMock = "fake-store-Port";
	private String ftpServerIPMock = "fake-ftp-IP";
	private String ftpServerPortMock = "fake-ftp-Port";
	private ConcurrentMap<String, ImageData> pendingImageFetchMapMock;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = new Properties();
		pendingImageFetchDBMock = mock(DB.class);
		pendingImageFetchMapMock = mock(ConcurrentMap.class);
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
		
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		doNothing().when(pendingImageFetchMapMock).put("image-data-1", imageDataMock);
		doNothing().when(pendingImageFetchMapMock).put("image-data-2", imageDataMock2);

		imageDataMock.setState(ImageState.FINISHED);
		imageStoreMock.updateImage(imageDataMock);
		verify(pendingImageFetchMapMock, times(4)).remove(imageDataMock);
		
		imageDataMock2.setState(ImageState.FINISHED);
		imageStoreMock.updateImage(imageDataMock2);
		verify(pendingImageFetchMapMock, times(4)).remove(imageDataMock2);
	}
	
	@Test
	public void testPrepareFetch() throws SQLException {
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		Mockito.when(imageStoreMock.lockImage(imageDataMock.getName())).thenReturn(true);
		doNothing().when(imageDataMock).setState(ImageState.FETCHING);
		doNothing().when(imageDataMock).setFederationMember("fake-federation-member-1");
		pendingImageFetchMapMock.put(imageDataMock.getName(), imageDataMock);

		imageStoreMock.updateImage(imageDataMock);
		imageStoreMock.unlockImage(imageDataMock.getName());
		
		Mockito.when(imageStoreMock.lockImage(imageDataMock2.getName())).thenReturn(true);
		doNothing().when(imageDataMock2).setState(ImageState.FETCHING);
		doNothing().when(imageDataMock2).setFederationMember("fake-federation-member-2");
		pendingImageFetchMapMock.put(imageDataMock2.getName(), imageDataMock2);

		imageStoreMock.updateImage(imageDataMock2);
		imageStoreMock.unlockImage(imageDataMock2.getName());
	}
	
	@Test
	public void testFinishFetch() throws SQLException {
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		doNothing().when(imageDataMock).setState(ImageState.FETCHED);
		imageStoreMock.updateImage(imageDataMock);
		pendingImageFetchMapMock.remove(imageDataMock.getName(), imageDataMock);
		
		doNothing().when(imageDataMock2).setState(ImageState.FETCHED);
		imageStoreMock.updateImage(imageDataMock2);
		pendingImageFetchMapMock.remove(imageDataMock2.getName(), imageDataMock2);
	}
	
	@Test
	public void testRollBackFetch() throws SQLException {
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		pendingImageFetchMapMock.remove(imageDataMock.getName(), imageDataMock);
		imageDataMock.setState(ImageState.FINISHED);
		imageStoreMock.updateImage(imageDataMock);
		
		pendingImageFetchMapMock.remove(imageDataMock2.getName(), imageDataMock2);
		imageDataMock2.setState(ImageState.FINISHED);
		imageStoreMock.updateImage(imageDataMock2);
	}
}
