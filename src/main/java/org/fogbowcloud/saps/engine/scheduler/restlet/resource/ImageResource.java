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
	private static final String DOWNLOADER_CONTAINER_REPOSITORY = "downloaderContainerRepository";
	private static final String DOWNLOADER_CONTAINER_TAG = "downloaderContainerTag";
	private static final String PREPROCESSOR_CONTAINER_TAG = "preProcessorContainerRepository";
	private static final String PREPROCESSOR_CONTAINER_REPOSITORY = "preProcessorContainerTag";
	private static final String WORKER_CONTAINER_REPOSITORY = "workerContainerRepository";
	private static final String WORKER_CONTAINER_TAG = "containerTag";
	private static final String DAY = "day";
	private static final String FORCE = "force";

	private static final Logger LOGGER = Logger.getLogger(ImageResource.class);

	private static final String ADD_IMAGES_MESSAGE_OK = "Tasks successfully added";

	private static final String USER_EMAIL = "userEmail";
	private static final String USER_PASSWORD = "userPass";

	private static final String DEFAULT_CONF_PATH = "config/sebal.conf";
	private static final String DEFAULT_DOWNLOADER_CONTAINER_REPOSITORY = "default_downloader_container_repository";
	private static final String DEFAULT_DOWNLOADER_CONTAINER_TAG = "default_downloader_container_tag";
	private static final String DEFAULT_PREPROCESSOR_CONTAINER_REPOSITORY = "default_preprocessor_container_repository";
	private static final String DEFAULT_PREPROCESSOR_CONTAINER_TAG = "default_preprocessor_container_tag";
	private static final String DEFAULT_WORKER_CONTAINER_REPOSITORY = "default_worker_container_repository";
	private static final String DEFAULT_WORKER_CONTAINER_TAG = "default_worker_container_tag";

	public ImageResource() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Get
	public Representation getTasks() throws Exception {
		Series<Header> series = (Series<Header>) getRequestAttributes()
				.get("org.restlet.http.headers");

		String userEmail = series.getFirstValue(USER_EMAIL, true);
		String userPass = series.getFirstValue(USER_PASSWORD, true);

		if (!authenticateUser(userEmail, userPass)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}

		LOGGER.info("Getting task");
		String taskId = (String) getRequest().getAttributes().get("taskId");

		LOGGER.debug("TaskID is " + taskId);

		if (taskId != null) {
			ImageTask imageTask = ((DatabaseApplication) getApplication()).getTask(taskId);
			JSONArray taskJSON = new JSONArray();
			try {
				taskJSON.put(imageTask.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image task " + imageTask, e);
			}

			return new StringRepresentation(taskJSON.toString(), MediaType.APPLICATION_JSON);
		}

		LOGGER.info("Getting all tasks");

		List<ImageTask> listOfTasks = ((DatabaseApplication) getApplication()).getTasks();
		JSONArray tasksJSON = new JSONArray();

		for (ImageTask imageTask : listOfTasks) {
			try {
				tasksJSON.put(imageTask.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image task " + imageTask, e);
			}
		}

		return new StringRepresentation(tasksJSON.toString(), MediaType.APPLICATION_JSON);
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

		String firstYear = form.getFirstValue(FIRST_YEAR);
		String lastYear = form.getFirstValue(LAST_YEAR);
		String region = form.getFirstValue(REGION);
		String dataSet = form.getFirstValue(DATASET);
		String downloaderContainerRepository = form.getFirstValue(DOWNLOADER_CONTAINER_REPOSITORY);
		String downloaderContainerTag = form.getFirstValue(DOWNLOADER_CONTAINER_TAG);
		String preProcessorContainerRepository = form
				.getFirstValue(PREPROCESSOR_CONTAINER_REPOSITORY);
		String preProcessorContainerTag = form.getFirstValue(PREPROCESSOR_CONTAINER_TAG);
		String workerContainerRepository = form.getFirstValue(WORKER_CONTAINER_REPOSITORY);
		String workerContainerTag = form.getFirstValue(WORKER_CONTAINER_TAG);
		LOGGER.debug("FirstYear " + firstYear + " LastYear " + lastYear + " Region " + region);

		try {
			verifyEntryConsistance(properties, region, dataSet, downloaderContainerRepository,
					downloaderContainerTag, preProcessorContainerRepository,
					preProcessorContainerTag, workerContainerRepository, workerContainerTag);

			List<Task> createdTasks = application.addTasks(firstYear, lastYear, region, dataSet,
					downloaderContainerRepository, downloaderContainerTag,
					preProcessorContainerRepository, preProcessorContainerTag,
					workerContainerRepository, workerContainerTag);
			if (application.isUserNotifiable(userEmail)) {
				Submission submission = new Submission(UUID.randomUUID().toString());
				for (Task imageTask : createdTasks) {
					application.addUserNotify(submission.getId(), imageTask.getId(), userEmail);
				}
			}
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, e);
		}

		return new StringRepresentation(ADD_IMAGES_MESSAGE_OK, MediaType.APPLICATION_JSON);
	}

	private void verifyEntryConsistance(Properties properties, String region, String dataSet,
			String downloaderContainerRepository, String downloaderContainerTag,
			String workerContainerRepository, String preProcessorContainerRepository,
			String preProcessorContainerTag, String workerContainerTag) {
		if (region == null || region.isEmpty() || dataSet == null || dataSet.isEmpty()) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}

		if (downloaderContainerRepository == null || downloaderContainerRepository.isEmpty()) {
			downloaderContainerRepository = properties
					.getProperty(DEFAULT_DOWNLOADER_CONTAINER_REPOSITORY);
			downloaderContainerTag = properties.getProperty(DEFAULT_DOWNLOADER_CONTAINER_TAG);

			LOGGER.debug(
					"Input Downloader container repository not passed...using default repository "
							+ downloaderContainerRepository);
		} else {
			if (downloaderContainerTag == null || downloaderContainerTag.isEmpty()) {
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
			}
		}

		if (preProcessorContainerRepository == null || preProcessorContainerRepository.isEmpty()) {
			preProcessorContainerRepository = properties
					.getProperty(DEFAULT_PREPROCESSOR_CONTAINER_REPOSITORY);
			preProcessorContainerTag = properties.getProperty(DEFAULT_PREPROCESSOR_CONTAINER_TAG);

			LOGGER.debug("Pre Processor container repository not passed...using default repository "
					+ preProcessorContainerRepository);
		} else {
			if (preProcessorContainerTag == null || preProcessorContainerTag.isEmpty()) {
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
			}
		}

		if (workerContainerRepository == null || workerContainerRepository.isEmpty()) {
			workerContainerRepository = properties.getProperty(DEFAULT_WORKER_CONTAINER_REPOSITORY);
			workerContainerTag = properties.getProperty(DEFAULT_WORKER_CONTAINER_TAG);

			LOGGER.debug("SebalVersion not passed...using default repository "
					+ workerContainerRepository);
		} else {
			if (workerContainerTag == null || workerContainerTag.isEmpty()) {
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
			}
		}
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
