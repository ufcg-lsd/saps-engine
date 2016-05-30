package org.fogbowcloud.sebal.fetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.CheckSumMD5ForFile;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.crawler.Crawler;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class Fetcher {

	// FIXME: test - 1
	// FIXME: FTP - 0

	private final Properties properties;
	private final ImageDataStore imageStore;
	private final File pendingImageFetchFile = new File("pending-image-f.db");
	private final DB pendingImageFetchDB = DBMaker.newFileDB(
			pendingImageFetchFile).make();
	private ConcurrentMap<String, ImageData> pendingImageFetchMap = pendingImageFetchDB
			.createHashMap("map").make();

	private String ftpServerIP;
	private String ftpServerPort;

	private static int MAX_FETCH_TRIES = 2;
	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes

	public static final Logger LOGGER = Logger.getLogger(Fetcher.class);

	public Fetcher(Properties properties, String imageStoreIP,
			String imageStorePort, String ftpServerIP, String ftpServerPort) {

		if (properties == null) {
			throw new IllegalArgumentException(
					"Properties arg must not be null.");
		}

		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(properties, imageStoreIP,
				imageStorePort);
		this.ftpServerIP = ftpServerIP;
		this.ftpServerPort = ftpServerPort;
	}

	public void exec() throws InterruptedException, IOException {

		LOGGER.info("Initializing fetcher... ");

		cleanUnfinishedFetchedData();

		while (true) {
			List<ImageData> imagesToFetch = imagesToFetch();
			for (ImageData iData : imagesToFetch) {
				fetchAndUpdateImage(iData);
			}
			Thread.sleep(DEFAULT_SCHEDULER_PERIOD);
		}
	}

	private void cleanUnfinishedFetchedData() {
		Collection<ImageData> data = pendingImageFetchMap.values();
		for (ImageData iData : data) {
			removeFromPendingAndUpdateState(iData);
		}
	}

	private void removeFromPendingAndUpdateState(final ImageData imageData) {
		// FIXME: add log
		try {
			imageData.setState(ImageState.FINISHED);
			imageStore.updateImage(imageData);
			pendingImageFetchMap.remove(imageData.getName());
		} catch (SQLException e1) {
			Crawler.LOGGER.error("Error while updating image data: "
					+ imageData.getName(), e1);
		}
	}

	private List<ImageData> imagesToFetch() {
		try {
			return imageStore.getIn(ImageState.FINISHED);
		} catch (SQLException e) {
			LOGGER.error("Error getting finished images.", e);
		}
		return Collections.EMPTY_LIST;
	}

	private void fetchAndUpdateImage(ImageData imageData) throws IOException, InterruptedException {

		try {
			prepareFetch(imageData);
			fetch(imageData, 0);
			
			if(!isFileCorrupted(imageData)) {
				finishFetch(imageData);			
			}
		} catch (SQLException e) {
			LOGGER.error("Couldn't fetch image " + imageData.getName() + ".", e);
			rollBackFetch(imageData);
		}
	}
	
	private boolean isFileCorrupted(ImageData imageData) throws SQLException {
		if(imageData.getState().equals(ImageState.CORRUPTED)) {
			pendingImageFetchMap.remove(imageData.getName());
			imageStore.updateImage(imageData);
			return true;
		}
		return false;
	}

	private void prepareFetch(ImageData imageData) throws SQLException {

		if (imageStore.lockImage(imageData.getName())) {
			imageData.setState(ImageState.FETCHING);
			imageData.setFederationMember(properties
					.getProperty("federation_member"));
			pendingImageFetchMap.put(imageData.getName(), imageData);

			imageStore.updateImage(imageData);
			imageStore.unlockImage(imageData.getName());
		}
	}

	private void finishFetch(ImageData imageData) throws SQLException, IOException {		
		imageData.setState(ImageState.FETCHED);
		imageData.setStationId(getStationId(imageData));
		imageData.setSebalVersion(properties.getProperty("sebal_version"));
		imageStore.updateImage(imageData);
		pendingImageFetchMap.remove(imageData.getName());
	}
	
	private String getStationId(ImageData imageData) throws IOException {
		String stationFilePath = properties.getProperty("sebal_export_path")
				+ "/results/" + imageData.getName() + "/" + imageData.getName()
				+ "_station.csv";
		
		BufferedReader reader = new BufferedReader(new FileReader(stationFilePath));
		String lineOne = reader.readLine();
		String[] stationAtt = lineOne.split(";");
		
		String stationId = stationAtt[0];
		return stationId;
	}

	private void rollBackFetch(ImageData imageData) {

		pendingImageFetchMap.remove(imageData.getName());
		try {
			imageData.setState(ImageState.FINISHED);
			imageStore.updateImage(imageData);
		} catch (SQLException e1) {
			Fetcher.LOGGER.error("Error while updating image data.", e1);
		}
	}

	// TODO: See if this is correct
	// FIXME: reduce code
	public void fetch(final ImageData imageData, int tries) throws IOException,
			InterruptedException, SQLException {
		
		if(tries > MAX_FETCH_TRIES) {
			LOGGER.debug("Max tries reached!\nFile is corrupted.");
			imageData.setState(ImageState.CORRUPTED);
			return;
		}
		
		String localVolumeResultsPath = properties
				.getProperty("fetcher_volume_path") + "/results";
		String localVolumeResultDir = localVolumeResultsPath + "/" + imageData;
		String remoteVolumeResultsPath = properties
				.getProperty("sebal_export_path")
				+ "/results/"
				+ imageData.getName();

		File remoteVolumeResultsDir = new File(remoteVolumeResultsPath);

		if (!remoteVolumeResultsDir.exists()
				&& !remoteVolumeResultsDir.isDirectory()) {
			LOGGER.error("This folder doesn't exist or is not a directory.");
			return;
		}

		ProcessBuilder builder = new ProcessBuilder();
		builder.command("sftp -P " + ftpServerPort + " "
				+ properties.getProperty("ftp_server_user") + "@" + ftpServerIP);
		builder.command("get -r " + remoteVolumeResultsDir);
		builder.command("quit");
		builder.command("mv -rf " + imageData.getName() + " "
				+ localVolumeResultsPath);

		Process p = builder.start();
		p.waitFor();

		if(CheckSumMD5ForFile.isFileCorrupted(imageData, new File(localVolumeResultDir))) {
			fetch(imageData, tries++);
		}
	}
}
