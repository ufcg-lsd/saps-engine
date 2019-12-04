package org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.utils.ProcessedImagesEmailBuilder;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.util.Arrays;
import java.util.Properties;

public class ProcessedImagesResource extends BaseResource {

	public static final Logger LOGGER = Logger.getLogger(ProcessedImagesResource.class);

	private static final String REQUEST_ATTR_PROCESSED_IMAGES = "images_id[]";

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

		ProcessedImagesEmailBuilder emailBuilder = new ProcessedImagesEmailBuilder(application, properties, userEmail,
				Arrays.asList(imageIds));

		Thread thread = new Thread(emailBuilder);
		thread.start();

		return new StringRepresentation("Email será enviado em breve.", MediaType.TEXT_PLAIN);
	}
}
