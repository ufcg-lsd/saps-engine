package org.fogbowcloud.saps.engine.core.dispatcher;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.utils.RegionUtil;
import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.catalog.jdbc.JDBCCatalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.dispatcher.utils.DatasetUtil;
import org.fogbowcloud.saps.engine.core.dispatcher.utils.DigestUtil;
import org.fogbowcloud.saps.engine.utils.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.utils.ExecutionScriptTagUtil;
import org.fogbowcloud.saps.engine.utils.retry.CatalogUtils;

public class SubmissionDispatcher {

    private final Catalog catalog;

    private static final Logger LOGGER = Logger.getLogger(SubmissionDispatcher.class);

    public SubmissionDispatcher(Catalog catalog) {
        this.catalog = catalog;
    }

    public SubmissionDispatcher(Properties properties) throws SQLException {
        this.catalog = new JDBCCatalog(properties);
    }

    /**
     * It adds new User in {@code Catalog}.
     *
     * @param email     user email used for authentication on the SAPS platform
     * @param name      user name on the SAPS platform
     * @param password  user password used for authentication on the SAPS platform
     * @param notify    informs the user about their tasks by email.<br>
     * @param state     informs if the user is able to authenticate on the SAPS platform (it for default is false)
     * @param adminRole administrative role: informs if the user is an administrator of the SAPS platform (it for default is false)
     */
    public void addUser(String email, String name, String password, boolean state, boolean notify,
                        boolean adminRole) {
        CatalogUtils.addNewUser(catalog, email, name, password, state, notify, adminRole,
                "add new user [" + email + "]");
    }

    /**
     * It gets {@code SapsUser} in {@code Catalog}.
     *
     * @return an {@code SapsUser} with equal email
     */
    public SapsUser getUser(String email) {
        return CatalogUtils.getUser(catalog, email, "get user [" + email + "] information");
    }

    /**
     * It adds a new Task in {@code Catalog}.<br>
     *
     * @param taskId                   an unique identifier for the SAPS task.
     * @param dataset                  it is the type of data set associated with the new task to be created. Their values ​​can be:<br>
     *                                 -- landsat_5: indicates that the task is related to the LANDSAT 5 satellite
     *                                 (https://www.usgs.gov/land-resources/nli/landsat/landsat-5)<br>
     *                                 -- landsat_7: indicates that the task is related to the LANDSAT 7 satellite
     *                                 (https://www.usgs.gov/land-resources/nli/landsat/landsat-7)<br>
     *                                 -- landsat_8: indicates that the task is related with the LANDSAT 8 satellite data set
     *                                 (https://www.usgs.gov/land-resources/nli/landsat/landsat-8)<br>
     * @param region                   is the location of the satellite data following the global notation system for Landsat data (WRS:
     *                                 https://landsat.gsfc.nasa.gov/the-worldwide-reference-system), following the PPPRRR form, where PPP
     *                                 is a length-3 to the path number and RRR is a length-3 to the row number.
     * @param date                     is the date on which the satellite data was collected following the YYYY/MM/DD format.
     * @param priority                 is an integer in the [0, 31] range that indicates the priority of task processing.
     * @param userEmail                it is the email of the user that has submitted the task
     * @param inputdownloadingPhaseTag is the version of the algorithm that will be used in the task's inputdownloading step
     * @param digestInputdownloading   is the version of the algorithm that will be used in the task's preprocessing step
     * @param preprocessingPhaseTag    is the version of the algorithm that will be used in the task's processing step
     * @param digestPreprocessing      is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the inputdownloading step (inputdownloadingPhaseTag)
     * @param processingPhaseTag       is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the preprocessing step (preprocessingPhaseTag)
     * @param digestProcessing         is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the processing step (processingPhaseTag)
     * @return the new {@code SapsImage} created and added to this {@code Catalog}.
     */
    private SapsImage addTask(String taskId, String dataset, String region, Date date, int priority,
                              String userEmail, String inputdownloadingPhaseTag, String digestInputdownloading,
                              String preprocessingPhaseTag, String digestPreprocessing, String processingPhaseTag,
                              String digestProcessing) {
        return CatalogUtils.addNewTask(catalog, taskId, dataset, region, date, priority, userEmail,
                inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag, digestPreprocessing,
                processingPhaseTag, digestProcessing, "add new task [" + taskId + "]");
    }

