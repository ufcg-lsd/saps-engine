package org.fogbowcloud.saps.engine.scheduler.restlet.resource;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.Submission;
import org.fogbowcloud.saps.engine.core.dispatcher.Task;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.scheduler.restlet.DatabaseApplication;
import org.json.JSONArray;
import org.json.JSONException;
import org.restlet.data.Form;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

public class ImageResource extends BaseResource {

	private static final String PURGE_MESSAGE_OK = "Tasks purged from database";
	private static final String FIRST_YEAR = "firstYear";
	private static final String LAST_YEAR = "lastYear";
	private static final String REGION = "region";
	private static final String DATASET = "dataSet";
	private static final String CONTAINER_REPOSITORY = "containerRepository";
	private static final String CONTAINER_TAG = "containerTag";
	private static final String DAY = "day";
	private static final String FORCE = "force";

	private static final Logger LOGGER = Logger.getLogger(ImageResource.class);

	private static final String ADD_IMAGES_MESSAGE_OK = "Tasks successfully added";

	private static final String USER_EMAIL = "userEmail";
	private static final String USER_PASSWORD = "userPass";

	private static final String DEFAULT_CONF_PATH = "config/sebal.conf";
	private static final String DEFAULT_CONTAINER_REPOSITORY = "default_container_repository";
	private static final String DEFAULT_CONTAINER_TAG = "default_container_tag";

	public ImageResource() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Get
	public Representation getImages() throws Exception {
		Series<Header> series = (Series<Header>) getRequestAttributes()
				.get("org.restlet.http.headers");

		String userEmail = series.getFirstValue(USER_EMAIL, true);
		String userPass = series.getFirstValue(USER_PASSWORD, true);

		if (!authenticateUser(userEmail, userPass)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}

		LOGGER.info("Getting image");
		String imageName = (String) getRequest().getAttributes().get("imgName");

		LOGGER.debug("ImageName is " + imageName);

		if (imageName != null) {
			ImageTask imageData = ((DatabaseApplication) getApplication()).getImage(imageName);
			JSONArray image = new JSONArray();
			try {
				image.put(imageData.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image data " + imageData, e);
			}

			return new StringRepresentation(image.toString(), MediaType.APPLICATION_JSON);
		}

		LOGGER.info("Getting all images");

		List<ImageTask> listOfImages = ((DatabaseApplication) getApplication()).getImages();
		JSONArray images = new JSONArray();

		for (ImageTask imageData : listOfImages) {
			try {
				images.put(imageData.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image data " + imageData, e);
			}
		}

		return new StringRepresentation(images.toString(), MediaType.APPLICATION_JSON);
	}

	@Post
	public StringRepresentation insertTasks(Representation entity) throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(DEFAULT_CONF_PATH);
		properties.load(input);

		Form form = new Form(entity);

		String userEmail = form.getFirstValue(USER_EMAIL, true);
		String userPass = form.getFirstValue(USER_PASSWORD, true);

		LOGGER.debug("POST with userEmail " + userEmail);
		if (!authenticateUser(userEmail, userPass)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}

		int firstYear = new Integer(form.getFirstValue(FIRST_YEAR));
		int lastYear = new Integer(form.getFirstValue(LAST_YEAR));
		String region = form.getFirstValue(REGION);
		String dataSet = form.getFirstValue(DATASET);
		String containerRepository = form.getFirstValue(CONTAINER_REPOSITORY);
		String containerTag = form.getFirstValue(CONTAINER_TAG);
		LOGGER.debug("FirstYear " + firstYear + " LastYear " + lastYear + " Region " + region);

		try {
			if (region == null || region.isEmpty() || dataSet == null || dataSet.isEmpty()) {
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
			}

			if (containerRepository == null || containerRepository.isEmpty()) {
				containerRepository = properties.getProperty(DEFAULT_CONTAINER_REPOSITORY);
				containerTag = properties.getProperty(DEFAULT_CONTAINER_TAG);

				LOGGER.debug("SebalVersion not passed...using default repository "
						+ containerRepository);
			} else {
				if (containerTag == null || containerTag.isEmpty()) {
					throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
				}
			}

			List<Task> createdTasks = application.addTasks(firstYear, lastYear, region, dataSet,
					containerRepository, containerTag);
			if (application.isUserNotifiable(userEmail)) {
				Submission submission = new Submission();
				submission.setId(UUID.randomUUID().toString());
				for (Task imageTask : createdTasks) {
					application.addUserNotify(submission.getId(), imageTask.getId(),
							imageTask.getImageTask().getCollectionTierName(), userEmail);
				}
			}
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, e);
		}

		return new StringRepresentation(ADD_IMAGES_MESSAGE_OK, MediaType.APPLICATION_JSON);
	}

	@Delete
	public StringRepresentation purgeTask(Representation entity) throws Exception {
		Form form = new Form(entity);

		String userEmail = form.getFirstValue(USER_EMAIL, true);
		String userPass = form.getFirstValue(USER_PASSWORD, true);

		LOGGER.debug("DELETE with userEmail " + userEmail);

		boolean mustBeAdmin = true;
		if (!authenticateUser(userEmail, userPass, mustBeAdmin)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}

		String day = form.getFirstValue(DAY);
		String force = form.getFirstValue(FORCE);

		LOGGER.debug("Purging tasks from day " + day);
		DatabaseApplication application = (DatabaseApplication) getApplication();

		try {
			application.purgeImage(day, force);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, e);
		}

		return new StringRepresentation(PURGE_MESSAGE_OK, MediaType.APPLICATION_JSON);
	}
}
