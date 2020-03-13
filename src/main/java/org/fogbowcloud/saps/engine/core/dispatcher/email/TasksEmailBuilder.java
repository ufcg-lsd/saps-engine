package org.fogbowcloud.saps.engine.core.dispatcher.email;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.AccessLink;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.DatabaseApplication;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;

public class TasksEmailBuilder implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(TasksEmailBuilder.class);

    private final class Task {
        private static final String ID = "taskId";
        private static final String IMAGE_REGION = "task_image_region";
        private static final String IMAGE_COLLECTION_NAME = "task_image_collection_name";
        private static final String IMAGE_DATE = "image_date";
        private static final String TASK_ACCESS_LINKS = "access_links";

        private final class AccessLink {
            private static final String NAME = "name";
            private static final String URL = "url";
        }
    }

    private final DatabaseApplication application;
    private final String userEmail;
    private final List<String> tasksId;
    private final PermanentStorage permanentStorage;
    private final String noReplyEmail;
    private final String noReplyPass;

    public TasksEmailBuilder(DatabaseApplication databaseApplication, PermanentStorage permanentStorage,
                             Properties properties, String userEmail, List<String> tasksId) throws InvalidPropertyException {
        if (!checkProperties(properties))
            throw new InvalidPropertyException("Missing properties to use the send email feature");

        this.application = databaseApplication;
        this.noReplyEmail = properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL);
        this.noReplyPass = properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS);
        this.userEmail = userEmail;
        this.tasksId = tasksId;
        this.permanentStorage = permanentStorage;
    }

    private boolean checkProperties(Properties properties) {
        String[] propertiesSet = {
                SapsPropertiesConstants.NO_REPLY_EMAIL,
                SapsPropertiesConstants.NO_REPLY_PASS
        };

        return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    @Override
    public void run() {
        LOGGER.info("Creating email for user [" + userEmail + " with tasks: " + tasksId);

        JSONArray tasks = generateAllTasksJsons();
        sendEmail(tasks);
    }

    private JSONArray generateAllTasksJsons() {
        JSONArray tasks = new JSONArray();
        for (String taskId : tasksId) {
			JSONObject task = generateTaskEmailJson(taskId);
			tasks.put(task);
        }
        return tasks;
    }

    private void sendEmail(JSONArray tasklist) {
        try {
            GoogleMail.Send(noReplyEmail, noReplyPass, userEmail, "[SAPS] Filter results",
                    tasklist.toString(2));
        } catch (MessagingException | JSONException e) {
            LOGGER.error("Failed to send email with images download links.", e);
        }
    }

    private void sendErrorEmail(StringBuilder errorBuilder) {
        if (!errorBuilder.toString().isEmpty()) {
            try {
                GoogleMail.Send(noReplyEmail, noReplyPass, noReplyEmail,
                        "[SAPS] Errors during image temporary link creation", errorBuilder.toString());
            } catch (MessagingException e) {
                LOGGER.error("Failed to send email with errors to admins.", e);
            }
        }
    }

    private JSONObject generateTaskEmailJson(String taskId) {
        SapsImage task = application.getTask(taskId);
        LOGGER.info("Creating JSON representation for task [" + taskId + "]");

        JSONArray accessLinksJson = new JSONArray();
        try {
            List<AccessLink> accessLinks = permanentStorage.generateAccessLinks(taskId);
            accessLinksJson = generateTaskLinksJson(accessLinks);
        } catch (TaskNotFoundException | IOException e) {
            LOGGER.error("Error while generate access links", e);
        } catch (JSONException e) {
            LOGGER.error("Error while generate task links json", e);
        }

        JSONObject result = new JSONObject();

        try {
            result.put(Task.ID, task.getTaskId());
            result.put(Task.IMAGE_REGION, task.getRegion());
            result.put(Task.IMAGE_COLLECTION_NAME, task.getCollectionTierName());
            result.put(Task.IMAGE_DATE, task.getImageDate());
            result.put(Task.TASK_ACCESS_LINKS, accessLinksJson);
        } catch (JSONException e) {
        	//TODO update error mensage
			LOGGER.error("", e);
        }

        return result;
    }

    private JSONArray generateTaskLinksJson(List<AccessLink> accessLinks) throws JSONException {
        JSONArray accessLinksJson = new JSONArray();

        for (AccessLink accessLink : accessLinks) {
            JSONObject accessLinkJson = new JSONObject();

            accessLinkJson.put(Task.AccessLink.NAME, accessLink.getName());
            accessLinkJson.put(Task.AccessLink.URL, accessLink.getUrl());

            accessLinksJson.put(accessLinkJson);
        }

        return accessLinksJson;
    }

}
