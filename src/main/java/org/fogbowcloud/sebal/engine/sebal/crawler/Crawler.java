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
import org.fogbowcloud.sebal.engine.scheduler.util.SebalPropertiesConstants;
import org.fogbowcloud.sebal.engine.sebal.FMask;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.USGSNasaRepository;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Crawler {
	
	private static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";
	
	private String crawlerIp;
	private String nfsPort;
	private String federationMember;
	
	private final Properties properties;

	private File pendingImageDownloadFile;
	protected DB pendingImageDownloadDB;
	protected ConcurrentMap<String, ImageData> pendingImageDownloadMap;

	private USGSNasaRepository usgsRepository;
	private final ImageDataStore imageStore;
	
	private FMask fmask;

	private String crawlerVersion;
	private String fmaskVersion;

	private int numberOfDownloadLinkRequests = 0;

	// Image dir size in bytes
	private static final long DEFAULT_IMAGE_DIR_SIZE = 356 * FileUtils.ONE_MB;

	public static final Logger LOGGER = Logger.getLogger(Crawler.class);


	public Crawler(Properties properties, String crawlerIP, String nfsPort,
			String federationMember) throws SQLException {
		this(properties, new JDBCImageDataStore(properties), new USGSNasaRepository(properties),
				crawlerIP, nfsPort, federationMember, new FMask());

		LOGGER.info("Creating crawler in federation " + federationMember);
	}

	protected Crawler(Properties properties, ImageDataStore imageStore,
			USGSNasaRepository usgsRepository, String crawlerIP,
			String nfsPort, String federationMember, FMask fmask) {
		try {
			checkProperties(properties, imageStore, usgsRepository, crawlerIP,
					nfsPort, federationMember, fmask);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Error while getting properties", e);
			System.exit(1);
		}
		
		this.crawlerIp = crawlerIP;
		this.nfsPort = nfsPort;
		
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

	private void checkProperties(Properties properties,
			ImageDataStore imageStore, USGSNasaRepository usgsRepository,
			String crawlerIP, String nfsPort, String federationMember,
			FMask fmask) throws IllegalArgumentException {
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
		
		if (crawlerIP == null) {
			throw new IllegalArgumentException(
					"Crawler IP arg must not be null.");
		}
		
		if (nfsPort == null) {
			throw new IllegalArgumentException(
					"NFS Port arg must not be null.");
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
	}

	public void exec() throws InterruptedException, IOException {
		checkVersionFileExists();
		registerDeployConfig();
		
		cleanUnfinishedDownloadedData(properties);

		try {			
			while (true) {
				cleanUnfinishedQueuedOutput(properties);
				reSubmitErrorImages(properties);
				purgeImagesFromVolume(properties);
				deleteFetchedResultsFromVolume(properties);

				long numToDownload = numberOfImagesToDownload();
				if (numToDownload > 0) {
					download(numToDownload);
				} else {
					Thread.sleep(Long.valueOf(properties
							.getProperty(SebalPropertiesConstants.DEFAULT_CRAWLER_PERIOD)));
				}
				
				numberOfDownloadLinkRequests = 0;
			}
		} catch (Throwable e) {
			LOGGER.error("Failed while downloading images", e);
		} finally {
			pendingImageDownloadDB.close();
		}
	}
	
	private void checkVersionFileExists() {
		if(!crawlerVersionFileExists() || !fmaskVersionFileExists()) {
			System.exit(1);
		}
	}
	
	protected boolean crawlerVersionFileExists() {
		this.crawlerVersion = getCrawlerVersion();

		if(crawlerVersion == null || crawlerVersion.isEmpty()) {
			LOGGER.error("Crawler version file does not exist");
			LOGGER.info("Restart Crawler infrastructure");
			
			return false;
		}
		
		return true;
	}
	
	protected boolean fmaskVersionFileExists() {
		try {			
			this.fmaskVersion = getFmaskVersion();
		} catch(IOException e) {
			LOGGER.error("Error while reading Fmask version file");
			return false;
		}		
		
		if(fmaskVersion == null || fmaskVersion.isEmpty()) {
			LOGGER.error("Fmask version file does not exist");
			LOGGER.info("Restart Crawler infrastructure");
			
			return false;
		}
		
		return true;
	}
	
	private void registerDeployConfig() {
		try {
			if (imageStore.deployConfigExists(federationMember)) {
				imageStore.removeDeployConfig(federationMember);
			}

			imageStore.addDeployConfig(crawlerIp, nfsPort, federationMember);
		} catch (SQLException e) {
			final String ss = e.getSQLState();
			if (!ss.equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) {
				LOGGER.error("Error while adding crawler configuration in DB",
						e);
				System.exit(1);
			}
		}
	}

	protected void cleanUnfinishedDownloadedData(Properties properties)
			throws IOException {
		Collection<ImageData> data = pendingImageDownloadMap.values();
		for (ImageData imageData : data) {
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	private void cleanUnfinishedQueuedOutput(Properties properties2)
			throws SQLException, IOException {
		List<ImageData> data = imageStore.getIn(ImageState.QUEUED);
		for (ImageData imageData : data) {
			deleteResultsFromDisk(imageData, properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH));
		}
	}
	
	protected void reSubmitErrorImages(Properties properties) {
		try {
			List<ImageData> errorImages = imageStore.getIn(ImageState.ERROR);

			for (ImageData imageData : errorImages) {
				treatAndSubmit(properties, imageData);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while re submitting images with error", e);
		}
	}

	protected void treatAndSubmit(Properties properties, ImageData imageData)
			throws SQLException {
		if (imageData.getFederationMember().equals(
				SebalPropertiesConstants.AZURE_FEDERATION_MEMBER)) {
			imageData.setFederationMember(this.federationMember);
		}

		if (imageData.getFederationMember().equals(
				this.federationMember)) {
			reSubmitImage(properties, imageData);
		}
	}

	protected void reSubmitImage(Properties properties, ImageData imageData)
			throws SQLException {
		if (imageNeedsToBeDownloaded(properties, imageData)) {
			if(numberOfDownloadLinkRequests < Integer.valueOf(properties.getProperty(SebalPropertiesConstants.MAX_USGS_DOWNLOAD_LINK_REQUESTS))) {
				imageData.setDownloadLink(usgsRepository.getImageDownloadLink(imageData.getName()));
				imageData.setState(ImageState.NOT_DOWNLOADED);
				updateErrorImage(imageData);
				numberOfDownloadLinkRequests++;
			}
		} else {
			imageData.setState(ImageState.DOWNLOADED);
			updateErrorImage(imageData);
		}
	}

	protected boolean imageNeedsToBeDownloaded(Properties properties,
			ImageData imageData) {
		File imageDir = getImageDir(properties, imageData);
		
		if(isThereImageInputs(imageDir)) {
			return false;
		}
		
		return true;
	}

	protected File getImageDir(Properties properties, ImageData imageData) {
		String exportPath = properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH);
		String imageDirPath = exportPath + File.separator + "images" + File.separator + imageData.getName();
		File imageDir = new File(imageDirPath);
		return imageDir;
	}
	
	protected boolean isThereImageInputs(File imageDir) {
		if(imageDir.exists() && imageDir.list().length > 0) {
			for(File file : imageDir.listFiles()) {
				if(file.getName().endsWith("MTLFmask")) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void updateErrorImage(ImageData imageData) throws SQLException {		
		imageData.setImageError(ImageData.NON_EXISTENT);
		imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		imageStore.updateImage(imageData);
		imageStore.addStateStamp(imageData.getName(),
				imageData.getState(), imageData.getUpdateTime());
	}

	protected void download(long maxImagesToDownload) throws SQLException,
			IOException {
		List<ImageData> imageDataList = new ArrayList<ImageData>();

		try {
			// This updates images in NOT_DOWNLOADED state to SELECTED
			// and sets this federation member as owner, and then gets all
			// images
			// marked as SELECTED
			imageDataList = imageStore.getImagesToDownload(federationMember,
					(int) maxImagesToDownload);
		} catch (SQLException e) {
			LOGGER.error("Error while accessing not downloaded images in DB", e);
		}

		for (ImageData imageData : imageDataList) {
			if(imageData.getFederationMember().equals(federationMember)) {				
				imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
				
				if (imageData != null) {
					addStateStamp(imageData);
					
					LOGGER.debug("Adding image " + imageData.getName()
							+ " to pending database");
					pendingImageDownloadMap.put(imageData.getName(), imageData);
					pendingImageDownloadDB.commit();
					
					downloadImage(imageData);
					LOGGER.info("Image " + imageData + " download finished");
				}
			}
		}

	}

	private void addStateStamp(ImageData imageData) {
		try {
			imageStore.addStateStamp(imageData.getName(),
					imageData.getState(), imageData.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state "
					+ imageData.getState() + " timestamp "
					+ imageData.getUpdateTime() + " in DB", e);
		}
	}

	protected long numberOfImagesToDownload() throws NumberFormatException,
			InterruptedException, IOException, SQLException {
		String volumeDirPath = properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH);
		File volumePath = getExportDirPath(volumeDirPath);
		if (volumePath.exists() && volumePath.isDirectory()) {
			long freeVolumeSpaceOutputDedicated = (volumePath.getTotalSpace() * 20)/100;
			long availableVolumeSpace = volumePath.getFreeSpace() - freeVolumeSpaceOutputDedicated;
			long numberOfImagesToDownload = availableVolumeSpace
					/ DEFAULT_IMAGE_DIR_SIZE;
			
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
			String imageDownloadLink = getUSGSRepository().getImageDownloadLink(imageData.getName());
			imageData.setDownloadLink(imageDownloadLink);
			
			LOGGER.debug("Image download link is " + imageData.getDownloadLink());
			
			updateToDownloadingState(imageData);
			usgsRepository.downloadImage(imageData);

			// running Fmask
			LOGGER.debug("Running Fmask for image " + imageData.getName());
			int exitValue = 0;			
			try {
				exitValue = fmask.runFmask(imageData,
						properties.getProperty(SebalPropertiesConstants.FMASK_SCRIPT_PATH),
						properties.getProperty(SebalPropertiesConstants.FMASK_TOOL_PATH),
						properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH));
			} catch (Exception e) {
				LOGGER.error("Error while running Fmask", e);
			}
			
			if (exitValue != 0) {
				LOGGER.error("It was not possible run Fmask for image "
						+ imageData);
				markImageWithErrorAndUpdateState(imageData, properties);
				return;
			}
			
			imageData.setCrawlerVersion(crawlerVersion);
			imageData.setFmaskVersion(fmaskVersion);

			updateToDownloadedState(imageData);

			pendingImageDownloadMap.remove(imageData.getName());
			pendingImageDownloadDB.commit();
			
			LOGGER.info("Image " + imageData + " was downloaded");
		} catch (Exception e) {
			LOGGER.error("Error when downloading image " + imageData, e);
			removeFromPendingAndUpdateState(imageData, properties);
		}
	}

	private void updateToDownloadedState(final ImageData imageData)
			throws IOException {
		imageData.setState(ImageState.DOWNLOADED);
		
		try {
			imageStore.updateImage(imageData);
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image " + imageData
					+ " to DB", e);
			removeFromPendingAndUpdateState(imageData, properties);
		}

		addStateStamp(imageData);
	}

	private void updateToDownloadingState(final ImageData imageData)
			throws IOException {
		imageData.setState(ImageState.DOWNLOADING);
		
		try {
			imageStore.updateImage(imageData);
			imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while updating image " + imageData
					+ " to DB", e);
			removeFromPendingAndUpdateState(imageData, properties);
		}
		
		addStateStamp(imageData);
	}

	protected String getFmaskVersion() throws IOException {
		File fmaskVersionFile = new File(properties.getProperty(SebalPropertiesConstants.FMASK_VERSION_FILE_PATH));
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
						properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH));

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
				imageData.setState(ImageState.SELECTED);
			}

			deleteImageFromDisk(imageData,
					properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH));

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

		String exportPath = properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH);

		String resultsPath = exportPath + File.separator + "results";

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageData imageData : setOfImageData) {
				String imageResultsPath = resultsPath + File.separator
						+ imageData.getName();
				File imageResultsDir = new File(imageResultsPath);

				if (imageData.getState().equals(ImageState.FETCHED)
						&& imageData.getFederationMember().equals(
								federationMember) && imageResultsDir.exists()) {
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

		LOGGER.debug("Deleting results for " + imageData + " from " + resultsDirPath);
		FileUtils.deleteDirectory(resultsDir);
	}

	protected void purgeImagesFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {
		List<ImageData> imagesToPurge = imageStore.getAllImages();

		String exportPath = properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH);

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageData imageData : imagesToPurge) {
				if (imageData.getImageStatus().equals(ImageData.PURGED)
						&& imageData.getFederationMember().equals(
								federationMember)) {
					LOGGER.debug("Purging image " + imageData);

					try {
						deleteImageFromDisk(imageData,
								properties.getProperty(SebalPropertiesConstants.SEBAL_EXPORT_PATH));
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

	public USGSNasaRepository getUSGSRepository() {
		return usgsRepository;
	}

	public void setUsgsRepository(USGSNasaRepository usgsRepository) {
		this.usgsRepository = usgsRepository;
	}
}
