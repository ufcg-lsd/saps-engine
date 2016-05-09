package org.fogbowcloud.sebal.crawler;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.FmaskUtils;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Crawler {

	//FIXME: code pending again - 1
	//FIXME: FTP - 0 (In infrastructure creation?)
	//FIXME: test - 1
	private final Properties properties;
	private NASARepository NASARepository;
	private final ImageDataStore imageStore;
	private static int maxSimultaneousDownload;
	private final DB pendingImageDownloadDB = DBMaker.newMemoryDB().make();
	private ConcurrentMap<String, ImageData> pendingImageDownloadMap = pendingImageDownloadDB.createHashMap("map").make();

	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes
	private static final long DEFAULT_IMAGE_DIR_SIZE = 356048730;

	public static final Logger LOGGER = Logger.getLogger(Crawler.class);

	public Crawler(Properties properties, String imageStoreIP,
			String imageStorePort) {

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}
		
		String maxSimultaneousDownloadStr = properties.getProperty("max_simultaneous_download");
		maxSimultaneousDownload = (maxSimultaneousDownloadStr == null ? 1
				: Integer.parseInt(maxSimultaneousDownloadStr));

		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort);
		this.NASARepository = new NASARepository(properties);
	}

	public void exec() throws InterruptedException, IOException {

		LOGGER.info("Initializing crawler... ");

		try {
			do {
				schedulePreviousDownloadsNotFinished();				
				deleteFetchedResultsFromVolume(properties);
				
				if (pendingImageDownloadMap.size() >= maxSimultaneousDownload) {
					LOGGER.debug("Already downloading " + pendingImageDownloadMap.size()
							+ "images and max allowed is " + maxSimultaneousDownload);
					return;
				}

				long numToDownload = numberOfImagesToDownload();
				if (numToDownload > 0) {
					download(numToDownload);
				} else {
					Thread.sleep(DEFAULT_SCHEDULER_PERIOD);
				}

			} while (thereIsImageToDownload());
		} catch (Throwable e) {
			LOGGER.error("Failed while download task.", e);
		}

		LOGGER.debug("All images downloaded.\nProcess finished.");
	}
	
	private void schedulePreviousDownloadsNotFinished() throws SQLException {
		List<ImageData> previousDownloads = imageStore.getIn(ImageState.DOWNLOADING);
		for (ImageData imageData : previousDownloads) {
			if (imageData.getFederationMember().equals(properties.getProperty("federation_member"))) {
				LOGGER.debug("The image " + imageData.getName()
						+ " is a previous download not finished.");
				pendingImageDownloadMap.put(imageData.getName(), imageData);
				downloadImage(imageData);
			}
		}
	}

	private boolean thereIsImageToDownload() throws SQLException {
		List<ImageData> imageData = imageStore.getIn(ImageState.NOT_DOWNLOADED);
		return !imageData.isEmpty();
	}

	private void download(long maxImagesToDownload) throws SQLException {

		// FIXME: check the implications of this cast
		List<ImageData> imageDataList = imageStore.getIn(
				ImageState.NOT_DOWNLOADED, (int) maxImagesToDownload);

		for (ImageData imageData : imageDataList) {
			if (imageData != null) {
				// FIXME: not good block
				while (imageStore.lockImage(imageData.getName())) {
					imageData.setState(ImageState.DOWNLOADING);
					imageData.setFederationMember(properties
							.getProperty("federation_member"));					
					pendingImageDownloadMap.put(imageData.getName(), imageData);
					imageStore.updateImage(imageData);
					imageStore.unlockImage(imageData.getName());

					downloadImage(imageData);
				}
			}
		}
	}

	private long numberOfImagesToDownload() {
		String volumeDirPath = properties.getProperty("sebal_export_path");
		File volumePath = new File(volumeDirPath);
		if (volumePath.exists() && volumePath.isDirectory()) {
			long availableVolumeSpace = volumePath.getFreeSpace();
			long numberOfImagesToDownload = availableVolumeSpace / DEFAULT_IMAGE_DIR_SIZE;

			return numberOfImagesToDownload / FileUtils.ONE_GB;
		} else {
			throw new RuntimeException("VolumePath: " + volumeDirPath
					+ " is not a directory or does not exist");
		}
	}

	private void downloadImage(final ImageData imageData) {
		try {
			// FIXME: it blocks?
			NASARepository.downloadImage(imageData);

			// running Fmask
			int exitValue = new FmaskUtils().runFmask(imageData, properties);
			if (exitValue != 0) {
				LOGGER.error("It was not possible run Fmask for image "
						+ imageData.getName());
				imageData.setFederationMember(ImageDataStore.NONE);
			}

			imageData.setState(ImageState.DOWNLOADED);
			imageStore.updateImage(imageData);
			pendingImageDownloadMap.remove(imageData.getName());
		} catch (Exception e) {
			LOGGER.error(
					"Couldn't download image " + imageData.getName() + ".", e);
			removeFromPendingAndUpdateState(imageData);
		}
	}
	
	private void removeFromPendingAndUpdateState(final ImageData imageData) {
		pendingImageDownloadMap.remove(imageData.getName());
		try {
			imageData.setFederationMember(ImageDataStore.NONE);
			imageData.setState(ImageState.NOT_DOWNLOADED);
			imageStore.updateImage(imageData);
		} catch (SQLException e1) {
			Crawler.LOGGER.error("Error while updating image data.", e1);
		}
	}

	private void deleteFetchedResultsFromVolume(Properties properties) throws IOException,
			InterruptedException, SQLException {
		
		List<ImageData> setOfImageData = imageStore.getAllImages();

		String exportPath = properties.getProperty("sebal_export_path");
				
		for (ImageData imageData : setOfImageData) {
			if (!exportPath.isEmpty() && exportPath != null) {
				String imageDirPath = exportPath + "/results/"
						+ imageData.getName();
				File imageDir = new File(imageDirPath);

				if (!imageDir.exists() || !imageDir.isDirectory()) {
					LOGGER.debug("This file does not exist!");
					return;
				}
				FileUtils.deleteDirectory(imageDir);
			} else {
				// FIXME: Implement solution for this 
				LOGGER.error("Volume directory path is null or empty!");
			}				
		}

		LOGGER.debug("Image files deleted successfully.");
	}

}