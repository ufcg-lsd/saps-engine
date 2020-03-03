package org.fogbowcloud.saps.engine.core.archiver.storage;

import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.INPUTDOWNLOADING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PREPROCESSING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PROCESSING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.SAPS_TASK_STAGE_DIR_PATTERN;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;

public class NfsPermanentStorage implements PermanentStorage {

    public static final Logger LOGGER = Logger.getLogger(NfsPermanentStorage.class);
    static final String NFS_STORAGE_TASK_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s";

    private final String nfsTempStoragePath;
    private final String nfsPermanentStoragePath;
    private final boolean debugMode;
    //FIXME Remove properties field and add new variables
    private Properties properties;


    public NfsPermanentStorage(Properties properties) throws PermanentStorageException {
        if (!checkProperties(properties))
            throw new PermanentStorageException("Error on validate the file. Missing properties for start Nfs Permanent Storage.");
        this.nfsTempStoragePath = properties.getProperty(SapsPropertiesConstants.SAPS_TEMP_STORAGE_PATH);
        this.nfsPermanentStoragePath = properties.getProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH);
        this.debugMode = properties.containsKey(SapsPropertiesConstants.SAPS_DEBUG_MODE) && properties
            .getProperty(SapsPropertiesConstants.SAPS_DEBUG_MODE).toLowerCase().equals("true");
        if (this.debugMode && !checkPropertiesDebugMode(properties))
            throw new PermanentStorageException("Error on validate the file. Missing properties for start Saps Controller.");
        this.properties = properties;
    }

    private boolean checkProperties(Properties properties) {
        String[] propertiesSet = {
            SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR,
            SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH
        };

        return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    private boolean checkPropertiesDebugMode(Properties properties) {
        if (!properties.containsKey(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR)) {
            LOGGER.error("Required property " + SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR
                + " was not set (it's necessary when debug mode)");
            return false;
        }

        LOGGER.debug("All properties for debug mode are set");
        return true;
    }

    @Override
    public boolean archive(SapsImage task) throws PermanentStorageException {
        String taskId = task.getTaskId();

        LOGGER.info("Archiving task [" + task.getTaskId() + "] to permanent storage.");

        String inputdownloadingLocalDir = String.format(SAPS_TASK_STAGE_DIR_PATTERN,
            nfsTempStoragePath, taskId, INPUTDOWNLOADING_DIR);
        String preprocessingLocalDir = String.format(SAPS_TASK_STAGE_DIR_PATTERN,
            nfsTempStoragePath, taskId, PREPROCESSING_DIR);
        String processingLocalDir = String.format(SAPS_TASK_STAGE_DIR_PATTERN, nfsTempStoragePath, taskId, PROCESSING_DIR);

        String nfsTaskDir = (task.getState() == ImageTaskState.FAILED && this.debugMode)
            ? properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR)
            : properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);
        String nfsTaskDirPath;

        try {
            nfsTaskDirPath = createTaskDir(nfsTaskDir, task.getTaskId());
        } catch (IOException e) {
            throw new PermanentStorageException("Could not create task dir [" + nfsTaskDir + "] on nfs storage [" + nfsPermanentStoragePath
                + "]", e);
        }

        try {
            copyDirToDir(inputdownloadingLocalDir, nfsTaskDirPath);
            copyDirToDir(preprocessingLocalDir, nfsTaskDirPath);
            copyDirToDir(processingLocalDir, nfsTaskDirPath);
        } catch (IOException e) {
            throw new PermanentStorageException("Error while copying local directories to nfs task dir [" + nfsTaskDirPath + "]", e);
        }
        return true;
    }

    @Override
    public boolean delete(SapsImage task) throws PermanentStorageException {
        String nfsTaskDir = (task.getState() == ImageTaskState.FAILED && this.debugMode)
            ? properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR)
            : properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);
        String taskDirPath = String.format(NFS_STORAGE_TASK_DIR_PATTERN, nfsPermanentStoragePath, nfsTaskDir, task.getTaskId());
        File taskDir = new File(taskDirPath);
        if (!taskDir.exists()) {
            throw new PermanentStorageException(
                "The task dir [" + taskDirPath + "] was not found on nfs storage directory ["
                    + nfsPermanentStoragePath + "]");
        }
        try {
            FileUtils.deleteDirectory(taskDir);
        } catch (IOException e) {
            throw new PermanentStorageException(e.getMessage(), e);
        }
        return true;
    }

    @Override
    public List<AccessLink> generateAccessLinks(String taskId) throws PermanentStorageException {
        return null;
    }

    private void copyDirToDir(String src, String dest) throws IOException {
        File srcDir = new File(src);
        File destDir = new File(dest);
        // The destination directory is created if it does not exist.
        // If the destination directory did exist, then this method merges the source with the destination, with the source taking precedence.
        LOGGER.debug("Copying [" + src + "] into [" + dest + "]");
        FileUtils.copyDirectoryToDirectory(srcDir, destDir);
    }

    private String createTaskDir(String tasksDir, String taskId) throws IOException {
        File storageDir = new File(nfsPermanentStoragePath);
        if (!storageDir.exists()) {
            throw new FileNotFoundException("The nfs storage directory [" + nfsPermanentStoragePath
                + "] was not found");
        }
        File nfsTaskDir = new File(String.format(NFS_STORAGE_TASK_DIR_PATTERN,
            nfsPermanentStoragePath, tasksDir, taskId));
        FileUtils.forceMkdir(nfsTaskDir);
        return nfsTaskDir.getAbsolutePath();

    }

}