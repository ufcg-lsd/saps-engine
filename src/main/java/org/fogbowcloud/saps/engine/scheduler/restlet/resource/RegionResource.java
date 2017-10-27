package org.fogbowcloud.saps.engine.scheduler.restlet.resource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RegionResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(ImageResource.class);

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

		Map<String, Integer> regionsFrequency = new HashMap<String, Integer>();
		for (ImageTask imageTask : imageTasks) {
			String region = imageTask.getRegion();
			if (!regionsFrequency.containsKey(region)) {
				regionsFrequency.put(region, new Integer(0));
			}
			regionsFrequency.put(region, regionsFrequency.get(region) + 1);
		}

		JSONArray regionsFrequencyJson = new JSONArray();
		try {
			JSONObject jsonObject = new JSONObject();
			for (String region : regionsFrequency.keySet()) {
				jsonObject.put(region, regionsFrequency.get(region));
			}
			regionsFrequencyJson.put(jsonObject);
		} catch (JSONException e) {
			LOGGER.error("Error while trying creating JSONObject");
		}

		return new StringRepresentation(regionsFrequencyJson.toString(),
				MediaType.APPLICATION_JSON);
	}
}
