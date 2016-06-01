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

	//FIXME: FTP - 0 (In infrastructure creation?)
	//FIXME: test - 1
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

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}

		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort);
		this.NASARepository = new NASARepository(properties);
		
		this.federationMember = federationMember;

		if(federationMember == null || federationMember.isEmpty()) {			
			LOGGER.error("Federation member field is empty!");
			return;
		}
		
		this.pendingImageDownloadFile = new File("pending-image-download.db");
		this.pendingImageDownloadDB = DBMaker.newFileDB(pendingImageDownloadFile).make();
		
		if(!pendingImageDownloadFile.exists() || !pendingImageDownloadFile.isFile()) {			
			this.pendingImageDownloadMap = pendingImageDownloadDB.createHashMap("map").make();
		} else {
			this.pendingImageDownloadMap = pendingImageDownloadDB.getHashMap("map");
		}
	}

	public void exec() throws InterruptedException, IOException {

		LOGGER.info("Initializing crawler... ");
		
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
		} catch (Throwable e) {
			LOGGER.error("Failed while download task.", e);
			e.printStackTrace();
		}

		pendingImageDownloadDB.close();
		LOGGER.debug("All images downloaded.\nProcess finished.");
	}
	
	private void cleanUnfinishedDownloadedData(Properties properties) throws IOException {
		Collection<ImageData> data = pendingImageDownloadMap.values();
		for(ImageData imageData : data) {
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	private boolean thereIsImageToDownload() throws SQLException {
		List<ImageData> imageData = imageStore.getIn(ImageState.NOT_DOWNLOADED);
		return !imageData.isEmpty();
	}

	protected void download(long maxImagesToDownload) throws SQLException, IOException {

		// FIXME: check the implications of this cast
		// This updates images in NOT_DOWNLOADED state to DOWNLOADING 
		// and sets this federation member as owner, and then gets all images marked as DOWNLOADING
		
		// FIXME: only get not purged images
		List<ImageData> imageDataList = imageStore.getImagesToDownload(
				federationMember, (int) maxImagesToDownload); 

		for (ImageData imageData : imageDataList) {
			if (imageData != null
					&& !imageData.getImageStatus().equals(ImageData.PURGED)) {
				// Updating pending dataBase and imageData
				// FIXME: put in SQL
				imageData.setUpdateTime(String.valueOf(System
						.currentTimeMillis()));
				pendingImageDownloadMap.put(imageData.getName(), imageData);
				pendingImageDownloadDB.commit();

				downloadImage(imageData);
			}
		}
	}

	protected long numberOfImagesToDownload() {
		
		//FIXME test
		String volumeDirPath = properties.getProperty("sebal_export_path");
		File volumePath = getExportDirPath(volumeDirPath);
		if (volumePath.exists() && volumePath.isDirectory()) {
			long availableVolumeSpace = volumePath.getFreeSpace();
			long numberOfImagesToDownload = availableVolumeSpace / DEFAULT_IMAGE_DIR_SIZE;
			//FIXME: ceil para o inteiro inferior
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
			int exitValue = new FMask().runFmask(imageData, properties);
			if (exitValue != 0) {
				LOGGER.error("It was not possible run Fmask for image "
						+ imageData.getName());
				imageData.setFederationMember(ImageDataStore.NONE);
			}			

			imageData.setState(ImageState.DOWNLOADED);
			imageData.setCreationTime(String.valueOf(System.currentTimeMillis()));
			imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
			imageStore.updateImage(imageData);
			pendingImageDownloadMap.remove(imageData.getName());
		} catch (Exception e) {
			LOGGER.error(
					"Couldn't download image " + imageData.getName() + ".", e);
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}
	
	private void removeFromPendingAndUpdateState(final ImageData imageData, Properties properties) throws IOException {
		//FIXME: add log
		//FIXME: clean up garbage data from volumes
		try {
			if(!imageData.getImageStatus().equals(ImageData.PURGED)) {				
				imageData.setFederationMember(ImageDataStore.NONE);
				imageData.setState(ImageState.NOT_DOWNLOADED);
				imageData.setUpdateTime(String.valueOf(System.currentTimeMillis()));
				imageStore.updateImage(imageData);
			}
			
			String exportPath = properties.getProperty("sebal_export_path");
			String imageDirPath = exportPath + "/images/"
					+ imageData.getName();
			File imageDir = new File(imageDirPath);

			if (!imageDir.exists() || !imageDir.isDirectory()) {
				LOGGER.debug("This file does not exist!");
				return;
			}
			
			FileUtils.deleteDirectory(imageDir);
			pendingImageDownloadMap.remove(imageData.getName());
		} catch (SQLException e1) {
			Crawler.LOGGER.error("Error while updating image data: " + imageData.getName(), e1);
			System.out.println("Error while updating image data: " + imageData.getName() + "\n" + e1);
		}
	}

	private void deleteFetchedResultsFromVolume(Properties properties) throws IOException,
			InterruptedException, SQLException {
		
		List<ImageData> setOfImageData = imageStore.getAllImages();

		String exportPath = properties.getProperty("sebal_export_path");
				
		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageData imageData : setOfImageData) {
				if (imageData.getState().equals(ImageState.FETCHED)) {
					String resultsDirPath = exportPath + "/results/"
							+ imageData.getName();
					File resultsDir = new File(resultsDirPath);

					if (!resultsDir.exists() || !resultsDir.isDirectory()) {
						LOGGER.debug("This file does not exist!");
						return;
					}
					FileUtils.deleteDirectory(resultsDir);
					LOGGER.debug("Image " + imageData.getName() + " result files deleted successfully.");
				} else {
					continue;
				}
			}
		} else {
			// FIXME: Implement solution for this
			LOGGER.error("Volume directory path is null or empty!");
		}
	}
	
	private void purgeImagesFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {
		List<ImageData> imagesToPurge = imageStore.getIn(ImageState.FINISHED);

		String exportPath = properties.getProperty("sebal_export_path");

		if (!exportPath.isEmpty() && exportPath != null) {			
			for (ImageData imageData : imagesToPurge) {
				//FIXME: compare federation member...if not, continue to another image
				if (imageData.getImageStatus().equals(ImageData.PURGED)) {
					String imageDirPath = exportPath + "/images/"
							+ imageData.getName();
					String resultsDirPath = exportPath + "/results/"
							+ imageData.getName();

					File imageDir = new File(imageDirPath);
					File resultsDir = new File(resultsDirPath);

					if (!imageDir.exists() || !imageDir.isDirectory()) {
						LOGGER.debug("Directory " + imageDirPath
								+ " does not exist!");
						return;
					}
					if (!resultsDir.exists() || !resultsDir.isDirectory()) {
						LOGGER.debug("Directory " + resultsDirPath
								+ " does not exist!");
						return;
					}

					FileUtils.deleteDirectory(imageDir);
					LOGGER.debug("Image " + imageData.getName()
							+ " image files deleted successfully.");
					FileUtils.deleteDirectory(resultsDir);
					LOGGER.debug("Image " + imageData.getName()
							+ " results files deleted successfully.");
				}
			}
		} else {
			// FIXME: Implement solution for this
			LOGGER.error("Volume directory path is null or empty!");
		}
	}

}
