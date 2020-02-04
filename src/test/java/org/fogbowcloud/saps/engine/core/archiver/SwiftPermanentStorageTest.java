package org.fogbowcloud.saps.engine.core.archiver;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.StartsWith;
import org.testng.Assert;

public class SwiftPermanentStorageTest {

    private SapsImage task01, task02;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        task01 = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");
        task02 = new SapsImage("2", "", "", new Date(), ImageTaskState.FAILED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");
    }

    private Properties createDefaultPropertiesWithoutArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("src/test/resources/archiver-test.conf");
        properties.load(input);
        return properties;
    }

    private Properties createDefaultPropertiesWithArchiverInDebugMode() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("src/test/resources/archiver-test-debug-mode.conf");
        properties.load(input);
        return properties;
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

        Assert.assertEquals(archiveTask01, false);
    }

    @Test
    public void testToArchiveFailureTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.argThat(new StartsWith("archiver")));

        boolean archiveTask02 = permanentStorage.archive(task02);

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void failureTestWhenTryingToArchiveFailureTaskWithoutArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.anyString());

        boolean archiveTask02 = permanentStorage.archive(task02);

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void testToArchiveSuccessfulTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.anyString());

        boolean archiveTask01 = permanentStorage.archive(task01);

        Assert.assertEquals(archiveTask01, true);
    }

    @Test
    public void failureTestWhenTryingToArchiveSuccessfulTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.anyString());

        boolean archiveTask01 = permanentStorage.archive(task01);

        Assert.assertEquals(archiveTask01, false);
    }

    @Test
    public void testToArchiveFailureTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.anyString());

        boolean archiveTask02 = permanentStorage.archive(task02);

        Assert.assertEquals(archiveTask02, false);
    }

    @Test
    public void failureTestWhenTryingToArchiveFailureTaskWithArchiverInDebugMode() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        Mockito.doAnswer((i) -> {
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.anyString());

        boolean archiveTask02 = permanentStorage.archive(task02);

        Assert.assertEquals(archiveTask02, false);
    }

    /*@Test
    public void testFailsTheFirstTimeWhenTryingToUploadTheSuccessfulTask() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultPropertiesWithoutArchiverInDebugMode();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        SapsImage task01 = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");

        Mockito.doAnswer((i) -> {
            System.out.println(i);
            throw new Exception();
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.anyString());

        boolean archiveTask01 = permanentStorage.archive(task01);

        Assert.assertEquals(archiveTask01, true);
    }*/
}
