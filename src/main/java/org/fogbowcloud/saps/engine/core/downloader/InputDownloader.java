package org.fogbowcloud.saps.engine.core.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.DockerUtil;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class InputDownloader {

	private final Properties properties;
	private final ImageDataStore imageStore;
	private String crawlerIp;
	private String crawlerPort;
	private String nfsPort;
	private String federationMember;
	private File pendingImageDownloadFile;

	protected DB pendingTaskDownloadDB;
	protected ConcurrentMap<String, ImageTask> pendingTaskDownloadMap;

	// Image dir size in bytes
	protected static final int MAX_IMAGES_TO_DOWNLOAD = 1;
	private static final long DEFAULT_IMAGE_DIR_SIZE = 180 * FileUtils.ONE_MB;
	private static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";
	private static final String PENDING_TASK_DOWNLOAD_DB_FILE = "pending-task-download.db";

	public static final Logger LOGGER = Logger.getLogger(InputDownloader.class);

	public InputDownloader(Properties properties, String inputDownloderIp, String InputDownloaderSshPort,
			String nfsPort, String federationMember) throws SQLException {
		this(properties, new JDBCImageDataStore(properties), inputDownloderIp,
				InputDownloaderSshPort, nfsPort, federationMember);
		LOGGER.info("Creating crawler in federation " + federationMember);
	}

	protected InputDownloader(Properties properties, ImageDataStore imageStore, String crawlerIP,
			String crawlerPort, String nfsPort, String federationMember) {
		try {
			checkProperties(properties, imageStore, crawlerIP, crawlerPort, nfsPort,
					federationMember);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Error while getting properties", e);
			System.exit(1);
		}

		this.crawlerIp = crawlerIP;
		this.crawlerPort = crawlerPort;
		this.nfsPort = nfsPort;

		this.properties = properties;
		this.imageStore = imageStore;
		this.federationMember = federationMember;

		this.pendingImageDownloadFile = new File(PENDING_TASK_DOWNLOAD_DB_FILE);
		this.pendingTaskDownloadDB = DBMaker.newFileDB(pendingImageDownloadFile).make();

		if (!pendingImageDownloadFile.exists() || !pendingImageDownloadFile.isFile()) {
			LOGGER.info("Creating map of pending images to download");
			this.pendingTaskDownloadMap = pendingTaskDownloadDB.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending images to download");
			this.pendingTaskDownloadMap = pendingTaskDownloadDB.getHashMap("map");
		}
	}

	private void checkProperties(Properties properties, ImageDataStore imageStore, String crawlerIP,
			String crawlerPort, String nfsPort, String federationMember)
			throws IllegalArgumentException {
		if (properties == null) {
			throw new IllegalArgumentException("Properties arg must not be null.");
		}

		if (imageStore == null) {
			throw new IllegalArgumentException("Imagestore arg must not be null.");
		}

		if (crawlerIP == null) {
			throw new IllegalArgumentException("Crawler IP arg must not be null.");
		}

		if (crawlerPort == null) {
			throw new IllegalArgumentException("Crawler Port arg must not be null.");
		}

		if (crawlerPort.isEmpty()) {
			throw new IllegalArgumentException("Crawler Port arg must not be null.");
		}

		if (nfsPort == null) {
			throw new IllegalArgumentException("NFS Port arg must not be null.");
		}

		if (federationMember == null) {
			throw new IllegalArgumentException("Federation member arg must not be null.");
		}

		if (federationMember.isEmpty()) {
			throw new IllegalArgumentException("Federation member arg must not be empty.");
		}
	}

	public void exec() throws InterruptedException, IOException {
		registerDeployConfig();
		cleanUnfinishedDownloadedData(properties);

		try {
			while (true) {
				cleanUnfinishedQueuedOutput(properties);
				purgeTasksFromVolume(properties);
				removeFailedTasksFromVolume(properties);
				deleteArchivedOutputsFromDisk(properties);

				double numToDownload = numberOfImagesToDownload();
				if (numToDownload > 0) {
					download();
				} else {
					Thread.sleep(Long.valueOf(properties
							.getProperty(SapsPropertiesConstants.DEFAULT_CRAWLER_PERIOD)));
				}
			}
		} catch (Throwable e) {
			LOGGER.error("Failed while downloading images", e);
		} finally {
			pendingTaskDownloadDB.close();
		}
	}
	
	private void registerDeployConfig() {
		try {
			if (imageStore.deployConfigExists(federationMember)) {
				imageStore.removeDeployConfig(federationMember);
			}

			imageStore.addDeployConfig(crawlerIp, crawlerPort, nfsPort, federationMember);
		} catch (SQLException e) {
			final String ss = e.getSQLState();
			if (!ss.equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) {
				LOGGER.error("Error while adding crawler configuration in DB", e);
				System.exit(1);
			}
		}
	}

	protected void cleanUnfinishedDownloadedData(Properties properties) throws IOException {
		Collection<ImageTask> tasks = pendingTaskDownloadMap.values();
		for (ImageTask imageTask : tasks) {
			removeFromPendingAndUpdateState(imageTask, properties);
		}
	}

	private void cleanUnfinishedQueuedOutput(Properties properties)
			throws SQLException, IOException {
		List<ImageTask> tasks = imageStore.getIn(ImageTaskState.READY);
		for (ImageTask imageTask : tasks) {
			deleteOutputsFromDisk(imageTask,
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));
		}
	}

	private void removeFailedTasksFromVolume(Properties properties)
			throws SQLException, IOException {
		List<ImageTask> tasks = imageStore.getIn(ImageTaskState.FAILED);
		for (ImageTask imageTask : tasks) {
			deleteInputsFromDisk(imageTask,
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));
			deleteOutputsFromDisk(imageTask,
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));
		}
	}

	protected File getImageDir(Properties properties, ImageTask imageTask) {
		String exportPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String imageDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "input";
		File imageDir = new File(imageDirPath);
		return imageDir;
	}

	protected boolean isThereImageInputs(File imageDir) {
		if (imageDir.exists() && imageDir.list().length > 0) {
			for (File file : imageDir.listFiles()) {
				if (file.getName().endsWith("MTLFmask")) {
					return true;
				}
			}
		}

		return false;
	}

	protected void download() throws SQLException, IOException {
		LOGGER.debug("maxImagesToDownload=" + MAX_IMAGES_TO_DOWNLOAD);
		List<ImageTask> tasksToDownload = new ArrayList<ImageTask>();

		try {
			// This updates images in CREATED state to DOWNLOADING
			// and sets this federation member as owner, and then gets all
			// images
			// marked as DOWNLOADING
			tasksToDownload = imageStore.getImagesToDownload(federationMember,
					MAX_IMAGES_TO_DOWNLOAD);
		} catch (SQLException e) {
			LOGGER.error("Error while accessing created tasks in DB", e);
		}

		for (ImageTask imageTask : tasksToDownload) {
			if (imageTask.getFederationMember().equals(federationMember)) {
				imageTask.setUpdateTime(getTaskUpdateTime(imageTask));

				if (imageTask != null) {
					addStateStamp(imageTask);

					LOGGER.debug("Adding task " + imageTask.getTaskId() + " to pending database");
					addTaskToPendingMap(imageTask);

					boolean isDownloadCompleted = downloadImage(imageTask);
					if (isDownloadCompleted) {
						LOGGER.info("Task " + imageTask.getTaskId() + " download is completed");
					} else {
						LOGGER.info("Task " + imageTask.getTaskId() + " download failed");
					}
				}
			}
		}
	}

	protected void addTaskToPendingMap(ImageTask imageTask) {
		pendingTaskDownloadMap.put(imageTask.getTaskId(), imageTask);
		pendingTaskDownloadDB.commit();
	}

	protected Timestamp getTaskUpdateTime(ImageTask imageTask) throws SQLException {
		return imageStore.getTask(imageTask.getTaskId()).getUpdateTime();
	}

	protected void addStateStamp(ImageTask imageTask) {
		try {
			imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
					imageTask.getUpdateTime());
		} catch (SQLException e) {
			LOGGER.error("Error while adding state " + imageTask.getState() + " timestamp "
					+ imageTask.getUpdateTime() + " in DB", e);
		}
	}

	protected double numberOfImagesToDownload()
			throws NumberFormatException, InterruptedException, IOException, SQLException {
		String exportDirPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		File exportDir = new File(exportDirPath);

		double maxInputUsage = Integer
				.valueOf(properties.getProperty(SapsPropertiesConstants.MAX_TASKS_TO_DOWNLOAD))
				* DEFAULT_IMAGE_DIR_SIZE;

		double numberOfImagesToDownload = 0.0;
		double cumulativeInputUsage = 0.0;
		String actualInputUsage = null;

		if (exportDir.exists() && exportDir.isDirectory()) {
			String[] taskDirNames = exportDir.list();
			for (String taskDir : taskDirNames) {
				String inputDirPath = exportDirPath + File.separator + taskDir + File.separator
						+ "data" + File.separator + "input";

				ProcessBuilder builder = new ProcessBuilder("du", "-sh", "-b", inputDirPath);
				LOGGER.debug("Executing command " + builder.command());

				try {
					Process p = builder.start();
					p.waitFor();

					actualInputUsage = getProcessOutput(p);
					String[] splited = actualInputUsage.split("\\s+");
					actualInputUsage = splited[0];

					cumulativeInputUsage += Double.valueOf(actualInputUsage);
				} catch (Exception e) {
					LOGGER.error("Error while getting input disk usage", e);
				}
			}

			double freeDisk = maxInputUsage - cumulativeInputUsage;
			numberOfImagesToDownload = freeDisk / DEFAULT_IMAGE_DIR_SIZE;

			LOGGER.info("maxInputUsage=" + maxInputUsage);
			LOGGER.info("cumulativeInputUsage=" + cumulativeInputUsage);
			LOGGER.info("numberOfImagesToDownload=" + numberOfImagesToDownload);
		} else {
			throw new RuntimeException(
					"ExportDirPath: " + exportDirPath + " is not a directory or does not exist");
		}

		return numberOfImagesToDownload;
	}
	
	private static String getProcessOutput(Process p) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(System.getProperty("line.separator"));
		}
		return stringBuilder.toString();
	}

	protected File getExportDirPath(String volumeDirPath) {
		return new File(volumeDirPath);
	}

	protected boolean downloadImage(final ImageTask imageTask) throws SQLException, IOException {
		DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

		try {
			LOGGER.debug("Image download link is " + imageTask.getDownloadLink());

			// TODO: modify container repository and tag field
			DockerUtil.pullImage(imageTask.getDownloaderContainerRepository(),
					imageTask.getDownloaderContainerTag());

			String containerId = DockerUtil.runMappedContainer(
					imageTask.getDownloaderContainerRepository(),
					imageTask.getDownloaderContainerTag(),
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH) + File.separator
							+ "data" + File.separator + imageTask.getTaskId() + File.separator + "input",
					properties.getProperty(SapsPropertiesConstants.SEBAL_CONTAINER_LINKED_PATH));

			String commandToRun = properties.getProperty(SapsPropertiesConstants.CONTAINER_SCRIPT)
					+ " " + imageTask.getDataSet() + " " + imageTask.getRegion() + " "
					+ dateFormater.format(imageTask.getImageDate()) + " "
					+ properties.getProperty(SapsPropertiesConstants.SEBAL_CONTAINER_LINKED_PATH);

			if (DockerUtil.execDockerCommand(containerId, commandToRun) == 0) {
				updateToDownloadedState(imageTask);

				pendingTaskDownloadMap.remove(imageTask.getTaskId());
				pendingTaskDownloadDB.commit();

				LOGGER.info("Image " + imageTask + " was downloaded");
				return true;
			} else {
				LOGGER.debug("Error while downloading image from task " + imageTask.getTaskId());
				removeFromPendingAndUpdateState(imageTask, properties);
			}

			DockerUtil.removeContainer(containerId);
		} catch (Exception e) {
			LOGGER.error("Error when downloading image " + imageTask, e);
			removeFromPendingAndUpdateState(imageTask, properties);
		}

		return false;
	}

	protected void removeTaskFromPendingMap(final ImageTask imageTask) {
		LOGGER.debug("Removing image task " + imageTask + " from pending image map");
		pendingTaskDownloadMap.remove(imageTask.getTaskId());
		pendingTaskDownloadDB.commit();
	}

	private void updateToDownloadedState(final ImageTask imageTask) throws IOException {
		imageTask.setState(ImageTaskState.DOWNLOADED);

		try {
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(getTaskUpdateTime(imageTask));
		} catch (SQLException e) {
			LOGGER.error("Error while updating task " + imageTask + " to DB", e);
			removeFromPendingAndUpdateState(imageTask, properties);
		}

		addStateStamp(imageTask);
	}

	private void removeFromPendingAndUpdateState(final ImageTask imageTask, Properties properties)
			throws IOException {
		if (imageTask.getFederationMember().equals(federationMember)) {
			LOGGER.debug("Rolling back " + imageTask + " to " + ImageTaskState.CREATED + " state");

			try {
				imageStore.removeStateStamp(imageTask.getTaskId(), imageTask.getState(),
						imageTask.getUpdateTime());
			} catch (SQLException e) {
				LOGGER.error("Error while removing state " + imageTask.getState() + " timestamp "
						+ imageTask.getUpdateTime() + " from DB");
			}

			imageTask.setFederationMember(ImageDataStore.NONE);
			imageTask.setState(ImageTaskState.CREATED);

			try {
				imageStore.updateImageTask(imageTask);
				imageTask.setUpdateTime(getTaskUpdateTime(imageTask));
			} catch (SQLException e) {
				LOGGER.error("Error while updating task " + imageTask.getTaskId(), e);
				imageTask.setFederationMember(federationMember);
				imageTask.setState(ImageTaskState.DOWNLOADING);
				return;
			}

			deleteImageFromDisk(imageTask,
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));

			removeTaskFromPendingMap(imageTask);
			LOGGER.info("Image task " + imageTask + " rolled back");
		}
	}

	protected void deleteImageFromDisk(final ImageTask imageTask, String exportPath)
			throws IOException {
		String imageDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "input";
		File taskInputDir = new File(imageDirPath);

		LOGGER.info("Removing task " + imageTask + " data under path " + imageDirPath);

		if (isImageOnDisk(imageDirPath, taskInputDir)) {
			FileUtils.cleanDirectory(taskInputDir);
		}
	}

	protected boolean isImageOnDisk(String imageDirPath, File imageDir) {
		if (!imageDir.exists() || !imageDir.isDirectory()) {
			LOGGER.info("Path " + imageDirPath + " does not exist");
			return false;
		}
		return true;
	}

	protected void deleteArchivedOutputsFromDisk(Properties properties)
			throws IOException, InterruptedException, SQLException {
		List<ImageTask> allTasks = imageStore.getAllTasks();
		String exportPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageTask imageTask : allTasks) {
				String taskOutputDirPath = exportPath + File.separator + imageTask.getTaskId()
						+ File.separator + "data" + File.separator + "output";
				File taskOutputDir = new File(taskOutputDirPath);

				if (imageTask.getState().equals(ImageTaskState.ARCHIVED)
						&& imageTask.getFederationMember().equals(federationMember)
						&& taskOutputDir.exists()) {
					LOGGER.debug("Task " + imageTask.getTaskId() + " already archived");
					LOGGER.info("Removing " + imageTask);

					try {
						deleteInputsFromDisk(imageTask, exportPath);
						deleteOutputsFromDisk(imageTask, exportPath);
					} catch (IOException e) {
						LOGGER.error("Error while deleting " + imageTask, e);
					}

					LOGGER.debug("Task " + imageTask + " result files deleted");
				} else {
					continue;
				}
			}
		} else {
			LOGGER.error("Export path is null or empty");
		}
	}

	private void deleteInputsFromDisk(ImageTask imageTask, String exportPath) throws IOException {
		String inputDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "input";
		File inputDir = new File(inputDirPath);

		if (!inputDir.exists() || !inputDir.isDirectory()) {
			return;
		}

		LOGGER.debug("Deleting input for " + imageTask + " from " + inputDirPath);
		FileUtils.cleanDirectory(inputDir);
	}

	private void deleteOutputsFromDisk(ImageTask imageTask, String exportPath) throws IOException {
		String outputDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "data" + File.separator + "output";
		File outputDir = new File(outputDirPath);

		if (!outputDir.exists() || !outputDir.isDirectory()) {
			return;
		}

		LOGGER.debug("Deleting output for " + imageTask + " from " + outputDirPath);
		FileUtils.cleanDirectory(outputDir);
	}

	protected void purgeTasksFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {
		List<ImageTask> tasksToPurge = imageStore.getAllTasks();

		String exportPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageTask imageTask : tasksToPurge) {
				if (imageTask.getImageStatus().equals(ImageTask.PURGED)
						&& imageTask.getFederationMember().equals(federationMember)) {
					LOGGER.debug("Purging task " + imageTask);

					try {
						deleteImageFromDisk(imageTask,
								properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));
						deleteOutputsFromDisk(imageTask, exportPath);
					} catch (IOException e) {
						LOGGER.error("Error while deleting " + imageTask, e);
					}

					LOGGER.debug("Task " + imageTask + " purged");
				}
			}
		} else {
			LOGGER.error("Export path is null or empty!");
		}
	}

	public DB getPendingTaskDB() {
		return this.pendingTaskDownloadDB;
	}

	public void setPendingTaskDB(DB pendingTaskDownloadDB) {
		this.pendingTaskDownloadDB = pendingTaskDownloadDB;
	}

	public ConcurrentMap<String, ImageTask> getPendingTaskMap() {
		return this.pendingTaskDownloadMap;
	}

	public void setPendingTaskMap(ConcurrentMap<String, ImageTask> pendingTaskDownloadMap) {
		this.pendingTaskDownloadMap = pendingTaskDownloadMap;
	}
}
