package org.fogbowcloud.saps.engine.core.dispatcher.email.keystone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
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
    public void createAccessId() throws Exception {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(CONFIG_FILE_PATH);
        properties.load(input);

        Map<String, String> credentials = new HashMap<>();
        credentials.put(KeystoneV3IdentityRequestHelper.AUTH_URL,
            properties.getProperty(SapsPropertiesConstants.SWIFT_AUTH_URL));
        credentials.put(KeystoneV3IdentityRequestHelper.PROJECT_ID,
            properties.getProperty(SapsPropertiesConstants.SWIFT_PROJECT_ID));
        credentials.put(KeystoneV3IdentityRequestHelper.USER_ID,
            properties.getProperty(SapsPropertiesConstants.SWIFT_USER_ID));
        credentials.put(KeystoneV3IdentityRequestHelper.PASSWORD,
            properties.getProperty(SapsPropertiesConstants.SWIFT_PASSWORD));

        String accessId = KeystoneV3IdentityRequestHelper.createAccessId(credentials);
        assertNotNull(accessId);
        assertFalse(accessId.isEmpty());
    }
}