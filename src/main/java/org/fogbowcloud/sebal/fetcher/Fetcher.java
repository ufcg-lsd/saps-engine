package org.fogbowcloud.sebal.fetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.NASARepository;
import org.fogbowcloud.sebal.crawler.Crawler;

public class Fetcher {
	
	private Properties properties;
	public ImageDataStore imageStore;
	int maxSimultaneousDownload;
	public Map<String, ImageData> pendingImageDownload = new HashMap<String, ImageData>();
	private ScheduledExecutorService executor;
	private String remoteRepositoryIP;
	
	private ExecutorService downloader = Executors.newFixedThreadPool(5);
	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes
	private static final int DEFAULT_MAX_SIMULTANEOUS_DOWNLOAD = 1;
	
	public static final Logger LOGGER = Logger.getLogger(Fetcher.class);
	
	public Fetcher(Properties properties, ImageDataStore imageStore,
			ScheduledExecutorService executor, String remoteRepositoryIP) {
		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}
		this.properties = properties;
		this.imageStore = imageStore;
		this.remoteRepositoryIP = remoteRepositoryIP;
		if (executor == null) {
			this.executor = Executors.newScheduledThreadPool(1);
		} else {
			this.executor = executor;
		}

		String maxSimultaneousDownloadStr = properties
				.getProperty("max_simultaneous_download");
		maxSimultaneousDownload = (maxSimultaneousDownloadStr == null ? DEFAULT_MAX_SIMULTANEOUS_DOWNLOAD
				: Integer.parseInt(maxSimultaneousDownloadStr));
	}
	
	public void init() {
		LOGGER.info("Initializing fetcher... ");
		
		// scheduling possible previous image download not finished before
		// process stopping
		try {
			schedulePreviousDownloadsNotFinished();
		} catch (SQLException e) {
			LOGGER.error("Error while scheduling previous downloads not finished.", e);
		}
		
		String schedulerPeriodStr = properties.getProperty("scheduler_period");
		long schedulerPeriod = (schedulerPeriodStr == null ? DEFAULT_SCHEDULER_PERIOD : Long
				.parseLong(schedulerPeriodStr));

		LOGGER.debug("scheduler period: " + schedulerPeriod);
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					if (pendingImageDownload.size() >= maxSimultaneousDownload) {
						LOGGER.debug("Already downloading " + pendingImageDownload.size()
								+ "images and max allowed is " + maxSimultaneousDownload);
						return;
					}

					ImageData imageData = selectImageToDownload();
					
					if (imageData == null) {
						LOGGER.debug("There is not image to download.");
						return;
					}
					
					downloadImage(imageData, remoteRepositoryIP);
				} catch (Throwable e) {
					LOGGER.error("Failed while download task.", e);
				}
			}

		}, 0, schedulerPeriod, TimeUnit.MILLISECONDS);
	}

	private void schedulePreviousDownloadsNotFinished() throws SQLException {
		List<ImageData> previousImagesDownloads = imageStore.getImageIn(ImageState.FETCHER_DOWNLOADING);
		for (ImageData imageData : previousImagesDownloads) {
			if (imageData.getFederationMember().equals(properties.getProperty("federation_member"))) {
				LOGGER.debug("The image " + imageData.getName()
						+ " is a previous download not finished.");
				pendingImageDownload.put(imageData.getName(), imageData);
				downloadImage(imageData, remoteRepositoryIP);
			}
		}
	}
	
	private ImageData selectImageToDownload() throws SQLException {
		LOGGER.debug("Searching for image to download.");
		List<ImageData> imageDataList = imageStore.getImageIn(ImageState.FINISHED,
				10);
		
		for (int i = 0; i < imageDataList.size(); i++) {
			ImageData imageData = imageDataList.get(i);
			
			if (imageStore.lockImage(imageData.getName())) {
				imageData.setState(ImageState.FETCHER_DOWNLOADING);
				imageData.setFederationMember(properties.getProperty("federation_member"));
				
				pendingImageDownload.put(imageData.getName(), imageData);
				imageStore.updateImage(imageData);
				
				imageStore.unlockImage(imageData.getName());
				return imageData;
			}
		}
		return null;
	}
	
	private void downloadImage(final ImageData imageData, final String remoteRepositoryIP) {
		downloader.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					imageData.setRemoteRepositoryIP(remoteRepositoryIP);
					downloadResultsInRepository(imageData, remoteRepositoryIP);

					//running Fmask					
					int exitValue = runFmask(imageData);					
					if (exitValue != 0) {
						LOGGER.error("It was not possible run Fmask for image " + imageData.getName());
//						removeFromPendingAndUpdateState(imageData);
//						return;
						imageData.setFederationMember(ImageDataStore.NONE);
					}
					
					imageData.setState(ImageState.DOWNLOADED);
					imageStore.updateImage(imageData);					
					pendingImageDownload.remove(imageData.getName());					
				} catch (Exception e) {
					LOGGER.error("Couldn't download image " + imageData.getName() + ".", e);
					removeFromPendingAndUpdateState(imageData);
				}
			}

			private void removeFromPendingAndUpdateState(final ImageData imageData) {
				pendingImageDownload.remove(imageData.getName());
				try {
					imageData.setFederationMember(ImageDataStore.NONE);
					imageData.setState(ImageState.NOT_DOWNLOADED);
					imageStore.updateImage(imageData);
				} catch (SQLException e1) {
					Crawler.LOGGER.error("Error while updating image data.", e1);
				}
			}

			private int runFmask(final ImageData imageData) throws IOException,
					FileNotFoundException, InterruptedException {
				File tempFile = File.createTempFile("temp-" + imageData.getName(), ".sh");
				FileOutputStream fos = new FileOutputStream(tempFile);

				FileInputStream fis = new FileInputStream(properties.getProperty("fmask_script_path"));
				String origExec = IOUtils.toString(fis);

				IOUtils.write(replaceVariables(origExec, imageData), fos);
				fos.close();
				
				ProcessBuilder builder = new ProcessBuilder("chmod", "+x", tempFile.getAbsolutePath());
				Process p = builder.start();
				p.waitFor();
				
				if (p.exitValue() != 0) {
					LOGGER.error("Error while running chmod +x command. Message=" + getError(p));
				} 
				LOGGER.debug("chmod +x command output=" + getOutput(p));

				builder = new ProcessBuilder("bash", tempFile.getAbsolutePath());
				p = builder.start();
				p.waitFor();
				
				if (p.exitValue() != 0) {
					LOGGER.error("Error while running fmask command. Message=" + getError(p));
				} 
				LOGGER.debug("run-fmask command output=" + getOutput(p));
				
				return p.exitValue();
			}

			private String getOutput(Process p) throws IOException {
				BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		        String out = new String();
		        while (true) {
		            String line = r.readLine();
		            if (line == null) { break; }
		            out += line;
		        }
		        return out;
			}

			private String getError(Process p) throws IOException {
				BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		        String error = new String();
		        while (true) {
		            String line = r.readLine();
		            if (line == null) { break; }
		            error += line;
		        }     
				return error;
			}
		});
	}
	
	public void downloadResultsInRepository(final ImageData imageData, String remoteRepositoryIP) throws Exception {
		HttpClient httpClient = initClient();
		HttpGet homeGet = new HttpGet(imageData.getDownloadLink());
		HttpResponse response = httpClient.execute(homeGet);

		String imageDirPath = remoteRepositoryIP + ":" + properties.getProperty("result_repository_path") + "/"
				+ imageData.getName();
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
	
	protected String replaceVariables(String command, ImageData imageData) {
		command = command.replaceAll(Pattern.quote("${IMAGE_NAME}"), imageData.getName());
		command = command.replaceAll(Pattern.quote("${IMAGES_MOUNT_POINT}"),
				properties.getProperty("image_repository"));
		command = command.replaceAll(Pattern.quote("${FMASK_TOOL}"),
				properties.getProperty("fmask_tool_path"));
		return command;
	}
}
