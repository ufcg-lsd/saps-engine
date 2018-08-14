package org.fogbowcloud.saps.engine.core.repository;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.*;
import java.util.Properties;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class TestUSGSNasaRepository {
	
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

	// we are removing searchForRegionInArea method
	@Ignore
	@Test
	public void testLatLongToWRS2Regions() throws IOException, JSONException {
//		Properties properties = new Properties();
//		properties.put("saps_export_path", "src/test/resources");
//		properties.put("max_usgs_download_link_requests", "10");
//		properties.put("max_simultaneous_download", "1");
//		properties.put("usgs_login_url", "https://ers.cr.usgs.gov/login/");
//		properties.put("usgs_json_url", "https://earthexplorer.usgs.gov/inventory/json");
//		properties.put("usgs_username", "username");
//		properties.put("usgs_password", "password");
//		properties.put("usgs_api_key_period", "300000");
//
//		USGSNasaRepository usgsNasaRepository = spy(new USGSNasaRepository(properties));
//
//		String content = "{ \"errorCode\":null, \"error\":\"\", \"data\":\"9ccf44a1c7e74d7f94769956b54cd889\", \"api_version\":\"1.0\" }";
//
//		doReturn(content).when(usgsNasaRepository).getLoginResponse();
//
//		JSONArray mockedResponse1 = new JSONArray("[{\"entityId\":\"LT52150651985144KIS00\"}]");
//		JSONArray mockedResponse2 = new JSONArray("[{\"entityId\":\"LT52150641985144KIS00\"}]");
//		JSONArray mockedResponse3 = new JSONArray("[{\"entityId\":\"LT52140651985144KIS00\"}]");
//		JSONArray mockedResponse4 = new JSONArray("[{\"entityId\":\"LT52140641985144KIS00\"}]");
//		JSONArray mockedResponse5 = new JSONArray("[{\"entityId\":\"LT52150651985144KIS00\"}, {\"entityId\":\"LT52150641985144KIS00\"}," +
//				"{\"entityId\":\"LT52140651985144KIS00\"}, {\"entityId\":\"LT52140641985144KIS00\"}]");
//
//		doReturn(mockedResponse1).when(usgsNasaRepository).searchForRegionInArea(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(),
//				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//		Set<String> regions = usgsNasaRepository.getRegionsFromArea("LANDSAT_TM_C1", 2000, 2001,
//				"-7.231189", "-36.784093","-7.231189", "-36.784093");
//		Assert.assertEquals(1, regions.size()); //region: 215, 65
//
//		doReturn(mockedResponse2).when(usgsNasaRepository).searchForRegionInArea(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(),
//				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//		regions = usgsNasaRepository.getRegionsFromArea("LANDSAT_TM_C1", 2000, 2001,
//				"-5.785213", "-36.474451","-5.785213", "-36.474451");
//		Assert.assertEquals(1, regions.size()); //region: 215, 64
//
//		doReturn(mockedResponse3).when(usgsNasaRepository).searchForRegionInArea(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(),
//				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//		regions = usgsNasaRepository.getRegionsFromArea("LANDSAT_TM_C1", 2000, 2001,
//				"-7.231189", "-35.239029","-7.231189", "-35.239029");
//		Assert.assertEquals(1, regions.size());//region: 214, 65
//
//		doReturn(mockedResponse4).when(usgsNasaRepository).searchForRegionInArea(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(),
//				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//		regions = usgsNasaRepository.getRegionsFromArea("LANDSAT_TM_C1", 2000, 2001,
//				"-5.785213", "-34.929386","-5.785213", "-34.929386");
//		Assert.assertEquals(1, regions.size());//region: 214, 64
//
//		doReturn(mockedResponse5).when(usgsNasaRepository).searchForRegionInArea(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(),
//				Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//		regions = usgsNasaRepository.getRegionsFromArea("LANDSAT_TM_C1", 2000, 2001,
//				"-5.785213", "-34.929386","-5.785213", "-34.929386");
//		Assert.assertEquals(4, regions.size());
	}
}
