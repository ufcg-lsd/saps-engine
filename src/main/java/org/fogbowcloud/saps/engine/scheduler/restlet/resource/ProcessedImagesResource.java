package org.fogbowcloud.saps.engine.scheduler.restlet.resource;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.notifier.GoogleMail;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.Properties;

public class ProcessedImagesResource extends BaseResource {

    public static final Logger LOGGER = Logger.getLogger(ProcessedImagesResource.class);

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String REQUEST_ATTR_PROCESSED_IMAGES = "images_id[]";
    private static final String TEMP_DIR_URL = "%s/%s?temp_url_sig=%s&temp_url_expires=%s";

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    public static String calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    @Post
    public Representation sendProcessedImagesToEmail(Representation representation) {
        Form form = new Form(representation);

        String userEmail = form.getFirstValue(UserResource.REQUEST_ATTR_USER_EMAIL, true);
        String userPass = form.getFirstValue(UserResource.REQUEST_ATTR_USERPASS, true);
        if (!authenticateUser(userEmail, userPass) || userEmail.equals("anonymous")) {
            throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
        }
        
        String[] imageIds = form.getValuesArray(REQUEST_ATTR_PROCESSED_IMAGES, true);
        Properties properties = application.getProperties();

        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Here is the list of images you selected.");
        int count = 0;
        for (String str: imageIds) {
            strBuilder.append("\n\n\n");
            try {
                ImageTask task = application.getTask(str);
                strBuilder.append("Task: ").append(task.getTaskId()).append("\n");
                strBuilder.append("Region: ").append(task.getRegion()).append("\n");
                strBuilder.append("Collection name: ").append(task.getCollectionTierName()).append("\n");
                strBuilder.append("Image date: ").append(task.getImageDate()).append("\n");

                String objectStoreUrl = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_URL);
                String objectStorePath = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH);
                String objectStoreKey = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY);
                Formatter objectStoreFormatter = new Formatter();
                objectStoreFormatter.format(objectStorePath, task.getTaskId());
                objectStorePath = objectStoreFormatter.toString();

                objectStoreFormatter.close();
                objectStoreFormatter = new Formatter();
                objectStoreFormatter.format("%s\n%s\n%s", "GET", Long.MAX_VALUE, objectStorePath);

                try {
                    String signature = calculateRFC2104HMAC(objectStoreFormatter.toString(), objectStoreKey);

                    objectStoreFormatter.close();
                    objectStoreFormatter = new Formatter();
                    objectStoreFormatter.format(
                            TEMP_DIR_URL,
                            objectStoreUrl,
                            objectStorePath,
                            signature,
                            Long.MAX_VALUE
                    );

                    strBuilder.append("Download: ").append(objectStoreFormatter.toString()).append("\n");
                    objectStoreFormatter.close();
                    count++;
                } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
                    LOGGER.error("Failed to generate signature", e);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to fetch task " + str, e);
            }
        }
        try {
            if (count == 0) throw new MessagingException("Could not generate message body.");
            GoogleMail.Send(
                    properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL),
                    properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS),
                    userEmail,
                    "[SAPS] Filter results",
                    strBuilder.toString()
            );
        } catch (MessagingException e) {
            LOGGER.error("Failed to send email", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to send email.\nInform the admins.");
        }

        return new StringRepresentation(strBuilder.toString(), MediaType.APPLICATION_JSON);
    }
}
