package org.fogbowcloud.infrastructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.occi.order.OrderAttribute;
import org.fogbowcloud.manager.occi.order.OrderConstants;
import org.fogbowcloud.manager.occi.storage.StorageAttribute;
import org.fogbowcloud.scheduler.core.http.HttpWrapper;
import org.apache.commons.io.IOUtils;

public class StorageInitializer {
	
	protected static final String LOCAL_TOKEN_HEADER = "local_token";
	protected static final String PLUGIN_PACKAGE = "org.fogbowcloud.manager.core.plugins.identity";
	protected static final String DEFAULT_URL = "http://localhost:8182";
	protected static final int DEFAULT_INTANCE_COUNT = 1;
	protected static final String DEFAULT_TYPE = OrderConstants.DEFAULT_TYPE;
	protected static final String DEFAULT_IMAGE = "fogbow-linux-x86";
	protected static final String NULL_VALUE = "null";
	protected static final String CATEGORY = "Category";
	protected static final String X_OCCI_ATTRIBUTE = "X-OCCI-Attribute";
	
	//Storage attributes
	protected static final String X_OCCI_ATTRIBUTE_STORAGE_SIZE = "occi.storage.size";
	protected static final String X_OCCI_ATTRIBUTE_CORE_ID = "occi.core.id";
	protected static final String X_OCCI_ATTRIBUTE_STORAGE_NAME = "occi.storage.name";
	protected static final String X_OCCI_ATTRIBUTE_STORAGE_STATUS = "occi.storage.status";
	
	//Storage status
	public static final String STORAGE_STATUS_ACTIVE = "active";
	public static final String STORAGE_STATUS_UNAVAILABLE = "unavailable";

	private Properties properties;
	private HttpWrapper httpWrapper;
	
	private static final Logger LOGGER = Logger.getLogger(StorageInitializer.class);
	
	public StorageInitializer(Properties properties) throws IOException {
		httpWrapper = new HttpWrapper();
		this.properties = properties;
	}
	
	public String orderStorage(Integer storageSize, String requirementsCloud) throws Exception {
		
		LOGGER.debug("Creating storage order...");
		
		if (requirementsCloud == null || requirementsCloud.isEmpty()) {
			throw new IllegalArgumentException("Requirement can not be null or empty");
		}
		
		List<Header> headers = mountOrderStorageHeader(storageSize, requirementsCloud);

		String url = properties.getProperty("infra_fogbow_manager_base_url");
		
		String authToken = normalizeTokenFile(properties.getProperty("infra_fogbow_token_public_key_filepath"));
		
		String postOrderResponse =  doRequest("post", url + "/" + OrderConstants.TERM, authToken, headers);
		String orderId = this.getOrderId(postOrderResponse);
		
		String getOrderResponse = doRequest("get", url + "/" + OrderConstants.TERM + "/" + orderId, authToken);
		String storageId = this.getStorageOrAttachmentId(getOrderResponse);
		
		///Map<String, String> storageAttributes = this.parseAttributes(requestInformations);

		while (storageId == null || storageId.equals("null")) {
			getOrderResponse = doRequest("get", url + "/" + OrderConstants.TERM + "/" + orderId, authToken);
			storageId = this.getStorageOrAttachmentId(getOrderResponse);
			Thread.sleep(5000);
		}
		
		LOGGER.debug("Process finished.");
		return storageId;
		
	}
	
	public String attachStorage(String instanceId,
			String globalStorageId) throws Exception {
		
		String url = properties.getProperty("infra_fogbow_manager_base_url");
		String[] splitStorageId = globalStorageId.split("@");
		String storageID = splitStorageId[0];
		
		storageID.trim();
		instanceId.trim();
		
		String authToken = normalizeTokenFile(properties.getProperty("infra_fogbow_token_public_key_filepath"));

		List<Header> headers = mountOrderAttachmentHeader(instanceId, storageID);

		String postAttachmentResponse = doRequest("post", url + "/" + OrderConstants.STORAGE_TERM + "/"
				+ OrderConstants.STORAGE_LINK_TERM + "/", authToken, headers);
		String attachmentID = getOrderId(postAttachmentResponse);
		
		return attachmentID;
	}
	
