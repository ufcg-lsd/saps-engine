package org.fogbowcloud.sebal.fetcher;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.FTPUtils;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;

public class Fetcher {
	
	private Properties properties;
	public ImageDataStore imageStore;
	int maxSimultaneousDownload;
	public Map<String, ImageData> pendingImageDownload = new HashMap<String, ImageData>();
	private static int allowedImagesToFetch;
	
	private String ftpServerIP;
	private String ftpServerPort;
	
	private ExecutorService downloader = Executors.newFixedThreadPool(5);
	private static final double DEFAULT_IMAGE_DIR_SIZE = 382304413.2864;
	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes
	private static final int DEFAULT_MAX_SIMULTANEOUS_DOWNLOAD = 1;
	
	public static final Logger LOGGER = Logger.getLogger(Fetcher.class);
	
	public Fetcher(Properties properties, String imageStoreIP,
			String imageStorePort, String ftpServerIP, String ftpServerPort) {
		this(properties, new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort), null, ftpServerIP, ftpServerPort);
	}
	
	public Fetcher(Properties properties, ImageDataStore imageStore,
			ScheduledExecutorService executor, String ftpServerIP, String ftpServerPort) {
		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}
		this.properties = properties;
		this.imageStore = imageStore;
		this.ftpServerIP = ftpServerIP;
		this.ftpServerPort = ftpServerPort;
		this.allowedImagesToFetch = 0;

		String maxSimultaneousDownloadStr = properties
				.getProperty("max_simultaneous_download");
		maxSimultaneousDownload = (maxSimultaneousDownloadStr == null ? DEFAULT_MAX_SIMULTANEOUS_DOWNLOAD
				: Integer.parseInt(maxSimultaneousDownloadStr));
	}
	
	public void init() {
		
		LOGGER.info("Initializing fetcher... ");
		
		try {
			int imagesFetched = 0;
			List<ImageData> setOfImageData = null;

			do {
				setOfImageData = imageStore.getAllImages();
				
				numberOfImagesToDownload();

				if (allowedImagesToFetch != 0) {
					ImageData imageData = selectImageToFetch();

					if (imageData == null) {
						LOGGER.debug("There is no image to download.");
						return;
					}

					fetchImage(imageData);
					setOfImageData = imageStore.getAllImages();
					imagesFetched++;
				} else
					Thread.sleep(DEFAULT_SCHEDULER_PERIOD);

			} while (existImagesNotFetched(setOfImageData) || imagesFetched == 0);
		} catch (Throwable e) {
			LOGGER.error("Failed while download task.", e);
		}

		LOGGER.debug("All images fetched.\nProcess finished.");
		
	}
	
	private void numberOfImagesToDownload() {
		double fetcherVolumeSize = Double.parseDouble(properties
				.getProperty("fetcher_volume_size"));

		double availableSize = 0;
		double exportDirSize = 0;
		File exportDir = new File(properties.getProperty("sebal_exports_path"));

		if (!(exportDir.exists() && exportDir.isDirectory())) {
			LOGGER.error("This directory doesn't exist or is not valid!");
			return;
		}

		exportDirSize = folderSize(exportDir);

		availableSize = fetcherVolumeSize - exportDirSize;
		
		double numberOfImagesToDownload = availableSize/DEFAULT_IMAGE_DIR_SIZE;
		
		this.allowedImagesToFetch = (int) numberOfImagesToDownload;
	}
	
	private boolean existImagesNotFetched(List<ImageData> setOfImageData) {
		int stateCount = 0;
		
		for(ImageData imageData : setOfImageData) {			
			if(imageData.getState().equals(ImageState.FINISHED))
				stateCount++;
		}
		
		if(stateCount != 0) {
			return true;
		} else
			return false;
	}
	
	public static double folderSize(File directory) {
	    double length = 0;
	    for (File file : directory.listFiles()) {
	        if (file.isFile())
	            length += file.length();
	        else
	            length += folderSize(file);
	    }
	    return length;
	}
	
	private ImageData selectImageToFetch() throws SQLException {
		LOGGER.debug("Searching for image to download.");
		List<ImageData> imageDataList = imageStore.getIn(ImageState.FINISHED,
				allowedImagesToFetch);
		
		for (int i = 0; i < imageDataList.size(); i++) {
			ImageData imageData = imageDataList.get(i);
			
			if (imageStore.lockImage(imageData.getName())) {
				imageData.setState(ImageState.FETCHING);
				imageData.setFederationMember(properties.getProperty("federation_member"));
				
				pendingImageDownload.put(imageData.getName(), imageData);
				imageStore.updateImage(imageData);
				
				imageStore.unlockImage(imageData.getName());
				return imageData;
			}
		}
		return null;
	}
	
	private void fetchImage(final ImageData imageData) {
		downloader.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					fetchResultsInStorage(imageData);

					imageData.setState(ImageState.FETCHED);
					imageStore.updateImage(imageData);					
					pendingImageDownload.remove(imageData.getName());					
				} catch (Exception e) {
					LOGGER.error("Couldn't fetch image " + imageData.getName() + ".", e);
					removeFromPendingAndUpdateState(imageData);
				}
			}

			private void removeFromPendingAndUpdateState(final ImageData imageData) {
				pendingImageDownload.remove(imageData.getName());
				try {
					imageData.setFederationMember(ImageDataStore.NONE);
					imageData.setState(ImageState.FINISHED);
					imageStore.updateImage(imageData);
				} catch (SQLException e1) {
					Fetcher.LOGGER.error("Error while updating image data.", e1);
				}
			}
			
		});
	}
	
	// TODO: See if this is correct
	public void fetchResultsInStorage(final ImageData imageData) throws Exception {
		
		FTPUtils ftpUtils = new FTPUtils(properties, ftpServerIP, ftpServerPort);
		
		ftpUtils.init(imageData);
		
		/*String resultsDirPath = properties.getProperty("sebal_export_path")
				+ "/results/" + imageData.getName();
		File resultsDir = new File(resultsDirPath);
		if (!resultsDir.exists() || !resultsDir.isDirectory()) {
			resultsDir.mkdirs();
		}
		
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(resultsDir);
		builder.command("wget -r ftp://"
				+ properties.getProperty("ftp_server_user") + ":"
				+ properties.getProperty("ftp_user_pass") + "@" + ftpServerIP
				+ "/results/" + imageData.getName());
		
		LOGGER.info("Fetching image " + imageData.getName() + " into volume.");
		Process p = builder.start();		
		p.waitFor();
		
		LOGGER.info("Image " + imageData.getName() + " fetched into volume.");*/
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
