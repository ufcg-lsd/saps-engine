package org.fogbowcloud.saps.engine.core.archiver.storage.swift;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Ignore;
import org.junit.Test;

public class SwiftAPIClientTest {

    private static final String CONFIG_FILE = "src/test/resources/config/archiver/normal-mode.conf";

    private Properties loadConfigFile(String path) throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(path);
        properties.load(input);
        return properties;
    }

    @Test
    @Ignore
    public void testListFiles() throws Exception {
        Properties properties = loadConfigFile(CONFIG_FILE);
        SwiftAPIClient client = new SwiftAPIClient(properties);
        String containerName = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.CONTAINER_NAME);
        String path = properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);
        client.listFiles(containerName, path);
    }
}