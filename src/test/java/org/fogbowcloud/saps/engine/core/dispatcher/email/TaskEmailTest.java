package org.fogbowcloud.saps.engine.core.dispatcher.email;

import com.google.gson.Gson;
import org.fogbowcloud.saps.engine.core.archiver.storage.AccessLink;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TaskEmailTest {

    private Gson jsonUtil;
    private SapsImage task;
    private static final String MOCK_ID = "task-email-id";
    private static final String MOCK_IMAGE_REGION = "task-image-region";
    private static final String MOCK_IMAGE_COLLECTION_NAME = "image-collection-name";
    private static final Date MOCK_IMAGE_DATE = new Date();
    private static final List<AccessLink> MOCK_ACCESS_LINKS = new LinkedList<>();

    @Before
    public void setUp() {
        jsonUtil = new Gson();
        task = new SapsImage(MOCK_ID, "", MOCK_IMAGE_REGION, MOCK_IMAGE_DATE, ImageTaskState.ARCHIVED,
                SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, 0, "", "", "", "", "",
                "", "", new Timestamp(1), new Timestamp(1), "", "");
    }

    @Test
    //TODO upgrade this test
    public void testToJson() {
        TaskEmail taskEmail = new TaskEmail(task, MOCK_ACCESS_LINKS);

        String taskEmailInString = jsonUtil.toJson(taskEmail);
        Assert.assertNotNull(taskEmailInString);
        jsonUtil.fromJson(taskEmailInString, Object.class);
    }

}
