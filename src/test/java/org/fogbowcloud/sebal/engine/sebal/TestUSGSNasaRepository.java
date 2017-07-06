package org.fogbowcloud.sebal.engine.sebal;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
	public void testDoGetDownloadLink() throws IOException {
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
	public void testUsgsDownloadURL() {
		// set up
		String imageName = "LT52160661994219";
		String stationOne = "LGN";
		String stationTwo = "CUB";
		
		String dataset = "LT5";
		String node = "EE";
		String product = "STANDARD";
		
		String responseOne = "{\"errorCode\":null,\"error\":\"\",\"data\":[],\"api_version\":\"1.2.1\",\"executionTime\":1.0967540740967}";
		String responseTwo = "{\"errorCode\":null,\"error\":\"\",\"data\":[\"https:\\/\\/dds.cr.usgs.gov"
				+ "\\/ltaauth\\/\\/hsm\\/lsat1\\/collection01\\/tm\\/T2\\/1994\\/216\\/66\\/LT05_L1GS_216066_19940807_20170112_01_T2.tar.gz?"
				+ "id=mf9l59vt8aq6kh2f01692vpk00&iid=LT52160661994219CUB00&did=318063661&ver=production\"],\"api_version\":\"1.2.1\","
				+ "\"executionTime\":1.1487548351288}";
		
		String expectedDownloadLink = "[\"https://dds.cr.usgs.gov/ltaauth//hsm/lsat1/collection01/tm/T2/1994/216/66/LT05_L1GS_216066_19940807_"
				+ "20170112_01_T2.tar.gz?id=mf9l59vt8aq6kh2f01692vpk00&iid=LT52160661994219CUB00&did=318063661&ver=production\"]";
		
		USGSNasaRepository usgsNasaRepository = spy(new USGSNasaRepository(
				sebalExportPath, usgsLoginUrl, usgsJsonUrl, usgsUserName,
				usgsPassword, usgsAPIPeriod));
		usgsNasaRepository.setUSGSAPIKey("fake-usgs-api-key");
		doReturn(responseOne).when(usgsNasaRepository).getDownloadHttpResponse(dataset, imageName + stationOne + "00", node, product);
		doReturn(responseTwo).when(usgsNasaRepository).getDownloadHttpResponse(dataset, imageName + stationTwo + "00", node, product);
		
		// exercise
		String downloadLinkOne = usgsNasaRepository.usgsDownloadURL(dataset, imageName + stationOne + "00", node, product);
		String downloadLinkTwo = usgsNasaRepository.usgsDownloadURL(dataset, imageName + stationTwo + "00", node, product);
		
		// expect
		Assert.assertEquals(null, downloadLinkOne);
		Assert.assertEquals(expectedDownloadLink, downloadLinkTwo);
	}
	
	@Test
	public void testNewSceneIdGet() {
		// set up
		String imageName = "LT52160661994219";
		String station = "CUB";
		
		String dataset = "LT5";
		String node = "EE";
		String product = "STANDARD";
		
		String response = "{\"errorCode\":null,\"error\":\"\",\"data\":[{\"acquisitionDate\":\"1994-08-07\""
				+ ",\"startTime\":\"1994-08-07\",\"endTime\":\"1994-08-07\",\"lowerLeftCoordinate\":"
				+ "{\"latitude\":-9.34939,\"longitude\":-39.63392},\"upperLeftCoordinate\":"
				+ "{\"latitude\":-7.75891,\"longitude\":-39.28683},\"upperRightCoordinate\":"
				+ "{\"latitude\":-8.00032,\"longitude\":-37.62675},\"lowerRightCoordinate\":"
				+ "{\"latitude\":-9.59178,\"longitude\":-37.96703},\"sceneBounds\":"
				+ "\"-39.63392,-9.59178,-37.62675,-7.75891\",\"browseUrl\":"
				+ "\"https:\\/\\/earthexplorer.usgs.gov\\/browse\\/tm\\/216\\/66\\/1994\\/LT05_L1GS_216066_19940807_20170112_01_T2_REFL.jpg\","
				+ "\"dataAccessUrl\":\"https:\\/\\/earthexplorer.usgs.gov\\/order"
				+ "\\/process?dataset_name=LANDSAT_TM_C1&ordered=LT52160661994219CUB00&node=INVSVC\",\"downloadUrl\":\"https:\\/\\/earthexplorer.usgs.gov"
				+ "\\/download\\/external\\/options\\/LANDSAT_TM_C1\\/LT52160661994219CUB00\\/INVSVC\\/\",\"entityId\":\"LT52160661994219CUB00\","
				+ "\"displayId\":\"LT05_L1GS_216066_19940807_20170112_01_T2\",\"metadataUrl\":\"https:\\/\\/earthexplorer.usgs.gov\\/metadata\\/xml"
				+ "\\/12266\\/LT52160661994219CUB00\\/\",\"fgdcMetadataUrl\":\"https:\\/\\/earthexplorer.usgs.gov\\/fgdc\\/12266\\/LT52160661994219CUB00"
				+ "\\/save_xml\",\"modifiedDate\":\"2017-01-14\",\"orderUrl\":\"https:\\/\\/earthexplorer.usgs.gov\\/order"
				+ "\\/process?dataset_name=LANDSAT_TM_C1&ordered=LT52160661994219CUB00&node=INVSVC\",\"bulkOrdered\":false,\"ordered\":false,\"summary\":"
				+ "\"Entity ID: LT05_L1GS_216066_19940807_20170112_01_T2, Acquisition Date: 07-AUG-94, Path: 216, Row: 66\"}],\"api_version\":"
				+ "\"1.2.1\",\"executionTime\":1.3724570274353}";

		String expectedNewSceneId = "LT05_L1GS_216066_19940807_20170112_01_T2";
		
		USGSNasaRepository usgsNasaRepository = spy(new USGSNasaRepository(
				sebalExportPath, usgsLoginUrl, usgsJsonUrl, usgsUserName,
				usgsPassword, usgsAPIPeriod));
		usgsNasaRepository.setUSGSAPIKey("fake-usgs-api-key");
		doReturn(response).when(usgsNasaRepository).getMetadataHttpResponse(dataset, imageName + station + "00", node, product);

		// exercise
		String newSceneId = usgsNasaRepository.getCollectionOneSceneId(dataset, imageName + station + "00", node, product);

		// expect
		Assert.assertEquals(expectedNewSceneId, newSceneId);
	}
}
