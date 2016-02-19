package org.fogbowcloud.scheduler.restlet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.manager.occi.model.HeaderUtils;
import org.fogbowcloud.manager.occi.model.OCCIHeaders;
import org.fogbowcloud.manager.occi.util.OCCIComputeApplication;
import org.fogbowcloud.manager.occi.util.OCCITestHelper;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TestGetRequest {

	private OCCITestHelper requestHelper;
	private String instanceLocation = HeaderUtils.X_OCCI_LOCATION_PREFIX + "http://localhost:"
			+ OCCITestHelper.ENDPOINT_PORT + OCCIComputeApplication.COMPUTE_TARGET
			+ "/b122f3ad-503c-4abb-8a55-ba8d90cfce9f";

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {
		this.requestHelper = new OCCITestHelper();

		Scheduler scheduler = Mockito.mock(Scheduler.class);
		
		Properties properties = new Properties();
		
		JDFSchedulerApplication jdfApp = spy(new JDFSchedulerApplication(scheduler, properties));
		

	}

	@Test
	public void testGetRequestContent() throws URISyntaxException, HttpException, IOException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE));
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	
	}

	@Test
	public void testGetRequest() throws URISyntaxException, HttpException, IOException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE));
		Assert.assertEquals(0, OCCITestHelper.getRequestIds(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	}

	@Test
	public void testGetRequestPlainContent() throws URISyntaxException, HttpException, IOException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE));
		Assert.assertEquals(0, OCCITestHelper.getRequestIds(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void testGetRequestWithoutContentHeader() throws URISyntaxException, HttpException, IOException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE));
		Assert.assertEquals(0, OCCITestHelper.getRequestIds(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void testGetRequestWithAcceptInvalidContent() throws URISyntaxException, HttpException, IOException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.ACCEPT, "invalid-content");
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);
		
		Assert.assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
	}

	@Test
	public void testGetRequestInvalidToken() throws URISyntaxException, HttpException, IOException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.INVALID_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
	}

	@Test
	public void testGetEmptyRequestWithAcceptHeader() throws URISyntaxException, HttpException, IOException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.ACCEPT, OCCIHeaders.TEXT_URI_LIST_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertEquals(0, OCCITestHelper.getURIList(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_URI_LIST_CONTENT_TYPE));
	}
	
	@Test
	public void testGetResquestTwoIdsDefaultAccept() throws URISyntaxException, HttpException,
			IOException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		// Get		
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		client = HttpClients.createMinimal();
		response = client.execute(get);

		//Default accept is text/plain
		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE));
		Assert.assertEquals(2, OCCITestHelper.getRequestIds(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	}

	@Test
	public void testGetResquestTwoIdsURIListAccept() throws URISyntaxException, HttpException,
			IOException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		// Get
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.ACCEPT, OCCIHeaders.TEXT_URI_LIST_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		client = HttpClients.createMinimal();
		response = client.execute(get);

		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		Assert.assertEquals(2, OCCITestHelper.getURIList(response).size());		
		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_URI_LIST_CONTENT_TYPE));
	}

	@Test
	public void testGetResquestManyIdsDefaultAccept() throws URISyntaxException, HttpException, IOException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		// Get
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		client = HttpClients.createMinimal();
		response = client.execute(get);
		//Default accept is text/plain
		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE));
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		Assert.assertEquals(30, OCCITestHelper.getRequestIds(response).size());
	}
	
	@Test
	public void testGetResquestManyIdsURIListAccept() throws URISyntaxException, HttpException, IOException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		// Get
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.ACCEPT, OCCIHeaders.TEXT_URI_LIST_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		client = HttpClients.createMinimal();
		response = client.execute(get);

		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		Assert.assertEquals(50, OCCITestHelper.getURIList(response).size());
		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_URI_LIST_CONTENT_TYPE));
	}

	@Test
	public void testGetSpecificRequest() throws URISyntaxException, ParseException, IOException,
			HttpException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		// Get
		HttpGet get = new HttpGet(OCCITestHelper.getRequestIdsPerLocationHeader(response).get(0));
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		client = HttpClients.createMinimal();
		response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader(OCCIHeaders.CONTENT_TYPE).getValue()
				.startsWith(OCCIHeaders.TEXT_PLAIN_CONTENT_TYPE));
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	}

	
	@Test
	public void testGetSpecificRequestWithMethodNotAllowed() throws URISyntaxException, ParseException, IOException,
			HttpException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		// Get
		HttpGet get = new HttpGet(OCCITestHelper.getRequestIdsPerLocationHeader(response).get(0));
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.ACCEPT, "invalid-accept");
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		client = HttpClients.createMinimal();
		response = client.execute(get);

		Assert.assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void testGetRequestNotFound() throws URISyntaxException, ParseException, IOException,
			HttpException {
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST + "not_found");
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
	}

	@Test
	public void testGetStatusRequest() throws URISyntaxException, ParseException, IOException,
			HttpException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		post.addHeader(OCCIHeaders.X_LOCAL_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		// Get
		HttpGet get = new HttpGet(OCCITestHelper.getRequestIdsPerLocationHeader(response).get(0));
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		client = HttpClients.createMinimal();
		response = client.execute(get);

		String requestDetails = EntityUtils.toString(response.getEntity(),
				String.valueOf(Charsets.UTF_8));
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	}

	@Test
	public void testGetResquestFilterWithAttribute() throws URISyntaxException, HttpException,
			IOException {
		// Post
		HttpPost post = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		post.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		post.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
				
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(post);
		EntityUtils.consume(response.getEntity());

		// Get		
		HttpGet get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		response = client.execute(get);

		Assert.assertEquals(2, OCCITestHelper.getRequestIds(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// Post
		HttpPost postTwo = new HttpPost(OCCITestHelper.URI_FOGBOW_REQUEST);
		postTwo.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		postTwo.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
				
		client = HttpClients.createMinimal();
		response = client.execute(postTwo);
		
		// Get
		get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		get.addHeader(OCCIHeaders.X_OCCI_ATTRIBUTE, "org.fogbowcloud.request.type=\"one-time\"");
		client = HttpClients.createMinimal();
		response = client.execute(get);

		Assert.assertEquals(2, OCCITestHelper.getRequestIds(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());	
		
		// Get 
		get = new HttpGet(OCCITestHelper.URI_FOGBOW_REQUEST);
		get.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		get.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, OCCITestHelper.FED_ACCESS_TOKEN);
		get.addHeader(OCCIHeaders.X_OCCI_ATTRIBUTE, "org.fogbowcloud.request.type=\"notfound\"");
		client = HttpClients.createMinimal();
		response = client.execute(get);
		
		Assert.assertEquals(0, OCCITestHelper.getRequestIds(response).size());
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());	
	}

	@After
	public void tearDown() throws Exception {
		this.requestHelper.stopComponent();
	}

}
