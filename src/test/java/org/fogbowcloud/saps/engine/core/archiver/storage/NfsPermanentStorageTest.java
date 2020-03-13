package org.fogbowcloud.saps.engine.core.archiver.storage;

import static org.fogbowcloud.saps.engine.core.archiver.storage.nfs.NfsPermanentStorage.NFS_STORAGE_TASK_DIR_PATTERN;
import static org.fogbowcloud.saps.engine.core.archiver.storage.nfs.NfsPermanentStorage.NFS_STORAGE_TASK_URL_PATTERN;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.INPUTDOWNLOADING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PREPROCESSING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PROCESSING_DIR;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.archiver.storage.nfs.NfsPermanentStorage;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NfsPermanentStorageTest {

    private static final String MOCK_TEMP_STORAGE_PATH = "src/test/resources/archiver-test";
    private static final String MOCK_NFS_STORAGE_PATH = "src/test/resources/nfs-storage-test";
    private static final String MOCK_NFS_TASKS_DIR_NAME = "archiver";
    private static final String MOCK_NFS_BASE_URL = "http://permanent-storage-access-ip";
    private static final String NONEXISTENT_NFS_STORAGE_PATH = "src/test/resources/nfs-storage-test-2";
    private static final String NFS_TASK_STAGE_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s" + File.separator + "%s";
    private static final String DEBUG_MODE_TRUE = "true";
    private static final String MOCK_NFS_DEBUG_TASKS_DIR_NAME = "debug";
    private static final String MOCK_TASK_ID = "task-id";

    private static class TestFile {
        private static final String INPUTDOWNLOADING = "file.ip";
        private static final String PREPROCESSING = "file.pp";
        private static final String PROCESSING = "file.p";
    }

    private static class TestGenerateAccessLink {
        private static class Inputdownloading {
            private static final String NAME = INPUTDOWNLOADING_DIR;
            private static final String URL = String.format(NFS_STORAGE_TASK_URL_PATTERN, MOCK_NFS_BASE_URL, MOCK_TASK_ID) +
                    File.separator + INPUTDOWNLOADING_DIR;
        }
        private static class Preprocessing {
            private static final String NAME = PREPROCESSING_DIR;
            private static final String URL = String.format(NFS_STORAGE_TASK_URL_PATTERN, MOCK_NFS_BASE_URL, MOCK_TASK_ID) +
                    File.separator + PREPROCESSING_DIR;
        }
        private static class Processing {
            private static final String NAME = PROCESSING_DIR;
            private static final String URL = String.format(NFS_STORAGE_TASK_URL_PATTERN, MOCK_NFS_BASE_URL, MOCK_TASK_ID) +
                    File.separator + PROCESSING_DIR;
        }
    }

    private Properties properties;

    @Before
    public void setUp() throws IOException {
        FileUtils.deleteDirectory(new File(MOCK_NFS_STORAGE_PATH));
        FileUtils.forceMkdir(new File(MOCK_NFS_STORAGE_PATH));
        properties =  new Properties();
        properties.setProperty(SapsPropertiesConstants.SAPS_TEMP_STORAGE_PATH,
            MOCK_TEMP_STORAGE_PATH);
        properties.setProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR,
            MOCK_NFS_TASKS_DIR_NAME);
        properties.setProperty(SapsPropertiesConstants.PERMANENT_STORAGE_BASE_URL,
                MOCK_NFS_BASE_URL);
    }

    @Test
    public void testArchive() throws InvalidPropertyException, IOException {
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        permanentStorage.archive(task.getTaskId(), task.isFailed());
        Assert.assertTrue(assertTaskDir(MOCK_NFS_TASKS_DIR_NAME, task.getTaskId()));
    }

    @Test
    public void testArchiveOnDebugMode() throws InvalidPropertyException, IOException {
        properties.setProperty(SapsPropertiesConstants.SAPS_DEBUG_MODE, DEBUG_MODE_TRUE);
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        properties.setProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR,
            MOCK_NFS_DEBUG_TASKS_DIR_NAME);

        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FAILED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        permanentStorage.archive(task.getTaskId(), task.isFailed());
        Assert.assertTrue(assertTaskDir(MOCK_NFS_DEBUG_TASKS_DIR_NAME, task.getTaskId()));
    }

    @Test(expected = IOException.class)
    public void testArchiveNonExistentNfsStoragePath() throws IOException, InvalidPropertyException {
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, NONEXISTENT_NFS_STORAGE_PATH);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        permanentStorage.archive(task.getTaskId(), task.isFailed());
    }

    @Test
    public void testDelete() throws Exception {
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        File taskTempDir = new File(MOCK_TEMP_STORAGE_PATH + File.separator + task.getTaskId());
        FileUtils.copyDirectoryToDirectory(taskTempDir, new File(MOCK_NFS_STORAGE_PATH + File.separator + MOCK_NFS_TASKS_DIR_NAME));
        File taskDir = new File(String.format(NFS_STORAGE_TASK_DIR_PATTERN, MOCK_NFS_STORAGE_PATH,
            MOCK_NFS_TASKS_DIR_NAME, task.getTaskId()));

        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        permanentStorage.delete(task.getTaskId(), task.isFailed());
        Assert.assertFalse(taskDir.exists());
    }

    @Test
    public void testDeleteOnDebugMode() throws Exception {

        properties.setProperty(SapsPropertiesConstants.SAPS_DEBUG_MODE, DEBUG_MODE_TRUE);
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        properties.setProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR,
            MOCK_NFS_DEBUG_TASKS_DIR_NAME);

        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FAILED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        String taskDirPath = String.format(NFS_STORAGE_TASK_DIR_PATTERN, MOCK_NFS_STORAGE_PATH,
            MOCK_NFS_DEBUG_TASKS_DIR_NAME, task.getTaskId());
        File taskDir = new File(taskDirPath);
        FileUtils.forceMkdir(taskDir);

        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        permanentStorage.delete(task.getTaskId(), task.isFailed());
        Assert.assertFalse(taskDir.exists());
    }

    @Test(expected = IOException.class)
    public void testDeleteNonExistentTaskDir() throws InvalidPropertyException, IOException {
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        String fakeTaskId = "fake";
        SapsImage task = new SapsImage(fakeTaskId, "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        permanentStorage.delete(task.getTaskId(), task.isFailed());
    }

    @Test
    public void testGenerateAccessLinksTaskDir() throws InvalidPropertyException, IOException, TaskNotFoundException {
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, MOCK_NFS_STORAGE_PATH);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);

        List<AccessLink> exceptedAccessLinksList = new LinkedList<>();
        exceptedAccessLinksList.add(new AccessLink(TestGenerateAccessLink.Inputdownloading.NAME, TestGenerateAccessLink.Inputdownloading.URL));
        exceptedAccessLinksList.add(new AccessLink(TestGenerateAccessLink.Preprocessing.NAME, TestGenerateAccessLink.Preprocessing.URL));
        exceptedAccessLinksList.add(new AccessLink(TestGenerateAccessLink.Processing.NAME, TestGenerateAccessLink.Processing.URL));

        Assert.assertEquals(exceptedAccessLinksList, permanentStorage.generateAccessLinks(MOCK_TASK_ID));
    }

    private boolean assertTaskDir(String taskDirName, String taskId) {
        File inputDir = new File(String.format(NFS_TASK_STAGE_DIR_PATTERN, MOCK_NFS_STORAGE_PATH, taskDirName, taskId, INPUTDOWNLOADING_DIR));
        File preprocessingDir = new File(String.format(NFS_TASK_STAGE_DIR_PATTERN, MOCK_NFS_STORAGE_PATH, taskDirName, taskId, PREPROCESSING_DIR));
        File processingDir = new File(String.format(NFS_TASK_STAGE_DIR_PATTERN, MOCK_NFS_STORAGE_PATH, taskDirName, taskId, PROCESSING_DIR));
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