package org.fogbowcloud.saps.engine.core.archiver.storage;

import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.INPUTDOWNLOADING_DIR_PATTERN;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PREPROCESSING_DIR_PATTERN;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PROCESSING_DIR_PATTERN;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.TASK_DIR_PATTERN;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;

public class NfsPermanentStorage implements PermanentStorage {

    public static final Logger LOGGER = Logger.getLogger(NfsPermanentStorage.class);
    private final String sapsExports;
    private final String nfsStoragePath;

    public NfsPermanentStorage(Properties properties) {
        this.sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
        this.nfsStoragePath = properties
            .getProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH);
    }

    @Override
    public boolean archive(SapsImage task) {
        String taskId = task.getTaskId();

        LOGGER.info("Archiving task [" + task.getTaskId() + "] to permanent storage.");

        String inputdownloadingLocalDir = String
            .format(INPUTDOWNLOADING_DIR_PATTERN, sapsExports, taskId);
        String preprocessingLocalDir = String
            .format(PREPROCESSING_DIR_PATTERN, sapsExports, taskId);
        String processingLocalDir = String.format(PROCESSING_DIR_PATTERN, sapsExports, taskId);

        String nfsTaskDirPath = createTaskDir(task.getTaskId());
        try {
            copyDirToDir(inputdownloadingLocalDir, nfsTaskDirPath);
            copyDirToDir(preprocessingLocalDir, nfsTaskDirPath);
            copyDirToDir(processingLocalDir, nfsTaskDirPath);
        } catch (IOException e) {
            throw new PermanentStorageException(
                "Error while copying local directories to nfs task dir [" + nfsTaskDirPath + "]",
                e);
        }
        return true;
    }

    @Override
    public boolean delete(SapsImage task) throws PermanentStorageException {
        String taskDirPath = String.format(TASK_DIR_PATTERN, nfsStoragePath, task.getTaskId());
        File taskDir = new File(taskDirPath);
        if (!taskDir.exists()) {
            throw new PermanentStorageException(
                "The task dir [" + taskDirPath + "] was not found on nfs storage directory ["
                    + nfsStoragePath + "]");
        }
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

    private String createTaskDir(String taskId) {
        File storage_dir = new File(nfsStoragePath);
        if (!storage_dir.exists()) {
            throw new PermanentStorageException(
                "The nfs storage directory [" + nfsStoragePath + "] was not found");
        }
        File nfsTaskDir = new File(String.format(TASK_DIR_PATTERN, nfsStoragePath, taskId));
        try {
            FileUtils.forceMkdir(nfsTaskDir);
        } catch (IOException e) {
            throw new PermanentStorageException(
                "Could not create task dir [" + nfsTaskDir.getAbsolutePath() + "] on nfs storage",
                e);
        }
        return nfsTaskDir.getAbsolutePath();

    }

}