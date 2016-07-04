package org.fogbowcloud.sebal.crawler;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mockito.Mockito;

public class TestCrawler {
	
	private Crawler crawler;
	private Properties properties;
	private DB pendingImageDownloadDBMock;
	private ImageDataStore imageStoreMock;
	private String imageStoreIPMock = "fake-image-store-ip";
	private String imageStorePortMock = "fake-image-store-port";
	private String fakeFederationMember = "fake-federation-member";
	private HTreeMap<String, ImageData> pendingImageDownloadMapMock;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		properties = new Properties();
		properties.setProperty("sebal_export_path", "export-path-mock");
		properties.setProperty("nasa_login_url", "nasa-url-mock");
		properties.setProperty("nasa_username", "nasa-username-mock");
		properties.setProperty("nasa_password", "nasa-password-mock");
		pendingImageDownloadDBMock = mock(DB.class);
		pendingImageDownloadMapMock = mock(HTreeMap.class);		
		crawler = spy(new Crawler(properties, imageStoreIPMock, imageStorePortMock, fakeFederationMember));
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
	public void testImageStoreSQLException() {
		imageStoreMock = new JDBCImageDataStore(properties, "diff-imagestore-ip",
				imageStorePortMock);
		
		try {
			List<ImageData> fakeList = imageStoreMock.getImagesToDownload(
					"diff-federation-member", 10);
			fail("Imagestore IP does not match");
		} catch (SQLException expectedException) {
			// do nothing
		}
	}
	
	@Test
	public void testMapDB() {
		exception.expect(Exception.class);
		
		ImageData imageData1 = mock(ImageData.class);
		ImageData imageData2 = mock(ImageData.class);
		
		doReturn(imageData1).when(pendingImageDownloadMapMock).put(eq("key1"), eq(imageData1));
		doReturn(imageData2).when(pendingImageDownloadMapMock).put(eq("key2"), eq(imageData2));
				
		verify(pendingImageDownloadMapMock, times(4)).put(eq("key1"), imageData1);
		doNothing().when(pendingImageDownloadDBMock).commit();
		verify(pendingImageDownloadMapMock, times(4)).put(eq("key2"), imageData2);
		doNothing().when(pendingImageDownloadDBMock).commit();
	}
	
	@Test
	public void testMapDBNull() {
		try {
			pendingImageDownloadMapMock = null;
			pendingImageDownloadMapMock.values();

			fail("Pending image map null");
		} catch (NullPointerException expectedNull) {
			// do nothing
		}
	}
	
	@Test
	public void testMapDBWrongKey() {
		ImageData imageDataMock = mock(ImageData.class);
		pendingImageDownloadMapMock.put("key1", imageDataMock);
		Object fakeVailure = 200;
		
		try {
			pendingImageDownloadMapMock.get((String)fakeVailure);

			fail("Failure due to wrong key");
		} catch (ClassCastException expectedClassCast) {
			// do nothing
		}
	}
	
	@Test
	public void testCleanUnfinishedData() throws SQLException, IOException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageData imageData1 = mock(ImageData.class);
		ImageData imageData2 = mock(ImageData.class);
		
		doNothing().when(pendingImageDownloadMapMock).put(eq("image-data-1"), eq(imageData1));
		doNothing().when(pendingImageDownloadMapMock).put(eq("image-data-2"), eq(imageData2));

		imageData1.setFederationMember(ImageDataStore.NONE);
		imageData1.setState(ImageState.NOT_DOWNLOADED);
		imageStoreMock.updateImage(imageData1);
		doReturn(imageData1).when(pendingImageDownloadMapMock).remove(imageData1);
		verify(pendingImageDownloadMapMock, times(4)).remove(imageData1);
		doNothing().when(pendingImageDownloadDBMock).commit();
		
		imageData2.setFederationMember(ImageDataStore.NONE);
		imageData2.setState(ImageState.NOT_DOWNLOADED);
		imageStoreMock.updateImage(imageData2);
		doReturn(imageData2).when(pendingImageDownloadMapMock).remove(imageData2);
		verify(pendingImageDownloadMapMock, times(4)).remove(imageData2);
	}
	
/*	@Test(expected=IOException.class)
	public void testDeleteFromDiskIOException() throws IOException {
		ImageData imageDataMock = mock(ImageData.class);
		crawler.deleteImageFromDisk(imageDataMock, "");
	}*/
	
	@Test
	public void testDeleteFetchedResultsFromVolume() throws SQLException, IOException {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		ImageData imageDataMock = mock(ImageData.class);
		
		doNothing().when(imageStoreMock).addImage(eq(imageDataMock.getName()), eq(imageDataMock.getDownloadLink()), eq(imageDataMock.getPriority()));
		verify(imageStoreMock, times(1)).addImage(eq(imageDataMock.getName()), eq(imageDataMock.getDownloadLink()), eq(imageDataMock.getPriority()));
		
		File exportFileMock = folder.newFile("export-path-mock");
		FileUtils fileUtilsMock = mock(FileUtils.class);
		
		verify(fileUtilsMock, times(4)).deleteDirectory(exportFileMock);
	}
	
	@Test
	public void testNumberOfImagesToDownload() throws NumberFormatException, InterruptedException, IOException, SQLException {
		String exportPath = "export-path-mock";
		File exportFileMock = mock(File.class);
		
		Mockito.when(exportFileMock.exists()).thenReturn(true);
		Mockito.when(exportFileMock.isDirectory()).thenReturn(true);
		
		Mockito.when(exportFileMock.getFreeSpace()).thenReturn((long)524288000);
		Mockito.when(crawler.getExportDirPath(exportPath)).thenReturn(exportFileMock);
		
		doReturn((long)1).when(crawler).numberOfImagesToDownload();
	}
	
	@Test
	public void testDownload() throws Exception {
		exception.expect(Exception.class);
		
		imageStoreMock = mock(JDBCImageDataStore.class);
		
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		doNothing().when(imageStoreMock).addImage(eq(imageDataMock.getName()), eq(imageDataMock.getDownloadLink()), eq(imageDataMock.getPriority()));
		verify(imageStoreMock, times(4)).addImage(eq(imageDataMock.getName()), eq(imageDataMock.getDownloadLink()), eq(imageDataMock.getPriority()));
		
		doNothing().when(imageStoreMock).addImage(eq(imageDataMock2.getName()), eq(imageDataMock2.getDownloadLink()), eq(imageDataMock2.getPriority()));
		verify(imageStoreMock, times(4)).addImage(eq(imageDataMock2.getName()), eq(imageDataMock2.getDownloadLink()), eq(imageDataMock2.getPriority()));
		
		doNothing().when(crawler).download(2);
		verify(crawler).download(2);
	}

}