package org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource;

import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.DatabaseApplication;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.restlet.data.Form;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

public class ImageResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(ImageResource.class);

	private static final String LOWER_LEFT = "lowerLeft";
	private static final String UPPER_RIGHT = "upperRight";
	private static final String PROCESSING_INIT_DATE = "initialDate";
	private static final String PROCESSING_FINAL_DATE = "finalDate";
	private static final String PROCESSING_INPUT_GATHERING_TAG = "inputGatheringTag";
	private static final String PROCESSING_INPUT_PREPROCESSING_TAG = "inputPreprocessingTag";
	private static final String PROCESSING_ALGORITHM_EXECUTION_TAG = "algorithmExecutionTag";
	private static final String PRIORITY = "priority";
	private static final String EMAIL = "email";

	private static final String ADD_IMAGES_MESSAGE_OK = "Tasks successfully added";
	private static final String ADD_IMAGES_MESSAGE_FAILURE = "Failed to add new tasks";

	public ImageResource() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Get
	public Representation getTasks() throws Exception {
		Series<Header> series = (Series<Header>) getRequestAttributes().get("org.restlet.http.headers");

		String userEmail = series.getFirstValue(UserResource.REQUEST_ATTR_USER_EMAIL, true);
		String userPass = series.getFirstValue(UserResource.REQUEST_ATTR_USERPASS, true);

		if (!authenticateUser(userEmail, userPass)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}

		String taskId = (String) getRequest().getAttributes().get("taskId");

		JSONArray tasksJSON;
		if (taskId != null) {
			LOGGER.info("Getting task");
			LOGGER.debug("TaskID is " + taskId);
			SapsImage imageTask = ((DatabaseApplication) getApplication()).getTask(taskId);
			tasksJSON = new JSONArray();
			try {
				tasksJSON.put(imageTask.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image task " + imageTask, e);
			}
		} else {
			LOGGER.info("Getting all tasks");

			List<SapsImage> listOfTasks = ((DatabaseApplication) getApplication()).getTasks();
			tasksJSON = new JSONArray();

			for (SapsImage imageTask : listOfTasks) {
				try {
					tasksJSON.put(imageTask.toJSON());
				} catch (JSONException e) {
					LOGGER.error("Error while creating JSON from image task " + imageTask, e);
				}
			}
		}

		return new StringRepresentation(tasksJSON.toString(), MediaType.APPLICATION_JSON);
	}

	@Post
	public StringRepresentation insertTasks(Representation entity) {
		Form form = new Form(entity);

		String userEmail = form.getFirstValue(UserResource.REQUEST_ATTR_USER_EMAIL, true);
		String userPass = form.getFirstValue(UserResource.REQUEST_ATTR_USERPASS, true);
		LOGGER.debug("POST with userEmail " + userEmail);
		if (!authenticateUser(userEmail, userPass) || userEmail.equals("anonymous")) {
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

		String inputdownloadingPhaseTag = form.getFirstValue(PROCESSING_INPUT_GATHERING_TAG);
		if (inputdownloadingPhaseTag.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Input Gathering must be informed.");
		String preprocessingPhaseTag = form.getFirstValue(PROCESSING_INPUT_PREPROCESSING_TAG);
		if (preprocessingPhaseTag.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Input Preprocessing must be informed.");
		String processingPhaseTag = form.getFirstValue(PROCESSING_ALGORITHM_EXECUTION_TAG);
		if (processingPhaseTag.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Algorithm Execution must be informed.");
		String priority = form.getFirstValue(PRIORITY);
		String email = form.getFirstValue(EMAIL);

		String builder = "Creating new image process with configuration:\n" + "\tLower Left: " + lowerLeftLatitude
				+ ", " + lowerLeftLongitude + "\n" + "\tUpper Right: " + upperRightLatitude + ", " + upperRightLongitude
				+ "\n" + "\tInterval: " + initDate + " - " + endDate + "\n" + "\tInputdownloading tag: "
				+ inputdownloadingPhaseTag + "\n" + "\tPreprocessing tag: " + preprocessingPhaseTag + "\n"
				+ "\tProcessing tag: " + processingPhaseTag + "\n" + "\tPriority: " + priority + "\n" + "\tEmail: "
				+ email;
		LOGGER.info(builder);

		try{
			application.addNewTasks(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, initDate,
					endDate, inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag, priority, email);
		} catch (Exception e) {
			LOGGER.error("Error while add news tasks.", e);
			return new StringRepresentation(ADD_IMAGES_MESSAGE_FAILURE, MediaType.TEXT_PLAIN);
		}

		return new StringRepresentation(ADD_IMAGES_MESSAGE_OK, MediaType.TEXT_PLAIN);
	}
}
