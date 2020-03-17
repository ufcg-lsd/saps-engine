package org.fogbowcloud.saps.engine.core.dispatcher.email;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.AccessLink;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.TaskNotFoundException;
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

    private final List<TaskEmail> tasksEmail;
    private final Gson jsonUtil;
    private final String userEmail;
    private final String noReplyEmail;
    private final String noReplyPass;

    public TasksEmailBuilder(Properties properties, String userEmail, List<TaskEmail> tasksEmail)
            throws InvalidPropertyException {
        if (!checkProperties(properties))
            throw new InvalidPropertyException("Missing properties to use the send email feature");
        if (checkFields(userEmail, tasksEmail))
            throw new IllegalArgumentException("Illegals arguments to use the send email feature");

        this.jsonUtil = new Gson();
        this.noReplyEmail = properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL);
        this.noReplyPass = properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS);
        this.userEmail = userEmail;
        this.tasksEmail = tasksEmail;
    }

    private boolean checkProperties(Properties properties) {
        String[] propertiesSet = {
                SapsPropertiesConstants.NO_REPLY_EMAIL,
                SapsPropertiesConstants.NO_REPLY_PASS
        };

        return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    private boolean checkFields(String userEmail, List<TaskEmail> tasksEmail) {
        return userEmail.trim().isEmpty() || Objects.isNull(userEmail) || Objects.isNull(tasksEmail);
    }

    @Override
    public void run() {
        try {
            String tasksEmail = generateTasksEmail();

            LOGGER.debug("Tasks JSON array: " + tasksEmail);

            GoogleMail.Send(noReplyEmail, noReplyPass, userEmail, EMAIL_TITLE, tasksEmail);
        } catch (MessagingException e) {
            LOGGER.error("Error while send email", e);
        }
    }

    private String generateTasksEmail() {
        LOGGER.info("Creating representation for tasks list: " + tasksEmail);

        return jsonUtil.toJson(tasksEmail);
    }

}
