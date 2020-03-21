package org.fogbowcloud.saps.engine.core.dispatcher.email.keystone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.util.Properties;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.junit.Ignore;
import org.junit.Test;

public class KeystoneV3IdentityPluginTest {

    private static final String CONFIG_FILE_PATH = "src/test/resources/dispatcher-test.conf";

    @Test
    @Ignore
    public void createToken() throws Exception {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(CONFIG_FILE_PATH);
        properties.load(input);

        String url = properties.getProperty(SapsPropertiesConstants.Openstack.IdentityService.API_URL);
        String projectId = properties.getProperty(SapsPropertiesConstants.Openstack.PROJECT_ID);
        String userId = properties.getProperty(SapsPropertiesConstants.Openstack.USER_ID);
        String password = properties.getProperty(SapsPropertiesConstants.Openstack.USER_PASSWORD);

        IdentityToken token = KeystoneV3IdentityRequestHelper.createIdentityToken(url, projectId, userId, password);
        assertToken(token);
    }

    private void assertToken(IdentityToken token) {
        assertNotNull(token.getAccessId());
        assertFalse(token.getAccessId().isEmpty());
    }
}