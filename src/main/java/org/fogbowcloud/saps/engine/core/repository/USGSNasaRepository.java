package org.fogbowcloud.saps.engine.core.repository;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Created by manel on 18/08/16.
 */
public class USGSNasaRepository implements INPERepository {

	private final String sapsExportPath;

	private final String usgsJsonUrl;
	private final String usgsUserName;
	private final String usgsPassword;
	private final String usgsAPIKeyPeriod;
	private String usgsAPIKey;

	// nodes
	private static final String EARTH_EXPLORER_NODE = "EE";
	// products
	private static final String LEVEL_1_PRODUCT = "STANDARD";

	// conf constants
	private static final String USGS_SEARCH_VERSION = "1.4.0";
	private static final String FIRST_YEAR_SUFFIX = "-01-01";
	private static final String LAST_YEAR_SUFFIX = "-12-31";
	private static final int MAX_RESULTS = 50000;

	private static final Logger LOGGER = Logger.getLogger(USGSNasaRepository.class);

	public USGSNasaRepository(Properties properties) {
		this(properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH),
				properties.getProperty(SapsPropertiesConstants.USGS_LOGIN_URL),
				properties.getProperty(SapsPropertiesConstants.USGS_JSON_URL),
				properties.getProperty(SapsPropertiesConstants.USGS_USERNAME),
				properties.getProperty(SapsPropertiesConstants.USGS_PASSWORD),
				properties.getProperty(SapsPropertiesConstants.USGS_API_KEY_PERIOD));
	}

	protected USGSNasaRepository(String sapsExportPath, String usgsLoginUrl, String usgsJsonUrl,
			String usgsUserName, String usgsPassword, String usgsAPIKeyPeriod) {
		Validate.notNull(sapsExportPath, "sapsExportPath cannot be null");

		Validate.notNull(usgsLoginUrl, "usgsLoginUrl cannot be null");
		Validate.notNull(usgsJsonUrl, "usgsJsonUrl cannot be null");
		Validate.notNull(usgsUserName, "usgsUserName cannot be null");
		Validate.notNull(usgsPassword, "usgsPassword cannot be null");
		Validate.notNull(usgsAPIKeyPeriod, "usgsAPIKeyPeriod cannot be null");

		this.sapsExportPath = sapsExportPath;
		this.usgsJsonUrl = usgsJsonUrl;
		this.usgsUserName = usgsUserName;
		this.usgsPassword = usgsPassword;
		this.usgsAPIKeyPeriod = usgsAPIKeyPeriod;
		setUSGSAPIKey(generateAPIKey());
		handleAPIKeyUpdate(Executors.newScheduledThreadPool(1));

		Validate.isTrue(directoryExists(sapsExportPath), "Sebal sapsExportPath directory "
				+ sapsExportPath + "does not exist.");
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

			return apiKeyRequestResponse.optString(SapsPropertiesConstants.DATA_JSON_KEY);
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

	protected boolean directoryExists(String path) {
		File f = new File(path);
		return (f.exists() && f.isDirectory());
	}

	@Override
	public void downloadImage(ImageTask imageData) throws IOException {
		// create target directory to store image
		String imageDirPath = imageDirPath(imageData);

		boolean wasCreated = createDirectoryToImage(imageDirPath);
		if (wasCreated) {

			String localImageFilePath = imageFilePath(imageData, imageDirPath);

			// clean if already exists (garbage collection)
			File localImageFile = new File(localImageFilePath);
			if (localImageFile.exists()) {
				LOGGER.info("File " + localImageFilePath
						+ " already exists. Will be removed before repeating download");
				localImageFile.delete();
			}

			LOGGER.info("Downloading image " + imageData.getCollectionTierName() + " into file "
					+ localImageFilePath);
			downloadInto(imageData, localImageFilePath);
		} else {
			throw new IOException("An error occurred while creating " + imageDirPath + " directory");
		}
	}

	protected String imageFilePath(ImageTask imageData, String imageDirPath) {
		return imageDirPath + File.separator + imageData.getCollectionTierName() + ".tar.gz";
	}

	protected String imageDirPath(ImageTask imageData) {
		return sapsExportPath + File.separator + "images" + File.separator
				+ imageData.getCollectionTierName();
	}

	private void downloadInto(ImageTask imageData, String targetFilePath) throws IOException {
		ProcessBuilder builder = new ProcessBuilder("curl", "-L", "-o", targetFilePath, "-X",
				"GET", imageData.getDownloadLink());
		LOGGER.debug("Command=" + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("ProcessOutput=" + p.exitValue());
		} catch (Exception e) {
			LOGGER.error("Error while downloading image " + imageData.getCollectionTierName()
					+ " from USGS", e);
		}
	}

	protected boolean createDirectoryToImage(String imageDirPath) {
		File imageDir = new File(imageDirPath);
		return imageDir.mkdirs();
	}

	public String getImageDownloadLink(String imageName) {
		if (usgsAPIKey != null && !usgsAPIKey.isEmpty()) {
			String link = doGetDownloadLink(imageName);
			if (link != null && !link.isEmpty()) {
				return link;
			}
		} else {
			LOGGER.error("USGS API key invalid");
		}

		return new String();
	}

	protected String doGetDownloadLink(String imageName) {
		String link = null;
		link = usgsDownloadURL(getDataSet(imageName), imageName, EARTH_EXPLORER_NODE,
				LEVEL_1_PRODUCT);

		if (link != null && !link.isEmpty()) {
			return link;
		}

		return null;
	}

	protected String getMetadataHttpResponse(String dataset, String sceneId, String node,
			String product) {

		JSONObject metadataJSONObj = new JSONObject();
		try {
			formatMetadataJSON(dataset, sceneId, node, product, metadataJSONObj);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting metadata JSON", e);
			return null;
		}

		String metadataJsonRequest = "jsonRequest=" + metadataJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST", "--data",
				metadataJsonRequest, usgsJsonUrl + File.separator + "metadata");
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

	private void formatMetadataJSON(String dataset, String sceneId, String node, String product,
			JSONObject metadataJSONObj) throws JSONException {
		JSONArray entityIDs = new JSONArray();
		JSONArray products = new JSONArray();
		entityIDs.put(sceneId);
		products.put(product);

		metadataJSONObj.put(SapsPropertiesConstants.DATASET_NAME_JSON_KEY, dataset);
		metadataJSONObj.put(SapsPropertiesConstants.API_KEY_JSON_KEY, usgsAPIKey);
		metadataJSONObj.put(SapsPropertiesConstants.NODE_JSON_KEY, node);
		metadataJSONObj.put(SapsPropertiesConstants.ENTITY_IDS_JSON_KEY, entityIDs);
		metadataJSONObj.put(SapsPropertiesConstants.PRODUCTS_JSON_KEY, products);
	}

	public List<String> getPossibleStations() {
		List<String> possibleStations = new ArrayList<String>();

		try {
			File file = new File(SapsPropertiesConstants.POSSIBLE_STATIONS_FILE_PATH);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				possibleStations.add(line);
			}
			fileReader.close();
		} catch (IOException e) {
			LOGGER.error("Error while getting possible stations from file", e);
		}

		return possibleStations;
	}

	private String getDataSet(String imageName) {
		if (imageName.startsWith(SapsPropertiesConstants.LANDSAT_5_PREFIX)) {
			return SapsPropertiesConstants.LANDSAT_5_DATASET;
		} else if (imageName.startsWith(SapsPropertiesConstants.LANDSAT_7_PREFIX)) {
			return SapsPropertiesConstants.LANDSAT_7_DATASET;
		} else if (imageName.startsWith(SapsPropertiesConstants.LANDSAT_8_PREFIX)) {
			return SapsPropertiesConstants.LANDSAT_8_DATASET;
		}

		return null;
	}

	protected String usgsDownloadURL(String dataset, String sceneId, String node, String product) {
		// GET DOWNLOAD LINKS
		String response = getDownloadHttpResponse(dataset, sceneId, node, product);

		try {
			JSONObject downloadRequestResponse = new JSONObject(response);
			String downloadLink = downloadRequestResponse.getString(
					SapsPropertiesConstants.DATA_JSON_KEY).replace("\\/", "/");
			downloadLink = downloadLink.replace("[", "");
			downloadLink = downloadLink.replace("]", "");
			downloadLink = downloadLink.replace("\"", "");

			LOGGER.debug("downloadLink=" + downloadLink);
			if (downloadLink != null && !downloadLink.isEmpty() && !downloadLink.equals("[]")) {
				LOGGER.debug("Image " + sceneId + "download link" + downloadLink + " obtained");
				return downloadLink;
			}
		} catch (Exception e) {
			LOGGER.error("Error while formating request response", e);
		}

		return null;
	}

	protected String getDownloadHttpResponse(String dataset, String sceneId, String node,
			String product) {

		JSONObject downloadJSONObj = new JSONObject();
		try {
			formatDownloadJSON(dataset, sceneId, node, product, downloadJSONObj);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting download JSON", e);
			return null;
		}

		String downloadJsonRequest = "jsonRequest=" + downloadJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST", "--data",
				downloadJsonRequest, usgsJsonUrl + File.separator + "download");
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

	private void formatDownloadJSON(String dataset, String sceneId, String node, String product,
			JSONObject downloadJSONObj) throws JSONException {
		JSONArray entityIDs = new JSONArray();
		JSONArray products = new JSONArray();
		entityIDs.put(sceneId);
		products.put(product);

		downloadJSONObj.put(SapsPropertiesConstants.DATASET_NAME_JSON_KEY, dataset);
		downloadJSONObj.put(SapsPropertiesConstants.API_KEY_JSON_KEY, usgsAPIKey);
		downloadJSONObj.put(SapsPropertiesConstants.NODE_JSON_KEY, node);
		downloadJSONObj.put(SapsPropertiesConstants.ENTITY_IDS_JSON_KEY, entityIDs);
		downloadJSONObj.put(SapsPropertiesConstants.PRODUCTS_JSON_KEY, products);
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
			formatSearchJSON(dataset, firstYear, lastYear, latitude, longitude, latitude, longitude, searchJSONObj);
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

	private void formatSearchJSON(String dataset, int firstYear, int lastYear, String lowerLeftLatitude,
			String lowerLeftLongitude, String upperRightLatitude, String upperRightLongitude, JSONObject searchJSONObj) throws JSONException {
		JSONObject spatialFilterObj = new JSONObject();
		JSONObject temporalFilterObj = new JSONObject();
		JSONObject lowerLeftObj = new JSONObject();
		JSONObject upperRightObj = new JSONObject();

		lowerLeftObj.put(SapsPropertiesConstants.LATITUDE_JSON_KEY, lowerLeftLatitude);
		lowerLeftObj.put(SapsPropertiesConstants.LONGITUDE_JSON_KEY, lowerLeftLongitude);
		upperRightObj.put(SapsPropertiesConstants.LATITUDE_JSON_KEY, upperRightLatitude);
		upperRightObj.put(SapsPropertiesConstants.LONGITUDE_JSON_KEY, upperRightLongitude);

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

	public Set<String> getRegionsFromArea(String dataset, int firstYear, int lastYear,
			String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude) {
		String parsedDataset = parseDataset(dataset);

		JSONArray jsonArray = searchForRegionInArea(parsedDataset, firstYear, lastYear,
				lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude);
		Set<String> regionsFound = new HashSet<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				String entityId = jsonArray.optJSONObject(i).get("entityId").toString();
				String region = entityId.substring(3, 9);
				regionsFound.add(region);
			} catch (JSONException e) {
				LOGGER.error("Error while formatting found regions JSON", e);
			}
		}
		return regionsFound;
	}

	private String parseDataset(String dataset) {
		if (dataset.equals(SapsPropertiesConstants.DATASET_LT5_TYPE)) {
			return SapsPropertiesConstants.LANDSAT_5_DATASET;
		} else if (dataset.equals(SapsPropertiesConstants.DATASET_LE7_TYPE)) {
			return SapsPropertiesConstants.LANDSAT_7_DATASET;
		} else if (dataset.equals(SapsPropertiesConstants.DATASET_LC8_TYPE)) {
			return SapsPropertiesConstants.LANDSAT_8_DATASET;
		}
		
		return null;
	}

	protected JSONArray searchForRegionInArea(String dataset, int firstYear, int lastYear, String lowerLeftLatitude,
			   String lowerLeftLongitude, String upperRightLatitude, String upperRightLongitude) {
		JSONObject searchJSONObj = new JSONObject();
		try {
			formatSearchJSON(dataset, firstYear, lastYear, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, searchJSONObj);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting search JSON", e);
			return null;
		}

		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject;
			jsonObject = new JSONObject(jsonParser.parse(
                    new InputStreamReader(requestForRegions(searchJSONObj), "UTF-8")).toString());
			return jsonObject.optJSONObject(SapsPropertiesConstants.DATA_JSON_KEY)
					.optJSONArray(SapsPropertiesConstants.RESULTS_JSON_KEY);
		} catch (Exception e) {
			LOGGER.error("Error while converting USGS response to JSON object.", e);
		}
		return new JSONArray();
	}

	private InputStream requestForRegions(JSONObject searchJSONObj){
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost request = new HttpPost(usgsJsonUrl + File.separator + "v" + File.separator
				+ USGS_SEARCH_VERSION + File.separator + "search");

		StringEntity params = null;
		try {
			params = new StringEntity("jsonRequest=" + searchJSONObj);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		request.setEntity(params);

		request.addHeader("content-type", "application/x-www-form-urlencoded");

		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (IOException e) {
			LOGGER.error("Error to send request to USGS.", e);
		}
		HttpEntity entity = response.getEntity();


		if (entity != null) {
			try {
				return entity.getContent();
			} catch (IOException e) {
				LOGGER.error("Error to get regions content from USGS' response.", e);
			}
		}
		return null;
	}
}
