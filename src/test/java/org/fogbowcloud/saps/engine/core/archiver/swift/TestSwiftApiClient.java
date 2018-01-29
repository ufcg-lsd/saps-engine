package org.fogbowcloud.saps.engine.core.archiver.swift;

import com.amazonaws.util.StringInputStream;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

public class TestSwiftApiClient {
    private static final String TEST_CONTAINER = "test_container";

    private SwiftAPIClient client;
    private Process process;

    @Before
    public void setUp() {
        Properties properties = new Properties();
        properties.setProperty(SapsPropertiesConstants.SWIFT_AUTH_URL, "swift_url");
        properties.setProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PROJECT_ID,"project_id");
        properties.setProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_USER_ID, "user_id");
        properties.setProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PASSWORD, "password");
        properties.setProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_AUTH_URL, "auth_url");
        properties.setProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL, "swift_url");
        properties.setProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_UPDATE_PERIOD, "60000");

        client = Mockito.spy(new SwiftAPIClient(properties));
        process = Mockito.mock(Process.class);
    }


    @Test(timeout = 10000)
    public void testStart() {
        // The method start call wait
        // This test checks if notify is called after a token is set
        Mockito.doReturn("token").when(client).generateToken();
        client.start();
    }

    @Test
    public void testCreateContainer() throws IOException, InterruptedException {
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doReturn(process).when(client).buildCreateContainerProcess(TEST_CONTAINER);
        client.createContainer(TEST_CONTAINER);
    }

    @Test
    public void testCreateContainerIOException() throws IOException, InterruptedException {
        // TODO should something happen when exception is thrown?
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doThrow(IOException.class).when(client).buildCreateContainerProcess(TEST_CONTAINER);
        client.createContainer(TEST_CONTAINER);
    }

    @Test
    public void testCreateContainerInterruptedException() throws IOException, InterruptedException {
        // TODO should something happen when exception is thrown?
        Mockito.doThrow(InterruptedException.class).when(process).waitFor();
        Mockito.doReturn(process).when(client).buildCreateContainerProcess(TEST_CONTAINER);
        client.createContainer(TEST_CONTAINER);
    }

    @Test
    public void testDeleteContainer() throws IOException, InterruptedException {
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doReturn(process).when(client).buildDeleteContainerProcess(TEST_CONTAINER);
        client.deleteContainer(TEST_CONTAINER);
    }

    @Test
    public void testDeleteContainerIOException() throws IOException, InterruptedException {
        // TODO should something happen when exception is thrown?
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doThrow(IOException.class).when(client).buildDeleteContainerProcess(TEST_CONTAINER);
        client.deleteContainer(TEST_CONTAINER);
    }

    @Test
    public void testDeleteContainerInterruptedException() throws IOException, InterruptedException {
        // TODO should something happen when exception is thrown?
        Mockito.doThrow(InterruptedException.class).when(process).waitFor();
        Mockito.doReturn(process).when(client).buildDeleteContainerProcess(TEST_CONTAINER);
        client.deleteContainer(TEST_CONTAINER);
    }

    @Test
    public void testIsContainerEmpty() {
        Mockito.doReturn(0).when(client).numberOfFilesInContainer(TEST_CONTAINER);
        Assert.assertTrue(client.isContainerEmpty(TEST_CONTAINER));
    }

    @Test
    public void testIsContainerNotEmpty() {
        Mockito.doReturn(1).when(client).numberOfFilesInContainer(TEST_CONTAINER);
        Assert.assertFalse(client.isContainerEmpty(TEST_CONTAINER));
    }

    @Test
    public void testUploadFiles() throws Exception {
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doReturn(0).when(process).exitValue();
        Mockito.doReturn(process).when(client).buildUploadFileProcess(
                Mockito.anyString(),
                Mockito.any(File.class),
                Mockito.anyString()
        );
        client.uploadFile(TEST_CONTAINER, new File("/tmp/testfile.txt"), "archiver/");
    }

    @Test
    public void testUploadFilesContainerAlreadyExists() throws Exception {
        StringInputStream s = new StringInputStream("409 Conflict");
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doReturn(1).when(process).exitValue();
        Mockito.doReturn(s).when(process).getErrorStream();
        Mockito.doReturn(process).when(client).buildUploadFileProcess(
                Mockito.anyString(),
                Mockito.any(File.class),
                Mockito.anyString()
        );
        client.uploadFile(TEST_CONTAINER, new File("/tmp/testfile.txt"), "archiver/");
    }

    @Test(expected = Exception.class)
    public void testUploadFilesContainerDoesNotExists() throws Exception {
        StringInputStream s = new StringInputStream("404 NotFound");
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doReturn(1).when(process).exitValue();
        Mockito.doReturn(s).when(process).getErrorStream();
        Mockito.doReturn(process).when(client).buildUploadFileProcess(
                Mockito.anyString(),
                Mockito.any(File.class),
                Mockito.anyString()
        );
        client.uploadFile(TEST_CONTAINER, new File("/tmp/testfile.txt"), "archiver/");
    }

    @Test(expected = IOException.class)
    public void testUploadFilesIOException() throws Exception {
        Mockito.doThrow(IOException.class).when(process).waitFor();
        Mockito.doReturn(1).when(process).exitValue();
        Mockito.doReturn(process).when(client).buildUploadFileProcess(
                Mockito.anyString(),
                Mockito.any(File.class),
                Mockito.anyString()
        );
        client.uploadFile(TEST_CONTAINER, new File("/tmp/testfile.txt"), "archiver/");
    }

    @Test(expected = InterruptedException.class)
    public void testUploadFilesInterruptedException() throws Exception {
        Mockito.doReturn(0).when(process).waitFor();
        Mockito.doReturn(1).when(process).exitValue();
        Mockito.doThrow(InterruptedException.class).when(client).buildUploadFileProcess(
                Mockito.anyString(),
                Mockito.any(File.class),
                Mockito.anyString()
        );
        client.uploadFile(TEST_CONTAINER, new File("/tmp/testfile.txt"), "archiver/");
    }
}
