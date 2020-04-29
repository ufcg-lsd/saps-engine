package org.fogbowcloud.saps.engine.core.archiver.storage.swift;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SwiftAPIClientTest {

    private static final String CONFIG_FILE = "src/test/resources/config/archiver/normal-mode.conf";
    private static final String MOCK_FILE = "mockFile";
    private static final String MOCK_FILE_PATH = "src/test/resources/" + MOCK_FILE;
    private static final String MOCK_TASK_ID = "task-id";
    private Properties properties;
    private SwiftAPIClient client;

    private Properties loadConfigFile(String path) throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(path);
        properties.load(input);
        return properties;
    }

    @Before
    public void setUp() throws IOException, InvalidPropertyException {
        properties = loadConfigFile(CONFIG_FILE);
        client = new SwiftAPIClient(properties);
    }

    @Test
    @Ignore
    public void testCreateContainer() throws Exception {
        String containerName = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.CONTAINER_NAME);
        client.createContainer(containerName);
    }

    @Test
    @Ignore
    public void testUploadFile() throws Exception {
        File file = new File(MOCK_FILE_PATH);
        String containerName = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.CONTAINER_NAME);
        String dir = properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR) + File.separator + MOCK_TASK_ID;
        client.uploadFile(containerName, file, dir);
    }

    @Test
    @Ignore
    public void testListFiles() throws Exception {
        String containerName = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.CONTAINER_NAME);
        String path = properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);
        String filePath = path + File.separator + MOCK_TASK_ID + File.separator + MOCK_FILE;
        List<String> files = client.listFiles(containerName, path);
        Assert.assertEquals(1, files.size());
        Assert.assertTrue(files.contains(filePath));
    }

    @Test
    @Ignore
    public void testExistsTask() throws Exception {
        String containerName = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.CONTAINER_NAME);
        String path = properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);
        boolean exists = client.existsTask(containerName, path, MOCK_TASK_ID);
        Assert.assertTrue(exists);
    }

    @Test
    @Ignore
    public void testDeleteFile() throws Exception {
        String containerName = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.CONTAINER_NAME);
        String filePath = properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR) + File.separator + MOCK_TASK_ID + File.separator + MOCK_FILE;
        client.deleteFile(containerName, filePath);
    }
}