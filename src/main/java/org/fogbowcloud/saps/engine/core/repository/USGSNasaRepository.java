package org.fogbowcloud.saps.engine.core.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by manel on 18/08/16.
 */
public class USGSNasaRepository implements INPERepository {

	private final String usgsJsonUrl;
	private final String usgsUserName;
	private final String usgsPassword;
	private final String usgsAPIKeyPeriod;
	private String usgsAPIKey;

	// conf constants
	private static final String USGS_SEARCH_VERSION = "1.4.0";
	private static final String FIRST_YEAR_SUFFIX = "-01-01";
	private static final String LAST_YEAR_SUFFIX = "-12-31";
	private static final int MAX_RESULTS = 50000;

	private static final Logger LOGGER = Logger.getLogger(USGSNasaRepository.class);

	public USGSNasaRepository(Properties properties) {
		this(properties.getProperty(SapsPropertiesConstants.USGS_LOGIN_URL), properties
				.getProperty(SapsPropertiesConstants.USGS_JSON_URL), properties
				.getProperty(SapsPropertiesConstants.USGS_USERNAME), properties
				.getProperty(SapsPropertiesConstants.USGS_PASSWORD), properties
				.getProperty(SapsPropertiesConstants.USGS_API_KEY_PERIOD));
	}

	protected USGSNasaRepository(String usgsLoginUrl, String usgsJsonUrl,
			String usgsUserName, String usgsPassword, String usgsAPIKeyPeriod) {

		Validate.notNull(usgsLoginUrl, "usgsLoginUrl cannot be null");
		Validate.notNull(usgsJsonUrl, "usgsJsonUrl cannot be null");
		Validate.notNull(usgsUserName, "usgsUserName cannot be null");
		Validate.notNull(usgsPassword, "usgsPassword cannot be null");
		Validate.notNull(usgsAPIKeyPeriod, "usgsAPIKeyPeriod cannot be null");

		this.usgsJsonUrl = usgsJsonUrl;
		this.usgsUserName = usgsUserName;
		this.usgsPassword = usgsPassword;
		this.usgsAPIKeyPeriod = usgsAPIKeyPeriod;
	}

