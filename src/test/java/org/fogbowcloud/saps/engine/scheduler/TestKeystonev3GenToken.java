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

    private boolean genericMethod(String imageId, String noReplayEmail, String noReplayPass) {
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
                SapsPropertiesConstants.NO_REPLY_EMAIL, noReplayEmail
        );
        properties.setProperty(
                SapsPropertiesConstants.NO_REPLY_PASS, noReplayPass
        );

        String[] images = {imageId};
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
                "sebal.no.reply@gmail.com",
                Arrays.asList(images)
        );
        emailBuilder.run();

        return emailBuilder.getFinishedWithoutFail();
    }

    @Test
    public void generateTestUrls() {
        Assert.assertTrue(
                genericMethod(
                        "139aca33-48f5-4bdf-9eba-dfbf64133d14",
                        "sebal.no.reply@gmail.com",
                        "noREsebal16"
                )
        );
    }

    @Test
    public void generateTestUrlsForNonExistentTask() {
        Assert.assertFalse(
                genericMethod(
                        "non_existent",
                        "sebal.no.reply@gmail.com",
                        "noREsebal16"
                )
        );
    }



    @Test
    public void generateTestUrlsWrongNoReplayEmail() {
        Assert.assertFalse(
                genericMethod(
                        "139aca33-48f5-4bdf-9eba-dfbf64133d14",
                        "non_existent@gmail.com",
                        ""
                )
        );
    }
}
