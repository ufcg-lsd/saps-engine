package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.File;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Test;

public class NfsPermanentStorageTest {

    private static final String nfsStorageTestPath = "src/test/resources/nfs-storage-test";
    private static final String sapsExportTestPath = "src/test/resources/archiver-test";

    @Test
    public void testArchive() {
        Properties properties = new Properties();
        properties.setProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH, sapsExportTestPath);
        properties.setProperty(SapsPropertiesConstants.NFS_PERMANENT_STORAGE_PATH, nfsStorageTestPath);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        permanentStorage.archive(task);
        Assert.assertTrue(assertTaskDir(task.getTaskId()));

    }

    private boolean assertTaskDir(String taskId) {
        File inputDir = new File(String.format(NfsPermanentStorage.INPUTDOWNLOADING_DIR_PATTERN,
            NfsPermanentStorageTest.nfsStorageTestPath, taskId));
        File preprocessingDir = new File(String
            .format(NfsPermanentStorage.PREPROCESSING_DIR_PATTERN,
                NfsPermanentStorageTest.nfsStorageTestPath, taskId));
        File processingDir = new File(String.format(NfsPermanentStorage.PROCESSING_DIR_PATTERN,
            NfsPermanentStorageTest.nfsStorageTestPath, taskId));
        return inputDir.exists() && preprocessingDir.exists() && processingDir.exists();
    }

    @Test
    public void testDelete() throws Exception {
        SapsImage task = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED,
            SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
            "", "", new Timestamp(1), new Timestamp(1), "", "");
        File taskDir = new File(String
            .format(NfsPermanentStorage.TASK_DIR_PATTERN, nfsStorageTestPath, task.getTaskId()));
        FileUtils.forceMkdir(taskDir);

        Properties properties = new Properties();
        properties.setProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH, sapsExportTestPath);
        properties.setProperty("nfs_storage_path", nfsStorageTestPath);
        PermanentStorage permanentStorage = new NfsPermanentStorage(properties);
        permanentStorage.delete(task);
        Assert.assertFalse(taskDir.exists());
    }
}