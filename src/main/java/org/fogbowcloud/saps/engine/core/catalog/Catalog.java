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

    //FIXME: maybe, refactor related information into a separated data structured, e.g.:
    // satellite data: dataset, region and date.
    // - SAPS schema: taskID, priority, userEmail.
    // - versions of the processing step algorithms: inputdownloadingPhaseTag, preprocessingPhaseTag and processingPhaseTag.<br>
    // - Docker: digestInputdownloading, digestPreprocessing and digestProcessing.
    //FIXME: by manel. I think we can use an UUID instead of the taskId string
    //FIXME: by manel. I think we can use an ENUM instead of the dataset string

    //FIXME: verify and doc the high priority (0 or 31?)

    //FIXME: by manel. do we really need this *phaseTag parameters?

    /**
     * It adds a new Task into this {@code Catalog}.<br>
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
     * @param user                     it is the email of the user that has submitted the task
     * @param inputdownloadingPhaseTag is the version of the algorithm that will be used in the task's inputdownloading step
     * @param digestInputdownloading   is the version of the algorithm that will be used in the task's preprocessing step
     * @param preprocessingPhaseTag    is the version of the algorithm that will be used in the task's processing step
     * @param digestPreprocessing      is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the inputdownloading step (inputdownloadingPhaseTag)
     * @param processingPhaseTag       is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the preprocessing step (preprocessingPhaseTag)
     * @param digestProcessing         is the immutable identifier (digest) of the Docker image of the version defined
     *                                 in the processing step (processingPhaseTag)
     *
     * @return the new {@code SapsImage} created and added to this {@code Catalog}.
     *
     * @throws CatalogException if any unexpected behavior occurs.
     */
    SapsImage addTask(String taskId, String dataset, String region, Date date, int priority, String user,
                      String inputdownloadingPhaseTag, String digestInputdownloading, String preprocessingPhaseTag,
                      String digestPreprocessing, String processingPhaseTag, String digestProcessing) throws CatalogException;

    void addStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException;

    //FIXME: by manel. we want to know the possible values of the parameters. which ones are mandatory? can we use
    // null or an empty strings?

    //FIXME: what if the user is added twice?

    /**
     * It adds a new user to this {@code Catalog}.
     *
     * @param userEmail user email used for authentication on the SAPS platform
     * @param userName user name on the SAPS platform
     * @param userPass user password used for authentication on the SAPS platform
     * @param isEnable informs if the user is able to authenticate on the SAPS platform
     * @param userNotify informs the user about their tasks by email
     * @param adminRole informs if the user is an administrator of the SAPS platform
     *
     * @throws CatalogException if any unexpected behavior occurs.
     *
     */
    void addUser(String userEmail, String userName, String userPass, boolean isEnable, boolean userNotify,
                 boolean adminRole) throws CatalogException;

    //FIXME: i think we should throw an exception when there is no imageTask in the Catalog
    void updateImageTask(SapsImage imageTask) throws CatalogException;

    //FIXME: we should explain we return an empty list in some cases
    /**
     * @return
     * @throws CatalogException
     */
    List<SapsImage> getAllTasks() throws CatalogException;

    List<SapsImage> getTasksByState(ImageTaskState... tasksStates) throws CatalogException;

    SapsImage getTaskById(String taskId) throws CatalogException, TaskNotFoundException;

    //FIXME: what if the userEmail is null? empty string? (doc it)
    SapsUser getUserByEmail(String userEmail) throws CatalogException, UserNotFoundException;

    //FIXME: what if the taskId does not exist in the catalog
    void removeStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException;

    //FIXME: it may return an empty list, right? doc-it
    List<SapsImage> filterTasks(ImageTaskState state, String region, Date initDate, Date endDate, String inputdownloadingTag,
                                String preprocessingTag, String processingTag) throws CatalogException;
}
