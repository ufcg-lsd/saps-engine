package org.fogbowcloud.sebal.crawler;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.FMask;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Crawler {

	// FIXME: FTP - 0 (In infrastructure creation?)
	private final Properties properties;
	private NASARepository NASARepository;
	private final ImageDataStore imageStore;
	private File pendingImageDownloadFile;
	private DB pendingImageDownloadDB;
	private ConcurrentMap<String, ImageData> pendingImageDownloadMap;
	private String federationMember;

	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes
	// Image dir size in bytes
	private static final long DEFAULT_IMAGE_DIR_SIZE = 356 * FileUtils.ONE_MB;

	public static final Logger LOGGER = Logger.getLogger(Crawler.class);

	public Crawler(Properties properties, String imageStoreIP,
			String imageStorePort, String federationMember) {

		LOGGER.info("Creating crawler");
		LOGGER.info("Imagestore " + imageStoreIP + ":" + imageStorePort
				+ " federationmember " + federationMember);

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}

		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort);
		this.NASARepository = new NASARepository(properties);

		this.federationMember = federationMember;

		if (federationMember == null) {
			throw new IllegalArgumentException(
					"Federation member arg must not be null.");
		}

		if (federationMember.isEmpty()) {
			throw new IllegalArgumentException(
					"Federation member arg must not be empty.");
		}

		this.pendingImageDownloadFile = new File("pending-image-download.db");
		this.pendingImageDownloadDB = DBMaker.newFileDB(
				pendingImageDownloadFile).make();

		if (!pendingImageDownloadFile.exists()
				|| !pendingImageDownloadFile.isFile()) {
			LOGGER.info("Creating map of pending images to download");
			this.pendingImageDownloadMap = pendingImageDownloadDB
					.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending images to download");
			this.pendingImageDownloadMap = pendingImageDownloadDB
					.getHashMap("map");
		}
	}

	public void exec() throws InterruptedException, IOException {

		cleanUnfinishedDownloadedData(properties);

		try {
			do {
				purgeImagesFromVolume(properties);
				deleteFetchedResultsFromVolume(properties);

				long numToDownload = numberOfImagesToDownload();
				if (numToDownload > 0) {
					download(numToDownload);
				} else {
					Thread.sleep(DEFAULT_SCHEDULER_PERIOD);
				}

			} while (thereIsImageToDownload());
			LOGGER.info("All images downloaded");
		} catch (Throwable e) {
			LOGGER.error("Failed while downloading task.", e);
		} finally {
			pendingImageDownloadDB.close();
		}
	}

	private void cleanUnfinishedDownloadedData(Properties properties)
			throws IOException {

		LOGGER.info("Starting garbage collector");
		Collection<ImageData> data = pendingImageDownloadMap.values();
		for (ImageData imageData : data) {
			removeFromPendingAndUpdateState(imageData, properties);
		}
		LOGGER.info("Garbage collect finished");
	}

	private boolean thereIsImageToDownload() throws SQLException {

		List<ImageData> imageData = imageStore.getIn(ImageState.NOT_DOWNLOADED);
		return !imageData.isEmpty();
	}

	protected void download(long maxImagesToDownload) throws SQLException,
			IOException {

		LOGGER.info("maxImagesToDownload " + maxImagesToDownload);

		// FIXME: check the implications of this cast
		// This updates images in NOT_DOWNLOADED state to DOWNLOADING
		// and sets this federation member as owner, and then gets all images
		// marked as DOWNLOADING

		List<ImageData> imageDataList = imageStore.getImagesToDownload(
				federationMember, (int) maxImagesToDownload);

		for (ImageData imageData : imageDataList) {
			if (imageData != null) {
				// Updating pending dataBase and imageData
				LOGGER.debug("Adding image " + imageData.getName()
						+ " to pending database");
				pendingImageDownloadMap.put(imageData.getName(), imageData);
				pendingImageDownloadDB.commit();

				downloadImage(imageData);
			}
		}

		LOGGER.info("Download finished");
	}

	protected long numberOfImagesToDownload() {

		String volumeDirPath = properties.getProperty("sebal_export_path");
		File volumePath = getExportDirPath(volumeDirPath);
		if (volumePath.exists() && volumePath.isDirectory()) {
			long availableVolumeSpace = volumePath.getFreeSpace();
			long numberOfImagesToDownload = availableVolumeSpace
					/ DEFAULT_IMAGE_DIR_SIZE;

			// FIXME: ceil para o inteiro inferior

			LOGGER.debug("volumePath " + volumePath + "availableVolumeSpace "
					+ availableVolumeSpace + " DEFAULT_IMAGE_DIR_SIZE "
					+ DEFAULT_IMAGE_DIR_SIZE + " numberOfImagesToDownload "
					+ numberOfImagesToDownload);
			return numberOfImagesToDownload;
		} else {
			throw new RuntimeException("VolumePath: " + volumeDirPath
					+ " is not a directory or does not exist");
		}
	}

	protected File getExportDirPath(String volumeDirPath) {
		return new File(volumeDirPath);
	}

	private void downloadImage(final ImageData imageData) throws IOException {
		
		try {
			// FIXME: it blocks?
			NASARepository.downloadImage(imageData);

			// running Fmask
			// TODO: insert source .profile before fmask execution
			LOGGER.debug("Running Fmask for image " + imageData.getName());

			int exitValue = new FMask().runFmask(imageData, properties);
			if (exitValue != 0) {
				LOGGER.error("It was not possible run Fmask for image "
						+ imageData.getName());
				imageData.setFederationMember(ImageDataStore.NONE);
			}

			// FIXME: should we continue if fmask terminates in error?

			imageData.setState(ImageState.DOWNLOADED);
			imageData
					.setCreationTime(String.valueOf(System.currentTimeMillis()));
			imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
			imageStore.updateImage(imageData);
			pendingImageDownloadMap.remove(imageData.getName());

			LOGGER.info("Image " + imageData + " was downloaded");

		} catch (Exception e) {
			LOGGER.error("Error when downloading image " + imageData, e);
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	private void removeFromPendingAndUpdateState(final ImageData imageData,
			Properties properties) throws IOException {
		
		// FIXME: test exceptions
		try {

			if (imageData.getFederationMember().equals(federationMember)) {

				LOGGER.debug("Rolling back " + imageData + " to "
						+ ImageState.NOT_DOWNLOADED);
				imageData.setFederationMember(ImageDataStore.NONE);
				imageData.setState(ImageState.NOT_DOWNLOADED);
				imageData.setUpdateTime(String.valueOf(System
						.currentTimeMillis()));
				imageStore.updateImage(imageData);

				deleteImageFromDisk(imageData, properties);

				LOGGER.debug("Removing image " + imageData
						+ " from pending image map");
				pendingImageDownloadMap.remove(imageData.getName());
				LOGGER.info("Image " + imageData + " rolled back");
			}

		} catch (SQLException e1) {
			Crawler.LOGGER.error("Error while updating image data: "
					+ imageData.getName(), e1);
		}
	}

	private void deleteImageFromDisk(final ImageData imageData,
			Properties properties) throws IOException {
		
		String exportPath = properties.getProperty("sebal_export_path");
		String imageDirPath = exportPath + "/images/" + imageData.getName();
		File imageDir = new File(imageDirPath);

		LOGGER.info("Removing image " + imageData + " data under path "
				+ imageDirPath);

		if (!imageDir.exists() || !imageDir.isDirectory()) {
			LOGGER.info("path " + imageDirPath + " does not exist");
			return;
		}

		FileUtils.deleteDirectory(imageDir);
	}

	private void deleteFetchedResultsFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {

		List<ImageData> setOfImageData = imageStore.getAllImages();

		String exportPath = properties.getProperty("sebal_export_path");

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageData imageData : setOfImageData) {
				if (imageData.getState().equals(ImageState.FETCHED)) {
					LOGGER.debug("Image " + imageData.getName() + " fetched");
					LOGGER.info("Removing" + imageData);

					// TODO: review this
					try {
						deleteResultsFromDisk(imageData, exportPath);
					} catch (IOException e) {
						LOGGER.error("Error while deleting " + imageData, e);
					}

					LOGGER.debug("Image " + imageData + " result files deleted");
				} else {
					continue;
				}
			}
		} else {
			// FIXME: Implement solution for this
			LOGGER.error("Volume directory path is null or empty");
		}
	}

	private void deleteResultsFromDisk(ImageData imageData, String exportPath)
			throws IOException {

		String resultsDirPath = exportPath + "/results/" + imageData.getName();
		File resultsDir = new File(resultsDirPath);

		if (!resultsDir.exists() || !resultsDir.isDirectory()) {
			LOGGER.info("This file does not exist!");
			return;
		}

		LOGGER.debug("Deleting image " + imageData + " from " + resultsDirPath);
		FileUtils.deleteDirectory(resultsDir);
	}

	private void purgeImagesFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {
		
		LOGGER.info("Starting purge");

		List<ImageData> imagesToPurge = imageStore.getIn(ImageState.FINISHED);

		String exportPath = properties.getProperty("sebal_export_path");

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageData imageData : imagesToPurge) {
				if (imageData.getImageStatus().equals(ImageData.PURGED)
						&& imageData.getFederationMember().equals(
								federationMember)) {
					LOGGER.debug("Purging image " + imageData);

					try {
						deleteImageFromDisk(imageData, properties);
						deleteResultsFromDisk(imageData, exportPath);
					} catch (IOException e) {
						LOGGER.error("Error while deleting " + imageData, e);
					}

					LOGGER.debug("Image " + imageData + " purged");
				}
			}
		} else {
			// FIXME: Implement solution for this
			LOGGER.error("Volume directory path is null or empty!");
		}
	}

}
