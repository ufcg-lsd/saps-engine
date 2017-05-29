package org.fogbowcloud.sebal.engine.sebal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.json.JSONObject;

/**
 * Created by manel on 18/08/16.
 */
public class USGSNasaRepository implements NASARepository {

    private static final Logger LOGGER = Logger.getLogger(USGSNasaRepository.class);
    
    private final String sebalExportPath;

    private final String usgsLoginUrl;
    private final String usgsJsonUrl;
    private final String usgsUserName;
    private final String usgsPassword;
    private final String usgsAPIKeyPeriod;
    private String usgsAPIKey;
    
    private static final String LANDSAT_5_PREFIX = "LT5";
    private static final String LANDSAT_7_PREFIX = "LE7";
    private static final String LANDSAT_8_PREFIX = "LT8";
    private static final String LANDSAT_5_NEW_COLLECTION_PREFIX = "LT05";
    private static final String LANDSAT_7_NEW_COLLECTION_PREFIX = "LE07";
    private static final String LANDSAT_8_NEW_COLLECTION_PREFIX = "LC08";

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
        this.usgsLoginUrl = usgsLoginUrl;
        this.usgsJsonUrl = usgsJsonUrl;
        this.usgsUserName = usgsUserName;
        this.usgsPassword = usgsPassword;
        this.usgsAPIKeyPeriod = usgsAPIKeyPeriod;   

        Validate.isTrue(directoryExists(sebalExportPath),
                "Sebal sebalExportPath directory " + sebalExportPath + "does not exist.");
    }

	public void handleAPIKeyUpdate(
			ScheduledExecutorService handleAPIKeyUpdateExecutor) {
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

			return apiKeyRequestResponse.getString("data");
		} catch (Throwable e) {
			LOGGER.error("Error while generating USGS API key", e);
		}

		return null;
	}

	protected String getLoginResponse() throws IOException,
			ClientProtocolException {		
		String loginJsonRequest = "jsonRequest={\"username\":\"" + usgsUserName
				+ "\",\"password\":\"" + usgsPassword
				+ "\",\"authType\":\"EROS\"}";
		
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

            LOGGER.info("Downloading image " + imageData.getName() + " into file " + localImageFilePath);

            File file = new File(localImageFilePath);
            downloadInto(imageData, file);
        } else {
            throw new IOException("An error occurred while creating " + imageDirPath + " directory");
        }
    }

    protected String imageFilePath(ImageData imageData, String imageDirPath) {
        return imageDirPath + File.separator + imageData.getName() + ".tar.gz";
    }

    protected String imageDirPath(ImageData imageData) {
        return sebalExportPath + File.separator + "images" + File.separator + imageData.getName();
    }

    private void downloadInto(ImageData imageData, File targetFile) throws IOException {

        HttpClient httpClient = initClient();

        HttpGet homeGet = new HttpGet(imageData.getDownloadLink());
        HttpResponse response = httpClient.execute(homeGet);

        OutputStream outStream = new FileOutputStream(targetFile);
		IOUtils.copy(response.getEntity().getContent(), outStream);
		outStream.close();
        
    }

    protected boolean createDirectoryToImage(String imageDirPath) {
        File imageDir = new File(imageDirPath);
        return imageDir.mkdirs();
    }

    private HttpClient initClient() throws IOException {

        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();

        HttpGet homeGet = new HttpGet(this.usgsLoginUrl);
        httpClient.execute(homeGet);

        HttpPost homePost = new HttpPost(usgsLoginUrl);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        nvps.add(new BasicNameValuePair("username", this.usgsUserName));
        nvps.add(new BasicNameValuePair("password", this.usgsPassword));
        nvps.add(new BasicNameValuePair("rememberMe", "0"));

        homePost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        HttpResponse homePostResponse = httpClient.execute(homePost);
        EntityUtils.toString(homePostResponse.getEntity());
        return httpClient;
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

		return null;
	}
	
	public String getImageDownloadLink(String imageName, List<String> possibleStations) {

		if (usgsAPIKey != null && !usgsAPIKey.isEmpty()) {
			String link = doGetDownloadLink(imageName, possibleStations);
			if (link != null && !link.isEmpty()) {
				return link;
			}
		} else {
			LOGGER.error("USGS API key invalid");
		}

		return null;
	}
	
	private String doGetDownloadLink(String imageName) {
		String link = null;
		link = usgsDownloadURL(getNewSceneDataSet(imageName), imageName,
				EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);
		
		if (link != null && !link.isEmpty()) {
			return link;
		}

		return null;
	}

	private String getNewSceneDataSet(String imageName) {
		if (imageName.startsWith(LANDSAT_5_NEW_COLLECTION_PREFIX)) {
			return LANDSAT_5_DATASET;
		} else if (imageName.startsWith(LANDSAT_7_NEW_COLLECTION_PREFIX)) {
			return LANDSAT_7_DATASET;
		} else if (imageName.startsWith(LANDSAT_8_NEW_COLLECTION_PREFIX)) {
			return LANDSAT_8_DATASET;
		}

		return null;
	}

	private String doGetDownloadLink(String imageName,
			List<String> possibleStations) {
		String link = null;
		for (String station : possibleStations) {
			String imageNameConcat = imageName.concat(station);
			link = usgsDownloadURL(getDataSet(imageNameConcat),
					imageNameConcat, EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);
			if (link != null && !link.isEmpty()) {
				imageName = getCollectionOneSceneId(
						getDataSet(imageNameConcat), imageNameConcat,
						EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);
				return link;
			}
		}

		return null;
	}
	
	private String getCollectionOneSceneId(String dataset, String oldSceneId,
			String node, String product) {
		// GET NEW SCENE ID        
		String response = getMetadataHttpResponse(dataset, oldSceneId, node, product);
		
		try {
			JSONObject metadataRequestResponse = new JSONObject(response);
			String newSceneId = metadataRequestResponse.getJSONObject("data").getString("displayId");
			
			
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

	private String getMetadataHttpResponse(String dataset, String sceneId,
			String node, String product) {
		String metadataJsonRequest = "jsonRequest={\"apiKey\":\"" + usgsAPIKey
				+ "\",\"datasetName\":\"" + dataset + "\",\"node\":\"" + node
				+ "\",\"entityIds\":[\"" + sceneId + "\"]}";

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
			String downloadLink = downloadRequestResponse.getString("data")
					.replace("\\/", "/");
			
			LOGGER.debug("downloadLink=" + downloadLink);
			if (downloadLink != null && !downloadLink.isEmpty()) {
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
		String downloadJsonRequest = "jsonRequest={\"datasetName\":\""
				+ dataset + "\",\"apiKey\":\"" + usgsAPIKey + "\",\"node\":\""
				+ node + "\",\"entityIds\":[\"" + sceneId
				+ "\"],\"products\":[\"" + product + "\"]}";

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
    
	protected void setUSGSAPIKey(String usgsAPIKey) {
		this.usgsAPIKey = usgsAPIKey;
	}
	
	protected String getUSGSAPIKey() {
		return this.usgsAPIKey;
	}
}
