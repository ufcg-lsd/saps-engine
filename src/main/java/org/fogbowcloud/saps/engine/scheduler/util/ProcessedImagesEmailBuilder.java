package org.fogbowcloud.saps.engine.scheduler.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.plugins.identity.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.scheduler.restlet.DatabaseApplication;
import org.fogbowcloud.saps.notifier.GoogleMail;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.*;

public class ProcessedImagesEmailBuilder implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ProcessedImagesEmailBuilder.class);

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String UNAVAILABLE = "UNAVAILABLE";
    private static final String TEMP_DIR_URL = "%s?temp_url_sig=%s&temp_url_expires=%s";

    private static final String PROJECT_ID = "projectId";
    private static final String PASSWORD = "password";
    private static final String USER_ID = "userId";
    private static final String AUTH_URL = "authUrl";
    private static final String TASK_ID = "taskId";
    private static final String REGION = "region";
    private static final String COLLECTION_TIER_NAME = "collectionTierName";
    private static final String IMAGE_DATE = "imageDate";
    private static final String NAME = "name";
    private static final String URL = "url";
    private static final String FILES = "files";
    private static final String STATUS = "status";

    private DatabaseApplication application;
    private Properties properties;
    private String userEmail;
    private List<String> images;

    public ProcessedImagesEmailBuilder(
            DatabaseApplication databaseApplication,
            Properties properties,
            String userEmail,
            List<String> images
    ) {
        this.application = databaseApplication;
        this.properties = properties;
        this.userEmail = userEmail;
        this.images = images;
    }

    @Override
    public void run() {
        StringBuilder builder = new StringBuilder();
        builder.append("Creating email for user ");
        builder.append(userEmail);
        builder.append(" with images:\n");
        for (String str: images) {
            builder.append(str).append("\n");
        }
        LOGGER.info(builder.toString());

        StringBuilder errorBuilder = new StringBuilder();
        JSONArray tasklist = generateAllTasksJsons(errorBuilder);
        sendTaskEmail(errorBuilder, tasklist);
        sendErrorEmail(errorBuilder);
    }

    JSONArray generateAllTasksJsons(StringBuilder errorBuilder) {
        JSONArray tasklist = new JSONArray();
        for (String str: images) {
            try {
                tasklist.put(generateTaskEmailJson(properties, str));
            } catch (IOException | URISyntaxException | IllegalArgumentException e) {
                LOGGER.error("Failed to fetch image from object store.", e);
                errorBuilder
                        .append("Failed to fetch image from object store.").append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                try {
                    JSONObject task = new JSONObject();
                    task.put(TASK_ID, str);
                    task.put(STATUS, UNAVAILABLE);
                    tasklist.put(task);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE task json.", e);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to fetch image from database.", e);
                errorBuilder
                        .append("Failed to fetch image from database.").append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                try {
                    JSONObject task = new JSONObject();
                    task.put(TASK_ID, str);
                    task.put(STATUS, UNAVAILABLE);
                    tasklist.put(task);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE task json.", e);
                }
            } catch (JSONException e) {
                LOGGER.error("Failed to create task json.", e);
                errorBuilder
                        .append("Failed to create task json.").append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                try {
                    JSONObject task = new JSONObject();
                    task.put(TASK_ID, str);
                    task.put(STATUS, UNAVAILABLE);
                    tasklist.put(task);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE task json.", e);
                }
            }
        }
        return tasklist;
    }

    private void sendTaskEmail(StringBuilder errorBuilder, JSONArray tasklist) {
        try {
            GoogleMail.Send(
                    properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL),
                    properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS),
                    userEmail,
                    "[SAPS] Filter results",
                    tasklist.toString(2)
            );
        } catch (MessagingException | JSONException e) {
            LOGGER.error("Failed to send email with images download links.", e);
            errorBuilder
                    .append("Failed to send email with images download links.").append("\n")
                    .append(ExceptionUtils.getStackTrace(e)).append("\n");
        }
    }

    private void sendErrorEmail(StringBuilder errorBuilder) {
        if (!errorBuilder.toString().isEmpty()) {
            try {
                GoogleMail.Send(
                        properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL),
                        properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS),
                        "sebal.no.reply@gmail.com",
                        "[SAPS] Errors during image temporary link creation",
                        errorBuilder.toString()
                );
            } catch (MessagingException e) {
                LOGGER.error("Failed to send email with errors to admins.", e);
            }
        }
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        
        String hexString = formatter.toString();
        formatter.close();
        
        return hexString;
    }

    private String calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    String generateTempURL(String swiftPath, String container, String filePath, String key)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        String path = swiftPath + "/" + container + "/" + filePath;

        Formatter objectStoreFormatter = new Formatter();
        objectStoreFormatter.format("%s\n%s\n%s", "GET", Long.MAX_VALUE, path);
        String signature = calculateRFC2104HMAC(objectStoreFormatter.toString(), key);
        objectStoreFormatter.close();

        objectStoreFormatter = new Formatter();
        objectStoreFormatter.format(
                TEMP_DIR_URL,
                path,
                signature,
                Long.MAX_VALUE
        );
        String res = objectStoreFormatter.toString();
        objectStoreFormatter.close();

        return res;
    }

    JSONObject generateTaskEmailJson(Properties properties, String imageid)
            throws URISyntaxException, IOException, SQLException, JSONException {
        JSONObject result = new JSONObject();

        String objectStoreHost = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_HOST);
        String objectStorePath = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH);
        String objectStoreContainer = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_CONTAINER);
        String objectStoreKey = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY);

        ImageTask task = application.getTask(imageid);

        List<String> files = getTaskFilesFromObjectStore(
                properties, objectStoreHost, objectStorePath, objectStoreContainer, task
        );

        result.put(TASK_ID, task.getTaskId());
        result.put(REGION, task.getRegion());
        result.put(COLLECTION_TIER_NAME, task.getCollectionTierName());
        result.put(IMAGE_DATE, task.getImageDate());

        JSONArray filelist = new JSONArray();
        for (String str: files) {
            File f = new File(str);
            String fileName = f.getName();
            try {
                JSONObject fileobject = new JSONObject();
                fileobject.put(NAME, fileName);
                fileobject.put(URL, "https://" + objectStoreHost + generateTempURL(
                        objectStorePath,
                        objectStoreContainer,
                        str,
                        objectStoreKey) + "&filename=" + fileName
                );
                filelist.put(fileobject);
            } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                LOGGER.error("Failed to generate download link for file " + str, e);
                try {
                    JSONObject fileobject = new JSONObject();
                    fileobject.put(NAME, fileName);
                    fileobject.put(URL, UNAVAILABLE);
                    filelist.put(fileobject);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE temp url json.", e);
                }
            }
        }
        result.put(FILES, filelist);

        return result;
    }

    List<String> getTaskFilesFromObjectStore(Properties properties, String objectStoreHost, String objectStorePath, String objectStoreContainer, ImageTask task) throws URISyntaxException, IOException {
        Token token = getKeystoneToken(properties);

        HttpClient client = HttpClients.createDefault();
        HttpGet httpget = prepObjectStoreRequest(objectStoreHost, objectStorePath, objectStoreContainer, task, token);
        HttpResponse response = client.execute(httpget);

        return Arrays.asList(EntityUtils.toString(response.getEntity()).split("\n"));
    }

    private HttpGet prepObjectStoreRequest(String objectStoreHost, String objectStorePath, String objectStoreContainer, ImageTask task, Token token) throws URISyntaxException {
        URI uri = new URIBuilder()
                .setScheme("https")
                .setHost(objectStoreHost)
                .setPath(objectStorePath + "/" + objectStoreContainer)
                .addParameter("path", "archiver/" + task.getTaskId() + "/data/output/")
                .build();
        LOGGER.debug("Getting list of files for task " + task.getTaskId() + " from " + uri);
        HttpGet httpget = new HttpGet(uri);
        httpget.addHeader("X-Auth-Token", token.getAccessId());
        return httpget;
    }

    private Token getKeystoneToken(Properties properties) {
        KeystoneV3IdentityPlugin keystone = new KeystoneV3IdentityPlugin(properties);
        Map<String, String> credentials = new HashMap<>();
        credentials.put(AUTH_URL, properties.getProperty(SapsPropertiesConstants.SWIFT_AUTH_URL));
        credentials.put(PROJECT_ID, properties.getProperty(SapsPropertiesConstants.SWIFT_PROJECT_ID));
        credentials.put(USER_ID, properties.getProperty(SapsPropertiesConstants.SWIFT_USER_ID));
        credentials.put(PASSWORD, properties.getProperty(SapsPropertiesConstants.SWIFT_PASSWORD));
        return keystone.createToken(credentials);
    }
}
