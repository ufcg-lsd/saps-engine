package org.fogbowcloud.saps.engine.scheduler.util;

import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.scheduler.restlet.DatabaseApplication;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class TestProcessedImagesEmailBuilder {

    private static final String IMAGE_TASK_ID = "task_id_101010101010";
    private static final String UNAVAILABLE = "UNAVAILABLE";

    @Test
    public void testGenerateTaskEmailJson() throws SQLException, IOException, URISyntaxException, JSONException {
        Properties properties = getProperties();
        ImageTask imageTask = getImageTask();
        List<String> imageIds = getImageIdList();
        DatabaseApplication application = getDatabaseApplication(imageTask);
        ProcessedImagesEmailBuilder emailBuilder = getProcessedImagesEmailBuilder(properties, imageIds, application);

        List<String> imageFiles = getImageFileList();
        doReturn(imageFiles).when(emailBuilder).getTaskFilesFromObjectStore(
                any(Properties.class), anyString(), anyString(), anyString(), any(ImageTask.class)
        );

        JSONObject jsonReturn;
        jsonReturn = emailBuilder.generateTaskEmailJson(properties, IMAGE_TASK_ID);

        Assert.assertEquals(imageTask.getTaskId(), jsonReturn.getString("taskId"));
        Assert.assertEquals(imageTask.getRegion(), jsonReturn.getString("region"));
        Assert.assertEquals(imageTask.getImageDate().toString(), jsonReturn.getString("imageDate"));
        Assert.assertEquals(imageTask.getCollectionTierName(), jsonReturn.getString("collectionTierName"));

        JSONArray files = jsonReturn.getJSONArray("files");
        Assert.assertEquals(imageFiles.size(), files.length());
        for (int i = 0; i < files.length(); i++) {
            boolean missing = true;
            for (String imageFile : imageFiles) {
                JSONObject f = files.getJSONObject(i);
                String name = f.getString("name");
                if (name.equals(imageFile)) {
                    String url = f.getString("url");
                    System.out.println(name + ": " + url);
                    checkUrlAvailable(properties, name, url);
                    missing = false;
                    break;
                }
            }
            if (missing) {
                Assert.fail("One of the files is missing");
            }
        }
    }

    @Test
    public void testGenerateTaskEmailJsonErrorGeneratingURL() throws SQLException, IOException, URISyntaxException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, JSONException {
        Properties properties = getProperties();
        ImageTask imageTask = getImageTask();
        List<String> imageIds = getImageIdList();
        DatabaseApplication application = getDatabaseApplication(imageTask);
        ProcessedImagesEmailBuilder emailBuilder = getProcessedImagesEmailBuilder(properties, imageIds, application);

        List<String> imageFiles = getImageFileList();
        String failingFile = imageFiles.get(3);

        doReturn(imageFiles).when(emailBuilder).getTaskFilesFromObjectStore(
                any(Properties.class), anyString(), anyString(), anyString(), any(ImageTask.class)
        );
        doThrow(SignatureException.class).when(emailBuilder).generateTempURL(
                anyString(),
                anyString(),
                Matchers.eq(failingFile),
                anyString()
        );

        JSONObject jsonReturn;
        jsonReturn = emailBuilder.generateTaskEmailJson(properties, IMAGE_TASK_ID);

        Assert.assertEquals(imageTask.getTaskId(), jsonReturn.getString("taskId"));
        Assert.assertEquals(imageTask.getRegion(), jsonReturn.getString("region"));
        Assert.assertEquals(imageTask.getImageDate().toString(), jsonReturn.getString("imageDate"));
        Assert.assertEquals(imageTask.getCollectionTierName(), jsonReturn.getString("collectionTierName"));

        JSONArray files = jsonReturn.getJSONArray("files");
        Assert.assertEquals(imageFiles.size(), files.length());
        for (int i = 0; i < files.length(); i++) {
            boolean missing = true;
            for (String imageFile : imageFiles) {
                JSONObject f = files.getJSONObject(i);
                String name = f.getString("name");
                if (name.equals(imageFile)) {
                    String url = f.getString("url");
                    System.out.println(name + ": " + url);
                    if (name.equals(failingFile)) {
                        Assert.assertEquals(UNAVAILABLE, url);
                    } else {
                        checkUrlAvailable(properties, name, url);
                    }
                    missing = false;
                    break;
                }
            }
            if (missing) {
                Assert.fail("One of the files is missing");
            }
        }
    }

    @Test
    public void testGenerateTaskEmailJsonErrorOnDatabase() throws SQLException, IOException, URISyntaxException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, JSONException {
        Properties properties = getProperties();
        ImageTask imageTask = getImageTask();
        List<String> imageIds = getImageIdList();
        DatabaseApplication application = getDatabaseApplication(imageTask);
        ProcessedImagesEmailBuilder emailBuilder = getProcessedImagesEmailBuilder(properties, imageIds, application);

        try {
            emailBuilder.generateTaskEmailJson(properties, "FAIL_TASK");
            Assert.fail("Should throw exception");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGenerateAllTasksJsons() throws SQLException, JSONException, IOException, URISyntaxException {
        Properties properties = getProperties();
        ImageTask imageTask = getImageTask();
        List<String> imageIds = getImageIdList();
        imageIds.add("FAIL_TASK");
        DatabaseApplication application = getDatabaseApplication(imageTask);
        ProcessedImagesEmailBuilder emailBuilder = getProcessedImagesEmailBuilder(properties, imageIds, application);

        List<String> imageFiles = getImageFileList();
        doReturn(imageFiles).when(emailBuilder).getTaskFilesFromObjectStore(
                any(Properties.class), anyString(), anyString(), anyString(), any(ImageTask.class)
        );

        StringBuilder errorBuilder = new StringBuilder();
        JSONArray tasks = emailBuilder.generateAllTasksJsons(errorBuilder);
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject obj = tasks.getJSONObject(i);
            boolean missing = true;
            for (String tid: imageIds) {
                String name = obj.getString("taskId");
                if (name.equals(tid)) {
                    System.out.println(obj.toString(2));
                    String status = obj.optString("status");
                    if (name.equals("FAIL_TASK")) {
                        Assert.assertEquals(UNAVAILABLE, status);
                    } else {
                        Assert.assertNotEquals(UNAVAILABLE, status);
                    }
                    missing = false;
                    break;
                }
            }
            if (missing) {
                Assert.fail("One of the tasks is missing");
            }
        }
    }

    private DatabaseApplication getDatabaseApplication(final ImageTask imageTask) throws SQLException {
        DatabaseApplication application = Mockito.mock(DatabaseApplication.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                String imageId = (String)args[0];
                if (imageId.equals(imageTask.getTaskId())) return imageTask;
                else throw new SQLException();
            }
        }).when(application).getTask(anyString());
        return application;
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty(SapsPropertiesConstants.SWIFT_AUTH_URL, "https://cloud.lsd.ufcg.edu.br:5000");
        properties.setProperty(SapsPropertiesConstants.SWIFT_PROJECT_ID, "3324431f606d4a74a060cf78c16fcb21");
        properties.setProperty(SapsPropertiesConstants.SWIFT_USER_ID, "3e57892203271c195f5d473fc84f484b8062103275ce6ad6e7bcd1baedf70d5c");
        properties.setProperty(SapsPropertiesConstants.SWIFT_PASSWORD, "123456");
        properties.setProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_HOST, "cloud.lsd.ufcg.edu.br:8080");
        properties.setProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH, "/swift/v1");
        properties.setProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_CONTAINER, "lsd_deploy");
        properties.setProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY, "qnjcKtALnbqHkOM0f0J9I8TWzANCI7Qj8");
        properties.setProperty(
                SapsPropertiesConstants.NO_REPLY_EMAIL, "sebal.no.reply@gmail.com"
        );
        properties.setProperty(
                SapsPropertiesConstants.NO_REPLY_PASS, "noREsebal16"
        );
        return properties;
    }

    private List<String> getImageIdList() {
        List<String> imageIds = new ArrayList<>();
        imageIds.add(IMAGE_TASK_ID);
        return imageIds;
    }

    private ImageTask getImageTask() {
        return new ImageTask(
                IMAGE_TASK_ID,
                "landsat-7",
                "263_065",
                new Date(),
                "NA",
                ImageTaskState.ARCHIVED,
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
    }

    private ProcessedImagesEmailBuilder getProcessedImagesEmailBuilder(Properties properties, List<String> imageIds, DatabaseApplication application) {
        return Mockito.spy(
                new ProcessedImagesEmailBuilder(
                        application, properties, "sebal.no.replay@gmail.com", imageIds
                )
        );
    }

    private void checkUrlAvailable(Properties properties, String name, String url) {
        Assert.assertNotEquals(UNAVAILABLE, url);
        Assert.assertTrue(url.contains(properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_HOST)));
        Assert.assertTrue(url.contains(properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH)));
        Assert.assertTrue(url.contains(properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_CONTAINER)));
        Assert.assertTrue(url.contains(name));
        Assert.assertTrue(url.contains("?temp_url_sig"));
        Assert.assertTrue(url.contains("&temp_url_expires"));
        Assert.assertTrue(url.contains("&filename="+name));
    }

    private List<String> getImageFileList() {
        List<String> imageFiles = new ArrayList<>();
        imageFiles.add("file1");
        imageFiles.add("file2");
        imageFiles.add("file3");
        imageFiles.add("file4");
        return imageFiles;
    }

}
