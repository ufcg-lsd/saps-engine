package org.fogbowcloud.sebal.fetcher;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mapdb.DB;

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
	private ImageDataStore imageStore;
	private String ftpServerIPMock = "fake-store-IP";
	private String ftpServerPortMock = "fake-store-Port";
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
	public void testExec() {
		
	}
	
	@Test
	public void testCleanUnfinishedFetchedData() {
		ImageData imageDataMock = mock(ImageData.class);
		ImageData imageDataMock2 = mock(ImageData.class);
		
		pendingImageFetchMapMock.put("key1", imageDataMock);
		pendingImageFetchMapMock.put("key2", imageDataMock2);
		
		verify(pendingImageFetchMapMock.remove("key1"));
		verify(pendingImageFetchMapMock.remove("key2"));
	}

}
