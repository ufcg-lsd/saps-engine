package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.repository.USGSNasaRepository;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SubmissionDispatcherTest {

    private JDBCImageDataStore imageStore;
    private USGSNasaRepository repository;
    private SubmissionDispatcherImpl dispatcher;

    /**
     *
     * @param startingYear
     * @param startingMonth 0-based, i.e., use 0 for january
     * @param startingDay
     * @param endingYear
     * @param endingMonth   0-based, i.e., use 0 for january
     * @param endingDay
     * @return
     */
    private SampleInput getSampleInput(int startingYear, int startingMonth, int startingDay,
            int endingYear, int endingMonth, int endingDay) {
        GregorianCalendar starting = new GregorianCalendar();
        starting.set(startingYear, startingMonth, startingDay);
        GregorianCalendar ending = new GregorianCalendar();
        ending.set(endingYear, endingMonth, endingDay);

        return new SampleInput("-8.676947", "-37.095067",
                "-8.676947", "-37.095067", starting.getTime(), ending.getTime(),
                "gathering", "preprocessing", "algorithm");
    }

    @Before
    public void setUp() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("datastore_ip", "");
        properties.setProperty("datastore_port", "");
        properties.setProperty("datastore_url_prefix", "jdbc:h2:mem:testdb");
        properties.setProperty("datastore_username", "testuser");
        properties.setProperty("datastore_password", "testuser");
        properties.setProperty("datastore_driver", "org.h2.Driver");
        properties.setProperty("datastore_name", "testdb");

        // USGS Properties
        properties.put("saps_export_path", "/local/exports");
        properties.put("max_usgs_download_link_requests", "10");
        properties.put("max_simultaneous_download", "1");
        properties.put("usgs_login_url", "https://ers.cr.usgs.gov/login/");
        properties.put("usgs_json_url", "https://earthexplorer.usgs.gov/inventory/json");
        properties.put("usgs_username", "username");
        properties.put("usgs_password", "password");
        properties.put("usgs_api_key_period", "300000");

        imageStore = new JDBCImageDataStore(properties);
        repository = new USGSNasaRepository(properties);

        dispatcher = new SubmissionDispatcherImpl(imageStore, repository);
    }

    @After
    public void tearDown() {
        imageStore.dispose();
    }

    @Test
    public void testFillDb() {
        SampleInput sampleInput = getSampleInput(1994, 7, 16, 1994, 7, 16);
        List<Task> tasks = dispatcher.fillDB(sampleInput.lowerLeftLatitude,
                sampleInput.lowerLeftLongitude, sampleInput.upperRightLatitude,
                sampleInput.upperRightLongitude, sampleInput.initDate, sampleInput.endDate,
                sampleInput.inputGathering, sampleInput.inputPreprocessing, sampleInput.algorithmExecution);

        Assert.assertEquals(1, tasks.size());
        Assert.assertEquals(SapsPropertiesConstants.LANDSAT_5_DATASET, tasks.get(0).getImageTask().getDataset());
    }

    @Test
    public void testInsertTasksWithSubmission() throws SQLException, IOException {
        SampleInput sampleInput = getSampleInput(2017, 1, 1, 2017, 1, 1);

        Submission submissionOne = new Submission("sub-1");
        Submission submissionTwo = new Submission("sub-2");
        Task taskOne = new Task("task-1");
        Task taskTwo = new Task("task-2");
        Task taskThree = new Task("task-3");

        Date date = new Date();

        ImageTask imageTaskOne = new ImageTask(
                taskOne.getId(),
                "LANDSAT_TM_C1",
                "region-53",
                date,
                ImageTask.NON_EXISTENT,
                ImageTaskState.CREATED,
                ImageTask.NON_EXISTENT,
                0,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                new Timestamp(new java.util.Date().getTime()),
                new Timestamp(new java.util.Date().getTime()),
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT
        );
        ImageTask imageTaskTwo = new ImageTask(
                taskTwo.getId(),
                "LANDSAT_TM_C1",
                "region-53",
                date,
                ImageTask.NON_EXISTENT,
                ImageTaskState.CREATED,
                ImageTask.NON_EXISTENT,
                0,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                new Timestamp(new java.util.Date().getTime()),
                new Timestamp(new java.util.Date().getTime()),
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT
        );
        ImageTask imageTaskThree = new ImageTask(
                taskThree.getId(),
                "LANDSAT_TM_C1",
                "region-53",
                date,
                ImageTask.NON_EXISTENT,
                ImageTaskState.CREATED,
                ImageTask.NON_EXISTENT,
                0,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT,
                new Timestamp(new java.util.Date().getTime()),
                new Timestamp(new java.util.Date().getTime()),
                ImageTask.NON_EXISTENT,
                ImageTask.NON_EXISTENT
        );

        taskOne.setImageTask(imageTaskOne);
        taskTwo.setImageTask(imageTaskTwo);
        taskThree.setImageTask(imageTaskThree);

        submissionOne.addTask(taskOne);
        submissionOne.addTask(taskTwo);
        submissionTwo.addTask(taskThree);

        imageStore.addImageTask(imageTaskOne);
        imageStore.addImageTask(imageTaskTwo);
        imageStore.addImageTask(imageTaskThree);

        dispatcher.addTaskNotificationIntoDB(submissionOne.getId(), taskOne.getId(),
                "email-1");
        dispatcher.addTaskNotificationIntoDB(submissionOne.getId(), taskTwo.getId(),
                "email-1");
        dispatcher.addTaskNotificationIntoDB(submissionTwo.getId(), taskThree.getId(),
                "email-2");

        Submission actualSubmissionOne = imageStore.getSubmission(submissionOne.getId());
        Submission actualSubmissionTwo = imageStore.getSubmission(submissionTwo.getId());

        Assert.assertEquals(submissionOne, actualSubmissionOne);
        Assert.assertEquals(submissionTwo, actualSubmissionTwo);

        List<Task> tasks = dispatcher.fillDB(sampleInput.lowerLeftLatitude,
                sampleInput.lowerLeftLongitude, sampleInput.upperRightLatitude,
                sampleInput.upperRightLongitude, sampleInput.initDate, sampleInput.endDate,
                sampleInput.inputGathering, sampleInput.inputPreprocessing, sampleInput.algorithmExecution);

        String[] satsInPresentOperation = new String[] {
                SapsPropertiesConstants.LANDSAT_7_DATASET, SapsPropertiesConstants.LANDSAT_8_DATASET };

        Assert.assertEquals(satsInPresentOperation.length, tasks.size());
        String[] iTaskDatasets = new String[satsInPresentOperation.length];
        for (int i = 0; i < satsInPresentOperation.length; i++) {
            iTaskDatasets[i] = tasks.get(i).getImageTask().getDataset();
        }

        Arrays.sort(satsInPresentOperation);
        Arrays.sort(iTaskDatasets);
        Assert.assertEquals(Arrays.toString(satsInPresentOperation), Arrays.toString(iTaskDatasets));
    }

    class SampleInput {

        String lowerLeftLatitude;
        String lowerLeftLongitude;
        String upperRightLatitude;
        String upperRightLongitude;
        Date initDate;
        Date endDate;
        String inputGathering;
        String inputPreprocessing;
        String algorithmExecution;

        public SampleInput(String lowerLeftLatitude, String lowerLeftLongitude,
                           String upperRightLatitude, String upperRightLongitude,
                           Date initDate, Date endDate, String inputGathering,
                           String inputPreprocessing, String algorithmExecution) {
            this.lowerLeftLatitude = lowerLeftLatitude;
            this.lowerLeftLongitude = lowerLeftLongitude;
            this.upperRightLatitude = upperRightLatitude;
            this.upperRightLongitude = upperRightLongitude;
            this.initDate = initDate;
            this.endDate = endDate;
            this.inputGathering = inputGathering;
            this.inputPreprocessing = inputPreprocessing;
            this.algorithmExecution = algorithmExecution;
        }

    }

}
