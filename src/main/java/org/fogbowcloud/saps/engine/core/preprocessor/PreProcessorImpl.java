package org.fogbowcloud.saps.engine.core.preprocessor;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.DockerUtil;
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

			String hostPath = this.getHostPath(imageTask);

			String containerPath = this.properties
					.getProperty(SapsPropertiesConstants.SAPS_CONTAINER_LINKED_PATH);

			this.createPreProcessingHostPath(hostPath);

			String containerId = this.raiseContainer(preProcessorTags, imageTask, hostPath,
					containerPath);

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
			String hostPath, String containerPath) throws Exception {

		String containerId = DockerUtil.runMappedContainer(preProcessorTags.getDockerRepository(),
				preProcessorTags.getDockerTag(), hostPath, containerPath);

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

		this.imageDataStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.PREPROCESSING);

		int dockerExecExitValue = DockerUtil.execDockerCommand(containerId, commandToRun);

		if (!DockerUtil.removeContainer(containerId)) {
			LOGGER.error("Error while trying to stop Container [" + containerId + "]");
		}

		if (dockerExecExitValue == 0) {

			this.imageDataStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.PREPROCESSED);
			LOGGER.debug("Image Task [" + imageTask.getTaskId() + "] preprocessed");

		} else {
			LOGGER.info("Docker container execution exit code [" + dockerExecExitValue + "]");
			this.imageDataStore.updateTaskState(imageTask.getTaskId(), ImageTaskState.FAILED);
			throw new Exception("Container preprocessing execution failed for ImageTask ["
					+ imageTask.getTaskId() + "]");
		}
	}

	protected ExecutionScriptTag getContainerImageTags(ImageTask imageTask) throws SapsException {
		return ExecutionScriptTagUtil.getExecutionScritpTag(imageTask.getInputPreprocessingTag(),
				ExecutionScriptTagUtil.PRE_PROCESSING);
	}

	protected String getHostPath(ImageTask imageTask) {
		return this.properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "data" + File.separator
				+ "preprocessing";
	}
}
