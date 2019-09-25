package org.fogbowcloud.saps.engine.core.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.DockerUtil;
import org.fogbowcloud.saps.engine.core.util.OSValidator;
import org.fogbowcloud.saps.engine.scheduler.core.exception.SapsException;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class InputDownloader {

	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private final Properties properties;
	private final ImageDataStore imageStore;
	private String inputDownloaderIp;
	private String inputDownloaderPort;
	private String inputDownloaderNfsPort;
	private final int maxDownloadAttempts;
	private String federationMember;
	private File pendingImageDownloadFile;

	protected DB pendingTaskDownloadDB;
	protected ConcurrentMap<String, ImageTask> pendingTaskDownloadMap;

	private static final String PENDING_TASK_DOWNLOAD_DB_FILE = "pending-task-download.db";
	private static final long DEFAULT_IMAGE_DIR_SIZE = 180 * FileUtils.ONE_MB;

	private static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";
	private static final int DEFAULT_MAX_DOWNLOAD_ATTEMPTS = 3;
	protected static final int MAX_IMAGES_TO_DOWNLOAD = 1;
	
	// Script execution codes
	protected static final int OK_SCRIPT_CODE = 0;
	protected static final int NOT_FOUNT_SCRIPT_CODE = 3;
	
	protected static final String IMAGE_NOT_FOUND_FAILED_MSG = "Input Downloader does not found image.";

	public static final Logger LOGGER = Logger.getLogger(InputDownloader.class);
	
	public InputDownloader(Properties properties, String inputDownloderIp,
			String InputDownloaderSshPort, String inputDownloaderNfsPort, String federationMember)
			throws SQLException {
		this(properties, new JDBCImageDataStore(properties), inputDownloderIp,
				InputDownloaderSshPort, inputDownloaderNfsPort, federationMember);
		LOGGER.info("Creating Input Downloader in federation " + federationMember);
	}

	protected InputDownloader(Properties properties, ImageDataStore imageStore,
			String inputDownloaderIP, String inputDownloaderPort, String inputDownloaderNfsPort,
			String federationMember) {
		try {
			checkProperties(properties, imageStore, inputDownloaderIP, inputDownloaderPort,
					inputDownloaderNfsPort, federationMember);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Error while getting properties", e);
			System.exit(1);
		}

		this.inputDownloaderIp = inputDownloaderIP;
		this.inputDownloaderPort = inputDownloaderPort;
		this.inputDownloaderNfsPort = inputDownloaderNfsPort;

		this.properties = properties;
		this.imageStore = imageStore;
		this.federationMember = federationMember;

		this.pendingImageDownloadFile = new File(PENDING_TASK_DOWNLOAD_DB_FILE);
		this.pendingTaskDownloadDB = DBMaker.newFileDB(pendingImageDownloadFile).make();

		maxDownloadAttempts = (properties
				.getProperty(SapsPropertiesConstants.MAX_DOWNLOAD_ATTEMPTS) == null)
						? DEFAULT_MAX_DOWNLOAD_ATTEMPTS
						: Integer.parseInt(properties
								.getProperty(SapsPropertiesConstants.MAX_DOWNLOAD_ATTEMPTS));

		if (!pendingImageDownloadFile.exists() || !pendingImageDownloadFile.isFile()) {
			LOGGER.info("Creating map of pending images to download");
			this.pendingTaskDownloadMap = pendingTaskDownloadDB.createHashMap("map").make();
		} else {
			LOGGER.info("Loading map of pending images to download");
			this.pendingTaskDownloadMap = pendingTaskDownloadDB.getHashMap("map");
		}
	}

	private void checkProperties(Properties properties, ImageDataStore imageStore,
			String inputDownloaderIP, String inputDownloaderPort, String inputDownloaderNfsPort,
			String federationMember) throws IllegalArgumentException {
		if (properties == null) {
			throw new IllegalArgumentException("Properties arg must not be null.");
		}

		if (imageStore == null) {
			throw new IllegalArgumentException("Imagestore arg must not be null.");
		}

		if (inputDownloaderIP == null) {
			throw new IllegalArgumentException("Crawler IP arg must not be null.");
		}

		if (inputDownloaderPort == null) {
			throw new IllegalArgumentException("Crawler Port arg must not be null.");
		}

		if (inputDownloaderPort.isEmpty()) {
			throw new IllegalArgumentException("Crawler Port arg must not be null.");
		}

		if (inputDownloaderNfsPort == null) {
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
				deleteArchivedTasksFromDisk(properties);

				double numToDownload = numberOfImagesToDownload();
				if (numToDownload > 0) {
					download();
				} else {
					Thread.sleep(Long.valueOf(properties
							.getProperty(SapsPropertiesConstants.DEFAULT_DOWNLOADER_PERIOD)));
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

			imageStore.addDeployConfig(inputDownloaderIp, inputDownloaderPort,
					inputDownloaderNfsPort, federationMember);
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
			deletePreprocessFromDisk(imageTask,
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));
			deleteOutputsFromDisk(imageTask,
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));
		}
	}

	protected File getInputDir(Properties properties, ImageTask imageTask) {
		String exportPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		String inputDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator + "inputdownloading";
		File imageDir = new File(inputDirPath);
		return imageDir;
	}

	protected boolean isThereTaskInputs(File taskDir) {
		if (taskDir.exists() && taskDir.list().length > 0) {
			for (File file : taskDir.listFiles()) {
				if (file.getName().endsWith("MTL.txt")) {
					return true;
				}
			}
		}

		return false;
	}

	// This updates images in CREATED state to DOWNLOADING
	// and sets this federation member as owner,
	// and then gets all images marked as DOWNLOADING
	protected void download() throws SQLException, IOException, SapsException {
		List<ImageTask> tasksToDownload = new ArrayList<ImageTask>();

		try {
			tasksToDownload = this.imageStore.getImagesToDownload(this.federationMember,
					MAX_IMAGES_TO_DOWNLOAD);
		} catch (SQLException e) {
			LOGGER.error("Error while accessing created tasks in Catalogue.", e);
		} catch (IndexOutOfBoundsException e) {
			return;
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
						storeMetadata(imageTask);
					} else {
						LOGGER.info("Task " + imageTask.getTaskId() + " download failed");
					}
				}
			}
		}
	}

	private void storeMetadata(ImageTask imageTask) throws SQLException, IOException {
		LOGGER.info("Storing metadata into Catalogue");
		if (replacePathsIntoFile(imageTask) && assertMetadataRegisterExists(imageTask)) {
			imageStore.updateMetadataInfo(getMetadataFilePath(imageTask), getOperatingSystem(),
					getKernelVersion(), SapsPropertiesConstants.INPUT_DOWNLOADER_COMPONENT_TYPE,
					imageTask.getTaskId());
		}
	}

	private boolean assertMetadataRegisterExists(ImageTask imageTask) throws SQLException {
		try {
			if (!imageStore.metadataRegisterExist(imageTask.getTaskId())) {
				LOGGER.debug("Task " + imageTask.getTaskId()
						+ " metadata register not exist yet...Creating one");
				imageStore.dispatchMetadataInfo(imageTask.getTaskId());
			}
		} catch (Exception e) {
			LOGGER.error("Error while updating metadata register for task " + imageTask, e);
			return false;
		}

		return true;
	}

	protected boolean replacePathsIntoFile(ImageTask imageTask) {
		String containerInputPath = properties
				.getProperty(SapsPropertiesConstants.SAPS_CONTAINER_INPUT_LINKED_PATH);
		String localInputPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "inputdownloading";

		Path path = Paths.get(getMetadataFilePath(imageTask));
		Charset charset = StandardCharsets.UTF_8;

		try {
			String content = new String(Files.readAllBytes(path), charset);
			content = content.replaceAll(containerInputPath, localInputPath);
			Files.write(path, content.getBytes(charset));
		} catch (IOException e) {
			LOGGER.error("Error while replacing " + containerInputPath + " for "
					+ localInputPath + " in " + getMetadataFilePath(imageTask) + " file");
			return false;
		}

		LOGGER.debug("Successfully replaced " + containerInputPath + " by " + localInputPath
				+ " in " + getMetadataFilePath(imageTask));
		return true;
	}

	private String getOperatingSystem() {
		if (OSValidator.isWindows()) {
			return "Windows";
		} else if (OSValidator.isMac()) {
			return "Mac";
		} else if (OSValidator.isUnix()) {
			return "Linux";
		} else if (OSValidator.isSolaris()) {
			return "Solaris";
		} else {
			return "Operating System Not Recognized";
		}
	}
	
	private String getKernelVersion() throws IOException {
		if (OSValidator.isUnix()) {
			ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c",
					"uname -r | cut -d \'-\' -f 1");
			Process p = null;
			try {
				p = pb.start();
				p.waitFor();
			} catch (IOException e) {
				LOGGER.error("Error while getting Linux kernel version", e);
			} catch (InterruptedException e) {
				LOGGER.error("Error while getting Linux kernel version", e);
			}

			return getProcessOutput(p);
		} else {
			return "Kernel version not recognized";
		}
	}

	private String getMetadataFilePath(ImageTask imageTask) {
		return properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH) + File.separator
				+ imageTask.getTaskId() + File.separator + "metadata" + File.separator
				+ "inputDescription.txt";
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
				.valueOf(properties.getProperty(SapsPropertiesConstants.MAX_NUMBER_OF_TASKS))
				* DEFAULT_IMAGE_DIR_SIZE;

		double numberOfImagesToDownload = 0.0;
		double cumulativeInputUsage = 0.0;
		String actualInputUsage = null;

		if (exportDir.exists() && exportDir.isDirectory()) {
			String[] taskDirNames = exportDir.list();
			for (String taskDir : taskDirNames) {
				if (!taskDir.equals("lost+found")) {
					String inputDirPath = exportDirPath + File.separator + taskDir + File.separator
							+ "inputdownloading";

					ProcessBuilder builder = new ProcessBuilder("du", "-sh", "-b", inputDirPath);
					try {
						Process p = builder.start();
						p.waitFor();

						actualInputUsage = getProcessOutput(p);
						
						if (actualInputUsage != null && !actualInputUsage.isEmpty()) {
							String[] splited = actualInputUsage.split("\\s+");
							actualInputUsage = splited[0];
						} else {
							actualInputUsage = "0";
						}

						cumulativeInputUsage += Double.valueOf(actualInputUsage);
					} catch (Exception e) {
						LOGGER.error("Error while getting input disk usage", e);
					}
				}
			}

			double freeDisk = maxInputUsage - cumulativeInputUsage;
			numberOfImagesToDownload = freeDisk / DEFAULT_IMAGE_DIR_SIZE;
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

	protected boolean downloadImage(final ImageTask imageTask) throws SQLException, IOException, SapsException {
		try {
			prepareTaskDirStructure(imageTask);
		} catch (Exception e) {
			LOGGER.error(e);
			removeFromPendingAndUpdateState(imageTask, properties);
			return false;
		}

		DateFormat dateFormater = new SimpleDateFormat(DATE_FORMAT);
		for (int currentTry = 0; currentTry < this.maxDownloadAttempts; currentTry++) {
			LOGGER.debug("Image download link is " + imageTask.getDownloadLink());
			String rootDirPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
					+ File.separator + imageTask.getTaskId();

			// Getting Input Downloader docker repository and tag
			ExecutionScriptTag inputDownloaderDockerInfo = ExecutionScriptTagUtil.getExecutionScritpTag(
					imageTask.getInputGatheringTag(), ExecutionScriptTagUtil.INPUT_DOWNLOADER);

			DockerUtil.pullImage(inputDownloaderDockerInfo.getDockerRepository(),
					inputDownloaderDockerInfo.getDockerTag());

			@SuppressWarnings("unchecked")
			Map<String, String> hostAndContainerDirMap = new HashedMap();
			hostAndContainerDirMap.put(rootDirPath, properties
					.getProperty(SapsPropertiesConstants.SAPS_CONTAINER_INPUT_LINKED_PATH));

			String containerId = DockerUtil.runMappedContainer(
					inputDownloaderDockerInfo.getDockerRepository(),
					inputDownloaderDockerInfo.getDockerTag(), hostAndContainerDirMap);

			String dataset = formatDataSet(imageTask.getDataset());

			String commandToRun = properties.getProperty(SapsPropertiesConstants.CONTAINER_SCRIPT) + " "
					+ properties.getProperty(SapsPropertiesConstants.SAPS_CONTAINER_INPUT_LINKED_PATH) + " " + dataset 
                                        + " " + imageTask.getRegion() + " " + dateFormater.format(imageTask.getImageDate());

			LOGGER.debug("Command: " + commandToRun);

			String usgsEnvVars = "-e USGS_USERNAME=" + properties.getProperty(SapsPropertiesConstants.USGS_USERNAME)
					+ " -e USGS_PASSWORD=" + properties.getProperty(SapsPropertiesConstants.USGS_PASSWORD);

			int dockerExecExitValue = DockerUtil.execDockerCommand(containerId, usgsEnvVars, commandToRun);
			DockerUtil.removeContainer(containerId);

			if (dockerExecExitValue == OK_SCRIPT_CODE) {
				updateToDownloadedState(imageTask);

				this.pendingTaskDownloadMap.remove(imageTask.getTaskId());
				this.pendingTaskDownloadDB.commit();

				LOGGER.info("Image " + imageTask + " was downloaded.");
				return true;
			} else if (dockerExecExitValue == NOT_FOUNT_SCRIPT_CODE) {			
				updateTaskStateToFailed(imageTask, IMAGE_NOT_FOUND_FAILED_MSG);
				updateTaskStatus(imageTask, ImageTask.UNAVAILABLE);

				this.pendingTaskDownloadMap.remove(imageTask.getTaskId());
				this.pendingTaskDownloadDB.commit();
				
				LOGGER.info("Image " + imageTask + " failed because image not found. "
						+ "This Image is " + ImageTask.UNAVAILABLE + ".");
				return false;
			} else {
				LOGGER.debug("Docker execution code for " + imageTask + " is " + dockerExecExitValue + ".");
				if (currentTry == maxDownloadAttempts - 1) {
					String errorMsg = "Error while downloading task...download retries "
							+ maxDownloadAttempts + " exceeded.";
					LOGGER.debug("Error while downloading image from task " + imageTask.getTaskId()
							+ " in the last try, removing task.");
					updateTaskStateToFailed(imageTask, errorMsg);
					
					this.pendingTaskDownloadMap.remove(imageTask.getTaskId());
					this.pendingTaskDownloadDB.commit();

					deleteAllTaskFiles(imageTask,
							properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));
				} else {
					LOGGER.debug("Error while downloading image from task " + imageTask.getTaskId()
							+ ". Trying " + (currentTry - maxDownloadAttempts) + " more time(s).");
				}
			}
		}

		return false;
	}

	private String formatDataSet(String dataset) {
		if (dataset.equals(SapsPropertiesConstants.LANDSAT_5_DATASET)) {
			return SapsPropertiesConstants.DATASET_LT5_TYPE;
		} else if (dataset.equals(SapsPropertiesConstants.LANDSAT_7_DATASET)) {
			return SapsPropertiesConstants.DATASET_LE7_TYPE;
		} else {
			return SapsPropertiesConstants.DATASET_LC8_TYPE;
		}
	}

	protected void prepareTaskDirStructure(ImageTask imageTask) throws Exception {
		LOGGER.info("Creating directory structure for task" + imageTask.getTaskId());

		String inputDirPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "inputdownloading";
		String outputDirPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "processing";
		String preProcessDirPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "preprocessing";

		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		File preProcessDir = new File(preProcessDirPath);

		if (!createDirectories(imageTask, inputDir, outputDir, preProcessDir)) {
			throw new Exception(
					"Error while creating directories for task " + imageTask.getTaskId());
		}
	}

	protected boolean createDirectories(ImageTask imageTask, File inputDir, File outputDir,
			File preProcessDir) throws Exception {
		try {
			if (!inputDir.exists()) {
				LOGGER.debug("Creating directory: " + inputDir.getAbsolutePath());
				inputDir.mkdirs();
			}

			if (!outputDir.exists()) {
				LOGGER.debug("Creating directory: " + outputDir.getAbsolutePath());
				outputDir.mkdirs();
			}

			if (!preProcessDir.exists()) {
				LOGGER.debug("Creating directory: " + preProcessDir.getAbsolutePath());
				preProcessDir.mkdirs();
			}
		} catch (Exception e) {
			LOGGER.error("Error while creating directories for task " + imageTask.getTaskId());
			return false;
		}

		return true;
	}

	protected void updateTaskStatus(ImageTask imageTask, String status) {
		LOGGER.debug("Updating image task " + imageTask + " status to " + status + ".");
		try {
			String id = imageTask.getTaskId();
			this.imageStore.updateTaskStatus(id, status);
		} catch (Exception e) {
			LOGGER.debug("Error while updating " + imageTask + " status to " + status + ".");
		}
	}
	
	protected void updateTaskStateToFailed(ImageTask imageTask, String errorMsg) {
		String id = imageTask.getTaskId();
		LOGGER.debug("Updating image task " + imageTask + " state to Failed.");
		try {
			imageStore.updateTaskState(id, ImageTaskState.FAILED);
		} catch (SQLException e) {
			LOGGER.debug("Error while updating " + imageTask + " state to Failed.");
		}
		try {
			LOGGER.debug("Updating image task " + imageTask + " with error reason.");
			imageStore.updateTaskError(id, errorMsg);
		} catch (SQLException e) {
			LOGGER.debug("Error while updating " + imageTask + ".");
		}
	}
	
	private void deleteInputsFromDisk(ImageTask imageTask, String exportPath) throws IOException {
		String inputDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "inputdownloading";
		File inputDir = new File(inputDirPath);

		if (!inputDir.exists() || !inputDir.isDirectory()) {
			return;
		}

		LOGGER.debug("Deleting input for " + imageTask + " from " + inputDirPath);
		FileUtils.deleteDirectory(inputDir);
	}

	private void deletePreprocessFromDisk(ImageTask imageTask, String exportPath)
			throws IOException {
		String preProcessDirPath = exportPath + File.separator + imageTask.getTaskId()
				+ File.separator + "preprocessing";
		File preProcessDir = new File(preProcessDirPath);

		if (!preProcessDir.exists() || !preProcessDir.isDirectory()) {
			return;
		}

		LOGGER.debug("Deleting input for " + imageTask + " from " + preProcessDirPath);
		FileUtils.deleteDirectory(preProcessDir);
	}

	private void deleteOutputsFromDisk(ImageTask imageTask, String exportPath) throws IOException {
		String outputDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "processing";
		File outputDir = new File(outputDirPath);

		if (!outputDir.exists() || !outputDir.isDirectory()) {
			return;
		}

		LOGGER.debug("Deleting output for " + imageTask + " from " + outputDirPath);
		FileUtils.deleteDirectory(outputDir);
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

			deleteTaskInputFromDisk(imageTask,
					properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH));

			removeTaskFromPendingMap(imageTask);
			LOGGER.info("Image task " + imageTask + " rolled back");
		}
	}

	protected void deleteTaskInputFromDisk(final ImageTask imageTask, String exportPath)
			throws IOException {
		String imageDirPath = exportPath + File.separator + imageTask.getTaskId() + File.separator
				+ "inputdownloading";
		File taskInputDir = new File(imageDirPath);

		LOGGER.info("Removing task " + imageTask + " data under path " + imageDirPath);

		if (isImageOnDisk(imageDirPath, taskInputDir)) {
			FileUtils.deleteDirectory(taskInputDir);
		}
	}

	protected boolean isImageOnDisk(String imageDirPath, File imageDir) {
		if (!imageDir.exists() || !imageDir.isDirectory()) {
			LOGGER.info("Path " + imageDirPath + " does not exist");
			return false;
		}
		return true;
	}

	protected void deleteArchivedTasksFromDisk(Properties properties)
			throws IOException, InterruptedException, SQLException {
		List<ImageTask> allTasks = imageStore.getAllTasks();
		String exportPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageTask imageTask : allTasks) {
				String taskOutputDirPath = exportPath + File.separator + imageTask.getTaskId()
						+ File.separator + "processing";
				File taskOutputDir = new File(taskOutputDirPath);

				if (imageTask.getState().equals(ImageTaskState.ARCHIVED)
						&& imageTask.getFederationMember().equals(federationMember)
						&& taskOutputDir.exists()) {
					LOGGER.debug("Task " + imageTask.getTaskId() + " already archived");
					LOGGER.info("Removing " + imageTask);

					try {
						deleteAllTaskFiles(imageTask, exportPath);
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

	private void deleteAllTaskFiles(ImageTask imageTask, String exportPath) throws IOException {
		String taskDirPath = exportPath + File.separator + imageTask.getTaskId();
		File taskDir = new File(taskDirPath);

		if (!taskDir.exists() || !taskDir.isDirectory()) {
			return;
		}

		LOGGER.debug("Deleting files for " + imageTask + " from " + taskDirPath);
		FileUtils.deleteDirectory(taskDir);
	}

	protected void purgeTasksFromVolume(Properties properties)
			throws IOException, InterruptedException, SQLException {
		List<ImageTask> tasksToPurge = imageStore.getAllTasks();

		String exportPath = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);

		if (!exportPath.isEmpty() && exportPath != null) {
			for (ImageTask imageTask : tasksToPurge) {
				if (imageTask.getStatus().equals(ImageTask.PURGED)
						&& imageTask.getFederationMember().equals(federationMember)) {
					LOGGER.debug("Purging task " + imageTask);

					try {
						deleteAllTaskFiles(imageTask, exportPath);
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
	
	protected ImageDataStore getImageStore() {
		return imageStore;
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
