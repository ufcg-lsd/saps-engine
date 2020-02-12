package org.fogbowcloud.saps.engine.core.archiver.storage;

import static org.fogbowcloud.saps.engine.core.archiver.storage.NfsPermanentStorage.NFS_STORAGE_TASK_DIR_PATTERN;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.INPUTDOWNLOADING_FOLDER;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PREPROCESSING_FOLDER;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PROCESSING_FOLDER;

import java.io.File;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NfsPermanentStorageTest {

    private static final String MOCK_SAPS_EXPORT_PATH = "src/test/resources/archiver-test";
    private static final String MOCK_NFS_STORAGE_PATH = "src/test/resources/nfs-storage-test";
    private static final String MOCK_NFS_TASKS_FOLDER = "archiver";
    private static final String NONEXISTENT_NFS_STORAGE_PATH = "src/test/resources/nfs-storage-test-2";
    private static final String NFS_TASK_STAGE_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s" + File.separator + "%s";
    private static final String DEBUG_MODE_TRUE = "true";
    private static final String MOCK_NFS_DEBUG_TASKS_FOLDER = "debug";

    private static class TestFile {
        private static final String INPUTDOWNLOADING = "file.ip";
        private static final String PREPROCESSING = "file.pp";
        private static final String PROCESSING = "file.p";
    }

    private Properties properties;

    @Before
    public void setUp() {
        properties =  new Properties();
        properties.setProperty(SapsPropertiesConstants.SAPS_TEMP_STORAGE_PATH, MOCK_SAPS_EXPORT_PATH);
        properties.setProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_FOLDER, MOCK_NFS_TASKS_FOLDER);
    }

    @Test
    public void testArchive() throws SapsException {
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        permanentStorage.archive(task);
        Assert.assertTrue(assertTaskDir(MOCK_NFS_TASKS_FOLDER, task.getTaskId()));
    }

    @Test
    public void testArchiveOnDebugMode() throws SapsException {
        properties.setProperty(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE, DEBUG_MODE_TRUE);
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        properties.setProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_FOLDER, MOCK_NFS_DEBUG_TASKS_FOLDER);

        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FAILED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        permanentStorage.archive(task);
        Assert.assertTrue(assertTaskDir(MOCK_NFS_DEBUG_TASKS_FOLDER, task.getTaskId()));
    }

    @Test(expected = PermanentStorageException.class)
    public void testArchiveNonExistentNfsStoragePath() throws SapsException {
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, NONEXISTENT_NFS_STORAGE_PATH);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        permanentStorage.archive(task);
    }

    @Test
    public void testDelete() throws Exception {
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        File taskDir = new File(String.format(NFS_STORAGE_TASK_DIR_PATTERN, MOCK_NFS_STORAGE_PATH,
            MOCK_NFS_TASKS_FOLDER, task.getTaskId()));
//        FileUtils.forceMkdir(taskDir);

        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        permanentStorage.delete(task);
        Assert.assertFalse(taskDir.exists());
    }

    @Test
    public void testDeleteOnDebugMode() throws Exception {
        properties.setProperty(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE, DEBUG_MODE_TRUE);
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        properties.setProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_FOLDER, MOCK_NFS_DEBUG_TASKS_FOLDER);

        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FAILED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        String taskDirPath = String.format(NFS_STORAGE_TASK_DIR_PATTERN, MOCK_NFS_STORAGE_PATH, MOCK_NFS_TASKS_FOLDER, task.getTaskId());
        File taskDir = new File(taskDirPath);

        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        permanentStorage.delete(task);
        Assert.assertFalse(taskDir.exists());
    }

    @Test(expected = PermanentStorageException.class)
    public void testDeleteNonExistentTaskDir() throws SapsException {
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        String fakeTaskId = "fake";
        SapsImage task = new SapsImage(fakeTaskId, "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        permanentStorage.delete(task);
    }

    private boolean assertTaskDir(String taskFolder, String taskId) {
        File inputDir = new File(String.format(NFS_TASK_STAGE_DIR_PATTERN, MOCK_NFS_STORAGE_PATH, taskFolder, taskId, INPUTDOWNLOADING_FOLDER));
        File preprocessingDir = new File(String.format(NFS_TASK_STAGE_DIR_PATTERN, MOCK_NFS_STORAGE_PATH, taskFolder, taskId, PREPROCESSING_FOLDER));
        File processingDir = new File(String.format(NFS_TASK_STAGE_DIR_PATTERN, MOCK_NFS_STORAGE_PATH, taskFolder, taskId, PROCESSING_FOLDER));
        return inputDir.exists() && preprocessingDir.exists() && processingDir.exists()
            && containsFile(inputDir, TestFile.INPUTDOWNLOADING) && containsFile(preprocessingDir, TestFile.PREPROCESSING)
            && containsFile(processingDir, TestFile.PROCESSING);
    }

    private boolean containsFile(File dir, String fileName) {
        if (dir.isDirectory()) {
            return Objects.requireNonNull(dir.listFiles((dir1, name) -> name.equals(fileName))).length != 0;
        }
        return false;
    }
}