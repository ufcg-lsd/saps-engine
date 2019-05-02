package org.fogbowcloud.saps.engine.infrastructure.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.List;
import org.apache.log4j.Logger;

public class HttpWrapper {

    private static final Logger LOGGER = Logger.getLogger(HttpWrapper.class);

    private static final int VERSION_NOT_SUPPORTED = 505;
    private static final int BAD_REQUEST = 400;

    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";

    public static final String HTTP_METHOD_POST = HttpPost.METHOD_NAME;
    public static final String HTTP_METHOD_GET = HttpGet.METHOD_NAME;
    public static final String HTTP_METHOD_DELETE = HttpDelete.METHOD_NAME;

    public static String doRequest(String method, String endpoint, List<Header> additionalHeaders) throws Exception {
        return doRequest(method, endpoint, additionalHeaders, null);
    }

    public static String doRequest(String method, String endpoint, List<Header> additionalHeaders, StringEntity body) throws Exception {
        HttpUriRequest request = instantiateRequest(method, endpoint, body);

        if (request != null) {
            request.setHeader(CONTENT_TYPE, APPLICATION_JSON);
            if (additionalHeaders != null) {
                for (Header header : additionalHeaders) {
                    request.addHeader(header);
                }
            }
        }

        final HttpResponse response = HttpClients.createMinimal().execute(request);

        return extractHttpEntity(response, request);
    }

    private static String extractHttpEntity(HttpResponse response, HttpUriRequest request) {
        HttpEntity entity = null;

        try {
            entity = response.getEntity();

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                return EntityUtils.toString(response.getEntity());

            } else if(statusCode >= BAD_REQUEST && statusCode <= VERSION_NOT_SUPPORTED) {
                final String errMsg = "Request to " + request.getURI() + " failed with status code " + statusCode;
                LOGGER.error(errMsg);
                throw new Exception(errMsg);
            } else {
                return response.getStatusLine().toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (entity != null) {
                    EntityUtils.toString(entity);
                }
            } catch (Exception e) {
                LOGGER.info("Error while trying extract HTTP Entity with error message: " + e.getMessage());
            }
        }
        return null;
    }

    private static HttpUriRequest instantiateRequest(String method, String endpoint, StringEntity body) {
        HttpUriRequest request = null;
        if (method.equalsIgnoreCase(HttpGet.METHOD_NAME)) {
            request = new HttpGet(endpoint);
        } else if (method.equalsIgnoreCase(HttpDelete.METHOD_NAME)) {
            request = new HttpDelete(endpoint);
        } else if (method.equalsIgnoreCase(HttpPost.METHOD_NAME)) {
            request = new HttpPost(endpoint);
            ((HttpPost) request).setEntity(body);
        }
        return request;
    }
}

