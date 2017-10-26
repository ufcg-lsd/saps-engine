package org.fogbowcloud.saps.engine.scheduler.restlet.resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

public class ImageResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(ImageResource.class);

	private static final String USER_EMAIL = "userEmail";
	private static final String USER_PASSWORD = "userPass";
	private static final String LOWER_LEFT = "lowerLeft";
	private static final String UPPER_RIGHT = "upperRight";
	private static final String PROCESSING_INIT_DATE = "initialDate";
	private static final String PROCESSING_FINAL_DATE = "finalDate";
	private static final String PROCESSING_INPUT_GATHERING_TAG = "inputGatheringTag";
	private static final String PROCESSING_INPUT_PREPROCESSING_TAG = "inputPreprocessingTag";
	private static final String PROCESSING_ALGORITHM_EXECUTION_TAG = "algorithmExecutionTag";

	private static final String ADD_IMAGES_MESSAGE_OK = "Tasks successfully added";
	private static final String PURGE_MESSAGE_OK = "Tasks purged from database";
	private static final String DAY = "day";
	private static final String FORCE = "force";


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

		String taskId = (String) getRequest().getAttributes().get("taskId");


		JSONArray tasksJSON;
		if (taskId != null) {
			LOGGER.info("Getting task");
			LOGGER.debug("TaskID is " + taskId);
			ImageTask imageTask = ((DatabaseApplication) getApplication()).getTask(taskId);
			tasksJSON = new JSONArray();
			try {
				tasksJSON.put(imageTask.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image task " + imageTask, e);
			}
		} else {
			LOGGER.info("Getting all tasks");

			List<ImageTask> listOfTasks = ((DatabaseApplication) getApplication()).getTasks();
			tasksJSON = new JSONArray();

			for (ImageTask imageTask : listOfTasks) {
				try {
					tasksJSON.put(imageTask.toJSON());
				} catch (JSONException e) {
					LOGGER.error("Error while creating JSON from image task " + imageTask, e);
				}
			}
		}

		return new StringRepresentation(tasksJSON.toString(), MediaType.APPLICATION_JSON);
	}

	private String extractCoordinate(Form form, String name, int index) {
		String data[] = form.getValuesArray(name + "[]");
		return data[index];
	}

	private Date extractDate(Form form, String name) throws ParseException {
		String data = form.getFirstValue(name);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.parse(data);
	}

	@Post
	public StringRepresentation insertTasks(Representation entity) {
		Form form = new Form(entity);

		String userEmail = form.getFirstValue(USER_EMAIL, true);
		String userPass = form.getFirstValue(USER_PASSWORD, true);
		LOGGER.debug("POST with userEmail " + userEmail);
		if (!authenticateUser(userEmail, userPass)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}

		String lowerLeftLatitude;
		String lowerLeftLongitude;
		String upperRightLatitude;
		String upperRightLongitude;
		try {
			lowerLeftLatitude = extractCoordinate(form, LOWER_LEFT, 0);
			lowerLeftLongitude = extractCoordinate(form, LOWER_LEFT, 1);
			upperRightLatitude = extractCoordinate(form, UPPER_RIGHT, 0);
			upperRightLongitude = extractCoordinate(form, UPPER_RIGHT, 1);
		} catch (Exception e) {
			LOGGER.error("Failed to parse coordinates of new processing.", e);
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "All coordinates must be informed.");
		}

		Date initDate;
		Date endDate;
		try {
			initDate = extractDate(form, PROCESSING_INIT_DATE);
			endDate = extractDate(form, PROCESSING_FINAL_DATE);
		} catch (Exception e) {
			LOGGER.error("Failed to parse dates of new processing.", e);
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "All dates must be informed.");
		}

		String inputGathering = form.getFirstValue(PROCESSING_INPUT_GATHERING_TAG);
		if (inputGathering.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Input Gathering must be informed.");
		String inputPreprocessing = form.getFirstValue(PROCESSING_INPUT_PREPROCESSING_TAG);
		if (inputPreprocessing.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Input Preprocessing must be informed.");
		String algorithmExecution = form.getFirstValue(PROCESSING_ALGORITHM_EXECUTION_TAG);
		if (algorithmExecution.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Algorithm Execution must be informed.");

		String builder = "Creating new image process with configuration:\n" +
				"\tLower Left: " + lowerLeftLatitude + ", " + lowerLeftLongitude + "\n" +
				"\tUpper Right: " + upperRightLatitude + ", " + upperRightLongitude + "\n" +
				"\tInterval: " + initDate + " - " + endDate + "\n" +
				"\tGathering: " + inputGathering + "\n" +
				"\tPreprocessing: " + inputPreprocessing + "\n" +
				"\tAlgorithm: " + algorithmExecution + "\n";
		LOGGER.info(builder);

		try {
			List<Task> createdTasks = application.addTasks(
					lowerLeftLatitude,
					lowerLeftLongitude,
					upperRightLatitude,
					upperRightLongitude,
					initDate,
					endDate,
					inputGathering,
					inputPreprocessing,
					algorithmExecution
			);
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

		return new StringRepresentation(ADD_IMAGES_MESSAGE_OK, MediaType.TEXT_PLAIN);
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
