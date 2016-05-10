package org.fogbowcloud.sebal.fetcher;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.FTPUtils;
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

	public void exec() throws InterruptedException {

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
			imageData.setFederationMember(ImageDataStore.NONE);
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

	private void fetchAndUpdateImage(ImageData imageData) {

		try {
			prepareFetch(imageData);
			fetch(imageData);
			finishFetch(imageData);
		} catch (SQLException e) {
			LOGGER.error("Couldn't fetch image " + imageData.getName() + ".", e);
			rollBackFetch(imageData);
		}
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

	private void finishFetch(ImageData imageData) throws SQLException {
		imageData.setState(ImageState.FETCHED);
		imageStore.updateImage(imageData);
		pendingImageFetchMap.remove(imageData.getName());
	}

	private void rollBackFetch(ImageData imageData) {

		pendingImageFetchMap.remove(imageData.getName());
		try {
			imageData.setFederationMember(ImageDataStore.NONE);
			imageData.setState(ImageState.FINISHED);
			imageStore.updateImage(imageData);
		} catch (SQLException e1) {
			Fetcher.LOGGER.error("Error while updating image data.", e1);
		}
	}

	// TODO: See if this is correct
	// FIXME: replace by fetch
	public void fetch(final ImageData imageData) {
		// FIXME: checkSum

		FTPUtils ftpUtils = new FTPUtils(properties, ftpServerIP, ftpServerPort);
		FTPUtils.init(imageData);

		/*
		 * String resultsDirPath = properties.getProperty("sebal_export_path") +
		 * "/results/" + imageData.getName(); File resultsDir = new
		 * File(resultsDirPath); if (!resultsDir.exists() ||
		 * !resultsDir.isDirectory()) { resultsDir.mkdirs(); }
		 * 
		 * ProcessBuilder builder = new ProcessBuilder();
		 * builder.directory(resultsDir); builder.command("wget -r ftp://" +
		 * properties.getProperty("ftp_server_user") + ":" +
		 * properties.getProperty("ftp_user_pass") + "@" + ftpServerIP +
		 * "/results/" + imageData.getName());
		 * 
		 * LOGGER.info("Fetching image " + imageData.getName() +
		 * " into volume."); Process p = builder.start(); p.waitFor();
		 * 
		 * LOGGER.info("Image " + imageData.getName() +
		 * " fetched into volume.");
		 */
	}

}
