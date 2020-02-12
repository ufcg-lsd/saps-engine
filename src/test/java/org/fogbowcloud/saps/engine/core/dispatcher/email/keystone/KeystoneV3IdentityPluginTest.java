package org.fogbowcloud.saps.engine.core.dispatcher.email.keystone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Ignore;
import org.junit.Test;

public class KeystoneV3IdentityPluginTest {

    private static final String CONFIG_FILE_PATH = "src/test/resources/dispatcher-test.conf";

    @Test
    @Ignore
    public void createAccessId() throws IOException, KeystoneException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(CONFIG_FILE_PATH);
        properties.load(input);
        KeystoneV3IdentityPlugin keystone = new KeystoneV3IdentityPlugin();

        Map<String, String> credentials = new HashMap<>();
        credentials.put(KeystoneV3IdentityPlugin.AUTH_URL,
            properties.getProperty(SapsPropertiesConstants.SWIFT_AUTH_URL));
        credentials.put(KeystoneV3IdentityPlugin.PROJECT_ID,
            properties.getProperty(SapsPropertiesConstants.SWIFT_PROJECT_ID));
        credentials.put(KeystoneV3IdentityPlugin.USER_ID,
            properties.getProperty(SapsPropertiesConstants.SWIFT_USER_ID));
        credentials.put(KeystoneV3IdentityPlugin.PASSWORD,
            properties.getProperty(SapsPropertiesConstants.SWIFT_PASSWORD));

        String accessId = keystone.createAccessId(credentials);
        assertNotNull(accessId);
        assertFalse(accessId.isEmpty());
    }
}