package org.fogbowcloud.sebal.crawler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.spy;

public class TestCrawler {
	
	private Properties properties;
	private DB pendingImageDownloadDBMock;
	private ImageDataStore imageStoreMock;
	private String imageStoreIPMock = "fake-image-store-ip";
	private String imageStorePortMock = "fake-image-store-port";
	private ConcurrentMap<String, ImageData> pendingImageDownloadMapMock;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		properties = new Properties();
		imageStoreMock = mock(JDBCImageDataStore.class);
		pendingImageDownloadDBMock = mock(DB.class);
		pendingImageDownloadMapMock = mock(ConcurrentMap.class);
	}
	
	@Test
	public void testImageStoreNullProperties() {
		exception.expect(Exception.class);
		
		imageStoreMock = new JDBCImageDataStore(null, imageStoreIPMock,
				imageStorePortMock);
	}

	@Test
	public void testImageStoreNotNull() {
		imageStoreMock = new JDBCImageDataStore(properties, imageStoreIPMock,
				imageStorePortMock);
		
		Assert.assertNotNull(imageStoreMock);
	}
	
	@Test
	public void testMapDB() throws IOException {
		exception.expect(Exception.class);
		
		File fileMock = folder.newFile("dbMapMock");
		pendingImageDownloadDBMock = DBMaker.newFileDB(fileMock).make();
		pendingImageDownloadMapMock = pendingImageDownloadDBMock.createHashMap("map").make();
		
		Assert.assertNotNull(pendingImageDownloadDBMock);
		
		ImageData imageData1 = mock(ImageData.class);
		ImageData imageData2 = mock(ImageData.class);
				
		verify(pendingImageDownloadMapMock, times(4)).put("key1", imageData1);
		verify(pendingImageDownloadMapMock, times(4)).put("key2", imageData2);
	}
	
	@Test
	public void testCleanUnfinishedData() throws SQLException {
		Collection<ImageData> data = pendingImageDownloadMapMock.values();
		Assert.assertNotNull(data);
		
		List<ImageData> listMock = (List)data;		
		Assert.assertNotNull(listMock);
		
		verify(listMock.get(0), times(4)).setFederationMember(ImageDataStore.NONE);
		verify(listMock.get(0), times(4)).setState(ImageState.NOT_DOWNLOADED);
		verify(imageStoreMock, times(4)).updateImage(listMock.get(0));
		verify(pendingImageDownloadMapMock, times(4)).remove(listMock.get(0));
		
		verify(listMock.get(1), times(4)).setFederationMember(ImageDataStore.NONE);
		verify(listMock.get(1), times(4)).setState(ImageState.NOT_DOWNLOADED);
		verify(imageStoreMock, times(4)).updateImage(listMock.get(1));
		verify(pendingImageDownloadMapMock, times(4)).remove(listMock.get(1));
	}
	
	@Test
	public void testDeleteFetchedResultsFromVolume() throws SQLException, IOException {
		ImageData imageDataMock = mock(ImageData.class);
		imageDataMock.setName("image-data");
		imageDataMock.setState(ImageState.FETCHED);
		imageDataMock.setDownloadLink("fake-download-link");
		imageDataMock.setPriority(0);
		
		imageStoreMock.addImage(imageDataMock.getName(), imageDataMock.getDownloadLink(), imageDataMock.getPriority());
		
		Assert.assertEquals(imageDataMock.getState(), imageStoreMock.getImage(imageDataMock.getName()).getState());
		
		File exportFileMock = mock(File.class);
		FileUtils fileUtilsMock = mock(FileUtils.class);
		
		verify(fileUtilsMock, times(4)).deleteDirectory(exportFileMock);
	}
	
	@Test
	public void testNumberOfImagesToDownload() {
		File exportFileMock = mock(File.class);
		
		long freeSpace = doReturn((long)524288000).when(exportFileMock).getFreeSpace();
		Assert.assertNotNull(freeSpace);
		
		long numOfImagesToDownload = freeSpace / 356 * FileUtils.ONE_MB;
		Assert.assertNotNull(numOfImagesToDownload);
	}
	
	@Test
	public void testDownload() throws SQLException {
		// Preparing environment
		ImageData imageDataMock = mock(ImageData.class);
		verify(imageDataMock, times(4)).setName("image1");
		verify(imageDataMock, times(4)).setState(ImageState.NOT_DOWNLOADED);
		verify(imageStoreMock, times(4)).addImage(imageDataMock.getName(), imageDataMock.getDownloadLink(), imageDataMock.getPriority());
		
		ImageData imageDataMock2 = mock(ImageData.class);
		verify(imageDataMock2, times(4)).setName("image2");
		verify(imageDataMock2, times(4)).setState(ImageState.NOT_DOWNLOADED);
		verify(imageStoreMock, times(4)).addImage(imageDataMock2.getName(), imageDataMock2.getDownloadLink(), imageDataMock2.getPriority());
		
		// Updating data
		doReturn(true).when(imageStoreMock.lockImage(imageDataMock.getName()));		
		verify(imageDataMock, times(4)).setState(ImageState.DOWNLOADING);
		verify(imageDataMock, times(4)).setFederationMember("federation-member-1");
		verify(pendingImageDownloadMapMock, times(4)).put(imageDataMock.getName(), imageDataMock);
		verify(imageStoreMock, times(4)).updateImage(imageDataMock);
		
		doReturn(true).when(imageStoreMock.lockImage(imageDataMock2.getName()));		
		verify(imageDataMock2, times(4)).setState(ImageState.DOWNLOADING);
		verify(imageDataMock2, times(4)).setFederationMember("federation-member-2");
		verify(pendingImageDownloadMapMock, times(4)).put(imageDataMock2.getName(), imageDataMock2);
		verify(imageStoreMock, times(4)).updateImage(imageDataMock2);
		
		NASARepository nasaRepositoryMock = mock(NASARepository.class);
		
	}
	
	@Test
	public void testExec() {
		
	}

}