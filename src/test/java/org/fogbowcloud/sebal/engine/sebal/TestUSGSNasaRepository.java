package org.fogbowcloud.sebal.engine.sebal;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
	
	// TODO: Update this test
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

		// FIXME
		//doReturn(httpResponse).when(usgsNasaRepository).getLoginResponse();

	    // exercise
		String apiKey = usgsNasaRepository.generateAPIKey();
		
	    // expect
	    Assert.assertNotNull(apiKey);
	    Assert.assertEquals("9ccf44a1c7e74d7f94769956b54cd889", apiKey);
	}
	
	// TODO: Update this test
	@Test
	public void testGetDownloadLinkResponse() throws ClientProtocolException,
			IOException, InterruptedException, JSONException {
		// set up
		String dataset = "fake-data-set";
		String sceneId = "fake-scene-id";
		String node = "fake-node";
		String product = "fake-product";

		HttpResponse httpResponse = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);

		String returnedDownloadLink = "http:\\/\\/fake-download-link.com\\/";

		String content = "{\"errorCode\":null,\"error\":\"\",\"data\":[\""
				+ returnedDownloadLink
				+ "\"],\"api_version\":\"1.2.1\",\"executionTime\":1.6076831817627}";

		InputStream contentInputStream = new ByteArrayInputStream(
				content.getBytes(UTF_8));
		doReturn(contentInputStream).when(httpEntity).getContent();
		doReturn(httpEntity).when(httpResponse).getEntity();

		BasicStatusLine basicStatus = new BasicStatusLine(new ProtocolVersion(
				"", 0, 0), HttpStatus.SC_OK, "");
		doReturn(basicStatus).when(httpResponse).getStatusLine();
		doReturn(new Header[0]).when(httpResponse).getAllHeaders();

		USGSNasaRepository usgsNasaRepository = spy(new USGSNasaRepository(
				sebalExportPath, usgsLoginUrl, usgsJsonUrl, usgsUserName,
				usgsPassword, usgsAPIPeriod));

		// FIXME
		//doReturn(httpResponse).when(usgsNasaRepository)
		//		.getDownloadResponse(dataset, sceneId, node, product);

		// exercise
		String formatedDownloadLink = usgsNasaRepository.usgsDownloadURL(
				dataset, sceneId, node, product);

		// expect
		Assert.assertNotNull(formatedDownloadLink);
		Assert.assertEquals("http://fake-download-link.com/",
				formatedDownloadLink);
	}
}
