package org.fogbowcloud.saps.engine.core.archiver;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
import org.fogbowcloud.saps.engine.core.database.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;

public class ArchiverTest {

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    private Properties createDefaultProperties() throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("src/test/resources/archiver-test.conf");
        properties.load(input);
        return properties;
    }

    @Test
    //TODO change name this test method
    public void test01() throws Exception {
        SwiftAPIClient swiftAPIClient = mock(SwiftAPIClient.class);
        Properties properties = createDefaultProperties();
        PermanentStorage permanentStorage = new SwiftPermanentStorage(properties, swiftAPIClient);

        SapsImage task01 = new SapsImage("1", "", "", new Date(), ImageTaskState.FINISHED, SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "", "", "", new Timestamp(1), new Timestamp(1), "", "");

        Mockito.doAnswer((i) -> {
            return null;
        }).when(swiftAPIClient).uploadFile(Mockito.anyString(), Mockito.any(File.class), Mockito.anyString());

        boolean archiveTask01 = permanentStorage.archive(task01);

        Assert.assertEquals(archiveTask01, true);
    }
}
