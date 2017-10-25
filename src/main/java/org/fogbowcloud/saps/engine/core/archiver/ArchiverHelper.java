package org.fogbowcloud.saps.engine.core.archiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.CheckSumMD5ForFile;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.mapdb.DB;

public class ArchiverHelper {

	protected static final int NUMBER_OF_INPUT_FILES = 11;
	protected static final int NUMBER_OF_RESULT_FILES = 7;

	public static final Logger LOGGER = Logger.getLogger(ArchiverHelper.class);

	protected void updatePendingMapAndDB(ImageTask imageTask, DB pendingTaskArchiveDB,
			ConcurrentMap<String, ImageTask> pendingTaskArchiveMap) {
		LOGGER.debug("Adding task " + imageTask + " to pending database");
		pendingTaskArchiveMap.put(imageTask.getTaskId(), imageTask);
		pendingTaskArchiveDB.commit();
	}

	protected void removeTaskFromPendingMap(ImageTask imageTask, DB pendingTaskArchiveDB,
			ConcurrentMap<String, ImageTask> pendingTaskArchiveMap) {
		LOGGER.info("Removing task " + imageTask + " from pending map.");
		pendingTaskArchiveMap.remove(imageTask.getTaskId());
		pendingTaskArchiveDB.commit();

		if (pendingTaskArchiveMap.containsKey(imageTask.getTaskId())) {
			LOGGER.debug("There is still register for task " + imageTask + " into Map DB");
		}
	}

	protected String getStationId(ImageTask imageTask, Properties properties) throws IOException {
		String localTaskInputPath = getLocalTaskInputPath(imageTask, properties);
		File localTaskInputDir = new File(localTaskInputPath);

		String stationFilePath = null;
		for (File file : localTaskInputDir.listFiles()) {
			if (file.getName().endsWith("_station.csv")) {
				stationFilePath = file.getName();
			}
		}

		File stationFile = new File(stationFilePath);
		if (stationFile.exists() && stationFile.isFile()) {
			BufferedReader reader = new BufferedReader(new FileReader(stationFile));
			String lineOne = reader.readLine();
			String[] stationAtt = lineOne.split(";");

			String stationId = stationAtt[0];
			reader.close();
			return stationId;
		} else {
			LOGGER.debug("Station file for task " + imageTask.getTaskId()
					+ " does not exist or is not a file!");
			return null;
		}
	}

	protected String getRemoteTaskInputPath(final ImageTask imageTask, Properties properties) {
		return properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH) + File.separator
				+ imageTask.getTaskId() + File.separator + "data" + File.separator + "input";
	}

	protected String getLocalTaskInputPath(ImageTask imageTask, Properties properties) {
		return properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "data" + File.separator
				+ "input";
	}

	protected String getRemoteTaskOutputPath(final ImageTask imageTask, Properties properties) {
		return properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH) + File.separator
				+ imageTask.getTaskId() + File.separator + "data" + File.separator + "output";
	}

	protected String getLocalTaskOutputPath(ImageTask imageTask, Properties properties) {
		return properties.getProperty(SapsPropertiesConstants.LOCAL_INPUT_OUTPUT_PATH)
				+ File.separator + imageTask.getTaskId() + File.separator + "data" + File.separator
				+ "output";
	}

	// TODO: see how to deal with this exception
	protected boolean isTaskCorrupted(ImageTask imageTask,
			ConcurrentMap<String, ImageTask> pendingTaskArchiveMap, ImageDataStore imageStore)
			throws SQLException {
		if (imageTask.getState().equals(ImageTaskState.CORRUPTED)) {
			pendingTaskArchiveMap.remove(imageTask.getTaskId());
			imageStore.updateImageTask(imageTask);
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
			return true;
		}
		return false;
	}

	protected boolean isTaskRolledBack(ImageTask imageTask) {
		if (imageTask.getState().equals(ImageTaskState.FINISHED)) {
			return true;
		}
		return false;
	}

	protected boolean resultsChecksumOK(ImageTask imageTask, File localTaskOutputDir)
			throws Exception {
		LOGGER.info("Checksum of " + imageTask + " output files");
		if (CheckSumMD5ForFile.isFileCorrupted(localTaskOutputDir)) {
			return false;
		}

		return true;
	}

	protected void createTimeoutAndMaxTriesFiles(File localTaskOutputDir) {
		LOGGER.debug("Generating timeout and max tries files");
		ProcessBuilder builder;
		Process p;

		for (File file : localTaskOutputDir.listFiles()) {
			if (file.getName().startsWith("temp-worker-run-") && file.getName().endsWith(".out")) {
				try {
					builder = new ProcessBuilder("/bin/bash", "scripts/create_timeout_file.sh",
							file.getAbsolutePath());
					p = builder.start();
					p.waitFor();

					builder = new ProcessBuilder("/bin/bash", "scripts/create_max_tries_file.sh",
							file.getAbsolutePath());
					p = builder.start();
					p.waitFor();
				} catch (Exception e) {
					LOGGER.debug("Error while generating timeout and max tries files", e);
				}
			}
		}
	}
}
