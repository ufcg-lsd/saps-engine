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

import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
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

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        task01 = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");
        task02 = new SapsImage("2", "", "", new Date(), ImageTaskState.FAILED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");

        filesTask01 = new ArrayList<String>();
        filesTask01.add("archiver/1/inputdownloading/file.ip");
        filesTask01.add("archiver/1/preprocessing/file.pp");
        filesTask01.add("archiver/1/processing/file.p");

        filesTask02 = new ArrayList<String>();
        filesTask02.add("trash/2/inputdownloading/file.ip");
    }

    private Properties createDefaultPropertiesWithoutArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("src/test/resources/archiver-test.conf");
        properties.load(input);
        return properties;
    }

    private Properties createFailurePropertiesWithoutArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("src/test/resources/archiver-test.failconf");
        properties.load(input);
        return properties;
    }

    private Properties createDefaultPropertiesWithArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("src/test/resources/archiver-test-debug-mode.conf");
        properties.load(input);
        return properties;
    }

    private Properties createFailurePropertiesWithArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("src/test/resources/archiver-test-debug-mode.failconf");
        properties.load(input);
        return properties;
    }

    @Test (expected = SapsException.class)
    public void failureTestToBuildArchiveBecausePropertiesIsNull() throws SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        new SwiftPermanentStorage(null, swiftAPIClient);
    }

    @Test (expected = SapsException.class)
    public void failureTestToBuildArchiveWithoutDebugMode() throws SapsException, IOException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createFailurePropertiesWithoutArchiverInDebugMode();
        new SwiftPermanentStorage(properties, swiftAPIClient);
    }

    @Test (expected = SapsException.class)
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
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(3)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask01, true);
    }

    @Test
    public void failureTestWhenTryingToArchiveSuccessfulTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask01, false);
    }

    @Test
    public void testToArchiveFailureTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(1)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void failureTestWhenTryingToArchiveFailureTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void testToArchiveSuccessfulTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(3)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask01, true);
    }

    @Test
    public void failureTestWhenTryingToArchiveSuccessfulTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        boolean archiveTask01 = permanentStorage.archive(task01);

        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask01, false);
    }

    @Test
    public void testToArchiveFailureTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(1)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void failureTestWhenTryingToArchiveFailureTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Mockito.verify(swiftAPIClient, Mockito.times(2)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("trash")));
        Mockito.verify(swiftAPIClient, Mockito.times(0)).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void testToDeleteSuccessfulTaskWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq("saps-test"), Mockito.eq("archiver/1"))).thenReturn(filesTask01);
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.eq("saps-test"), Mockito.argThat(new StartsWith("archiver/1")));

        boolean deleteTask01 = permanentStorage.delete(task01);

        Assert.assertEquals(deleteTask01, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix("saps-test", "archiver/1");
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile("saps-test", "archiver/1/inputdownloading/file.ip");
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile("saps-test", "archiver/1/preprocessing/file.pp");
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile("saps-test", "archiver/1/processing/file.p");
    }

    @Test
    public void testToDeleteFailureTaskWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq("saps-test"), Mockito.eq("trash/2"))).thenReturn(filesTask02);
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.eq("saps-test"), Mockito.argThat(new StartsWith("trash/2")));

        boolean deleteTask02 = permanentStorage.delete(task02);

        Assert.assertEquals(deleteTask02, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix("saps-test", "trash/2");
        Mockito.verify(swiftAPIClient, Mockito.times(1)).deleteFile("saps-test", "trash/2/inputdownloading/file.ip");
    }

    @Test
    public void testToDeleteSuccessfulTaskButListFilesWithPrefixMethodReturnsEmptyListWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq("saps-test"), Mockito.eq("archiver/1"))).thenReturn(new ArrayList<String>());
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.anyString(), Mockito.anyString());

        boolean deleteTask01 = permanentStorage.delete(task01);

        Assert.assertEquals(deleteTask01, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix("saps-test", "archiver/1");
        Mockito.verify(swiftAPIClient, Mockito.times(0)).deleteFile(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testToDeleteFailureTaskButListFilesWithPrefixMethodReturnsEmptyListWithoutArchiverInDebugMode() throws IOException, SapsException {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.when(swiftAPIClient.listFilesWithPrefix(Mockito.eq("saps-test"), Mockito.eq("trash/2"))).thenReturn(new ArrayList<String>());
        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).deleteFile(Mockito.anyString(), Mockito.anyString());

        boolean deleteTask02 = permanentStorage.delete(task02);

        Assert.assertEquals(deleteTask02, true);
        Mockito.verify(swiftAPIClient, Mockito.times(1)).listFilesWithPrefix("saps-test", "trash/2");
        Mockito.verify(swiftAPIClient, Mockito.times(0)).deleteFile(Mockito.anyString(), Mockito.anyString());
    }
}