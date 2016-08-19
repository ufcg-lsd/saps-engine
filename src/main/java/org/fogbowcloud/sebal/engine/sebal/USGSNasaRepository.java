package org.fogbowcloud.sebal.engine.sebal;

import org.apache.commons.io.IOUtils;
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

    //properties.getProperty("sebal_export_path")

    public USGSNasaRepository(String sebalExportPath, String usgsLoginUrl, String usgsUserName, String usgsPassword,
                              String usgsCLIPath) {

        //FIXME: check directory exists.
        this.sebalExportPath = sebalExportPath;

        //FIXME: check not null
        this.usgsLoginUrl = usgsLoginUrl;
        this.usgsUserName = usgsUserName;
        this.usgsPassword = usgsPassword;

        this.usgsCLIPath = usgsCLIPath;
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
                LOGGER.info("File " + localImageFilePath + " already exists. Will remove it before repeat download");
                localImageFile.delete();
            }

            LOGGER.info("Downloading image " + imageData.getName() + " into file " + localImageFilePath);

            File file = new File(localImageFilePath);
            downloadInto(imageData, file);
        } else {
            throw new IOException("An error occurred while creating " + imageDirPath + " directory");
        }
    }

    private String imageFilePath(ImageData imageData, String imageDirPath) {
        return imageDirPath + File.separator + imageData.getName() + ".tar.gz";
    }

    private String imageDirPath(ImageData imageData) {
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

    private boolean createDirectoryToImage(String imageDirPath) {
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

    @Override
    public Map<String, String> getDownloadLinks(File imageListFile) throws IOException {

        //call python API
        return null;
    }

    private Map<String, String> doGetDownloadLinks(Collection<String> imageNames) throws IOException {

        //usgs download-url [dataset] [entity/scene id] --node [node] --product [product]

        //do_call
//        Response response = usgsDownloadURL(dataset, sceneId, node, product);
//        if (response.exitValue != 0) {
//                //FIXME: catch this IOException
//                LOGGER.error("Error while running command. Process exit value = " + String.valueOf(p.exitValue()) + " Message="
//                        + response.err);
//            }
//        }


        //generate map based on response.out
        return null;
    }

    class Response {
        public String out;
        public String err;
        public int exitValue;
    }

    protected Response usgsDownloadURL(String dataset, String sceneId, String node, String product) throws IOException,
            InterruptedException {
        //FIXME: log command lin
        ProcessBuilder builder = new ProcessBuilder(this.usgsCLIPath, dataset, sceneId, "--" + node, "--" + product);
        //FIXME: create response object
//        return builder.start();
        return null;
    }

//FIXME: duplicate code
    private static String getOutput(Process p) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(
                p.getInputStream()));
        String out = new String();
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            out += line;
        }
        return out;
    }

    private static String getError(Process p) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(
                p.getErrorStream()));
        String error = new String();
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            error += line;
        }
        return error;
    }
}
