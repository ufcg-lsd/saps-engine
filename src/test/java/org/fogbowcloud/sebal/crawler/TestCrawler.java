package org.fogbowcloud.sebal.crawler;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

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
import org.mapdb.HTreeMap;
import org.mockito.Mockito;

public class TestCrawler {
	
	private Crawler crawler;
	private Properties properties;
	private DB pendingImageDownloadDBMock;
	private ImageDataStore imageStoreMock;
	private String imageStoreIPMock = "fake-image-store-ip";
	private String imageStorePortMock = "fake-image-store-port";
	private HTreeMap<String, ImageData> pendingImageDownloadMapMock;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		properties = new Properties();
		properties.setProperty("sebal_export_path", "export-path-mock");
		properties.setProperty("nasa_login_url", "nasa-url-mock");
		properties.setProperty("nasa_username", "nasa-username-mock");
		properties.setProperty("nasa_password", "nasa-password-mock");
		crawler = spy(new Crawler(properties, imageStoreIPMock, imageStorePortMock));
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
	public void testMapDB() throws IOException {
		exception.expect(Exception.class);
		
		File fileMock = folder.newFile("dbMapMock");
		pendingImageDownloadDBMock = DBMaker.newFileDB(fileMock).make();
		pendingImageDownloadMapMock = pendingImageDownloadDBMock.createHashMap("map").make();
		
		Assert.assertNotNull(pendingImageDownloadDBMock);
		
		ImageData imageData1 = mock(ImageData.class);
		ImageData imageData2 = mock(ImageData.class);
		
		doReturn(imageData1).when(pendingImageDownloadMapMock).put(eq("key1"), eq(imageData1));
		doReturn(imageData2).when(pendingImageDownloadMapMock).put(eq("key2"), eq(imageData2));
				
		verify(pendingImageDownloadMapMock, times(4)).put(eq("key1"), imageData1);
		verify(pendingImageDownloadMapMock, times(4)).put(eq("key2"), imageData2);
	}
	
	@Test
	public void testCleanUnfinishedData() throws SQLException {
		exception.expect(Exception.class);
		
		ImageData imageData1 = mock(ImageData.class);
		ImageData imageData2 = mock(ImageData.class);
		
		doNothing().when(pendingImageDownloadMapMock).put("image-data-1", imageData1);
		doNothing().when(pendingImageDownloadMapMock).put("image-data-2", imageData2);

		imageData1.setFederationMember(ImageDataStore.NONE);
		imageData1.setState(ImageState.NOT_DOWNLOADED);
		imageStoreMock.updateImage(imageData1);
		verify(pendingImageDownloadMapMock, times(4)).remove(imageData1);
		
		imageData2.setFederationMember(ImageDataStore.NONE);
		imageData2.setState(ImageState.NOT_DOWNLOADED);
		imageStoreMock.updateImage(imageData2);
		verify(pendingImageDownloadMapMock, times(4)).remove(imageData2);
	}
	
	@Test
	public void testDeleteFetchedResultsFromVolume() throws SQLException, IOException {
		ImageData imageDataMock = mock(ImageData.class);
		imageDataMock.setName("image-data");
		imageDataMock.setState(ImageState.FETCHED);
		imageDataMock.setDownloadLink("fake-download-link");
		imageDataMock.setPriority(0);
		
		imageStoreMock.addImage(imageDataMock.getName(), imageDataMock.getDownloadLink(), imageDataMock.getPriority());
		
		File exportFileMock = mock(File.class);
		FileUtils fileUtilsMock = mock(FileUtils.class);
		
		verify(fileUtilsMock, times(4)).deleteDirectory(exportFileMock);
	}
	
	@Test
	public void testNumberOfImagesToDownload() throws IOException {
		String exportPath = "export-path-mock";
		File exportFileMock = mock(File.class);
		
		doReturn(true).when(exportFileMock).exists();
		doReturn(true).when(exportFileMock).isDirectory();
		
		doReturn((long)524288000).when(exportFileMock).getFreeSpace();
		doReturn(exportFileMock).when(crawler).getExportDirPath(exportPath);
		
		crawler.numberOfImagesToDownload();
	}
	
	@Test
	public void testDownload() throws Exception {
		NASARepository nasaRepositoryMock = mock(NASARepository.class);

		// Preparing environment
		ImageData imageDataMock = mock(ImageData.class);
		doNothing().when(imageDataMock).setName("image1");
		doNothing().when(imageDataMock).setState(ImageState.NOT_DOWNLOADED);
		verify(imageStoreMock, times(4)).addImage(imageDataMock.getName(), imageDataMock.getDownloadLink(), imageDataMock.getPriority());
		
		ImageData imageDataMock2 = mock(ImageData.class);
		doNothing().when(imageDataMock2).setName("image2");
		doNothing().when(imageDataMock2).setState(ImageState.NOT_DOWNLOADED);
		verify(imageStoreMock, times(4)).addImage(imageDataMock2.getName(), imageDataMock2.getDownloadLink(), imageDataMock2.getPriority());
		
		// Updating and downloading data test
		Mockito.when(imageStoreMock.lockImage(imageDataMock.getName())).thenReturn(true);
		doNothing().when(imageDataMock).setState(ImageState.DOWNLOADING);
		doNothing().when(imageDataMock).setFederationMember("federation-member-1");
		pendingImageDownloadMapMock.put(imageDataMock.getName(), imageDataMock);
		verify(pendingImageDownloadDBMock, times(4)).commit();
		doNothing().when(imageStoreMock).updateImage(imageDataMock);
		doNothing().when(nasaRepositoryMock).downloadImage(imageDataMock);		
		
		Mockito.when(imageStoreMock.lockImage(imageDataMock2.getName())).thenReturn(true);
		doNothing().when(imageDataMock2).setState(ImageState.DOWNLOADING);
		doNothing().when(imageDataMock2).setFederationMember("federation-member-2");
		pendingImageDownloadMapMock.put(imageDataMock2.getName(), imageDataMock2);
		verify(pendingImageDownloadDBMock, times(4)).commit();
		doNothing().when(imageStoreMock).updateImage(imageDataMock2);
		doNothing().when(nasaRepositoryMock).downloadImage(imageDataMock2);
	}

}