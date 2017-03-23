package org.fogbowcloud.sebal.engine.sebal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
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

    //dataset
    private static final String LANDSAT_5_DATASET = "LANDSAT_TM";
    private static final String LANDSAT_7_DATASET = "LSR_LANDSAT_ETM_COMBINED";
    private static final String LANDSAT_8_DATASET = "LANDSAT_8";
    
    // nodes
    private static final String EARTH_EXPLORER_NODE = "EE";
    // products
    private static final String LEVEL_1_PRODUCT = "STANDARD";
    
    // conf constants
    private static final String SEBAL_EXPORT_PATH = "sebal_export_path";
    private static final String USGS_LOGIN_URL = "nasa_login_url";
    private static final String USGS_JSON_URL = "nasa_json_url";
    private static final String USGS_USERNAME = "usgs_username";
    private static final String USGS_PASSWORD = "usgs_password";
    private static final String USGS_API_KEY_PERIOD = "usgs_api_key_period";

    
    public USGSNasaRepository(Properties properties) {
		this(properties.getProperty(SEBAL_EXPORT_PATH), properties
				.getProperty(USGS_LOGIN_URL), properties
				.getProperty(USGS_JSON_URL), properties
				.getProperty(USGS_USERNAME), properties
				.getProperty(USGS_PASSWORD), properties
				.getProperty(USGS_API_KEY_PERIOD));
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
        
		handleAPIKeyUpdate(Executors.newScheduledThreadPool(1));
    }

	private void handleAPIKeyUpdate(
			ScheduledExecutorService handleAPIKeyUpdateExecutor) {
		LOGGER.debug("Turning on handle USGS API key update.");

		handleAPIKeyUpdateExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				setUSGSAPIKey(generateAPIKey());
			}

		}, 0, Integer.parseInt(usgsAPIKeyPeriod), TimeUnit.MILLISECONDS);
	}

	private String generateAPIKey() {

		try {
			BasicCookieStore cookieStore = new BasicCookieStore();
			HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
			
			String loginJsonRequest = "{\"username\":\"" + usgsUserName
					+ "\",\"password\":\"" + usgsPassword
					+ "\",\"authType\":\"EROS\"}";
			
			HttpGet homeGet = new HttpGet(usgsJsonUrl + File.separator
					+ "login?jsonRequest=" + loginJsonRequest);
	        httpClient.execute(homeGet);
	        
	        HttpResponse response = httpClient.execute(homeGet);
			JSONObject apiKeyRequestResponse = new JSONObject(response);

			return apiKeyRequestResponse.getString("data");
		} catch (Throwable e) {
			LOGGER.error("Error while generating USGS API key", e);
		}

		return null;
	}
    
	private boolean directoryExists(String path) {
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

	private String doGetDownloadLink(String imageName) {

		String link = null;

		try {
			link = usgsDownloadURL(getDataSet(imageName), imageName,
					EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);
		} catch (Throwable e) {
			LOGGER.error("Error while getting download link for image "
					+ imageName, e);
		}

		return link;
	}

    private String getDataSet(String imageName) {
    	if(imageName.startsWith(LANDSAT_5_PREFIX)) {
    		return LANDSAT_5_DATASET;
    	} else if(imageName.startsWith(LANDSAT_7_PREFIX)) {
    		return LANDSAT_7_DATASET;
    	} else if(imageName.startsWith(LANDSAT_8_PREFIX)) {
    		return LANDSAT_8_DATASET;
    	}
    	
        return null;
    }

	protected String usgsDownloadURL(String dataset, String sceneId,
			String node, String product) throws IOException,
			InterruptedException, JSONException {

		BasicCookieStore cookieStore = new BasicCookieStore();
		HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
		String downloadJsonRequest = "{\"datasetName\":\"" + dataset
				+ "\",\"apiKey\":\"" + usgsAPIKey + "\",\"node\":\"" + node
				+ "\",\"entityIds\":[\"" + sceneId + "\"],\"products\":[\""
				+ product + "\"]}";

		// GET DOWNLOAD LINKS
		HttpGet homeGet = new HttpGet(usgsJsonUrl + File.separator
				+ "download?jsonRequest=" + downloadJsonRequest);
        httpClient.execute(homeGet);
        
        HttpResponse response = httpClient.execute(homeGet);
		JSONObject downloadRequestResponse = new JSONObject(response);

		int firstDownloadUrl = 0;
		String formatedResponse = downloadRequestResponse.getJSONArray("data")
				.getString(firstDownloadUrl).replace("\\/", "/");
		String downloadLink = downloadRequestResponse.getJSONArray("data").getString(firstDownloadUrl);

		if(formatedResponse != null && !formatedResponse.isEmpty()) {
			LOGGER.debug("Image " + sceneId + "download link" + downloadLink + " obtained");
			return downloadLink;
		}
		
		return null;
	}
    
	private void setUSGSAPIKey(String usgsAPIKey) {
		this.usgsAPIKey = usgsAPIKey;
	}
}