    private void addTimestampTaskInCatalog(SapsImage task, String message) {
        CatalogUtils.addTimestampTask(catalog, task);
    }

    /**
     * It gets information about a new processing submission and extract N {@code SapsImage} for adds them in {@code Catalog}.
     *
     * @param lowerLeftLatitude        is a geographic coordinate plus the lower left defined
     *                                 in the sphere which is the angle between the plane of the equator and the
     *                                 normal to the reference surface indicating the vertex of the polygon formed
     *                                 together with the information lowerLeftLongitude, upperRightLatitude and
     *                                 upperRightLongitude.
     * @param lowerLeftLongitude       is a geographic coordinate plus the lower left defined
     *                                 in the sphere measured in degrees, from 0 to 180 towards east or west, from
     *                                 the Greenwich Meridian indicating the vertex of the polygon formed together
     *                                 with the information lowerLeftLatitude, upperRightLatitude and
     *                                 upperRightLongitude.
     * @param upperRightLatitude       is a geographic coordinate plus the upper right defined
     *                                 in the sphere which is the angle between the plane of the equator and the
     *                                 normal to the reference surface indicating the vertex of the polygon formed
     *                                 together with the information lowerLeftLatitude, lowerLeftLongitude and
     *                                 upperRightLongitude.
     * @param upperRightLongitude      is a geographic coordinate plus the upper right
     *                                 defined in the sphere measured in degrees, from 0 to 180 towards east or
     *                                 west, from the Greenwich Meridian indicating the vertex of the polygon formed
     *                                 together with the information lowerLeftLatitude, lowerLeftLongitude and
     *                                 upperRightLatitude.
     * @param initDate                 it is the starting date (according to the Gregorian calendar) of
     *                                 the interval in which the satellite data collection date must belong. If it
     *                                 belongs, a SAPS task will be created to process the satellite data.
     * @param endDate                  It is the end date (according to the Gregorian calendar) of the
     *                                 interval in which the satellite data collection date must belong. If this
     *                                 belongs, a SAPS task will be created to process the satellite data.
     * @param inputdownloadingPhaseTag is the version of the algorithm that will be used
     *                                 in the task's inputdownloading step.
     * @param preprocessingPhaseTag    is the version of the algorithm that will be used in
     *                                 the task's preprocessing step.
     * @param processingPhaseTag       is the version of the algorithm that will be used in
     *                                 the task's processing step.
     * @param priority                 it is an integer in the range 0 to 31 that indicates how priority
     *                                 the task processing is.
     * @param userEmail                it is the email of the task owner (this information is obtained
     *                                 automatically by the authenticated user on the platform).
     */
    public void addTasks(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
                         String upperRightLongitude, Date initDate, Date endDate, String inputdownloadingPhaseTag,
                         String preprocessingPhaseTag, String processingPhaseTag, int priority, String userEmail) throws Exception {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(initDate);
        GregorianCalendar endCal = new GregorianCalendar();
        endCal.setTime(endDate);
        endCal.add(Calendar.DAY_OF_YEAR, 1);

        ExecutionScriptTag imageDockerInputdownloading = ExecutionScriptTagUtil
                .getExecutionScriptTag(inputdownloadingPhaseTag, ExecutionScriptTagUtil.INPUT_DOWNLOADER);
        ExecutionScriptTag imageDockerPreprocessing = ExecutionScriptTagUtil
                .getExecutionScriptTag(preprocessingPhaseTag, ExecutionScriptTagUtil.PRE_PROCESSING);
        ExecutionScriptTag imageDockerProcessing = ExecutionScriptTagUtil.getExecutionScriptTag(processingPhaseTag,
                ExecutionScriptTagUtil.PROCESSING);

        String digestInputdownloading = DigestUtil.getDigest(imageDockerInputdownloading);
        String digestPreprocessing = DigestUtil.getDigest(imageDockerPreprocessing);
        String digestProcessing = DigestUtil.getDigest(imageDockerProcessing);

        Set<String> regions = RegionUtil.regionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
                upperRightLongitude);

