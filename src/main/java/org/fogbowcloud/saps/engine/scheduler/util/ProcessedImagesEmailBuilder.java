package org.fogbowcloud.saps.engine.scheduler.util;

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
    private static final String TEMP_DIR_URL = "%s?temp_url_sig=%s&temp_url_expires=%s";

    private static final String PROJECT_ID = "projectId";
    private static final String PASSWORD = "password";
    private static final String USER_ID = "userId";
    private static final String AUTH_URL = "authUrl";

    private DatabaseApplication application;
    private Properties properties;
    private String userEmail;
    private List<String> images;
    private String result;

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
        this.result = null;
    }

    public String getResult() {
        return result;
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
        JSONArray tasklist = new JSONArray();
        for (String str: images) {
            try {
                tasklist.put(generateTaskEmailString(properties, str));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Failed to fetch image from object store.", e);
                errorBuilder
                        .append("Failed to fetch image from object store.")
                        .append(e.getMessage())
                        .append(Arrays.toString(e.getStackTrace()));
                try {
                    JSONObject task = new JSONObject();
                    task.put("task", str);
                    task.put("status", "UNAVAILABLE");
                    tasklist.put(task);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE task json.", e);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to fetch image from database.", e);
                errorBuilder
                        .append("Failed to fetch image from database.")
                        .append(e.getMessage())
                        .append(Arrays.toString(e.getStackTrace()));
                try {
                    JSONObject task = new JSONObject();
                    task.put("task", str);
                    task.put("status", "UNAVAILABLE");
                    tasklist.put(task);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE task json.", e);
                }
            } catch (JSONException e) {
                LOGGER.error("Failed to create task json.", e);
                errorBuilder
                        .append("Failed to create task json.")
                        .append(e.getMessage())
                        .append(Arrays.toString(e.getStackTrace()));
                try {
                    JSONObject task = new JSONObject();
                    task.put("task", str);
                    task.put("status", "UNAVAILABLE");
                    tasklist.put(task);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE task json.", e);
                }
            }
        }
        try {
            GoogleMail.Send(
                    properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL),
                    properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS),
                    userEmail,
                    "[SAPS] Filter results",
                    tasklist.toString()
            );
        } catch (MessagingException e) {
            LOGGER.error("Failed to send email with images download links.", e);
            errorBuilder
                    .append("Failed to send email with images download links.")
                    .append(e.getMessage())
                    .append(Arrays.toString(e.getStackTrace()));
        }
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
        result = tasklist.toString();
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    private static String calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    private static String generateTempURL(String swiftPath, String container, String filePath, String key)
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

    private JSONObject generateTaskEmailString(Properties properties, String imageid)
            throws URISyntaxException, IOException, SQLException, JSONException {
        JSONObject result = new JSONObject();

        String objectStoreHost = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_HOST);
        String objectStorePath = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH);
        String objectStoreContainer = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_CONTAINER);
        String objectStoreKey = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY);

        ImageTask task = application.getTask(imageid);

        KeystoneV3IdentityPlugin keystone = new KeystoneV3IdentityPlugin(properties);
        Map<String, String> credentials = new HashMap<>();
        credentials.put(AUTH_URL, properties.getProperty(SapsPropertiesConstants.SWIFT_AUTH_URL));
        credentials.put(PROJECT_ID, properties.getProperty(SapsPropertiesConstants.SWIFT_PROJECT_ID));
        credentials.put(USER_ID, properties.getProperty(SapsPropertiesConstants.SWIFT_USER_ID));
        credentials.put(PASSWORD, properties.getProperty(SapsPropertiesConstants.SWIFT_PASSWORD));
        Token token = keystone.createToken(credentials);

        HttpClient client = HttpClients.createDefault();
        URI uri = new URIBuilder()
                .setScheme("https")
                .setHost(objectStoreHost)
                .setPath(objectStorePath + "/" + objectStoreContainer)
                .addParameter("path", "archiver/" + task.getTaskId() + "/data/output/")
                .build();
        LOGGER.debug("Getting list of files for task " + task.getTaskId() + " from " + uri);
        HttpGet httpget = new HttpGet(uri);
        httpget.addHeader("X-Auth-Token", token.getAccessId());
        HttpResponse response = client.execute(httpget);

        String[] files = EntityUtils.toString(response.getEntity()).split("\n");

        result.put("task", task.getTaskId());
        result.put("region", task.getRegion());
        result.put("collectionTierName", task.getCollectionTierName());
        result.put("imageDate", task.getImageDate());

        JSONArray filelist = new JSONArray();
        for (String str: files) {
            File f = new File(str);
            String fileName = f.getName();
            try {
                JSONObject fileobject = new JSONObject();
                fileobject.put("name", fileName);
                fileobject.put("url", "https://" + objectStoreHost + generateTempURL(
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
                    fileobject.put("name", fileName);
                    fileobject.put("url", "UNAVAILABLE");
                    filelist.put(fileobject);
                } catch (JSONException e1) {
                    LOGGER.error("Failed to create UNAVAILABLE temp url json.", e);
                }
            }
        }
        result.put("files", filelist);

        return result;
    }
}
