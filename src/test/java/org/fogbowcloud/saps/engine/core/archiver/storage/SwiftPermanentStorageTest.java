package org.fogbowcloud.saps.engine.core.archiver.storage;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.archiver.storage.swift.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.swift.SwiftAPIClient;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.StartsWith;
import org.testng.Assert;

public class SwiftPermanentStorageTest {

    private SapsImage task01, task02;
    private List<String> filesTask01, filesTask02;

    private final class ArchiverConfigFilePath {
        private final class Fail {
            private static final String NORMAL_MODE = "src/test/resources/config/archiver/normal-mode.failconf";
            private static final String DEBUG_MODE = "src/test/resources/config/archiver/debug-mode.failconf";
        }

        private final class Success {
            private static final String NORMAL_MODE = "src/test/resources/config/archiver/normal-mode.conf";
            private static final String DEBUG_MODE = "src/test/resources/config/archiver/debug-mode.conf";
        }
    }

    private final String MOCK_SWIFT_FOLDER_PREFIX = "archiver";
    private final String MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS = "trash";
    private final String MOCK_CONTAINER_NAME = "saps-test";

    private final class Dirs {
        private static final String Task01 = "1";
        private static final String Task02 = "2";
    }

    private final class Files {
        private final class Task01 {
            private static final String INPUTDOWNLOADING_FILE = Dirs.Task01 + "/inputdownloading/file.ip";
            private static final String PREPROCESSING_FILE = "/preprocessing/file.pp";
            private static final String PROCESSING_FILE = "/processing/file.p";
        }
        private final class Task02 {
            private static final String INPUTDOWNLOADING_FILE = Dirs.Task02 + "inputdownloading/file.ip";
        }
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        task01 = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");
        task02 = new SapsImage("2", "", "", new Date(), ImageTaskState.FAILED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");

        filesTask01 = new ArrayList<String>();
        filesTask01.add(MOCK_SWIFT_FOLDER_PREFIX + "/" + Files.Task01.INPUTDOWNLOADING_FILE);
        filesTask01.add(MOCK_SWIFT_FOLDER_PREFIX + "/" + Files.Task01.PREPROCESSING_FILE);
        filesTask01.add(MOCK_SWIFT_FOLDER_PREFIX + "/" + Files.Task01.PROCESSING_FILE);

        filesTask02 = new ArrayList<String>();
        filesTask02.add(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS + "/" + Files.Task02.INPUTDOWNLOADING_FILE);
    }

    private Properties createDefaultPropertiesWithoutArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(ArchiverConfigFilePath.Success.NORMAL_MODE);
        properties.load(input);
        return properties;
    }

    private Properties createFailurePropertiesWithoutArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(ArchiverConfigFilePath.Fail.NORMAL_MODE);
        properties.load(input);
        return properties;
    }

    private Properties createDefaultPropertiesWithArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(ArchiverConfigFilePath.Success.DEBUG_MODE);
        properties.load(input);
        return properties;
    }

    private Properties createFailurePropertiesWithArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(ArchiverConfigFilePath.Fail.DEBUG_MODE);
        properties.load(input);
        return properties;
    }

    @Test(expected = SapsException.class)
    public void failureTestToBuildArchiveBecausePropertiesIsNull() throws SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        new SwiftPermanentStorage(null, swiftAPIClient);
    }

    @Test(expected = SapsException.class)
    public void failureTestToBuildArchiveWithoutDebugMode() throws SapsException, IOException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createFailurePropertiesWithoutArchiverInDebugMode();
        new SwiftPermanentStorage(properties, swiftAPIClient);
    }

    @Test(expected = SapsException.class)
    public void failureTestToBuildArchiveInDebugMode() throws SapsException, IOException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createFailurePropertiesWithArchiverInDebugMode();
        new SwiftPermanentStorage(properties, swiftAPIClient);
    }

    @Test
    public void testToArchiveSuccessfulTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(3)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask01, true);
    }

    @Test
    public void failureTestWhenTryingToArchiveSuccessfulTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask01, false);
    }

    @Test
    public void testToArchiveFailureTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(1)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void failureTestWhenTryingToArchiveFailureTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void testToArchiveSuccessfulTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(3)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask01, true);
    }

    @Test
    public void failureTestWhenTryingToArchiveSuccessfulTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask01, false);
    }

    @Test
    public void testToArchiveFailureTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(1)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void failureTestWhenTryingToArchiveFailureTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)));
        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX)));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void testToDeleteSuccessfulTaskWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq(MOCK_CONTAINER_NAME), Mockito.eq(MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task01))).thenReturn(filesTask01);
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.eq(MOCK_CONTAINER_NAME), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task01)));

        boolean deleteTask01 = permanentStorage.delete(task01);

        Assert.assertEquals(deleteTask01, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task01);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX + "/" + Files.Task01.INPUTDOWNLOADING_FILE);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX + "/" + Files.Task01.PREPROCESSING_FILE);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX + "/" + Files.Task01.PROCESSING_FILE);
    }

    @Test
    public void testToDeleteFailureTaskWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq(MOCK_CONTAINER_NAME), Mockito.eq(MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task02))).thenReturn(filesTask02);
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.eq(MOCK_CONTAINER_NAME), Mockito.argThat(new StartsWith(MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task02)));

        boolean deleteTask02 = permanentStorage.delete(task02);

        Assert.assertEquals(deleteTask02, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task02);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS + "/" + Files.Task02.INPUTDOWNLOADING_FILE);
    }

    @Test
    public void testToDeleteSuccessfulTaskButListFilesWithPrefixMethodReturnsEmptyListWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq(MOCK_CONTAINER_NAME), Mockito.eq(MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task01))).thenReturn(new ArrayList<String>());
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.anyString(), Mockito.anyString());

        boolean deleteTask01 = permanentStorage.delete(task01);

        Assert.assertEquals(deleteTask01, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task01);
        Mockito.verify(swiftAPIClient, Mockito.times(0)).deleteFile(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testToDeleteFailureTaskButListFilesWithPrefixMethodReturnsEmptyListWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq(MOCK_CONTAINER_NAME), Mockito.eq(MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task02))).thenReturn(new ArrayList<String>());
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.anyString(), Mockito.anyString());

        boolean deleteTask02 = permanentStorage.delete(task02);

        Assert.assertEquals(deleteTask02, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix(MOCK_CONTAINER_NAME, MOCK_SWIFT_FOLDER_PREFIX + "/" + Dirs.Task02);
        Mockito.verify(swiftAPIClient, Mockito.times(0)).deleteFile(Mockito.anyString(), Mockito.anyString());
    }
}
