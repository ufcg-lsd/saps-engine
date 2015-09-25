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
import org.fogbowcloud.manager.occi.request.RequestConstants;

public class HttpWrapper {

    private final HttpClient client;

    public HttpWrapper() {
        this.client = createHttpClient();
    }

    private static HttpClient createHttpClient() {
//        HttpClient client = new DefaultHttpClient();
//        BasicHttpParams params = new BasicHttpParams();
//        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
//                HttpVersion.HTTP_1_1);
//        ConnPerRouteBean.setMaxTotalConnections(params, 5);
//        ConnManagerParams.setMaxConnectionsPerRoute(params, 
//                new ConnPerRouteBean(5));
//        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params,
//                client.getConnectionManager().getSchemeRegistry());
//        cm.
//        cm.setMaxTotal(1);
//        cm.setDefaultMaxPerRoute(1);
//    	PoolingHttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager();
//    	ccm.setMaxTotal(1);
		return HttpClients.createMinimal();
//        return new DefaultHttpClient(params);
    }

    public String doRequest(String method, String endpoint, String authToken,
            List<Header> additionalHeaders) throws Exception {
        HttpUriRequest request = null;
        if (method.equals("get")) {
            request = new HttpGet(endpoint);
        } else if (method.equals("delete")) {
            request = new HttpDelete(endpoint);
        } else if (method.equals("post")) {
            request = new HttpPost(endpoint);
        }
        request.addHeader(OCCIHeaders.CONTENT_TYPE,
                OCCIHeaders.OCCI_CONTENT_TYPE);
        if (authToken != null) {
            request.addHeader(OCCIHeaders.X_FEDERATION_AUTH_TOKEN, authToken);
            request.addHeader(OCCIHeaders.X_LOCAL_AUTH_TOKEN, authToken);
        }
        for (Header header : additionalHeaders) {
            request.addHeader(header);
        }
        HttpResponse response = createHttpClient().execute(request);
        HttpEntity entity = null;
        try {
            entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
                    || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                Header locationHeader = getLocationHeader(response
                        .getAllHeaders());
                if (locationHeader != null
                        && locationHeader.getValue().contains(
                                RequestConstants.TERM)) {
                    return generateLocationHeaderResponse(locationHeader);
                } else {
                    return EntityUtils.toString(response.getEntity());
                }
            } else {
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
