package org.fogbowcloud.sebal.engine.sebal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by manel on 18/08/16.
 */
public class USGSNasaRepository implements NASARepository {

	private static final String USGS_NULL_RESPONSE = "null";

	private static final Logger LOGGER = Logger.getLogger(USGSNasaRepository.class);
    
    private final String sebalExportPath;

    private final String usgsJsonUrl;
    private final String usgsUserName;
    private final String usgsPassword;
    private final String usgsAPIKeyPeriod;
    private String usgsAPIKey;
    
    private static final String LANDSAT_5_PREFIX = "LT5";
    private static final String LANDSAT_7_PREFIX = "LE7";
    private static final String LANDSAT_8_PREFIX = "LC8";

    //dataset
    private static final String LANDSAT_5_DATASET = "LANDSAT_TM_C1";
    private static final String LANDSAT_7_DATASET = "LANDSAT_ETM_C1";
    private static final String LANDSAT_8_DATASET = "LANDSAT_8_C1";
    
    // nodes
    private static final String EARTH_EXPLORER_NODE = "EE";
    // products
    private static final String LEVEL_1_PRODUCT = "STANDARD";
    
    // conf constants
    private static final String SEBAL_EXPORT_PATH = "sebal_export_path";

	private static final int MAX_RESULTS = 1000;

    
    public USGSNasaRepository(Properties properties) {
		this(properties.getProperty(SEBAL_EXPORT_PATH),
				properties.getProperty(SebalPropertiesConstants.USGS_LOGIN_URL),
				properties.getProperty(SebalPropertiesConstants.USGS_JSON_URL),
				properties.getProperty(SebalPropertiesConstants.USGS_USERNAME),
				properties.getProperty(SebalPropertiesConstants.USGS_PASSWORD),
				properties.getProperty(SebalPropertiesConstants.USGS_API_KEY_PERIOD));
    }

	protected USGSNasaRepository(String sebalExportPath, String usgsLoginUrl,
			String usgsJsonUrl, String usgsUserName, String usgsPassword,
			String usgsAPIKeyPeriod) {
        Validate.notNull(sebalExportPath, "sebalExportPath cannot be null");

        Validate.notNull(usgsLoginUrl, "usgsLoginUrl cannot be null");
        Validate.notNull(usgsJsonUrl, "usgsJsonUrl cannot be null");
        Validate.notNull(usgsUserName, "usgsUserName cannot be null");
        Validate.notNull(usgsPassword, "usgsPassword cannot be null");
        Validate.notNull(usgsAPIKeyPeriod, "usgsAPIKeyPeriod cannot be null");

        this.sebalExportPath = sebalExportPath;
        this.usgsJsonUrl = usgsJsonUrl;
        this.usgsUserName = usgsUserName;
        this.usgsPassword = usgsPassword;
        this.usgsAPIKeyPeriod = usgsAPIKeyPeriod;   

        Validate.isTrue(directoryExists(sebalExportPath),
                "Sebal sebalExportPath directory " + sebalExportPath + "does not exist.");
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

			return apiKeyRequestResponse.getString(SebalPropertiesConstants.DATA_JSON_KEY);
		} catch (Throwable e) {
			LOGGER.error("Error while generating USGS API key", e);
		}

