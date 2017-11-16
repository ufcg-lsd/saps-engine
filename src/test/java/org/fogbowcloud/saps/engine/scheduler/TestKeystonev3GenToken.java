package org.fogbowcloud.saps.engine.scheduler;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.manager.core.plugins.identity.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.manager.core.util.HttpRequestUtil;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.scheduler.restlet.DatabaseApplication;
import org.fogbowcloud.saps.engine.scheduler.util.ProcessedImagesEmailBuilder;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static org.mockito.Mockito.doReturn;

public class TestKeystonev3GenToken {

    /*
        This tests are not unit tests, they were created as a shortcut to some
        methods for prototyping, but are not extensive enough for proper testing.
     */

    @Test
    public void testGenToken() {
        Properties properties = new Properties();

        KeystoneV3IdentityPlugin keystonePlugin = new KeystoneV3IdentityPlugin(properties);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("authUrl", "https://cloud.lsd.ufcg.edu.br:5000");
        credentials.put("projectId", "3324431f606d4a74a060cf78c16fcb21 ");
        credentials.put("userId", "3e57892203271c195f5d473fc84f484b8062103275ce6ad6e7bcd1baedf70d5c");
        credentials.put("password", "nc3SRPS2");

        Token token = keystonePlugin.createToken(credentials);

        try {
            URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("cloud.lsd.ufcg.edu.br:8080")
                    .setPath("/swift/v1/lsd_deploy")
                    .addParameter("path", "archiver/cd3f97b7-6151-47da-8893-d1fa63e14374/data/output/")
                    .build();
            HttpGet httpget = new HttpGet(uri);
            httpget.addHeader("X-Auth-Token", token.getAccessId());
            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(httpget);
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            System.out.println(
                    EntityUtils.toString(response.getEntity(), "utf-8")
            );
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void generateTestUrls() {
        Properties properties = new Properties();
        properties.setProperty("swift_auth_url", "https://cloud.lsd.ufcg.edu.br:5000");
        properties.setProperty("swift_project_id", "3324431f606d4a74a060cf78c16fcb21");
        properties.setProperty("swift_user_id", "3e57892203271c195f5d473fc84f484b8062103275ce6ad6e7bcd1baedf70d5c");
        properties.setProperty("swift_password", "nc3SRPS2");
        properties.setProperty("swift_object_store_host", "cloud.lsd.ufcg.edu.br:8080");
        properties.setProperty("swift_object_store_path", "/swift/v1");
        properties.setProperty("swift_object_store_container", "lsd_deploy");
        properties.setProperty("swift_object_store_key", "qnjcKtALnbqHkOM0f0J9I8TWzANCI7Qj8");
        properties.setProperty(
                SapsPropertiesConstants.NO_REPLY_EMAIL, "sebal.no.reply@gmail.com"
        );
        properties.setProperty(
                SapsPropertiesConstants.NO_REPLY_PASS, "noREsebal16"
        );

        String[] images = {"139aca33-48f5-4bdf-9eba-dfbf64133d14"};
        DatabaseApplication application = Mockito.mock(DatabaseApplication.class);

        for (String image: images) {
            ImageTask imageTask = new ImageTask(
                    image,
                    "NA",
                    "NA",
                    new Date(),
                    "NA",
                    ImageTaskState.CREATED,
                    "NA",
                    0,
                    "NA",
                    "Default",
                    "Default",
                    "Default",
                    "NA",
                    "NA",
                    new Timestamp(System.nanoTime()),
                    new Timestamp(System.nanoTime()),
                    "NA",
                    "NA"
            );

            try {
                doReturn(imageTask).when(application).getTask(image);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        ProcessedImagesEmailBuilder emailBuilder = new ProcessedImagesEmailBuilder(
                application,
                properties,
                "sebal.no.replay@gmail.com",
                Arrays.asList(images)
        );
        emailBuilder.run();
    }
}
