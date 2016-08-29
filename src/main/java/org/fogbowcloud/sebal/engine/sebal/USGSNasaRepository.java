package org.fogbowcloud.sebal.engine.sebal;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by manel on 18/08/16.
 */
public class USGSNasaRepository implements NASARepository {

    private static final Logger LOGGER = Logger.getLogger(USGSNasaRepository.class);

    private final String sebalExportPath;

    private final String usgsLoginUrl;
    private final String usgsUserName;
    private final String usgsPassword;
    private final String usgsCLIPath;

    //dataset
    private static final String DATASET = "LANDSAT_TM";
    // nodes
    private static final String EARTH_EXPLORER_NODE = "EE";
    // products
    private static final String LEVEL_1_PRODUCT = "STANDARD";
    
    // conf constants
    private static final String SEBAL_EXPORT_PATH = "sebal_export_path";
    private static final String USGS_LOGIN_URL = "nasa_login_url";
    private static final String USGS_USERNAME = "usgs_username";
    private static final String USGS_PASSWORD = "usgs_password";
    private static final String USGS_CLI_PATH = "usgs_cli_path";
    
    public USGSNasaRepository(Properties properties) {
		this(properties.getProperty(SEBAL_EXPORT_PATH), properties
				.getProperty(USGS_LOGIN_URL), properties
				.getProperty(USGS_USERNAME), properties
				.getProperty(USGS_PASSWORD), properties
				.getProperty(USGS_CLI_PATH));
    }

    protected USGSNasaRepository(String sebalExportPath, String usgsLoginUrl, String usgsUserName,
                              String usgsPassword, String usgsCLIPath) {

        Validate.notNull(sebalExportPath, "sebalExportPath cannot be null");

        Validate.notNull(usgsLoginUrl, "usgsLoginUrl cannot be null");
        Validate.notNull(usgsUserName, "usgsUserName cannot be null");
        Validate.notNull(usgsPassword, "usgsPassword cannot be null");
        Validate.notNull(usgsCLIPath, "usgsCLIPath cannot be null");

        this.sebalExportPath = sebalExportPath;
        this.usgsLoginUrl = usgsLoginUrl;
        this.usgsUserName = usgsUserName;
        this.usgsPassword = usgsPassword;
        this.usgsCLIPath = usgsCLIPath;

        Validate.isTrue(directoryExists(sebalExportPath),
                "Sebal sebalExportPath directory " + sebalExportPath + "does not exist.");

        Validate.isTrue(fileExists(usgsCLIPath),
                "usgsCLIPath file " + usgsCLIPath + "does not exist.");
    }

    private boolean directoryExists(String path) {
        File f = new File(path);
        return (f.exists() && f.isDirectory());
    }

    private boolean fileExists(String path) {
        File f = new File(path);
        return (f.exists() && !f.isDirectory());
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

        //FIXME: refactor no use usgs API

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

    @Override
    public Map<String, String> getDownloadLinks(File imageListFile) throws IOException {
        //FIXME: this will be removed from API after we get USGS working
    	//FIXME: see if Charsets import is correct
    	Collection<String> imageNames = FileUtils.readLines(imageListFile, Charsets.UTF_8);
        return doGetDownloadLinks(imageNames);
    }

    private Map<String, String> doGetDownloadLinks(Collection<String> imageNames) {

        Map<String, String> links = new HashedMap();

        for(String imageName: imageNames) {
            String link = doGetDownloadLink(imageName);
            if (link != null) {
                links.put(imageName, link);
            }
        }

        return links;
    }

    private String doGetDownloadLink(String imageName) {

        String link = null;

        try {
            Response response = usgsDownloadURL(getDataSet(), imageName, EARTH_EXPLORER_NODE, LEVEL_1_PRODUCT);

            if (response.exitValue != 0) {
                LOGGER.error("Error while running command. " + response);
            } else {
                LOGGER.debug("Command successfully executed. " + response);
                link = response.out;
            }
        } catch (Throwable e) {
            LOGGER.error("Error while running command", e) ;
        }

        return link;
    }

    private String getDataSet() {
        return DATASET;
    }

    protected Response usgsDownloadURL(String dataset, String sceneId, String node, String product)
            throws IOException, InterruptedException {

        //usgs download-url [dataset] [entity/scene id] --node [node] --product [product]
        //FIXME: is it possible to download a list of scenes at once?

        ProcessBuilder builder = new ProcessBuilder(this.usgsCLIPath, dataset, sceneId, "--" + node, "--" + product);
        LOGGER.debug("Executing command " + builder.command());

        Process p = builder.start();
        p.waitFor();

        String output = ProcessUtil.getOutput(p);
        String err = ProcessUtil.getError(p);

        int exitValue = p.exitValue();

        Response response = new Response(output, err, exitValue);
        return response;
    }

    private class Response {

        public String out;
        public String err;
        public int exitValue;

        public Response(String out, String err, int exitValue) {

            if (out == null) {
                throw new IllegalArgumentException("out parameter cannot be null");
            }

            if (err == null) {
                throw new IllegalArgumentException("err parameter cannot be null");
            }

            this.out = out;
            this.err = err;
            this.exitValue = exitValue;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "out='" + out + '\'' +
                    ", err='" + err + '\'' +
                    ", exitValue=" + exitValue +
                    '}';
        }
    }

}
