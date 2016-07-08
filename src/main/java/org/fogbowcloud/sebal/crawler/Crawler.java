package org.fogbowcloud.sebal.crawler;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
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

	//FIXME: replace sebal export path constant in other places
	protected static final String SEBAL_EXPORT_PATH = "sebal_export_path";
	protected static final String FMASK_TOOL_PATH = "fmask_tool_path";
	protected static final String FMASK_SCRIPT_PATH = "fmask_script_path";
	private final Properties properties;
	private NASARepository nasaRepository;
	private final ImageDataStore imageStore;
	private File pendingImageDownloadFile;
	protected DB pendingImageDownloadDB;
	protected ConcurrentMap<String, ImageData> pendingImageDownloadMap;
	private String federationMember;
	private FMask fmask;

	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes
	// Image dir size in bytes
	private static final long DEFAULT_IMAGE_DIR_SIZE = 356 * FileUtils.ONE_MB;

	public static final Logger LOGGER = Logger.getLogger(Crawler.class);

	public Crawler(Properties properties, String imageStoreIP,
			String imageStorePort, String federationMember) {
		
		this(properties, new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort), new NASARepository(properties),
				federationMember, new FMask());
		
		LOGGER.info("Creating crawler");
		LOGGER.info("Imagestore " + imageStoreIP + ":" + imageStorePort
				+ " federationmember " + federationMember);
	}
	
	protected Crawler(Properties properties, ImageDataStore imageStore,
			NASARepository nasaRepository, String federationMember,
			FMask fmask) {

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}
		
		if(imageStore == null) {
			throw new IllegalArgumentException(
					"Imagestore arg must not be null.");
		}
		
		if(nasaRepository == null) {
			throw new IllegalArgumentException(
					"NASARepository arg must not be null.");
		}
		
		if (federationMember == null) {
			throw new IllegalArgumentException(
					"Federation member arg must not be null.");
		}

		if (federationMember.isEmpty()) {
			throw new IllegalArgumentException(
					"Federation member arg must not be empty.");
		}
		
		if (fmask == null) {
			throw new IllegalArgumentException(
					"Fmask arg must not be empty.");
		}

		this.properties = properties;
		this.imageStore = imageStore;
		this.nasaRepository = nasaRepository;
		this.federationMember = federationMember;
		this.fmask = fmask;
		
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

		try {
			while(true) {
				cleanUnfinishedDownloadedData(properties);
				purgeImagesFromVolume(properties);
				deleteFetchedResultsFromVolume(properties);

				long numToDownload = numberOfImagesToDownload();				
				if (numToDownload > 0) {
					download(numToDownload);
				} else {
					Thread.sleep(DEFAULT_SCHEDULER_PERIOD);
				}
			}
		} catch (Throwable e) {
			LOGGER.error("Failed while downloading task.", e);
		} finally {
			pendingImageDownloadDB.close();
		}
	}

	protected void cleanUnfinishedDownloadedData(Properties properties)
			throws IOException {

		LOGGER.info("Starting garbage collector");
		Collection<ImageData> data = pendingImageDownloadMap.values();
		for (ImageData imageData : data) {
			removeFromPendingAndUpdateState(imageData, properties);
		}
		LOGGER.info("Garbage collect finished");
	}

	protected void download(long maxImagesToDownload) throws SQLException,
			IOException {

		LOGGER.info("maxImagesToDownload " + maxImagesToDownload);

		List<ImageData> imageDataList = new ArrayList<ImageData>();

		try {
			// FIXME: check the implications of this cast
			// This updates images in NOT_DOWNLOADED state to DOWNLOADING
			// and sets this federation member as owner, and then gets all images
			// marked as DOWNLOADING
			imageDataList = imageStore.getImagesToDownload(federationMember,
					(int) maxImagesToDownload);
		} catch (SQLException e) {
			// TODO: deal with this better
			LOGGER.error("Error while accessing not downloaded images in DB");
		}

		for (ImageData imageData : imageDataList) {
			if (imageData != null) {
				try {
					imageStore.addStateStamp(imageData.getName(),
							imageData.getState(), imageData.getUpdateTime());
				} catch (SQLException e) {
					LOGGER.error("Error while adding state "
							+ imageData.getState() + " timestamp "
							+ imageData.getUpdateTime() + " in DB");
					continue;
				}
				LOGGER.debug("Adding image " + imageData.getName()
						+ " to pending database");
				pendingImageDownloadMap.put(imageData.getName(), imageData);
				pendingImageDownloadDB.commit();

				downloadImage(imageData);
			}
		}

		LOGGER.info("Download finished");
	}

	protected long numberOfImagesToDownload() throws NumberFormatException, InterruptedException, IOException, SQLException {
		
		String volumeDirPath = properties.getProperty(SEBAL_EXPORT_PATH);
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

	protected void downloadImage(final ImageData imageData) throws SQLException, IOException {
		
		try {
			// FIXME: it blocks?
			nasaRepository.downloadImage(imageData);

			// running Fmask
			// TODO: insert source .profile before fmask execution
			LOGGER.debug("Running Fmask for image " + imageData.getName());

			int exitValue = fmask.runFmask(imageData,
					properties.getProperty(FMASK_SCRIPT_PATH),
					properties.getProperty(FMASK_TOOL_PATH),
					properties.getProperty(SEBAL_EXPORT_PATH));
			if (exitValue != 0) {
				LOGGER.error("It was not possible run Fmask for image "
						+ imageData);
				markImageWithErrorAndUpdateState(imageData, properties);
				return;
			}

			imageData.setState(ImageState.DOWNLOADED);
			Date updateTime = new Date(Calendar.getInstance().getTimeInMillis());
			
			imageData.setCreationTime(updateTime);
			imageData.setUpdateTime(updateTime);
			
			try {				
				imageStore.updateImage(imageData);
			} catch(SQLException e) {
				LOGGER.error("Error while updating image " + imageData + " to DB");
				removeFromPendingAndUpdateState(imageData, properties);
			}
			
			try {
				imageStore.addStateStamp(imageData.getName(),
						imageData.getState(), imageData.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while adding state " + imageData.getState()
						+ " timestamp " + imageData.getUpdateTime() + " in DB");
			}
			
			pendingImageDownloadMap.remove(imageData.getName());

			LOGGER.info("Image " + imageData + " was downloaded");

		} catch (Exception e) {
			LOGGER.error("Error when downloading image " + imageData, e);
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	private void markImageWithErrorAndUpdateState(ImageData imageData,
			Properties properties) throws IOException {
		
		try {
			if (imageData.getFederationMember().equals(federationMember)) {
				imageData.setState(ImageState.ERROR);
				imageData
						.setImageError("It was not possible run Fmask for image");
				imageData.setUpdateTime(new Date(Calendar.getInstance()
						.getTimeInMillis()));
				imageStore.updateImage(imageData);

				deleteImageFromDisk(imageData,
						properties.getProperty(SEBAL_EXPORT_PATH));

				LOGGER.debug("Removing image " + imageData
						+ " from pending image map");
				pendingImageDownloadMap.remove(imageData.getName());
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image data: "
					+ imageData.getName(), e);
			removeFromPendingAndUpdateState(imageData, properties);
		}		
	}

	private void removeFromPendingAndUpdateState(final ImageData imageData,
			Properties properties) throws IOException {

		if (imageData.getFederationMember().equals(federationMember)) {

			LOGGER.debug("Rolling back " + imageData + " to "
					+ ImageState.NOT_DOWNLOADED);

			try {
				imageStore.removeStateStamp(imageData.getName(),
						imageData.getState(), imageData.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while removing state "
						+ imageData.getState() + " timestamp "
						+ imageData.getUpdateTime() + " from DB");
			}

			imageData.setFederationMember(ImageDataStore.NONE);
			imageData.setState(ImageState.NOT_DOWNLOADED);
			imageData.setUpdateTime(new Date(Calendar.getInstance()
					.getTimeInMillis()));
			
			try {
				imageStore.updateImage(imageData);
			} catch (SQLException e) {
				Crawler.LOGGER.error("Error while updating image data: "
						+ imageData.getName(), e);
				imageData.setFederationMember(federationMember);
				imageData.setState(ImageState.DOWNLOADING);
			}

			deleteImageFromDisk(imageData,
					properties.getProperty(SEBAL_EXPORT_PATH));

			LOGGER.debug("Removing image " + imageData
					+ " from pending image map");
			pendingImageDownloadMap.remove(imageData.getName());
			LOGGER.info("Image " + imageData + " rolled back");
		}

	}

	protected void deleteImageFromDisk(final ImageData imageData,
			String exportPath) throws IOException {
		
		String imageDirPath = exportPath + "/images/" + imageData.getName();
		File imageDir = new File(imageDirPath);

		LOGGER.info("Removing image " + imageData + " data under path "
				+ imageDirPath);

		if(isImageOnDisk(imageDirPath, imageDir)) {
			FileUtils.deleteDirectory(imageDir);			
		}
	}

	protected boolean isImageOnDisk(String imageDirPath, File imageDir) {
		if (!imageDir.exists() || !imageDir.isDirectory()) {
			LOGGER.info("path " + imageDirPath + " does not exist");
			return false;
		}
		return true;
	}

	protected void deleteFetchedResultsFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {

		List<ImageData> setOfImageData = imageStore.getAllImages();

		String exportPath = properties.getProperty(SEBAL_EXPORT_PATH);

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageData imageData : setOfImageData) {
				if (imageData.getState().equals(ImageState.FETCHED)
						&& imageData.getFederationMember().equals(
								federationMember)) {
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

	protected void purgeImagesFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {
		
		LOGGER.info("Starting purge");

		List<ImageData> imagesToPurge = imageStore.getIn(ImageState.FINISHED);

		String exportPath = properties.getProperty(SEBAL_EXPORT_PATH);

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageData imageData : imagesToPurge) {
				if (imageData.getImageStatus().equals(ImageData.PURGED)
						&& imageData.getFederationMember().equals(
								federationMember)) {
					LOGGER.debug("Purging image " + imageData);

					try {
						deleteImageFromDisk(imageData, properties.getProperty(SEBAL_EXPORT_PATH));
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
