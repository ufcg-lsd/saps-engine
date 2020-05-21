package org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.AccessLink;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageType;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.archiver.storage.nfs.NfsPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.swift.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.gson.Gson;

public class LinkResource extends BaseResource {

    private static final Logger LOGGER = Logger.getLogger(EmailResource.class);

    private static final String REQUEST_ATTR_TASK_ID = "taskId";
    
    private final Gson gson = new Gson();;
    
    @Get
    public Representation getTaskLinks(Representation representation) {
        Form form = new Form(representation);

        String userEmail = form.getFirstValue(UserResource.REQUEST_ATTR_USER_EMAIL, true);
        String userPass = form.getFirstValue(UserResource.REQUEST_ATTR_USERPASS, true);

        //FIXME I think that authenticateUser should throw an exception itself once
        // the authentication process hasn't worked... - by @raonismaneoto
        if (!authenticateUser(userEmail, userPass))
            throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);

        String taskId = form.getFirstValue(REQUEST_ATTR_TASK_ID, true);
        Properties properties = application.getProperties();

        try {
        	SapsImage sapsTask = application.getTask(taskId);
        	
            //TODO check if it is possible to reuse permanent storage instead of always creating another one
            PermanentStorage permanentStorage = createPermanentStorage(properties);
            
            List<AccessLink> links = permanentStorage.generateAccessLinks(sapsTask);

            return new StringRepresentation(gson.toJson(links), MediaType.APPLICATION_JSON);
        } catch (TaskNotFoundException e) {
            LOGGER.error("Error while getting task by id", e);
        } catch (Exception e) {
            LOGGER.error("Error while create permanent storage", e);
        }
        return new StringRepresentation("An error occurred while sending the email, please try again later.", MediaType.TEXT_PLAIN);
    }

    private PermanentStorage createPermanentStorage(Properties properties)
            throws Exception {
        String permanentStorageType = properties
                .getProperty(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE);
        //FIXME replace this to a more flexible approach to avoid if/switchs. something
        // akin the RAS approach to load the plugins - by @thiagomanel and @raonismaneoto
        if (PermanentStorageType.SWIFT.toString().equalsIgnoreCase(permanentStorageType)) {
            return new SwiftPermanentStorage(properties);
        } else if (PermanentStorageType.NFS.toString().equalsIgnoreCase(permanentStorageType)) {
            return new NfsPermanentStorage(properties);
        }
        throw new IOException("Failed to recognize type of permanent storage");
    }
    
}
