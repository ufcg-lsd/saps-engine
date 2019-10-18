package org.fogbowcloud.saps.engine.restlet.resource;

import java.sql.SQLException;
import java.util.*;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RegionResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(ImageResource.class);

	private static final String LOWER_LEFT = "lowerLeft";
	private static final String UPPER_RIGHT = "upperRight";
	private static final String PROCESSING_INIT_DATE = "initialDate";
	private static final String PROCESSING_FINAL_DATE = "finalDate";
	private static final String PROCESSING_INPUT_GATHERING_TAG = "inputGatheringTag";
	private static final String PROCESSING_INPUT_PREPROCESSING_TAG = "inputPreprocessingTag";
	private static final String PROCESSING_ALGORITHM_EXECUTION_TAG = "algorithmExecutionTag";

	public RegionResource() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Get
	public Representation getNumberImagesProcessedByRegion() throws SQLException {
		
		Series<Header> series = (Series<Header>) getRequestAttributes()
				.get("org.restlet.http.headers");

		String userEmail = series.getFirstValue(UserResource.REQUEST_ATTR_USER_EMAIL, true);
		String userPass = series.getFirstValue(UserResource.REQUEST_ATTR_USERPASS, true);

		if (!authenticateUser(userEmail, userPass)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
		}
		
		List<ImageTask> imageTasks = this.application.getTasksInState(ImageTaskState.ARCHIVED);

		Map<String, Integer> regionsFrequency = new HashMap<>();
		for (ImageTask imageTask : imageTasks) {
			String region = imageTask.getRegion();
			if (!regionsFrequency.containsKey(region)) {
				regionsFrequency.put(region, 0);
			}
			regionsFrequency.put(region, regionsFrequency.get(region) + 1);
		}

		JSONArray result = new JSONArray();
		try {
			for (String region : regionsFrequency.keySet()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("region", region);
				jsonObject.put("count", regionsFrequency.get(region));
				result.put(jsonObject);
			}
		} catch (JSONException e) {
			LOGGER.error("Error while trying creating JSONObject");
		}

		return new StringRepresentation(result.toString(),
				MediaType.APPLICATION_JSON);
	}

	@Post
	public Representation getProcessedImagesInInterval(Representation representation) {
		Form form = new Form(representation);

		String userEmail = form.getFirstValue(UserResource.REQUEST_ATTR_USER_EMAIL, true);
		String userPass = form.getFirstValue(UserResource.REQUEST_ATTR_USERPASS, true);
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

		String inputGathering = form.getFirstValue(PROCESSING_INPUT_GATHERING_TAG);
		if (inputGathering.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Input Gathering must be informed.");
		String inputPreprocessing = form.getFirstValue(PROCESSING_INPUT_PREPROCESSING_TAG);
		if (inputPreprocessing.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Input Preprocessing must be informed.");
		String algorithmExecution = form.getFirstValue(PROCESSING_ALGORITHM_EXECUTION_TAG);
		if (algorithmExecution.isEmpty())
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Algorithm Execution must be informed.");

		String builder = "Recovering processed images with settings:\n" +
				"\tLower Left: " + lowerLeftLatitude + ", " + lowerLeftLongitude + "\n" +
				"\tUpper Right: " + upperRightLatitude + ", " + upperRightLongitude + "\n" +
				"\tInterval: " + initDate + " - " + endDate + "\n" +
				"\tGathering: " + inputGathering + "\n" +
				"\tPreprocessing: " + inputPreprocessing + "\n" +
				"\tAlgorithm: " + algorithmExecution + "\n";
		LOGGER.info(builder);

		// TODO uncomment when USGS comes back up
		List<ImageTask> tasks = application.searchProcessedTasks(
				lowerLeftLatitude,
				lowerLeftLongitude,
				upperRightLatitude,
				upperRightLongitude,
				initDate,
				endDate,
				inputPreprocessing,
				inputGathering,
				algorithmExecution
		);
		JSONArray arr = new JSONArray();
		for (ImageTask task: tasks) {
			try {
				arr.put(task.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Failed to build JSON object of Image Task", e);
			}
		}
		JSONObject resObj = new JSONObject();
		try {
			resObj.put("result", arr);
		} catch (JSONException e) {
			LOGGER.error("Failed to build response JSON object", e);
		}

		return new StringRepresentation(resObj.toString(), MediaType.APPLICATION_JSON);
	}
}
