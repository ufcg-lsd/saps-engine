package org.fogbowcloud.saps.engine.core.archiver;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mapdb.DB;
import org.mapdb.HTreeMap;

public class TestArchiver {
	
	private Properties properties;
	private DB pendingImageFetchDBMock;
	private ImageDataStore imageStoreMock;
	private String imageStoreIPMock = "fake-store-IP";
	private String imageStorePortMock = "fake-store-Port";
	private HTreeMap<String, ImageTask> pendingImageFetchMapMock;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = new Properties();
		pendingImageFetchDBMock = mock(DB.class);
		pendingImageFetchMapMock = mock(HTreeMap.class);
	}
	
	@Test
	public void testImageStoreNullProperties() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = spy(new JDBCImageDataStore(null));
	}

	@Test
	public void testCleanUnfinishedFetchedData() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageTask imageData1 = mock(ImageTask.class);
		ImageTask imageData2 = mock(ImageTask.class);
		
		doNothing().when(pendingImageFetchMapMock).put(eq("image-data-1"), eq(imageData1));
		doNothing().when(pendingImageFetchMapMock).put(eq("image-data-2"), eq(imageData2));
		doNothing().when(pendingImageFetchDBMock).commit();

		imageData1.setState(ImageTaskState.FINISHED);
		imageStoreMock.updateImageTask(imageData1);
		verify(pendingImageFetchMapMock, times(4)).remove(imageData1);
		doNothing().when(pendingImageFetchDBMock).commit();
		
		imageData2.setState(ImageTaskState.FINISHED);
		imageStoreMock.updateImageTask(imageData2);
		verify(pendingImageFetchMapMock, times(4)).remove(imageData2);
		doNothing().when(pendingImageFetchDBMock).commit();
	}
	
	@Test
	public void testPrepareFetch() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);

		ImageTask imageDataMock = mock(ImageTask.class);
		ImageTask imageDataMock2 = mock(ImageTask.class);
		
		doReturn(true).when(imageStoreMock).lockTask(imageDataMock.getName());
		doNothing().when(imageDataMock).setState(ImageTaskState.ARCHIVING);
		doNothing().when(imageDataMock).setFederationMember("fake-federation-member-1");
		doReturn(imageDataMock).when(pendingImageFetchMapMock).put(imageDataMock.getName(), imageDataMock);
		doNothing().when(pendingImageFetchDBMock).commit();

		imageStoreMock.updateImageTask(imageDataMock);
		imageStoreMock.unlockTask(imageDataMock.getName());
		
		doReturn(true).when(imageStoreMock).lockTask(imageDataMock2.getName());
		doNothing().when(imageDataMock2).setState(ImageTaskState.ARCHIVING);
		doNothing().when(imageDataMock2).setFederationMember("fake-federation-member-2");
		doReturn(imageDataMock2).when(pendingImageFetchMapMock).put(imageDataMock2.getName(), imageDataMock2);
		doNothing().when(pendingImageFetchDBMock).commit();
		
		imageStoreMock.updateImageTask(imageDataMock2);
		imageStoreMock.unlockTask(imageDataMock2.getName());
	}
	
	@Test
	public void testFinishFetch() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageTask imageDataMock = mock(ImageTask.class);
		ImageTask imageDataMock2 = mock(ImageTask.class);
		
		doNothing().when(imageDataMock).setState(ImageTaskState.ARCHIVED);
		doNothing().when(imageStoreMock).updateImageTask(imageDataMock);
		doReturn(imageDataMock).when(pendingImageFetchMapMock).remove(imageDataMock.getName(), imageDataMock);
		doNothing().when(pendingImageFetchDBMock).commit();
		
		doNothing().when(imageDataMock2).setState(ImageTaskState.ARCHIVED);
		doNothing().when(imageStoreMock).updateImageTask(imageDataMock2);
		doReturn(imageDataMock2).when(pendingImageFetchMapMock).remove(imageDataMock2.getName(), imageDataMock2);
		doNothing().when(pendingImageFetchDBMock).commit();
	}
	
	@Test
	public void testRollBackFetch() throws SQLException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageTask imageDataMock = mock(ImageTask.class);
		ImageTask imageDataMock2 = mock(ImageTask.class);
		
		doReturn(imageDataMock).when(pendingImageFetchMapMock).remove(imageDataMock.getName(), imageDataMock);
		doNothing().when(pendingImageFetchDBMock).commit();
		doNothing().when(imageDataMock).setState(ImageTaskState.FINISHED);
		doNothing().when(imageStoreMock).updateImageTask(imageDataMock);
		
		doReturn(imageDataMock2).when(pendingImageFetchMapMock).remove(imageDataMock2.getName(), imageDataMock2);
		doNothing().when(pendingImageFetchDBMock).commit();
		doNothing().when(imageDataMock2).setState(ImageTaskState.FINISHED);
		doNothing().when(imageStoreMock).updateImageTask(imageDataMock2);
	}
}
