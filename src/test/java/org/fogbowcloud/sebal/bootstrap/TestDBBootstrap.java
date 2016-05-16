package org.fogbowcloud.sebal.bootstrap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestDBBootstrap {
	
	
	private String imageStoreIPMock = "fake-store-IP";
	private String imageStorePortMock = "fake-store-Port";
	private Properties properties;

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() {
		properties = new Properties();
	}

	@Test
	public void testDBBootstrapNullProperties() {
		exception.expect(Exception.class);
		
		DBBootstrap dbBootstrap = new DBBootstrap(null, imageStoreIPMock, imageStorePortMock);
	}

	@Test
	public void testCreateDBBootstrap() {
		DBBootstrap dbBootstrap = new DBBootstrap(properties, imageStoreIPMock, imageStorePortMock);
		
		Assert.assertNotNull(dbBootstrap.getImageStore());
		Assert.assertNotNull(dbBootstrap.getNasaRepository());
	}
	
	@Test
	public void testFillDB() throws ClientProtocolException, UnsupportedEncodingException, IOException, SQLException {
		DBBootstrap dbBootstrap = spy(new DBBootstrap(properties, imageStoreIPMock, imageStorePortMock));
		String firstYearMock = "1600";
		String lastYearMock = "1601";
		String regionsPathMock = "fake-regions-path";
		String regionMock = "fake-region";
		String imageNameMock = "fake-image-name";
		NASARepository fakeRep = mock(NASARepository.class);
		
		JDBCImageDataStore fakeImageStore = mock(JDBCImageDataStore.class);
		
		int yearMock = 1600;
		List<String> fakeRegionsMock = new ArrayList<String>();
		Map<String, String> imagesDownloadLinksMock = new HashMap<String, String> ();
		fakeRegionsMock.add("21BA5");
		fakeRegionsMock.add("21BA6");
		String fakeImageName1 = "key1";
		String fakeImageLink1 = "value1";
		imagesDownloadLinksMock.put(fakeImageName1, fakeImageLink1);
		String fakeImageName2 = "key2";
		String fakeImageLink2 = "value2";
		imagesDownloadLinksMock.put(fakeImageName2, fakeImageLink2);
		doNothing().when(fakeImageStore).addImage(eq(fakeImageName1), eq(fakeImageLink1), anyInt());
		doNothing().when(fakeImageStore).addImage(eq(fakeImageName2), eq(fakeImageLink2), anyInt());
		doReturn(imageNameMock).when(dbBootstrap).createImageList(regionMock, yearMock);
		doReturn(fakeImageStore).when(dbBootstrap).getImageStore();
		doReturn(fakeRep).when(dbBootstrap).getNasaRepository();
		doReturn(fakeRegionsMock).when(dbBootstrap).getRegions(regionsPathMock);
		doReturn(imagesDownloadLinksMock).when(fakeRep).checkExistingImages(any(File.class));
		
		dbBootstrap.fillDB(firstYearMock, lastYearMock, regionsPathMock);
		verify(fakeImageStore, times(4)).addImage(eq(fakeImageName1), eq(fakeImageLink1), anyInt());
		verify(fakeImageStore, times(4)).addImage(eq(fakeImageName2), eq(fakeImageLink2), anyInt());
		
	}

}
