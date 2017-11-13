package org.fogbowcloud.saps.engine.core.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.map.HashedMap;
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

public class PreProcessorImpl implements PreProcessor {

	private ImageDataStore imageDataStore;
	private Properties properties;

	public static final Logger LOGGER = Logger.getLogger(PreProcessorImpl.class);

	public PreProcessorImpl(Properties properties) throws SQLException {
		this(properties, new JDBCImageDataStore(properties));
	}

	protected PreProcessorImpl(Properties properties, ImageDataStore imageDataStore) {
		this.properties = properties;
		this.imageDataStore = imageDataStore;
	}

	@Override
	public void preProcessImage(ImageTask imageTask) {

		try {
			ExecutionScriptTag preProcessorTags = this.getContainerImageTags(imageTask);

			this.getDockerImage(preProcessorTags);

			String hostPreProcessingPath = this.getHostPreProcessingPath(imageTask);

			String containerPreProcessingPath = this.properties
					.getProperty(SapsPropertiesConstants.SAPS_CONTAINER_INPUT_LINKED_PATH);

			this.createPreProcessingHostPath(hostPreProcessingPath);

			String hostMetadataPath = this.getHostMetadataPath(imageTask);

			String containerMetadataPath = this.properties
					.getProperty(SapsPropertiesConstants.SAPS_CONTAINER_METADATA_LINKED_PATH);

			@SuppressWarnings("unchecked")
			Map<String, String> hostAndContainerDirMap = new HashedMap();
			hostAndContainerDirMap.put(hostPreProcessingPath, containerPreProcessingPath);
			hostAndContainerDirMap.put(hostMetadataPath, containerMetadataPath);

			String containerId = this.raiseContainer(preProcessorTags, imageTask,
					hostAndContainerDirMap);

			String commandToRun = SapsPropertiesConstants.DEFAULT_PREPROCESSOR_RUN_SCRIPT_COMMAND;

			this.executeContainer(containerId, commandToRun, imageTask);

		} catch (Exception e) {
			LOGGER.error(
					"Failed in the preprocessing of Image Task [" + imageTask.getTaskId() + "]", e);
		}

	}

	@Override
	public void exec() {
		LOGGER.info("Executing PreProcessor...");
		try {
			while (true) {

				int imagesLimit = 1;

				List<ImageTask> downloadedImages = this.imageDataStore
						.getIn(ImageTaskState.DOWNLOADED, imagesLimit);

				for (ImageTask imageTask : downloadedImages) {
					LOGGER.info("Preprocessing Image Task [" + imageTask.getTaskId() + "]");
					this.preProcessImage(imageTask);
				}

				Thread.sleep(Long.valueOf(this.properties.getProperty(
						SapsPropertiesConstants.PREPROCESSOR_EXECUTION_PERIOD,
						SapsPropertiesConstants.DEFAULT_PREPROCESSOR_EXECUTION_PERIOD)));
			}
		} catch (SQLException e) {
			LOGGER.error("Failed while getting the Downloaded Images from DataBase", e);
		} catch (Exception e) {
			LOGGER.error("Bad number format exception at "
					+ SapsPropertiesConstants.PREPROCESSOR_EXECUTION_PERIOD + " value", e);
		}
	}

	protected void getDockerImage(ExecutionScriptTag preProcessorTags) throws Exception {

		if (!DockerUtil.pullImage(preProcessorTags.getDockerRepository(),
				preProcessorTags.getDockerTag())) {
			throw new Exception("Was not possible get Docker Image from ["
					+ preProcessorTags.getDockerRepository() + "]:["
					+ preProcessorTags.getDockerTag() + "]");
		}
	}

	protected String raiseContainer(ExecutionScriptTag preProcessorTags, ImageTask imageTask,
			Map<String, String> hostAndContainerDirMap) throws Exception {
		String containerId = DockerUtil.runMappedContainer(preProcessorTags.getDockerRepository(),
				preProcessorTags.getDockerTag(), hostAndContainerDirMap);

		if (containerId.isEmpty()) {
			throw new Exception("Was not possible raise the Docker Container ["
					+ preProcessorTags.getDockerRepository() + "]:["
					+ preProcessorTags.getDockerTag() + "]");
		}

		return containerId;
	}

	private void createPreProcessingHostPath(String hostPath) throws Exception {
		File file = new File(hostPath);
		if (!file.exists()) {
			LOGGER.info("Creating directory [" + hostPath + "]");
			if (!file.mkdirs()) {
				throw new Exception(
						"Was not possible create the PreProcessing directory [" + hostPath + "]");
			}
		}
	}

	protected void executeContainer(String containerId, String commandToRun, ImageTask imageTask)
			throws Exception {

		String imageTaskId = imageTask.getTaskId();
		this.imageDataStore.updateTaskState(imageTaskId, ImageTaskState.PREPROCESSING);
		addStateStamp(imageTaskId);

		int dockerExecExitValue = DockerUtil.execDockerCommand(containerId, commandToRun);

		if (!DockerUtil.removeContainer(containerId)) {
			LOGGER.error("Error while trying to stop Container [" + containerId + "]");
		}

		if (dockerExecExitValue == 0) {
			this.imageDataStore.updateTaskState(imageTaskId, ImageTaskState.PREPROCESSED);
			LOGGER.debug("Image Task [" + imageTaskId + "] preprocessed");
		} else {
			LOGGER.info("Docker container execution exit code [" + dockerExecExitValue + "]");
			this.imageDataStore.updateTaskState(imageTaskId, ImageTaskState.FAILED);
			throw new Exception("Container preprocessing execution failed for ImageTask ["
					+ imageTaskId + "]");
		}
		addStateStamp(imageTaskId);
		storeMetadata(imageTask);
	}

	protected File getExportDirPath(String volumeDirPath) {
		return new File(volumeDirPath);
	}

	protected void addStateStamp(String imageTaskId) {
		ImageTask imageTask = null;
		try {
			imageTask = this.imageDataStore.getTask(imageTaskId);			
			this.imageDataStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(), imageTask.getUpdateTime());
		} catch (Exception e) {
			ImageTaskState state = imageTask != null ? imageTask.getState() : null;
			Timestamp updateTime = imageTask != null ? imageTask.getUpdateTime() : null;
			LOGGER.warn("Error while adding state " + state + " timestamp " + updateTime + " in Catalogue", e);
		}
	}
	
	private void storeMetadata(ImageTask imageTask) throws SQLException, IOException {
		LOGGER.info("Storing metadata into Catalogue");
		imageDataStore.updateMetadataInfo(getMetadataFilePath(imageTask), getOperatingSystem(),
				getKernelVersion(), SapsPropertiesConstants.PREPROCESSOR_COMPONENT_TYPE,
				imageTask.getTaskId());
	}

	private String getMetadataFilePath(ImageTask imageTask) {
		return properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH) + File.separator
				+ imageTask.getTaskId() + File.separator + "metadata" + File.separator
				+ "preprocessingDescription.txt";
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

	protected ExecutionScriptTag getContainerImageTags(ImageTask imageTask) throws SapsException {
		return ExecutionScriptTagUtil.getExecutionScritpTag(imageTask.getInputPreprocessingTag(),
				ExecutionScriptTagUtil.PRE_PROCESSING);
	}

	protected String getHostPreProcessingPath(ImageTask imageTask) {
		return this.properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "data" + File.separator
				+ "preprocessing";
	}
	
	protected String getHostMetadataPath(ImageTask imageTask) {
		return this.properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "metadata";
	}

}