	public String testStorage(String globalStorageId) throws Exception {
		
		String url = properties.getProperty("infra_fogbow_manager_base_url");
		
		String authToken = normalizeTokenFile(properties
				.getProperty("infra_fogbow_token_public_key_filepath"));
		
		try {
			String getStorageResponse = doRequest("get", url + "/" + OrderConstants.STORAGE_TERM + "/" + globalStorageId, authToken);
			
			Map<String, String> storageAttributes = parseAttributes(getStorageResponse);
			String status = storageAttributes.get(X_OCCI_ATTRIBUTE_STORAGE_STATUS);
			if(status != null && !status.isEmpty()){
				return STORAGE_STATUS_ACTIVE;
			}else{
				return STORAGE_STATUS_UNAVAILABLE;
			}

		} catch (Exception e) {
			return STORAGE_STATUS_UNAVAILABLE;
		}
		
	}
	
	
	private List<Header> mountOrderStorageHeader(Integer storageSize, String requirementsCloud) {
		List<Header> headers = new LinkedList<Header>();
		
		headers.add(new BasicHeader("Category", OrderConstants.TERM
				+ "; scheme=\"" + OrderConstants.SCHEME + "\"; class=\""
				+ OrderConstants.KIND_CLASS + "\""));
		headers.add(new BasicHeader("X-OCCI-Attribute",
				OrderAttribute.INSTANCE_COUNT.getValue() + "=" + 1));
		headers.add(new BasicHeader("X-OCCI-Attribute", OrderAttribute.TYPE
				.getValue() + "=" + OrderConstants.DEFAULT_TYPE));

		headers.add(new BasicHeader("X-OCCI-Attribute",
				OrderAttribute.STORAGE_SIZE.getValue() + "=" + storageSize.intValue()));
		
		headers.add(new BasicHeader("X-OCCI-Attribute", OrderAttribute.RESOURCE_KIND
				.getValue() + "=" + "storage"));
		
		headers.add(new BasicHeader("X-OCCI-Attribute",
				"org.fogbowcloud.request.requirements" + "=" + requirementsCloud));
		return headers;
	}
	
	private List<Header> mountOrderAttachmentHeader(String instanceId, String storageID) {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Category", OrderConstants.STORAGELINK_TERM
				+ "; scheme=\"" + OrderConstants.INFRASTRUCTURE_OCCI_SCHEME
				+ "\"; class=\"" + OrderConstants.KIND_CLASS + "\""));
		headers.add(new BasicHeader("X-OCCI-Attribute", StorageAttribute.SOURCE
				.getValue() + "=" + instanceId));
		headers.add(new BasicHeader("X-OCCI-Attribute", StorageAttribute.TARGET
				.getValue() + "=" + storageID));
		return headers;
	}
	
	private String doRequest(String method, String endpoint,
			String authToken) throws Exception {
		return doRequest(method, endpoint, authToken, new LinkedList<Header>());
	}

	private String doRequest(String method, String endpoint, String authToken, 
			List<Header> additionalHeaders) throws Exception {
		return httpWrapper.doRequest(method, endpoint, authToken, additionalHeaders);
	}
	
	private String getOrderId(String postOrderResponse) {
		String[] requestRes = postOrderResponse.split(":");
		String[] requestId = requestRes[requestRes.length - 1].split("/");
		return requestId[requestId.length - 1];
	}
	
	private String getStorageOrAttachmentId(String getOrderResponse) {
		String[] setOfAttributeInfo = getOrderResponse.split("\n");
		
		String attributeId = setOfAttributeInfo[setOfAttributeInfo.length - 1];
		String[] attributeIdSplit = attributeId.split("\"");
		String id = attributeIdSplit[attributeIdSplit.length - 1];
		
		return id;
	}

	private Map<String, String> parseAttributes(String response) {
		Map<String, String> atts = new HashMap<String, String>();
		for (String responseLine : response.split("\n")) {
			if (responseLine.contains(X_OCCI_ATTRIBUTE+": ")) {
				String[] responseLineSplit = responseLine.substring((X_OCCI_ATTRIBUTE+": ").length()).split("=");
				String valueStr = responseLineSplit[1].trim().replace("\"", "");
				if (!valueStr.equals(NULL_VALUE)) {
					atts.put(responseLineSplit[0].trim(), valueStr);
				}
			}
		}
		return atts;
	}
	
	private String normalizeToken(String token) {
		if (token == null) {
			return null;
		}				
		return token.replace("\n", "");
	}
	
	private String normalizeTokenFile(String tokenPath) {
		if (tokenPath == null) {
			return null;
		}
		
		String token;
		
		File tokenFile = new File(tokenPath);
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

	
}