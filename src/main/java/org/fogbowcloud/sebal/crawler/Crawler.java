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
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;

public class Crawler {

	//FIXME: code pending again - 1
	//FIXME: FTP - 0 (In infrastructure creation?)
	//FIXME: test - 1
	
	private final Properties properties;
	private final ImageDataStore imageStore;
	private NASARepository NASARepository;
	private Map<String, ImageData> pendingImageDownload = new HashMap<String, ImageData>();
	private static int maxSimultaneousDownload;
	private static String volumePath;

	private static final double DEFAULT_IMAGE_DIR_SIZE = 0.35604873;
	private static final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes

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
		this.volumePath = properties.getProperty("sebal_export_path");
		this.NASARepository = new NASARepository(properties);
	}

	public void exec() throws InterruptedException, IOException {

		LOGGER.info("Initializing crawler... ");

		try {
			do {
				schedulePreviousDownloadsNotFinished();				
				deleteFetchedResultsFromVolume(properties);
				
				if (pendingImageDownload.size() >= maxSimultaneousDownload) {
					LOGGER.debug("Already downloading " + pendingImageDownload.size()
							+ "images and max allowed is " + maxSimultaneousDownload);
					return;
				}

				int numToDownload = numberOfImagesToDownload();
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
				pendingImageDownload.put(imageData.getName(), imageData);
				downloadImage(imageData);
			}
		}
	}

	private boolean thereIsImageToDownload() throws SQLException {
		List<ImageData> imageData = imageStore.getIn(ImageState.NOT_DOWNLOADED);
		return !imageData.isEmpty();
	}

	private void download(int maxImagesToDownload) throws SQLException {

		List<ImageData> imageDataList = imageStore.getIn(
				ImageState.NOT_DOWNLOADED, maxImagesToDownload);

		for (ImageData imageData : imageDataList) {

			if (imageData != null) {

				// FIXME: not good block
				while (imageStore.lockImage(imageData.getName())) {
					imageData.setState(ImageState.DOWNLOADING);
					imageData.setFederationMember(properties
							.getProperty("federation_member"));					
					pendingImageDownload.put(imageData.getName(), imageData);
					imageStore.updateImage(imageData);
					imageStore.unlockImage(imageData.getName());

					downloadImage(imageData);
				}
			}
		}
	}

	private int numberOfImagesToDownload() {
		// FIXME: why not to calculate at execution time?
		double totalVolumeSize = Double.parseDouble(properties
				.getProperty("default_volume_size"));

		double usedSpace = usedVolumeSpace();
		double availableSize = totalVolumeSize - usedSpace;

		double numberOfImagesToDownload = availableSize
				/ DEFAULT_IMAGE_DIR_SIZE;

		// FIXME: this cast ceil or floors the values?
		return (int) numberOfImagesToDownload;
	}

	private void downloadImage(final ImageData imageData) {

		try {
			// FIXME: it blocks?
			NASARepository.downloadImage(imageData);

			// running Fmask
			int exitValue = runFmask(imageData);
			if (exitValue != 0) {
				LOGGER.error("It was not possible run Fmask for image "
						+ imageData.getName());
				imageData.setFederationMember(ImageDataStore.NONE);
			}

			imageData.setState(ImageState.DOWNLOADED);
			imageStore.updateImage(imageData);
			pendingImageDownload.remove(imageData.getName());
		} catch (Exception e) {
			LOGGER.error(
					"Couldn't download image " + imageData.getName() + ".", e);
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

	//FIXME: move to a class?
	private int runFmask(final ImageData imageData) throws IOException,
			FileNotFoundException, InterruptedException {

		File tempFile = File.createTempFile("temp-" + imageData.getName(),
				".sh");
		FileOutputStream fos = new FileOutputStream(tempFile);

		FileInputStream fis = new FileInputStream(
				properties.getProperty("fmask_script_path"));
		String origExec = IOUtils.toString(fis);

		IOUtils.write(replaceVariables(origExec, imageData), fos);
		fos.close();

		ProcessBuilder builder = new ProcessBuilder("chmod", "+x",
				tempFile.getAbsolutePath());
		Process p = builder.start();
		p.waitFor();

		if (p.exitValue() != 0) {
			LOGGER.error("Error while running chmod +x command. Message="
					+ getError(p));
		}
		LOGGER.debug("chmod +x command output=" + getOutput(p));

		builder = new ProcessBuilder("bash", tempFile.getAbsolutePath());
		p = builder.start();
		p.waitFor();

		if (p.exitValue() != 0) {
			LOGGER.error("Error while running fmask command. Message="
					+ getError(p));
		}
		LOGGER.debug("run-fmask command output=" + getOutput(p));

		return p.exitValue();
	}

	private static String getOutput(Process p) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String out = new String();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			out += line;
		}
		return out;
	}

	private static String getError(Process p) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		String error = new String();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			error += line;
		}
		return error;
	}

	// TODO: see if this is correct
	private void deleteFetchedResultsFromVolume(Properties properties) throws IOException,
			InterruptedException, SQLException {
		
		List<ImageData> setOfImageData = imageStore.getAllImages();

		String imagesDirPath = properties.getProperty("sebal_export_path")
				+ "/results";
		File imagesDir = new File(imagesDirPath);

		if (!imagesDir.exists() || !imagesDir.isDirectory()) {
			LOGGER.debug("This file does not exist!");
			return;
		}

		ProcessBuilder pb = null;

		try {
			// FIXME: replace by file utils call
			for (ImageData imageData : setOfImageData) {
				pb = new ProcessBuilder("rm -r " + imagesDirPath + "/"
						+ imageData.getName());
				Process p = pb.start();
				p.waitFor();
			}

			LOGGER.debug("Image files deleted successfully.");
		} catch (InterruptedException e) {
			LOGGER.error("Error while deleting files!");
			e.printStackTrace();
		}
	}

	private double usedVolumeSpace() {
		File volumeDirectory = new File(volumePath);

		if ((volumeDirectory.exists() && volumeDirectory.isDirectory())) {
			return FileUtils.sizeOfDirectory(volumeDirectory);
		} else {
			throw new RuntimeException("VolumePath: " + volumePath
					+ " is not a directory or does not exist");
		}
	}

	private String replaceVariables(String command, ImageData imageData) {
		command = command.replaceAll(Pattern.quote("${IMAGE_NAME}"),
				imageData.getName());
		command = command.replaceAll(Pattern.quote("${IMAGES_MOUNT_POINT}"),
				properties.getProperty("image_repository"));
		command = command.replaceAll(Pattern.quote("${FMASK_TOOL}"),
				properties.getProperty("fmask_tool_path"));
		return command;
	}

}