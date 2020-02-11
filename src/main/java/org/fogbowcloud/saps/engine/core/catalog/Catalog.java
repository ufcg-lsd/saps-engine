package org.fogbowcloud.saps.engine.core.catalog;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.fogbowcloud.saps.engine.core.catalog.exceptions.CatalogException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.UserNotFoundException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public interface Catalog {

    /**
     * This function sends information from a new SAPS task to the communication mechanism with the Catalog in order to
     * insert it into the schema, causing the  platform to receive a new workload.<br>
     * Note 1: The digest is obtained automatically when the task is submitted to SAPS.<br>
     * Note 2: This information belongs to different classes of subjects on the SAPS platform, we have information on:<br>
     * - satellite data: dataset, region and date.<br>
     * - SAPS schema: taskID, priority, userEmail.<br>
     * - versions of the processing step algorithms: inputdownloadingPhaseTag, preprocessingPhaseTag and processingPhaseTag.<br>
     * - Docker: digestInputdownloading, digestPreprocessing and digestProcessing.<br>
     *
     * @param taskId                   a unique identifier for the SAPS task created automatically using a UUID class (immutable
     *                                 universally unique identifier, represents a 128-bit value)
     * @param dataset                  it is the type of data set of a certain satellite that this task belongs to, being an enum that
     *                                 is used by the steps of the task processing for the correct execution of the algorithms.
     *                                 Their values ​​can be:<br>
     *                                 -- landsat_5: indicates that the task belongs to the LANDSAT 5 satellite dataset
     *                                 (https://www.usgs.gov/land-resources/nli/landsat/landsat-5)<br>
     *                                 -- landsat_7: indicates that the task belongs to the LANDSAT 7 satellite dataset
     *                                 (https://www.usgs.gov/land-resources/nli/landsat/landsat-7)<br>
     *                                 -- landsat_8: indicates that the task belongs to the LANDSAT 8 satellite data set
     *                                 (https://www.usgs.gov/land-resources/nli/landsat/landsat-8)<br>
     * @param region                   is the location of the satellite data following the global notation system for Landsat data (WRS:
     *                                 https://landsat.gsfc.nasa.gov/the-worldwide-reference-system), following the PPPRRR form, where P
     *                                 is the path number (with 3 characters) and R is the row number (also with 3 characters)
     * @param date                     is the date on which the satellite data was collected following the
     *                                 Gregorian calendar. Its value is a string in the format YYYY/MM/DD, where Y
     *                                 is the year with 4 characters, M is the month with 2 characters and D is the
     *                                 day with 2 characters
     * @param priority                 is an integer in the range 0 to 31 that indicates the priority of task processing
     * @param user                     it is the email of the task owner (this information is obtained automatically by
     *                                 the authenticated user on the platform)
     * @param inputdownloadingPhaseTag is the version of the algorithm that will be used in the task's inputdownloading step
     * @param digestInputdownloading   is the version of the algorithm that will be used in the task's preprocessing step
     * @param preprocessingPhaseTag    is the version of the algorithm that will be used in the task's processing step
     * @param digestPreprocessing      is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the inputdownloading step (inputdownloadingPhaseTag)
     * @param processingPhaseTag       is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the preprocessing step (preprocessingPhaseTag)
     * @param digestProcessing         is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the processing step (processingPhaseTag)
     * @return an object with the SAPS task information (cannot be null)
     * @throws CatalogException if any unexpected behavior occurs.
     */
    SapsImage addTask(String taskId, String dataset, String region, Date date, int priority, String user,
                      String inputdownloadingPhaseTag, String digestInputdownloading, String preprocessingPhaseTag,
                      String digestPreprocessing, String processingPhaseTag, String digestProcessing) throws CatalogException;

    void addStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException;

    /**
     * This function is responsible for passing on the information of a new SAPS user to the communication approach
     * with the Catalog that he will try until he succeeds. The email (primary key of the SAPS user scheme), name
     * and password are defined by the user in which the email and password will be used for authentication on the
     * SAPS platform.
     *
     * @param userEmail user email used for authentication on the SAPS platform
     * @param userName user name on the SAPS platform
     * @param userPass user password used for authentication on the SAPS platform
     * @param isEnable informs if the user is able to authenticate on the SAPS platform
     * @param userNotify informs the user about their tasks by email
     * @param adminRole informs if the user is an administrator of the SAPS platform
     * @throws CatalogException if any unexpected behavior occurs.
     */
    void addUser(String userEmail, String userName, String userPass, boolean isEnable, boolean userNotify,
                 boolean adminRole) throws CatalogException;

    void updateImageTask(SapsImage imageTask) throws CatalogException;

    List<SapsImage> getAllTasks() throws CatalogException;

    List<SapsImage> getTasksByState(ImageTaskState... tasksStates) throws CatalogException;

    SapsImage getTaskById(String taskId) throws CatalogException, TaskNotFoundException;

    SapsUser getUserByEmail(String userEmail) throws CatalogException, UserNotFoundException;

    void removeStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException;

    List<SapsImage> filterTasks(ImageTaskState state, String region, Date initDate, Date endDate, String inputdownloadingTag,
                                String preprocessingTag, String processingTag) throws CatalogException;
}
