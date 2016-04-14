package org.fogbowcloud.sebal.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
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
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;

public class Crawler {

	public Properties properties;
	public ImageDataStore imageStore;
	int maxSimultaneousDownload;
	public Map<String, ImageData> pendingImageDownload = new HashMap<String, ImageData>();
	private ScheduledExecutorService executor;
	private NASARepository NASARepository;

	private ExecutorService downloader = Executors.newFixedThreadPool(5);
	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes
	private static final int DEFAULT_MAX_SIMULTANEOUS_DOWNLOAD = 1;

	public static final Logger LOGGER = Logger.getLogger(Crawler.class);

	public Crawler(Properties properties, String imageStoreIP,
			String imageStorePort) {
		this(properties, new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort), null, imageStoreIP, imageStorePort);
	}

	public Crawler(Properties properties, ImageDataStore imageStore,
			ScheduledExecutorService executor, String imageStoreIP,
			String imageStorePort) {
		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}
		this.properties = properties;
		this.imageStore = imageStore;
		this.NASARepository = new NASARepository(properties);
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
		LOGGER.info("Initializing crawler... ");
		
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
					
					downloadImage(imageData);
				} catch (Throwable e) {
					LOGGER.error("Failed while download task.", e);
				}
			}

		}, 0, schedulerPeriod, TimeUnit.MILLISECONDS);
	}

	private void schedulePreviousDownloadsNotFinished() throws SQLException {
		List<ImageData> previousImagesDownloads = imageStore.getIn(ImageState.DOWNLOADING);
		for (ImageData imageData : previousImagesDownloads) {
			if (imageData.getFederationMember().equals(properties.getProperty("federation_member"))) {
				LOGGER.debug("The image " + imageData.getName()
						+ " is a previous download not finished.");
				pendingImageDownload.put(imageData.getName(), imageData);
				downloadImage(imageData);
			}
		}
	}

	private ImageData selectImageToDownload() throws SQLException {
		LOGGER.debug("Searching for image to download.");
		List<ImageData> imageDataList = imageStore.getIn(ImageState.NOT_DOWNLOADED,
				10);
		
		for (int i = 0; i < imageDataList.size(); i++) {
			ImageData imageData = imageDataList.get(i);
			
			if (imageStore.lockImage(imageData.getName())) {
				imageData.setState(ImageState.DOWNLOADING);
				imageData.setFederationMember(properties.getProperty("federation_member"));
				
				pendingImageDownload.put(imageData.getName(), imageData);
				imageStore.updateImage(imageData);
				
				imageStore.unlockImage(imageData.getName());
				return imageData;
			}
		}
		return null;
	}
	
	private void downloadImage(final ImageData imageData) {
		downloader.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					NASARepository.downloadImage(imageData);

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

	protected String replaceVariables(String command, ImageData imageData) {
		command = command.replaceAll(Pattern.quote("${IMAGE_NAME}"), imageData.getName());
		command = command.replaceAll(Pattern.quote("${IMAGES_MOUNT_POINT}"),
				properties.getProperty("image_repository"));
		command = command.replaceAll(Pattern.quote("${FMASK_TOOL}"),
				properties.getProperty("fmask_tool_path"));
		return command;
	}

	/*
	 * This method must be used only for testing
	 */
	protected void setNASARepository(NASARepository nasaRepository) {
		this.NASARepository = nasaRepository;
		
	}
	
	/*
	 * This method must be used only for testing
	 */
	protected void setImageDownloader(ExecutorService imageDownloader) {
		this.downloader = imageDownloader;
		
	}

}