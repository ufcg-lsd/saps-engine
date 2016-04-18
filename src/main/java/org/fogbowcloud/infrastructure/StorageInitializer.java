package org.fogbowcloud.infrastructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.occi.model.HeaderUtils;
import org.fogbowcloud.manager.occi.model.OCCIHeaders;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.storage.StorageAttribute;

public class StorageInitializer {
	
	protected static final String LOCAL_TOKEN_HEADER = "local_token";
	protected static final String PLUGIN_PACKAGE = "org.fogbowcloud.manager.core.plugins.identity";
	protected static final String DEFAULT_URL = "http://localhost:8182";
	protected static final int DEFAULT_INTANCE_COUNT = 1;
	protected static final String DEFAULT_TYPE = OrderConstants.DEFAULT_TYPE;
	protected static final String DEFAULT_IMAGE = "fogbow-linux-x86";

	private static HttpClient client;
	
	private static final Logger LOGGER = Logger.getLogger(StorageInitializer.class);
	
	public void init() throws Exception {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream("src/main/resources/");
		properties.load(input);
		orderStorage(properties);
	}
	
	private void orderStorage(Properties properties) throws Exception {
		LOGGER.debug("Creating storage order...");
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Category", OrderConstants.TERM
				+ "; scheme=\"" + OrderConstants.SCHEME + "\"; class=\""
				+ OrderConstants.KIND_CLASS + "\""));
		headers.add(new BasicHeader("X-OCCI-Attribute",
				OrderAttribute.INSTANCE_ID.getValue() + "="
						+ OrderConstants.DEVICE_ID_DEFAULT));
		headers.add(new BasicHeader("X-OCCI-Attribute",
				OrderAttribute.INSTANCE_COUNT.getValue() + "=" + 1));
		headers.add(new BasicHeader("X-OCCI-Attribute", OrderAttribute.TYPE
				.getValue() + "=" + OrderConstants.DEFAULT_TYPE));

		//TODO: insert correct size
		headers.add(new BasicHeader("X-OCCI-Attribute",
				OrderAttribute.STORAGE_SIZE.getValue() + "=" + 1024));
		
		String url = System.getenv("FOGBOW_URL") == null ? DEFAULT_URL : System
				.getenv("FOGBOW_URL");
		
		String authToken = normalizeTokenFile(properties.getProperty("infra_fogbow_token_public_key_filepath"));
		if (authToken == null) {
			authToken = normalizeToken(properties.getProperty("infra_fogbow_token_public_key_filepath"));
		}
		
		doRequest("get", url + "/" + OrderConstants.TERM + "/" + OrderConstants.DEVICE_ID_DEFAULT, authToken);
		
		LOGGER.debug("Attaching storage to instance...");
		attachStorage(properties);
		
		LOGGER.debug("Process finished.");
	}
	
	private void attachStorage(Properties properties) throws URISyntaxException, HttpException,
			IOException {
		String url = "attachment_url";

		String authToken = normalizeTokenFile(properties.getProperty("infra_fogbow_token_public_key_filepath"));

		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Category", OrderConstants.STORAGELINK_TERM
				+ "; scheme=\"" + OrderConstants.INFRASTRUCTURE_OCCI_SCHEME
				+ "\"; class=\"" + OrderConstants.KIND_CLASS + "\""));
		// TODO: insert correct computeId and mountPoint
		headers.add(new BasicHeader("X-OCCI-Attribute", StorageAttribute.SOURCE
				.getValue() + "=" + "computeId"));
		headers.add(new BasicHeader("X-OCCI-Attribute", StorageAttribute.TARGET
				.getValue() + "=" + StorageAttribute.DEVICE_ID));
		headers.add(new BasicHeader("X-OCCI-Attribute",
				StorageAttribute.DEVICE_ID.getValue() + "=" + "mountPoint"));

		doRequest("post", url + "/" + OrderConstants.STORAGE_TERM + "/"
				+ OrderConstants.STORAGE_LINK_TERM + "/", authToken, headers);
	}
	
	protected static String normalizeToken(String token) {
		if (token == null) {
			return null;
		}				
		return token.replace("\n", "");
	}
	
	protected static String normalizeTokenFile(String token) {
		if (token == null) {
			return null;
		}		
		File tokenFile = new File(token);
		if (tokenFile.exists()) {
			try {
				token = IOUtils.toString(new FileInputStream(tokenFile));
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}		
		return token.replace("\n", "");
	}	

	private static void doRequest(String method, String endpoint,
			String authToken) throws URISyntaxException, HttpException,
			IOException {
		doRequest(method, endpoint, authToken, new LinkedList<Header>());
	}

	private static void doRequest(String method, String endpoint, String authToken, 
			List<Header> additionalHeaders) throws URISyntaxException, HttpException, IOException {
		HttpUriRequest request = null;
		if (method.equals("get")) {
			request = new HttpGet(endpoint);
		} else if (method.equals("delete")) {
			request = new HttpDelete(endpoint);
		} else if (method.equals("post")) {
			request = new HttpPost(endpoint);
		}
		request.addHeader(OCCIHeaders.CONTENT_TYPE, OCCIHeaders.OCCI_CONTENT_TYPE);
		if (authToken != null) {
			request.addHeader(OCCIHeaders.X_AUTH_TOKEN, authToken);
		}
		for (Header header : additionalHeaders) {
			request.addHeader(header);
		}
		
		if (client == null) {
			client = new DefaultHttpClient();
			HttpParams params = new BasicHttpParams();
			params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			client = new DefaultHttpClient(new ThreadSafeClientConnManager(params, client
					.getConnectionManager().getSchemeRegistry()), params);
		}

		HttpResponse response = client.execute(request);

		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
				|| response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
			Header locationHeader = getLocationHeader(response.getAllHeaders());
			if (locationHeader != null && locationHeader.getValue().contains(OrderConstants.TERM)) {
				System.out.println(generateLocationHeaderResponse(locationHeader));
			} else {
				System.out.println(EntityUtils.toString(response.getEntity()));
			}
		} else {
			System.out.println(response.getStatusLine().toString());
		}
	}
	
	protected static Header getLocationHeader(Header[] headers) {
		Header locationHeader = null;
		for (Header header : headers) {	
			if (header.getName().equals("Location")) {
				locationHeader = header;
			}
		}
		return locationHeader;
	}
	
	protected static String generateLocationHeaderResponse(Header header) {
		String[] locations = header.getValue().split(",");
		String response = "";
		for (String location : locations) {
			response += HeaderUtils.X_OCCI_LOCATION_PREFIX + location + "\n";
		}
		return response.trim();
	}

	public static void setClient(HttpClient client) {
		StorageInitializer.client = client;
	}
	
}