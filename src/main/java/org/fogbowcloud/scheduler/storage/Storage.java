package org.fogbowcloud.scheduler.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
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
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.occi.model.HeaderUtils;
import org.fogbowcloud.manager.occi.model.OCCIHeaders;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public class Storage {
	
	protected static final String LOCAL_TOKEN_HEADER = "local_token";
	protected static final String PLUGIN_PACKAGE = "org.fogbowcloud.manager.core.plugins.identity";
	protected static final String DEFAULT_URL = "http://localhost:8182";
	protected static final int DEFAULT_INTANCE_COUNT = 1;
	protected static final String DEFAULT_TYPE = OrderConstants.DEFAULT_TYPE;
	protected static final String DEFAULT_IMAGE = "fogbow-linux-x86";

	private static HttpClient client;
	private static IdentityPlugin identityPlugin;
	
	private static final Logger LOGGER = Logger.getLogger(Storage.class);
	
	private void orderStorage() throws Exception {
		JCommander jc = new JCommander();
		OrderCommand order = new OrderCommand();
		jc.addCommand("order", order);
		StorageCommand storage = new StorageCommand();
		jc.addCommand("storage", storage);
		AttachmentCommand attachment = new AttachmentCommand();
        jc.addCommand("attachment", attachment);
        
        String parsedCommand = jc.getParsedCommand();
        
		if (parsedCommand.equals("order")) {
			List<Header> headers = new LinkedList<Header>();
			headers.add(new BasicHeader("Category", OrderConstants.TERM
					+ "; scheme=\"" + OrderConstants.SCHEME + "\"; class=\""
					+ OrderConstants.KIND_CLASS + "\""));
			headers.add(new BasicHeader("X-OCCI-Attribute",
					OrderAttribute.INSTANCE_COUNT.getValue() + "="
							+ order.instanceCount));
			headers.add(new BasicHeader("X-OCCI-Attribute", OrderAttribute.TYPE
					.getValue() + "=" + order.type));
			
			if (order.create) {
				if (order.resourceKind != null && order.resourceKind.equals(OrderConstants.STORAGE_TERM)) {
					if (order.size != null) {
						headers.add(new BasicHeader("X-OCCI-Attribute", 
								OrderAttribute.STORAGE_SIZE.getValue() + "=" + order.size));						
					} else {
						System.out.println("Size is required when resoure kind is storage");
						return;
					}
				} else {
					System.out.println("Resource Storage is required. Types allowed : compute, storage");
					return;
				}
			}
		} else if (parsedCommand.equals("storage")) {
			String url = storage.url;
			
			String authToken = normalizeTokenFile(storage.authFile);
			if (authToken == null) {
				authToken = normalizeToken(storage.authToken);
			}
			
			if (storage.get) {
				if (storage.delete) {
					jc.usage();
					return;							
				}	
				
				if (storage.storageId == null) {
					doRequest("get", url + "/" + OrderConstants.STORAGE_TERM, authToken);
					return;
				} 
				doRequest("get", url + "/" + OrderConstants.STORAGE_TERM + "/" + storage.storageId, authToken);
			} else if (storage.delete) {
				if (storage.get) {
					jc.usage();
					return;							
				}

				if (storage.storageId == null) {
					doRequest("delete", url + "/" + OrderConstants.STORAGE_TERM, authToken);
					return;
				} 
				doRequest("delete", url + "/" + OrderConstants.STORAGE_TERM + "/" + storage.storageId, authToken);
			} else {
				jc.usage();
				return;				
			}
		}
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

	private static void doRequest(String method, String endpoint, String authToken) throws URISyntaxException, HttpException, IOException {
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
		
		
		/*if (client == null) {
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
		}*/
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
		Storage.client = client;
	}

	private static class Command {
		@Parameter(names = "--url", description = "fogbow manager url")
		String url = System.getenv("FOGBOW_URL") == null ? DEFAULT_URL : System
				.getenv("FOGBOW_URL");
	}
	
	private static class AuthedCommand extends Command {
		@Parameter(names = "--auth-token", description = "auth token")
		String authToken = null;
		
		@Parameter(names = "--auth-file", description = "auth file")
		String authFile = null;
	}

	@Parameters(separators = "=", commandDescription = "Instance storage operations")
	private static class StorageCommand extends AuthedCommand {
		@Parameter(names = "--get", description = "Get instance storage")
		Boolean get = false;

		@Parameter(names = "--delete", description = "Delete instance storage")
		Boolean delete = false;	

		@Parameter(names = "--id", description = "Instance storage id")
		String storageId = null;		
	}
	
	@Parameters(separators = "=", commandDescription = "Attachment operations")
	private static class AttachmentCommand extends AuthedCommand {
		@Parameter(names = "--create", description = "Attachment create")
		Boolean create = false;
		
		@Parameter(names = "--delete", description = "Attachment delete")
		Boolean delete = false;		

		@Parameter(names = "--get", description = "Get attachment")
		Boolean get = false;	

		@Parameter(names = "--id", description = "Attachment id")
		String id = null;
		
		@Parameter(names = "--storageId", description = "Storage id attribute")
		String storageId = null;
		
		@Parameter(names = "--computeId", description = "Compute id attribute")
		String computeId = null;		
		
		@Parameter(names = "--mountPoint", description = "Mount point attribute")
		String mountPoint = null;				
	}

	@Parameters(separators = "=", commandDescription = "Order operations")
	private static class OrderCommand extends AuthedCommand {
		@Parameter(names = "--get", description = "Get order")
		Boolean get = false;

		@Parameter(names = "--create", description = "Create order")
		Boolean create = false;

		@Parameter(names = "--delete", description = "Delete order")
		Boolean delete = false;

		@Parameter(names = "--id", description = "Order id")
		String orderId = null;

		@Parameter(names = "--n", description = "Instance count")
		int instanceCount = DEFAULT_INTANCE_COUNT;

		@Parameter(names = "--image", description = "Instance image")
		String image = DEFAULT_IMAGE;

		@Parameter(names = "--flavor", description = "Instance flavor")
		String flavor = null;

		@Parameter(names = "--type", description = "Order type (one-time|persistent)")
		String type = DEFAULT_TYPE;
		
		@Parameter(names = "--public-key", description = "Public key")
		String publicKey = null;
		
		@Parameter(names = "--requirements", description = "Requirements", variableArity = true)
		List<String> requirements = null;
		
		@Parameter(names = "--user-data-file", description = "User data file for cloud init")
		String userDataFile = null;
		
		@Parameter(names = "--user-data-file-content-type", description = "Content type of user data file for cloud init")
		String userDataFileContentType = null;
		
		@Parameter(names = "--size", description = "Size instance storage")
		String size = null;
		
		@Parameter(names = "--resource-kind", description = "Resource kind")
		String resourceKind = null;
	}
	
}
