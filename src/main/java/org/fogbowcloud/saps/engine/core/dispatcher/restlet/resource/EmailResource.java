package org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.AccessLink;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageType;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.archiver.storage.nfs.NfsPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.swift.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.dispatcher.email.TaskEmail;
import org.fogbowcloud.saps.engine.core.dispatcher.email.TasksEmailBuilder;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class EmailResource extends BaseResource {

    private static final Logger LOGGER = Logger.getLogger(EmailResource.class);

    //TODO update value this to "tasks_id[]" (requires a change to the Dashboard component)
    private static final String REQUEST_ATTR_PROCESSED_TASKS = "images_id[]";

    @Post
    public Representation sendTaskToEmail(Representation representation) {
        Form form = new Form(representation);

        String userEmail = form.getFirstValue(UserResource.REQUEST_ATTR_USER_EMAIL, true);
        String userPass = form.getFirstValue(UserResource.REQUEST_ATTR_USERPASS, true);

        if (!authenticateUser(userEmail, userPass) || userEmail.equals("anonymous"))
            throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);

        String[] tasksId = form.getValuesArray(REQUEST_ATTR_PROCESSED_TASKS, true);
        Properties properties = application.getProperties();

        try {
            //TODO check if it is possible to reuse permanent storage instead of always creating another one
            PermanentStorage permanentStorage = createPermanentStorage(properties);

            List<TaskEmail> tasksEmail = new LinkedList<>();
            for(String taskId : Arrays.asList(tasksId)){
                SapsImage currentTask = application.getTask(taskId);
                List<AccessLink> currentTaskAccessLinks = permanentStorage.generateAccessLinks(currentTask.getTaskId());
                TaskEmail taskEmail = new TaskEmail(currentTask.getTaskId(), currentTask.getRegion(),
                        currentTask.getCollectionTierName(), currentTask.getImageDate(), currentTaskAccessLinks);
                tasksEmail.add(taskEmail);
            }

            TasksEmailBuilder emailBuilder = new TasksEmailBuilder(properties, userEmail, tasksEmail);
            Thread thread = new Thread(emailBuilder);
            thread.start();

            return new StringRepresentation("Email will be sent soon.", MediaType.TEXT_PLAIN);
        } catch (PermanentStorageException | IOException e) {
            LOGGER.error("Error while create permanent storage", e);
        } catch (InvalidPropertyException e) {
            LOGGER.error("Error while execute send email feature", e);
        } catch (TaskNotFoundException e) {
            LOGGER.error("Error while getting task by id", e);
        }
        return new StringRepresentation("An error occurred while sending the email, please try again later.", MediaType.TEXT_PLAIN);
    }

    private PermanentStorage createPermanentStorage(Properties properties)
            throws PermanentStorageException, IOException {
        String permanentStorageType = properties
                .getProperty(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE);
        if (PermanentStorageType.SWIFT.toString().equalsIgnoreCase(permanentStorageType)) {
            return new SwiftPermanentStorage(properties);
        } else if (PermanentStorageType.NFS.toString().equalsIgnoreCase(permanentStorageType)) {
            return new NfsPermanentStorage(properties);
        }
        throw new IOException("Failed to recognize type of permanent storage");
    }
}