	public void handleAPIKeyUpdate(ScheduledExecutorService handleAPIKeyUpdateExecutor) {
		LOGGER.debug("Turning on handle USGS API key update.");

		handleAPIKeyUpdateExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				setUSGSAPIKey(generateAPIKey());
			}

		}, 0, Integer.parseInt(usgsAPIKeyPeriod), TimeUnit.MILLISECONDS);
	}

	protected String generateAPIKey() {
		try {
			String response = getLoginResponse();
			JSONObject apiKeyRequestResponse = new JSONObject(response);

			return apiKeyRequestResponse.getString(SapsPropertiesConstants.DATA_JSON_KEY);
		} catch (Throwable e) {
			LOGGER.error("Error while generating USGS API key", e);
		}

		return null;
	}

	protected String getLoginResponse() throws IOException, ClientProtocolException {

		JSONObject loginJSONObj = new JSONObject();
		try {
			loginJSONObj.put(SapsPropertiesConstants.USERNAME_JSON_KEY, usgsUserName);
			loginJSONObj.put(SapsPropertiesConstants.PASSWORD_JSON_KEY, usgsPassword);
			loginJSONObj.put(SapsPropertiesConstants.AUTH_TYPE_JSON_KEY,
					SapsPropertiesConstants.EROS_JSON_VALUE);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting login JSON", e);
			return null;
		}

		String loginJsonRequest = "jsonRequest=" + loginJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST", "--data",
				loginJsonRequest, usgsJsonUrl + File.separator + "login");
		LOGGER.debug("Command=" + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			return getProcessOutput(p);
		} catch (Exception e) {
			LOGGER.error("Error while logging in USGS", e);
		}

		return null;
	}

	private String getProcessOutput(Process p) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(System.getProperty("line.separator"));
		}
		return stringBuilder.toString();
	}

	protected void setUSGSAPIKey(String usgsAPIKey) {
		this.usgsAPIKey = usgsAPIKey;
	}

	protected String getUSGSAPIKey() {
		return this.usgsAPIKey;
	}

	public JSONArray getAvailableImagesInRange(String dataSet, int firstYear, int lastYear,
			String region) {
		String latitude;
		String longitude;

		try {
			JSONObject regionJSON = getRegionJSON(region);
			latitude = regionJSON.getString(SapsPropertiesConstants.LATITUDE_JSON_KEY);
			longitude = regionJSON.getString(SapsPropertiesConstants.LONGITUDE_JSON_KEY);
		} catch (JSONException e) {
			LOGGER.error("Error while getting coordinates from region JSON", e);
			return null;
		}

		return searchForImagesInRange(dataSet, firstYear, lastYear, latitude, longitude);
	}

	private JSONObject getRegionJSON(String region) throws JSONException {
		String jsonData = readFile(SapsPropertiesConstants.TILES_COORDINATES_FILE_PATH);
		JSONObject regionsJSON = new JSONObject(jsonData);
		JSONArray tiles = regionsJSON.getJSONArray(SapsPropertiesConstants.TILES_JSON_KEY);
		for (int i = 0; i < tiles.length(); i++) {
			if (tiles.getJSONObject(i).getString(SapsPropertiesConstants.TILE_ID_JSON_KEY)
					.equals(region)) {
				return tiles.getJSONObject(i);
			}
		}

		return null;
	}

	private static String readFile(String filename) {
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				line = br.readLine();
			}
			result = sb.toString();
			br.close();
		} catch (Exception e) {
			LOGGER.error("Error while reading regions JSON file", e);
		}

		return result;
	}

	private JSONArray searchForImagesInRange(String dataset, int firstYear, int lastYear,
			String latitude, String longitude) {

		JSONObject searchJSONObj = new JSONObject();
		try {
			formatSearchJSON(dataset, firstYear, lastYear, latitude, longitude, searchJSONObj);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting search JSON", e);
			return null;
		}

		String searchJsonRequest = "jsonRequest=" + searchJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST", "--data",
				searchJsonRequest, usgsJsonUrl + File.separator + "v" + File.separator
						+ USGS_SEARCH_VERSION + File.separator + "search");
		LOGGER.debug("Command=" + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			JSONObject searchResponse = new JSONObject(getProcessOutput(p));
			return searchResponse.getJSONObject(SapsPropertiesConstants.DATA_JSON_KEY)
					.getJSONArray(SapsPropertiesConstants.RESULTS_JSON_KEY);
		} catch (Exception e) {
			LOGGER.error("Error while logging in USGS", e);
		}

		return null;
	}

	private void formatSearchJSON(String dataset, int firstYear, int lastYear, String latitude,
			String longitude, JSONObject searchJSONObj) throws JSONException {
		JSONObject spatialFilterObj = new JSONObject();
		JSONObject temporalFilterObj = new JSONObject();
		JSONObject lowerLeftObj = new JSONObject();
		JSONObject upperRightObj = new JSONObject();

		lowerLeftObj.put(SapsPropertiesConstants.LATITUDE_JSON_KEY, latitude);
		lowerLeftObj.put(SapsPropertiesConstants.LONGITUDE_JSON_KEY, longitude);
		upperRightObj.put(SapsPropertiesConstants.LATITUDE_JSON_KEY, latitude);
		upperRightObj.put(SapsPropertiesConstants.LONGITUDE_JSON_KEY, longitude);

		spatialFilterObj.put(SapsPropertiesConstants.FILTER_TYPE_JSON_KEY,
				SapsPropertiesConstants.MBR_JSON_VALUE);
		spatialFilterObj.put(SapsPropertiesConstants.LOWER_LEFT_JSON_KEY, lowerLeftObj);
		spatialFilterObj.put(SapsPropertiesConstants.UPPER_RIGHT_JSON_KEY, upperRightObj);

		temporalFilterObj.put(SapsPropertiesConstants.DATE_FIELD_JSON_KEY,
				SapsPropertiesConstants.SEARCH_DATE_JSON_VALUE);
		temporalFilterObj.put(SapsPropertiesConstants.START_DATE_JSON_KEY, firstYear
				+ FIRST_YEAR_SUFFIX);
		temporalFilterObj.put(SapsPropertiesConstants.END_DATE_JSON_KEY, lastYear
				+ LAST_YEAR_SUFFIX);

		searchJSONObj.put(SapsPropertiesConstants.API_KEY_JSON_KEY, usgsAPIKey);
		searchJSONObj.put(SapsPropertiesConstants.DATASET_NAME_JSON_KEY, dataset);
		searchJSONObj.put(SapsPropertiesConstants.SPATIAL_FILTER_JSON_KEY, spatialFilterObj);
		searchJSONObj.put(SapsPropertiesConstants.TEMPORAL_FILTER_JSON_KEY, temporalFilterObj);
		searchJSONObj.put(SapsPropertiesConstants.MAX_RESULTS_JSON_KEY, MAX_RESULTS);
		searchJSONObj.put(SapsPropertiesConstants.SORT_ORDER_JSON_KEY,
				SapsPropertiesConstants.ASC_JSON_VALUE);
	}
}
