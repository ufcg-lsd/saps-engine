package org.fogbowcloud.saps.engine.core.repository;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

	protected String getLoginResponse() throws IOException {

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

	private String getDataSet(String imageName) {
		if (imageName.startsWith(SapsPropertiesConstants.LANDSAT_5_PREFIX)) {
			return SapsPropertiesConstants.DATASET_LT5_TYPE;
		} else if (imageName.startsWith(SapsPropertiesConstants.LANDSAT_7_PREFIX)) {
			return SapsPropertiesConstants.DATASET_LE7_TYPE;
		} else if (imageName.startsWith(SapsPropertiesConstants.LANDSAT_8_PREFIX)) {
			return SapsPropertiesConstants.DATASET_LC8_TYPE;
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
			LOGGER.error("Error while formatting request response", e);
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
	
	/**
	 * Performs conversion between a region represented by two pointers and returns it as 
	 * a set of Landsat WRS-2 ID
	 */
	public Set<String> getRegionsFromArea(String lowerLeftLatitude, String lowerLeftLongitude,
			String upperRightLatitude, String upperRightLongitude) {
		
		LOGGER.debug("Getting landsat ID of: \n"
				+ "lower left latitude: " + lowerLeftLatitude + "\n"
				+ "lower left longitude: " + lowerLeftLongitude + "\n"
				+ "upper right latitude: " + upperRightLatitude + "\n"
				+ "upper right longitude: " + upperRightLongitude);
		
        String[] middlePoint = getMiddlePoint(lowerLeftLatitude, lowerLeftLongitude,
        		upperRightLatitude, upperRightLongitude).split(" ");
        
        LOGGER.debug("Setting middle point(reference point to the LandSat region): + \n" + 
        		"latitude: " + middlePoint[0] + "\n" +  
        		"longitude: " +  middlePoint[1]);
        
        String regionIds = "";
        try {
            regionIds = getRegionIds(middlePoint[0], middlePoint[1]).trim();
        } catch (IOException | InterruptedException e) {
        	LOGGER.error("Error while calling the ConvertToWRS script");
            e.printStackTrace();
        }
        Set<String> regionsFound = new HashSet<>(Arrays.asList(regionIds.split(" ")));
       
        LOGGER.debug("Returned regions as set: ");
        int regionsCount = 1;
        for (String s: regionsFound) {
        	LOGGER.debug(regionsCount + "# " + s);
        	regionsCount++;
        }
        
		return regionsFound;
	}
	
	/**
	 * Gets a region on the map represented by two points (the two vertexes of the region) and returns the middle point
	 */
	private String getMiddlePoint(String lowerLeftLatitude, String lowerLeftLongitude,
                                 String upperRightLatitude, String upperRightLongitude) {
	    double lat1 = Double.parseDouble(lowerLeftLatitude);
	    double lon1 = Double.parseDouble(lowerLeftLongitude);
	    double lat2 = Double.parseDouble(upperRightLatitude);
	    double lon2 = Double.parseDouble(upperRightLongitude);

        double dLon = Math.toRadians(lon2 - lon1);

        //convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        String result = Math.toDegrees(lat3) + " " + Math.toDegrees(lon3);
        return result;
    }
	
	/**
	 * Gets the Landsat WRS-2 ID's given a point (latitude, longitude) by calling get_wrs.py script
	 */
	private static String getRegionIds(String latitude, String longitude) throws IOException, InterruptedException {
		
		LOGGER.debug("Calling get_wrs.py and passing (" + latitude + ", " + longitude + ") as parameter" );
		Process processBuildScript = new ProcessBuilder(
		        "python", "./scripts/get_wrs.py",
                latitude, longitude).start();
		
		LOGGER.debug("Waiting for the process...");
		processBuildScript.waitFor();
		LOGGER.debug("Process ended.");
		
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(processBuildScript.getInputStream()));
        StringBuilder builder = new StringBuilder();

        String line;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }

        String result = builder.toString();
        
        LOGGER.debug("Process output (regions ID's): \n" + result);
        
        return result;
    }
}