        while (cal.before(endCal)) {
            int startingYear = cal.get(Calendar.YEAR);
            List<String> datasets = DatasetUtil.getSatsInOperationByYear(startingYear);

            for (String dataset : datasets) {
                LOGGER.debug("Adding new tasks with dataset " + dataset);

                for (String region : regions) {
                    String taskId = UUID.randomUUID().toString();

                    SapsImage task = addTask(taskId, dataset, region, cal.getTime(), priority, userEmail,
                            inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag,
                            digestPreprocessing, processingPhaseTag, digestProcessing);
                    addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    public List<SapsImage> getAllTasks() {
        return CatalogUtils.getAllTasks(catalog, "get all tasks");
    }

    public SapsImage getTaskById(String taskId) {
        return CatalogUtils.getTaskById(catalog, taskId, "gets task with id [" + taskId + "]");
    }

    public List<SapsImage> getTasksByState(ImageTaskState state) throws SQLException {
        return CatalogUtils.getTasks(catalog, state
        );
    }

    /**
     * It gets processed {@code SapsImage} in {@code Catalog} by filtering for parameters.
     *
     * @param lowerLeftLatitude        is a geographic coordinate plus the lower left defined
     *                                 in the sphere which is the angle between the plane of the equator and the
     *                                 normal to the reference surface indicating the vertex of the polygon formed
     *                                 together with the information lowerLeftLongitude, upperRightLatitude and
     *                                 upperRightLongitude.
     * @param lowerLeftLongitude       is a geographic coordinate plus the lower left defined
     *                                 in the sphere measured in degrees, from 0 to 180 towards east or west, from
     *                                 the Greenwich Meridian indicating the vertex of the polygon formed together
     *                                 with the information lowerLeftLatitude, upperRightLatitude and
     *                                 upperRightLongitude.
     * @param upperRightLatitude       is a geographic coordinate plus the upper right defined
     *                                 in the sphere which is the angle between the plane of the equator and the
     *                                 normal to the reference surface indicating the vertex of the polygon formed
     *                                 together with the information lowerLeftLatitude, lowerLeftLongitude and
     *                                 upperRightLongitude.
     * @param upperRightLongitude      is a geographic coordinate plus the upper right
     *                                 defined in the sphere measured in degrees, from 0 to 180 towards east or
     *                                 west, from the Greenwich Meridian indicating the vertex of the polygon formed
     *                                 together with the information lowerLeftLatitude, lowerLeftLongitude and
     *                                 upperRightLatitude.
     * @param initDate                 it is the starting date (according to the Gregorian calendar) of
     *                                 the interval in which the satellite data collection date must belong. If it
     *                                 belongs, a SAPS task will be created to process the satellite data.
     * @param endDate                  It is the end date (according to the Gregorian calendar) of the
     *                                 interval in which the satellite data collection date must belong. If this
     *                                 belongs, a SAPS task will be created to process the satellite data.
     * @param inputdownloadingPhaseTag is the version of the algorithm that will be used
     *                                 in the task's inputdownloading step.
     * @param preprocessingPhaseTag    is the version of the algorithm that will be used in
     *                                 the task's preprocessing step.
     * @param processingPhaseTag       is the version of the algorithm that will be used in
     *                                 the task's processing step.
     */
    public List<SapsImage> getProcessedTasks(String lowerLeftLatitude, String lowerLeftLongitude,
                                             String upperRightLatitude, String upperRightLongitude, Date initDate, Date endDate,
                                             String inputdownloadingPhaseTag, String preprocessingPhaseTag, String processingPhaseTag) {

        List<SapsImage> filteredTasks = new ArrayList<>();
        Set<String> regions = RegionUtil.regionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
                upperRightLongitude);

        for (String region : regions) {
            List<SapsImage> tasksInCurrentRegion = CatalogUtils.getProcessedTasks(catalog, region, initDate, endDate,
                    inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag,
                    "gets all processed tasks with region [" + region + "], inputdownloading tag ["
                            + inputdownloadingPhaseTag + "], preprocessing tag [" + preprocessingPhaseTag
                            + "], processing tag [" + processingPhaseTag + "] beetwen " + initDate + " and " + endDate);
            filteredTasks.addAll(tasksInCurrentRegion);
        }
        return filteredTasks;
    }
}