		return null;
	}

	protected String getLoginResponse() throws IOException,
			ClientProtocolException {
		
		JSONObject loginJSONObj = new JSONObject();		
		try {
			loginJSONObj.put(SebalPropertiesConstants.USERNAME_JSON_KEY, usgsUserName);
			loginJSONObj.put(SebalPropertiesConstants.PASSWORD_JSON_KEY, usgsPassword);
			loginJSONObj.put(SebalPropertiesConstants.AUTH_TYPE_JSON_KEY, SebalPropertiesConstants.EROS_JSON_VALUE);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting login JSON", e);
			return null;
		}
		
		String loginJsonRequest = "jsonRequest=" + loginJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST",
				"--data", loginJsonRequest, usgsJsonUrl
						+ File.separator + "login");
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
    public void downloadImage(ImageData imageData) throws IOException {
        //create target directory to store image
        String imageDirPath = imageDirPath(imageData);

        boolean wasCreated = createDirectoryToImage(imageDirPath);
        if (wasCreated) {

            String localImageFilePath = imageFilePath(imageData, imageDirPath);

            //clean if already exists (garbage collection)
            File localImageFile = new File(localImageFilePath);
            if (localImageFile.exists()) {
                LOGGER.info("File " + localImageFilePath + " already exists. Will be removed before repeating download");
                localImageFile.delete();
            }

            LOGGER.info("Downloading image " + imageData.getCollectionTierName() + " into file " + localImageFilePath);
            downloadInto(imageData, localImageFilePath);
        } else {
            throw new IOException("An error occurred while creating " + imageDirPath + " directory");
        }
    }

    protected String imageFilePath(ImageData imageData, String imageDirPath) {
        return imageDirPath + File.separator + imageData.getCollectionTierName() + ".tar.gz";
    }

    protected String imageDirPath(ImageData imageData) {
        return sebalExportPath + File.separator + "images" + File.separator + imageData.getCollectionTierName();
    }

    private void downloadInto(ImageData imageData, String targetFilePath) throws IOException {
		ProcessBuilder builder = new ProcessBuilder("curl", "-L", "-o",
				targetFilePath, "-X", "GET", imageData.getDownloadLink());
		LOGGER.debug("Command=" + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("ProcessOutput=" + p.exitValue());
		} catch (Exception e) {
			LOGGER.error("Error while downloading image " + imageData.getCollectionTierName() + " from USGS", e);
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
	
	public Map<String, String> getImageDownloadLink(String imageName, List<String> possibleStations) {
		if (usgsAPIKey != null && !usgsAPIKey.isEmpty()) {
			Map<String, String> imageNameDownloadLink = doGetDownloadLink(imageName, possibleStations);
			if (imageNameDownloadLink != null && !imageNameDownloadLink.isEmpty()) {
				return imageNameDownloadLink;
			}
		} else {
			LOGGER.error("USGS API key invalid");
		}

		return new HashMap<String, String>();
	}
	
	private String doGetDownloadLink(String imageName) {
		String link = null;
		link = usgsDownloadURL(getDataSet(imageName), imageName,
				EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);
		
		if (link != null && !link.isEmpty()) {
			return link;
		}

		return null;
	}

	protected Map<String, String> doGetDownloadLink(String imageName,
			List<String> possibleStations) {
		String link = null;
		for (String station : possibleStations) {
			for(int level = 0; level < 3; level++) {
				String imageNameConcat = imageName.concat(station + "0" + level);
				link = usgsDownloadURL(getDataSet(imageNameConcat),
						imageNameConcat, EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);
				if (link != null && !link.isEmpty()
						&& !link.equals(USGS_NULL_RESPONSE)) {
					Map<String, String> imageNameDownloadLink = new HashMap<String, String>();
					imageNameDownloadLink.put(imageNameConcat, link);
					return imageNameDownloadLink;
				}
			}
		}

		return null;
	}
	
	protected String getCollectionOneSceneId(String dataset, String oldSceneId,
			String node, String product) {
		// GET NEW SCENE ID        
		String response = getMetadataHttpResponse(dataset, oldSceneId, node, product);
		
		try {
			JSONObject metadataRequestResponse = new JSONObject(response);
			JSONArray dataJSONArray = metadataRequestResponse.getJSONArray(SebalPropertiesConstants.DATA_JSON_KEY);
			JSONObject jsonObject = dataJSONArray.getJSONObject(0);
			String newSceneId = jsonObject.getString("displayId");			
			
			LOGGER.debug("newSceneId=" + newSceneId);
			if (newSceneId != null && !newSceneId.isEmpty()) {
				LOGGER.debug("Image " + newSceneId + "download link"
						+ newSceneId + " obtained");
				return newSceneId;
			}
		} catch (Exception e) {
			LOGGER.error("Error while formating request response", e);
		}		
		
		return null;
	}

	protected String getMetadataHttpResponse(String dataset, String sceneId,
			String node, String product) {
		
		JSONObject metadataJSONObj = new JSONObject();		
		try {
			formatMetadataJSON(dataset, sceneId, node, product, metadataJSONObj);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting metadata JSON", e);
			return null;
		}
		
		String metadataJsonRequest = "jsonRequest=" + metadataJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST",
				"--data", metadataJsonRequest, usgsJsonUrl + File.separator
						+ "metadata");
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

	private void formatMetadataJSON(String dataset, String sceneId,
			String node, String product, JSONObject metadataJSONObj) throws JSONException {
		JSONArray entityIDs = new JSONArray();
		JSONArray products = new JSONArray();
		entityIDs.put(sceneId);
		products.put(product);
		
		metadataJSONObj.put(SebalPropertiesConstants.DATASET_NAME_JSON_KEY, dataset);
		metadataJSONObj.put(SebalPropertiesConstants.API_KEY_JSON_KEY, usgsAPIKey);
		metadataJSONObj.put(SebalPropertiesConstants.NODE_JSON_KEY, node);
		metadataJSONObj.put(SebalPropertiesConstants.ENTITY_IDS_JSON_KEY, entityIDs);
		metadataJSONObj.put(SebalPropertiesConstants.PRODUCTS_JSON_KEY, products);
	}

	public List<String> getPossibleStations() {
		List<String> possibleStations = new ArrayList<String>();

		try {
			File file = new File(
					SebalPropertiesConstants.POSSIBLE_STATIONS_FILE_PATH);
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
		if (imageName.startsWith(LANDSAT_5_PREFIX)) {
			return LANDSAT_5_DATASET;
		} else if (imageName.startsWith(LANDSAT_7_PREFIX)) {
			return LANDSAT_7_DATASET;
		} else if (imageName.startsWith(LANDSAT_8_PREFIX)) {
			return LANDSAT_8_DATASET;
		}

		return null;
	}

	protected String usgsDownloadURL(String dataset, String sceneId,
			String node, String product) {	
		// GET DOWNLOAD LINKS        
		String response = getDownloadHttpResponse(dataset, sceneId, node, product);
		
		try {
			JSONObject downloadRequestResponse = new JSONObject(response);
			String downloadLink = downloadRequestResponse.getString(SebalPropertiesConstants.DATA_JSON_KEY)
					.replace("\\/", "/");
			downloadLink = downloadLink.replace("[", "");
			downloadLink = downloadLink.replace("]", "");
			downloadLink = downloadLink.replace("\"", "");
			
			LOGGER.debug("downloadLink=" + downloadLink);
			if (downloadLink != null && !downloadLink.isEmpty() && !downloadLink.equals("[]")) {
				LOGGER.debug("Image " + sceneId + "download link"
						+ downloadLink + " obtained");
				return downloadLink;
			}
		} catch (Exception e) {
			LOGGER.error("Error while formating request response", e);
		}		
		
		return null;
	}

	protected String getDownloadHttpResponse(String dataset, String sceneId,
			String node, String product) {
		
		JSONObject downloadJSONObj = new JSONObject();		
		try {
			formatDownloadJSON(dataset, sceneId, node, product, downloadJSONObj);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting download JSON", e);
			return null;
		}
		
		String downloadJsonRequest = "jsonRequest=" + downloadJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST",
				"--data", downloadJsonRequest, usgsJsonUrl + File.separator
						+ "download");
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
    
	private void formatDownloadJSON(String dataset, String sceneId, String node,
			String product, JSONObject downloadJSONObj) throws JSONException {
		JSONArray entityIDs = new JSONArray();
		JSONArray products = new JSONArray();
		entityIDs.put(sceneId);
		products.put(product);
		
		downloadJSONObj.put(SebalPropertiesConstants.DATASET_NAME_JSON_KEY, dataset);
		downloadJSONObj.put(SebalPropertiesConstants.API_KEY_JSON_KEY, usgsAPIKey);
		downloadJSONObj.put(SebalPropertiesConstants.NODE_JSON_KEY, node);
		downloadJSONObj.put(SebalPropertiesConstants.ENTITY_IDS_JSON_KEY, entityIDs);
		downloadJSONObj.put(SebalPropertiesConstants.PRODUCTS_JSON_KEY, products);
	}

	protected void setUSGSAPIKey(String usgsAPIKey) {
		this.usgsAPIKey = usgsAPIKey;
	}
	
	protected String getUSGSAPIKey() {
		return this.usgsAPIKey;
	}

	public String getNewSceneId(String imageName) {
		return getCollectionOneSceneId(getDataSet(imageName),
				imageName, EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);
	}

	public JSONArray getAvailableImagesInRange(String dataSet, int firstYear,
			int lastYear, String region) {
		double latitude = 0;
		double longitude = 0;
		
		try {
			JSONObject regionJSON = getRegionJSON(region);
			latitude = regionJSON.getDouble(SebalPropertiesConstants.LATITUDE_JSON_KEY);
			longitude = regionJSON.getDouble(SebalPropertiesConstants.LONGITUDE_JSON_KEY);
		} catch (JSONException e) {
			LOGGER.error("Error while getting coordinates from region JSON", e);
			return null;
		}
		
		return searchForImagesInRange(dataSet, firstYear, lastYear, latitude, longitude);
	}

	private JSONObject getRegionJSON(String region) throws JSONException {
		String jsonData = readFile(SebalPropertiesConstants.TILES_COORDINATES_FILE_PATH);
	    JSONObject regionsJSON = new JSONObject(jsonData);
	    JSONArray tiles = regionsJSON.getJSONArray("tiles");
	    for(int i = 0; i < tiles.length(); i++) {
	    	if(tiles.getJSONObject(i).getString("id").equals(region)) {
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
	    } catch(Exception e) {
	        LOGGER.error("Error while reading regions JSON file", e);
	    }
	    
	    return result;
	}

	private JSONArray searchForImagesInRange(String dataset, int firstYear, int lastYear,
			double latitude, double longitude) {
		
		JSONObject searchJSONObj = new JSONObject();		
		try {
			formatSearchJSON(dataset, firstYear, lastYear, latitude, longitude,
					searchJSONObj);
		} catch (JSONException e) {
			LOGGER.error("Error while formatting search JSON", e);
			return null;
		}
				
		String searchJsonRequest = "jsonRequest=" + searchJSONObj.toString();
		ProcessBuilder builder = new ProcessBuilder("curl", "-X", "POST", "--data", searchJsonRequest, usgsJsonUrl + File.separator + "download");
		LOGGER.debug("Command=" + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			JSONObject searchResponse = new JSONObject(getProcessOutput(p));
			return searchResponse.getJSONObject(SebalPropertiesConstants.DATA_JSON_KEY).getJSONArray("results");
		} catch (Exception e) {
			LOGGER.error("Error while logging in USGS", e);
		}

		return null;
	}

	private void formatSearchJSON(String dataset, int firstYear, int lastYear,
			double latitude, double longitude, JSONObject searchJSONObj)
			throws JSONException {
		JSONObject spatialFilterObj = new JSONObject();
		JSONObject temporalFilterObj = new JSONObject();
		JSONObject lowerLeftObj = new JSONObject();
		JSONObject upperRightObj = new JSONObject();
		
		lowerLeftObj.put(SebalPropertiesConstants.LATITUDE_JSON_KEY, latitude);
		lowerLeftObj.put(SebalPropertiesConstants.LONGITUDE_JSON_KEY, longitude);
		upperRightObj.put(SebalPropertiesConstants.LATITUDE_JSON_KEY, latitude);
		upperRightObj.put(SebalPropertiesConstants.LONGITUDE_JSON_KEY, longitude);
		
		spatialFilterObj.put(SebalPropertiesConstants.FILTER_TYPE_JSON_KEY, SebalPropertiesConstants.MBR_JSON_VALUE);
		spatialFilterObj.put(SebalPropertiesConstants.LOWER_LEFT_JSON_KEY, lowerLeftObj);
		spatialFilterObj.put(SebalPropertiesConstants.UPPER_RIGHT_JSON_KEY, upperRightObj);			
		
		temporalFilterObj.put(SebalPropertiesConstants.DATE_FIELD_JSON_KEY, SebalPropertiesConstants.SEARCH_DATE_JSON_VALUE);
		temporalFilterObj.put(SebalPropertiesConstants.START_DATE_JSON_KEY, firstYear);
		temporalFilterObj.put(SebalPropertiesConstants.END_DATE_JSON_KEY, lastYear);
		
		searchJSONObj.put(SebalPropertiesConstants.API_KEY_JSON_KEY, usgsAPIKey);
		searchJSONObj.put(SebalPropertiesConstants.DATASET_NAME_JSON_KEY, dataset);
		searchJSONObj.put(SebalPropertiesConstants.SPATIAL_FILTER_JSON_KEY, spatialFilterObj);
		searchJSONObj.put(SebalPropertiesConstants.TEMPORAL_FILTER_JSON_KEY, temporalFilterObj);
		searchJSONObj.put(SebalPropertiesConstants.MAX_RESULTS_JSON_KEY, MAX_RESULTS);
		searchJSONObj.put(SebalPropertiesConstants.SORT_ORDER_JSON_KEY, SebalPropertiesConstants.ASC_JSON_VALUE);
	}
}
