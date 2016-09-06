package org.fogbowcloud.sebal.engine.sebal.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.FMask;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.USGSNasaRepository;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Crawler {

	protected static final String SEBAL_EXPORT_PATH = "sebal_export_path";
	protected static final String FMASK_TOOL_PATH = "fmask_tool_path";
	protected static final String FMASK_SCRIPT_PATH = "fmask_script_path";
	private static final String FMASK_VERSION_FILE_PATH = "crawler/fmask-version";
	private final Properties properties;
	private USGSNasaRepository usgsRepository;
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
			String imageStorePort, String federationMember) throws SQLException {

		this(properties, new JDBCImageDataStore(properties), new USGSNasaRepository(properties),
				federationMember, new FMask());

		LOGGER.info("Creating crawler");
		LOGGER.info("Imagestore " + imageStoreIP + ":" + imageStorePort
				+ " federationmember " + federationMember);
	}

	protected Crawler(Properties properties, ImageDataStore imageStore,
					  USGSNasaRepository usgsRepository, String federationMember, FMask fmask) {

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}

		if (imageStore == null) {
			throw new IllegalArgumentException(
					"Imagestore arg must not be null.");
		}

		if (usgsRepository == null) {
			throw new IllegalArgumentException(
					"USGSRepository arg must not be null.");
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
			throw new IllegalArgumentException("Fmask arg must not be empty.");
		}

		this.properties = properties;
		this.imageStore = imageStore;
		this.usgsRepository = usgsRepository;
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
		cleanUnfinishedDownloadedData(properties);

		try {
			while (true) {
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
			LOGGER.error("Failed while downloading images", e);
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
			// This updates images in NOT_DOWNLOADED state to DOWNLOADING
			// and sets this federation member as owner, and then gets all
			// images
			// marked as DOWNLOADING
			imageDataList = imageStore.getImagesToDownload(federationMember,
					(int) maxImagesToDownload);
			
			// FIXME: see a way to deal with this problem better than this
			if(imageDataList.isEmpty()) {
				return;
			}
		} catch (SQLException e) {
			// TODO: deal with this better
			LOGGER.error("Error while accessing not downloaded images in DB", e);
		}

		for (ImageData imageData : imageDataList) {
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			
			if (imageData != null) {
				try {
					imageStore.addStateStamp(imageData.getName(),
							imageData.getState(), imageData.getUpdateTime());
				} catch (SQLException e) {
					LOGGER.error("Error while adding state "
							+ imageData.getState() + " timestamp "
							+ imageData.getUpdateTime() + " in DB", e);
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

	protected long numberOfImagesToDownload() throws NumberFormatException,
			InterruptedException, IOException, SQLException {

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

	protected void downloadImage(final ImageData imageData)
			throws SQLException, IOException {

		try {
			usgsRepository.downloadImage(imageData);

			// running Fmask
			// TODO: insert source .profile before fmask execution
			LOGGER.debug("Running Fmask for image " + imageData.getName());

			int exitValue = 0;
			
			try {
				exitValue = fmask.runFmask(imageData,
						properties.getProperty(FMASK_SCRIPT_PATH),
						properties.getProperty(FMASK_TOOL_PATH),
						properties.getProperty(SEBAL_EXPORT_PATH));
			} catch (Exception e) {
				LOGGER.error("Error while running Fmask", e);
			}
			
			if (exitValue != 0) {
				LOGGER.error("It was not possible run Fmask for image "
						+ imageData);
				markImageWithErrorAndUpdateState(imageData, properties);
				return;
			}			
			
			imageData.setState(ImageState.DOWNLOADED);
			
			String fmaskVersion = getFmaskVersion();
			String crawlerVersion = getCrawlerVersion();
			
			if(fmaskVersion == null || fmaskVersion.isEmpty()) {
				LOGGER.error("Fmask version file does not exist");
				LOGGER.info("Restart Crawler infrastructure");
				
				removeFromPendingAndUpdateState(imageData, properties);
				deleteImageFromDisk(imageData, properties.getProperty(SEBAL_EXPORT_PATH));
				System.exit(1);
			}
			
			if(crawlerVersion == null || crawlerVersion.isEmpty()) {
				LOGGER.error("Crawler version file does not exist");
				LOGGER.info("Restart Crawler infrastructure");
				
				removeFromPendingAndUpdateState(imageData, properties);
				deleteImageFromDisk(imageData, properties.getProperty(SEBAL_EXPORT_PATH));
				System.exit(1);
			}
			
			imageData.setCrawlerVersion(crawlerVersion);
			imageData.setFmaskVersion(fmaskVersion);
			
			if(crawlerVersion == null || crawlerVersion.isEmpty()) {
				LOGGER.error("Crawler version file does not exist");
				LOGGER.info("Restart Crawler infrastructure");
				
				removeFromPendingAndUpdateState(imageData, properties);
				deleteImageFromDisk(imageData, properties.getProperty(SEBAL_EXPORT_PATH));
				System.exit(1);
			}

			try {
				imageStore.updateImage(imageData);
				imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while updating image " + imageData
						+ " to DB", e);
				removeFromPendingAndUpdateState(imageData, properties);
			}

			try {
				imageStore.addStateStamp(imageData.getName(),
						imageData.getState(), imageData.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while adding state " + imageData.getState()
						+ " timestamp " + imageData.getUpdateTime() + " in DB", e);
			}

			pendingImageDownloadMap.remove(imageData.getName());
			pendingImageDownloadDB.commit();

			LOGGER.info("Image " + imageData + " was downloaded");

		} catch (Exception e) {
			LOGGER.error("Error when downloading image " + imageData, e);
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	protected String getFmaskVersion() throws IOException {
		File fmaskVersionFile = new File(FMASK_VERSION_FILE_PATH);
		FileInputStream fileInputStream = new FileInputStream(fmaskVersionFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
		 
		String line = null;
		String fmaskVersion = null;
		while ((line = br.readLine()) != null) {
			fmaskVersion = line;
		}
	 
		br.close();
		return fmaskVersion;
	}

	private void markImageWithErrorAndUpdateState(ImageData imageData,
			Properties properties) throws IOException {

		try {
			if (imageData.getFederationMember().equals(federationMember)) {
				imageData.setState(ImageState.ERROR);
				imageData.setImageError("It was not possible run Fmask for image");			
				
				imageStore.updateImage(imageData);
				imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());

				deleteImageFromDisk(imageData,
						properties.getProperty(SEBAL_EXPORT_PATH));

				LOGGER.debug("Removing image " + imageData
						+ " from pending image map");
				pendingImageDownloadMap.remove(imageData.getName());
				pendingImageDownloadDB.commit();
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image data: " + imageData.getName(), e);
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	private void removeFromPendingAndUpdateState(final ImageData imageData,
			Properties properties) throws IOException {

		if (imageData.getFederationMember().equals(federationMember)) {

			LOGGER.debug("Rolling back " + imageData + " to "
					+ ImageState.NOT_DOWNLOADED + " state");

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

			try {
				imageStore.updateImage(imageData);
				imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
			} catch (SQLException e) {
				Crawler.LOGGER.error("Error while updating image data "
						+ imageData.getName(), e);
				imageData.setFederationMember(federationMember);
				imageData.setState(ImageState.DOWNLOADING);
			}

			deleteImageFromDisk(imageData,
					properties.getProperty(SEBAL_EXPORT_PATH));

			LOGGER.debug("Removing image " + imageData
					+ " from pending image map");
			pendingImageDownloadMap.remove(imageData.getName());
			pendingImageDownloadDB.commit();
			LOGGER.info("Image " + imageData + " rolled back");
		}
	}

	protected void deleteImageFromDisk(final ImageData imageData,
			String exportPath) throws IOException {

		String imageDirPath = exportPath + "/images/" + imageData.getName();
		File imageDir = new File(imageDirPath);

		LOGGER.info("Removing image " + imageData + " data under path "
				+ imageDirPath);

		if (isImageOnDisk(imageDirPath, imageDir)) {
			FileUtils.deleteDirectory(imageDir);
		}
	}

	protected boolean isImageOnDisk(String imageDirPath, File imageDir) {
		if (!imageDir.exists() || !imageDir.isDirectory()) {
			LOGGER.info("Path " + imageDirPath + " does not exist");
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
			LOGGER.error("Export path is null or empty");
		}
	}

	private void deleteResultsFromDisk(ImageData imageData, String exportPath)
			throws IOException {

		String resultsDirPath = exportPath + "/results/" + imageData.getName();
		File resultsDir = new File(resultsDirPath);

		if (!resultsDir.exists() || !resultsDir.isDirectory()) {
			LOGGER.debug("Path" + resultsDirPath + " does not exist or is not a directory");
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
						deleteImageFromDisk(imageData,
								properties.getProperty(SEBAL_EXPORT_PATH));
						deleteResultsFromDisk(imageData, exportPath);
					} catch (IOException e) {
						LOGGER.error("Error while deleting " + imageData, e);
					}

					LOGGER.debug("Image " + imageData + " purged");
				}
			}
		} else {
			LOGGER.error("Export path is null or empty!");
		}
	}
	
	protected String getCrawlerVersion() {
		
		String sebalEngineDirPath = System.getProperty("user.dir");
		File sebalEngineDir = new File(sebalEngineDirPath);
		String[] sebalEngineVersionFileSplit = null;
		
		if (sebalEngineDir.exists() && sebalEngineDir.isDirectory()) {			
			for (File file : sebalEngineDir.listFiles()) {				
				if (file.getName().startsWith("sebal-engine.version.")) {
					sebalEngineVersionFileSplit = file.getName()
							.split("\\.");					
					return sebalEngineVersionFileSplit[2];
				}
			}			
		}
		
		return null;
	}
}
