package org.fogbowcloud.sebal.engine.sebal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class DefaultNASARepository implements NASARepository {
	
	Properties properties;	
	public static final Logger LOGGER = Logger.getLogger(DefaultNASARepository.class);

	public DefaultNASARepository(Properties properties) {
		this.properties = properties;
	}

	public void downloadImage(final ImageData imageData) throws IOException {

		HttpClient httpClient = initClient();
		HttpGet homeGet = new HttpGet(imageData.getDownloadLink());
		HttpResponse response = httpClient.execute(homeGet);
		
		String imageDirPath = properties.getProperty("sebal_export_path") + "/images/" + imageData.getName();
		File imageDir = new File(imageDirPath);
		if (!imageDir.exists() || !imageDir.isDirectory()) {
			imageDir.mkdirs();
		}
		
		String localImageFilePath = imageDirPath + "/" + imageData.getName() + ".tar.gz";
		
		File localImageFile = new File(localImageFilePath);
		if (localImageFile.exists()) {
			LOGGER.debug("The file for image " + imageData.getName()
					+ " already exist, but may not be downloaded successfully. The file "
					+ localImageFilePath + " will be download again.");
			localImageFile.delete();			
		}
		
		LOGGER.info("Downloading image " + imageData.getName() + " into file " + localImageFilePath);
		File file = new File(localImageFilePath);
		OutputStream outStream = new FileOutputStream(file);
		IOUtils.copy(response.getEntity().getContent(), outStream);
		outStream.close();
		
		/*// saving SEBAL-engine version
		ProcessBuilder builder = new ProcessBuilder("/bin/bash", "scripts/sebal-engine-version.sh", localImageFilePath);
		Process p = builder.start();
		p.waitFor();*/
	}

	private HttpClient initClient() throws IOException, ClientProtocolException,
			UnsupportedEncodingException {
		BasicCookieStore cookieStore = new BasicCookieStore();
		HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore)
				.build();

		HttpGet homeGet = new HttpGet(properties.getProperty("nasa_login_url"));
		httpClient.execute(homeGet);

		HttpPost homePost = new HttpPost(properties.getProperty("nasa_login_url"));

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();

		nvps.add(new BasicNameValuePair("username", properties.getProperty("nasa_username")));
		nvps.add(new BasicNameValuePair("password", properties.getProperty("nasa_password")));
		nvps.add(new BasicNameValuePair("rememberMe", "0"));

		homePost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		HttpResponse homePostResponse = httpClient.execute(homePost);
		EntityUtils.toString(homePostResponse.getEntity());
		return httpClient;
	}

	public Map<String, String> getDownloadLinks(File imageListFile) throws IOException {
		HttpClient httpClient = initClient();
		String baseURL = "http://earthexplorer.usgs.gov";
		HttpPost httpPost = new HttpPost(baseURL + "/filelist");
		HttpEntity reqEntity = MultipartEntityBuilder.create()
				.addPart("filelistType", new StringBody("single", ContentType.TEXT_PLAIN))
				.addPart("datasetSelection", new StringBody("3119", ContentType.TEXT_PLAIN))
				.addPart("singleFileFormat", new StringBody("comma", ContentType.TEXT_PLAIN))
				.addPart("singleInputFile", 
						new FileBody(imageListFile, ContentType.TEXT_PLAIN, imageListFile.getName()))
				.addPart("multipleFileFormat", new StringBody("gv", ContentType.TEXT_PLAIN))
				.build();

		httpPost.setEntity(reqEntity);
		HttpResponse httpPostResponse = httpClient.execute(httpPost);

		Map<String, String> imageAndDownloadLink = new HashMap<String, String>();
		if (httpPostResponse.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
			// selecting download option
			String selectLocation = httpPostResponse.getFirstHeader("Location").getValue();			
			
			LOGGER.debug("BaseURL is " + baseURL + " and Location is " + selectLocation);
			httpPost = new HttpPost(baseURL + selectLocation);
			
			reqEntity = MultipartEntityBuilder
					.create()
					.addPart("orderType",
							new StringBody("bulk_dta", ContentType.TEXT_PLAIN))
					.addPart("dlOptions_3119[]",
							new StringBody("STANDARD", ContentType.TEXT_PLAIN)).build();
			
			LOGGER.debug("reqEntity is " + reqEntity.toString());

			httpPost.setEntity(reqEntity);
			httpPostResponse = httpClient.execute(httpPost);

			if (httpPostResponse.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				String imagesLocation = httpPostResponse.getFirstHeader("Location")
						.getValue();
				HttpGet httpGet = new HttpGet(baseURL + imagesLocation);
				HttpResponse httpGetResponse = httpClient.execute(httpGet);

				if (httpGetResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String htmlResponse = EntityUtils.toString(httpGetResponse.getEntity());
					// processing html to get image names and download links
					Document html = Jsoup.parse(htmlResponse);
					
//					System.out.println(html);
					
					for (Element element : html.body().getElementById("downloadTable")
							.getElementsByTag("tbody").get(0).getElementsByTag("tr")) {
						List<Element> rowEl = element.getElementsByTag("td");
						
						/*
						 * Row element format example:
						 * rowEl.get(0) = L4-5 TM
						 * rowEl.get(1) = LT52150651990073CUB00
						 * rowEl.get(2) = Level 1 Product
						 * rowEl.get(3) = /download/3119/LT52150651990073CUB00/STANDARD/BulkDownload
						 */
						String imageName = rowEl.get(1).text();
						String downloadLink = baseURL
								+ rowEl.get(3).getElementsByTag("a").attr("href");
						imageAndDownloadLink.put(imageName, downloadLink);
					}
				} else {
					LOGGER.warn("HTTP Get download response was not the expected (200 OK). It was: "
							+ httpGetResponse.getStatusLine());
				}
			} else {
				LOGGER.warn("HTTP Post /filelist/select  response was not the expected (302 MOVED TEMPORARILY). It was: "
						+ httpPostResponse.getStatusLine());
			}
		} else {
			LOGGER.warn("HTTP Post /filelist download response was not the expected (302 MOVED TEMPORARILY). It was: "
					+ httpPostResponse.getStatusLine());
		}
		return imageAndDownloadLink;
	}

}
