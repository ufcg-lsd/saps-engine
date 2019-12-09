package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.util.CheckSumMD5ForFile;
import org.mapdb.DB;

public class ArchiverHelper {

	protected static final int NUMBER_OF_INPUT_FILES = 11;
	protected static final int NUMBER_OF_RESULT_FILES = 7;

	public static final Logger LOGGER = Logger.getLogger(ArchiverHelper.class);

	protected void updatePendingMapAndDB(SapsImage imageTask, DB pendingTaskArchiveDB,
			ConcurrentMap<String, SapsImage> pendingTaskArchiveMap) {
		LOGGER.debug("Adding task " + imageTask + " to pending database");
		pendingTaskArchiveMap.put(imageTask.getTaskId(), imageTask);
		pendingTaskArchiveDB.commit();
	}

	protected void removeTaskFromPendingMap(SapsImage imageTask, DB pendingTaskArchiveDB,
			ConcurrentMap<String, SapsImage> pendingTaskArchiveMap) {
		LOGGER.info("Removing task " + imageTask + " from pending map.");
		pendingTaskArchiveMap.remove(imageTask.getTaskId());
		pendingTaskArchiveDB.commit();

		if (pendingTaskArchiveMap.containsKey(imageTask.getTaskId())) {
			LOGGER.debug("There is still register for task " + imageTask + " into Map DB");
		}
	}

	protected boolean resultsChecksumOK(SapsImage imageTask, File localTaskOutputDir) throws Exception {
		LOGGER.info("Checksum of " + imageTask + " output files");
		if (CheckSumMD5ForFile.isFileCorrupted(localTaskOutputDir)) {
			return false;
		}

		return true;
	}
}
