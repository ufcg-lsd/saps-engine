package org.fogbowcloud.sebal.engine.sebal;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestUSGSNasaRepository {
	
	private static final String TEST_USGS_NASA_REPOSITORY_CONF_PATH = "src/test/resources/TestUSGSNasaRepository.conf";
	private static final String UTF_8 = "UTF-8";
	
	private String usgsUserName;
	private String usgsPassword;
	private String usgsLoginUrl;
	private String usgsJsonUrl;
	private String usgsAPIPeriod;
	private String sebalExportPath;
	
	@Before
	public void setUp() {
		usgsUserName = "fake-user-name";
		usgsPassword = "fake-password";
		usgsLoginUrl = "fake-login-url";
		usgsJsonUrl = "fake-json-url";
		usgsAPIPeriod = "10000";
		sebalExportPath = "/tmp";
	}
	
	@Test
	public void testGenerateAPIKeyResponse() throws ClientProtocolException, IOException {
		// set up
		HttpResponse httpResponse = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);

		String content = "{ \"errorCode\":null, \"error\":\"\", \"data\":\"9ccf44a1c7e74d7f94769956b54cd889\", \"api_version\":\"1.0\" }";

		InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
		doReturn(contentInputStream).when(httpEntity).getContent();
		doReturn(httpEntity).when(httpResponse).getEntity();
		
		BasicStatusLine basicStatus = new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_OK, "");
		doReturn(basicStatus).when(httpResponse).getStatusLine();
		doReturn(new Header[0]).when(httpResponse).getAllHeaders();

		USGSNasaRepository usgsNasaRepository = spy(new USGSNasaRepository(
				sebalExportPath, usgsLoginUrl, usgsJsonUrl, usgsUserName,
				usgsPassword, usgsAPIPeriod));

		doReturn(content).when(usgsNasaRepository).getLoginResponse();

	    // exercise
		String apiKey = usgsNasaRepository.generateAPIKey();
		
	    // expect
	    Assert.assertNotNull(apiKey);
	    Assert.assertEquals("9ccf44a1c7e74d7f94769956b54cd889", apiKey);
	}
	
	@Test
	public void testDoGetDownloadLinkLT5() throws IOException {
		// set up
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(TEST_USGS_NASA_REPOSITORY_CONF_PATH);
		properties.load(input);
		
		String imageName = "LT52160661994219";
		String stationOne = "LGN";
		String stationTwo = "CUB";
		
		List<String> possibleStations = new ArrayList<String>();
		possibleStations.add(stationOne);
		possibleStations.add(stationTwo);
				
		USGSNasaRepository usgsNasaRepository = new USGSNasaRepository(properties);
		usgsNasaRepository.setUSGSAPIKey(usgsNasaRepository.generateAPIKey());
		
		// exercise
		Map<String, String> downloadLinks = usgsNasaRepository.doGetDownloadLink(imageName, possibleStations);
		
		// expect
		Assert.assertNotNull(downloadLinks.get(imageName + stationTwo + "00"));
		Assert.assertFalse(downloadLinks.get(imageName + stationTwo + "00").isEmpty());
	}
	
	@Test
	public void testDoGetDownloadLinkLE7() throws IOException {
		// set up
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(TEST_USGS_NASA_REPOSITORY_CONF_PATH);
		properties.load(input);
		
		String imageName = "LE70160392004262";
		String stationOne = "LGN";
		String stationTwo = "CUB";
		String stationThree = "EDC";
		
		List<String> possibleStations = new ArrayList<String>();
		possibleStations.add(stationOne);
		possibleStations.add(stationTwo);
		possibleStations.add(stationThree);
				
		USGSNasaRepository usgsNasaRepository = new USGSNasaRepository(properties);
		usgsNasaRepository.setUSGSAPIKey(usgsNasaRepository.generateAPIKey());
		
		// exercise
		Map<String, String> downloadLinks = usgsNasaRepository.doGetDownloadLink(imageName, possibleStations);
		
		// expect
		Assert.assertNotNull(downloadLinks.get(imageName + stationThree + "02"));
		Assert.assertFalse(downloadLinks.get(imageName + stationThree + "02").isEmpty());
	}
}
