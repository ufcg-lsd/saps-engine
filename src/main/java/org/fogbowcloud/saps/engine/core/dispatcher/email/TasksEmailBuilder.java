package org.fogbowcloud.saps.engine.core.dispatcher.email;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;

import javax.mail.MessagingException;
import java.util.*;

public class TasksEmailBuilder implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(TasksEmailBuilder.class);
    private static final String EMAIL_TITLE = "[SAPS] Results of selected tasks";

    private final List<TaskEmail> tasksEmail;
    private final Gson gson;
    private final String userEmail;
    private final String noReplyEmail;
    private final String noReplyPass;

    public TasksEmailBuilder(String noReplyEmail, String noReplyPass, String userEmail,
                             List<TaskEmail> tasksEmail) {
        if (checkFields(noReplyEmail, noReplyPass, userEmail, tasksEmail))
            throw new IllegalArgumentException("Illegals arguments to use the send email feature");

        this.gson = new Gson();
        this.noReplyEmail = noReplyEmail;
        this.noReplyPass = noReplyPass;
        this.userEmail = userEmail;
        this.tasksEmail = tasksEmail;
    }

    private boolean checkFields(String noReplyEmail, String noReplyPass, String userEmail,
                                List<TaskEmail> tasksEmail) {
        return noReplyEmail.trim().isEmpty() || Objects.isNull(noReplyEmail) ||
                noReplyPass.trim().isEmpty() || Objects.isNull(noReplyPass) ||
                userEmail.trim().isEmpty() || Objects.isNull(userEmail) ||
                Objects.isNull(tasksEmail);
    }

    @Override
    public void run() {
        try {
            String tasksEmail = generateTasksEmail();

            LOGGER.debug("Tasks JSON array: " + tasksEmail);

            sendEmail(tasksEmail);
        } catch (MessagingException e) {
            LOGGER.error("Error while send email", e);
        }
    }

    private String generateTasksEmail() {
        LOGGER.info("Creating representation for tasks list: " + tasksEmail);

        return gson.toJson(tasksEmail);
    }

    private void sendEmail(String body) throws MessagingException {
        LOGGER.info("Sending email: " + body);

        GoogleMail.Send(noReplyEmail, noReplyPass, userEmail, EMAIL_TITLE, body);
    }

}
