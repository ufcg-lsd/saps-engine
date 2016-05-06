package org.fogbowcloud.scheduler.core.http;

import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.manager.occi.model.HeaderUtils;
import org.fogbowcloud.manager.occi.model.OCCIHeaders;
import org.fogbowcloud.manager.occi.order.OrderConstants;

public class HttpWrapper {

    private static final int SERVER_SIDE_ERRO_MAX = 505;
	private static final int CLIENT_SIDE_CODE_ERRO_INIT = 400;
	private final HttpClient client;

    public HttpWrapper() {
        this.client = createHttpClient();
    }

    private static HttpClient createHttpClient() {
		return HttpClients.createMinimal();
    }

    public String doRequest(String method, String endpoint, String authToken, List<Header> additionalHeaders) throws Exception {
        
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
//            request.addHeader(OCCIHeaders.X_LOCAL_AUTH_TOKEN, authToken);
        }
        for (Header header : additionalHeaders) {
            request.addHeader(header);
        }
        
        HttpResponse response = createHttpClient().execute(request);
        HttpEntity entity = null;
        
        try {
        	
            entity = response.getEntity();
            
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
            	
                Header locationHeader = getLocationHeader(response.getAllHeaders());
                
                if (locationHeader != null && locationHeader.getValue().contains(OrderConstants.TERM)) {
                    return generateLocationHeaderResponse(locationHeader);
                } else {
                    return EntityUtils.toString(response.getEntity());
                }
                
            }else if(statusCode >= CLIENT_SIDE_CODE_ERRO_INIT && statusCode <= SERVER_SIDE_ERRO_MAX){
            	throw new Exception("Erro on request - Method ["+method+"] Endpoit: ["+endpoint+"] - Status: "+statusCode+" -  Msg: "+response.getStatusLine().toString());
            }else {
                return response.getStatusLine().toString();
            }
            
        } finally {
            try {
                if (entity != null) {
                	EntityUtils.toString(entity);
                }
            } catch (Exception e) {
                // Do nothing
            }
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
}
