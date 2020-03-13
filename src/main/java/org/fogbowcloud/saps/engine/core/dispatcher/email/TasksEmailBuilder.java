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
    private static final String EMAIL_TITLE = "[SAPS] Results of selected tasks";

    private final class Task {
        private static final String ID = "taskId";
        private static final String IMAGE_REGION = "task_image_region";
        private static final String IMAGE_COLLECTION_NAME = "task_image_collection_name";
        private static final String IMAGE_DATE = "image_date";
        private static final String ACCESS_LINKS = "access_links";

        private final class AccessLink {
            private static final String NAME = "name";
            private static final String URL = "url";
        }
    }

    private final PermanentStorage permanentStorage;
    private final DatabaseApplication application;
    private final List<String> tasksId;
    private final String userEmail;
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
        try {
            JSONArray tasks = generateAllTasksJsonArray();

            LOGGER.debug("Tasks JSON array: " + tasks.toString());

            GoogleMail.Send(noReplyEmail, noReplyPass, userEmail, EMAIL_TITLE, tasks.toString(2));
        } catch (JSONException e) {
            LOGGER.error("Error while working with an object of the JSON type", e);
        } catch (MessagingException e) {
            LOGGER.error("Error while send email", e);
        }
    }

    private JSONArray generateAllTasksJsonArray() throws JSONException {
        LOGGER.info("Creating JSON representation for tasks list: " + tasksId);

        JSONArray tasks = new JSONArray();

        for (String taskId : tasksId) {
            JSONObject task = generateTaskEmailJson(taskId);
            tasks.put(task);
        }

        return tasks;
    }

    private JSONObject generateTaskEmailJson(String taskId) throws JSONException {
        LOGGER.info("Creating JSON representation for task [" + taskId + "]");

        SapsImage task = application.getTask(taskId);
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

        result.put(Task.ID, task.getTaskId());
        result.put(Task.IMAGE_REGION, task.getRegion());
        result.put(Task.IMAGE_COLLECTION_NAME, task.getCollectionTierName());
        result.put(Task.IMAGE_DATE, task.getImageDate());
        result.put(Task.ACCESS_LINKS, accessLinksJson);

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
