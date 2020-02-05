package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;

public class NfsPermanentStorage implements PermanentStorage {

	private final String sapsExports;
	private final String nfsStoragePath;

	public static final String TASK_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator;
	public static final String INPUTDOWNLOADING_DIR_PATTERN = TASK_DIR_PATTERN + "inputdownloading";
	public static final String PREPROCESSING_DIR_PATTERN = TASK_DIR_PATTERN + "preprocessing";
	public static final String PROCESSING_DIR_PATTERN = TASK_DIR_PATTERN + "processing";
	public static final Logger LOGGER = Logger.getLogger(NfsPermanentStorage.class);

	public NfsPermanentStorage(Properties properties) {
		this.sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
		this.nfsStoragePath = properties.getProperty("nfs_storage_path");
	}

	@Override
	public boolean archive(SapsImage task) {
		String taskId = task.getTaskId();

		LOGGER.info("Archiving task [" + task.getTaskId() + "] to permanent storage.");

		String inputdownloadingLocalDir = String.format(INPUTDOWNLOADING_DIR_PATTERN, sapsExports, taskId);
		String preprocessingLocalDir = String.format(PREPROCESSING_DIR_PATTERN, sapsExports, taskId);
		String processingLocalDir = String.format(PROCESSING_DIR_PATTERN, sapsExports, taskId);

		try {
			File nfsTaskDir = new File(String.format(TASK_DIR_PATTERN, nfsStoragePath, taskId));
			FileUtils.forceMkdir(nfsTaskDir);
			copyDirToDir(inputdownloadingLocalDir, nfsTaskDir.getAbsolutePath());
			copyDirToDir(preprocessingLocalDir, nfsTaskDir.getAbsolutePath());
			copyDirToDir(processingLocalDir, nfsTaskDir.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public boolean delete(SapsImage task) throws PermanentStorageException {
		String taskDirPath = String.format(TASK_DIR_PATTERN, nfsStoragePath, task.getTaskId());
		File taskDir = new File(taskDirPath);
		try {
			FileUtils.deleteDirectory(taskDir);
		} catch (IOException e) {
			throw new PermanentStorageException(e.getMessage(), e);
		}
		return true;
	}

	private void copyDirToDir(String src, String dest) throws IOException {
		File srcDir = new File(src);
		File destDir = new File(dest);
		// The destination directory is created if it does not exist.
		// If the destination directory did exist, then this method merges the source with the destination, with the source taking precedence.
		LOGGER.debug("Copying [" + src + "] into [" + dest + "]");
		FileUtils.copyDirectoryToDirectory(srcDir, destDir);
	}

}