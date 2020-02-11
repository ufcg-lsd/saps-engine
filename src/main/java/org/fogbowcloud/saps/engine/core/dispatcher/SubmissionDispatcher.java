package org.fogbowcloud.saps.engine.core.dispatcher;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.utils.RegionUtil;
import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.catalog.JDBCCatalog;
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

	public SubmissionDispatcher(Properties properties) throws SQLException {
		this.catalog  = new JDBCCatalog(properties);
	}

	/**
	 * This function is responsible for passing on the information of a new SAPS
	 * user to the communication approach with the Catalog that he will try until he
	 * succeeds. The email (primary key of the SAPS user scheme), name and password
	 * are defined by the user in which the email and password will be used for
	 * authentication on the SAPS platform. There are three other pieces of
	 * information that are:<br>
	 * - notify: informs the user about their tasks by email.<br>
	 * - state: informs if the user is able to authenticate on the SAPS
	 * platform.<br>
	 * - administrative role: informs if the user is an administrator of the SAPS
	 * platform.<br>
	 * These three pieces of information are booleans and are controlled by an
	 * administrator. By default, state and administrative function are false.
	 * 
	 */
	public void addUserInCatalog(String email, String name, String password, boolean state, boolean notify,
			boolean adminRole) {
		CatalogUtils.addNewUser(catalog, email, name, password, state, notify, adminRole,
				"add new user [" + email + "]");
	}

	/**
	 * 
	 * This function calls a function that uses the approach of communicating with
	 * the Catalog trying to even try to retrieve the user's information based on
	 * the email passed by parameter (which is the primary key of the SAPS user
	 * scheme, that is, there are not two users with same email). The return of this
	 * function is an object that contains the information retrieved from the User.
	 */
	public SapsUser getUserInCatalog(String email) {
		return CatalogUtils.getUser(catalog, email, "get user [" + email + "] information");
	}

	/**
	 * This function sends information from a new SAPS task to the communication
	 * mechanism with the Catalog in order to insert it into the schema, causing the
	 * platform to receive a new workload. This information is:<br>
	 * 
	 * - TaskId: a unique identifier for the SAPS task created automatically using a
	 * UUID class (immutable universally unique identifier, represents a 128-bit
	 * value).<br>
	 *
	 * - Dataset: it is the type of data set of a certain satellite that this task
	 * belongs to, being an enum that is used by the steps of the task processing
	 * for the correct execution of the algorithms. Their values ​​can be:<br>
	 * 
	 * -- landsat_5: indicates that the task belongs to the LANDSAT 5 satellite
	 * dataset (https://www.usgs.gov/land-resources/nli/landsat/landsat-5).<br>
	 * 
	 * -- landsat_7: indicates that the task belongs to the LANDSAT 7 satellite
	 * dataset (https://www.usgs.gov/land-resources/nli/landsat/landsat-7).<br>
	 * 
	 * -- landsat_8: indicates that the task belongs to the LANDSAT 8 satellite data
	 * set (https://www.usgs.gov/land-resources/nli/landsat/landsat-8).<br>
	 * 
	 * - Region: is the location of the satellite data following the global notation
	 * system for Landsat data (WRS:
	 * https://landsat.gsfc.nasa.gov/the-worldwide-reference-system), following the
	 * PPPRRR form, where P is the path number (with 3 characters) and R is the row
	 * number (also with 3 characters).<br>
	 * 
	 * - date: is the date on which the satellite data was collected following the
	 * Gregorian calendar. Its value is a string in the format YYYY/MM/DD, where Y
	 * is the year with 4 characters, M is the month with 2 characters and D is the
	 * day with 2 characters.<br>
	 * 
	 * - priority: it is an integer in the range 0 to 31 that indicates how priority
	 * the task processing is.<br>
	 * 
	 * - userEmail: it is the email of the task owner (this information is obtained
	 * automatically by the authenticated user on the platform).<br>
	 * 
	 * - inputdownloadingPhaseTag: is the version of the algorithm that will be used
	 * in the task's inputdownloading step.<br>
	 * 
	 * - preprocessingPhaseTag: is the version of the algorithm that will be used in
	 * the task's preprocessing step.<br>
	 * 
	 * - processingPhaseTag: is the version of the algorithm that will be used in
	 * the task's processing step.<br>
	 * 
	 * - digestInputdownloading: is the immutable identifier (digest) of the Docker
	 * image of the version defined in the inputdownloading step
	 * (inputdownloadingPhaseTag).<br>
	 * 
	 * - digestPreprocessing: is the immutable identifier (digest) of the Docker
	 * image of the version defined in the preprocessing step
	 * (preprocessingPhaseTag).<br>
	 * 
	 * - digestProcessing: is the immutable identifier (digest) of the Docker image
	 * of the version defined in the processing step (processingPhaseTag).<br>
	 * 
	 * The return of this function is an object with the SAPS task information.<br>
	 * 
	 * Note 1: The digest is obtained automatically when the task is submitted to
	 * SAPS.<br>
	 * 
	 * Note 2: This information belongs to different classes of subjects on the SAP
	 * platform, we have information on:<br>
	 * 
	 * - satellite data: dataset, region and date.<br>
	 * 
	 * - SAPS schema: taskID, priority, userEmail.<br>
	 * 
	 * - versions of the processing step algorithms: inputdownloadingPhaseTag,
	 * preprocessingPhaseTag and processingPhaseTag.<br>
	 * 
	 * - Docker: digestInputdownloading, digestPreprocessing and
	 * digestProcessing.<br>
	 */
	private SapsImage addNewTaskInCatalog(String taskId, String dataset, String region, Date date, int priority,
			String userEmail, String inputdownloadingPhaseTag, String digestInputdownloading,
			String preprocessingPhaseTag, String digestPreprocessing, String processingPhaseTag,
			String digestProcessing) {
		return CatalogUtils.addNewTask(catalog, taskId, dataset, region, date, priority, userEmail,
				inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag, digestPreprocessing,
				processingPhaseTag, digestProcessing, "add new task [" + taskId + "]");
	}

	/**
	 * This function is responsible for creating a new timestamp of the task in the
	 * Catalog, and this communication action is carried out using the retry
	 * approach. Its general use is given right after a change in the status of a
	 * task (e.g. a task has changed state and you want to record that change and
	 * its timestamp for some analysis or processing log).<br>
	 * 
	 * 
	 * Note: The message parameter is a string that will be displayed in the SAPS
	 * log before attempting to communicate with the Catalog using the retry
	 * approach.
	 */
	private void addTimestampTaskInCatalog(SapsImage task, String message) {
		CatalogUtils.addTimestampTask(catalog, task, message);
	}

	/**
	 * This function is responsible for receiving information regarding a new
	 * processing submission made on the SAPS platform and it is desired to extract
	 * the N tasks (from this information) and insert them in the Catalog generating
	 * workload. Are they:<br>
	 * 
	 * - lowerLeftLatitude: is a geographic coordinate plus the lower left defined
	 * in the sphere which is the angle between the plane of the equator and the
	 * normal to the reference surface indicating the vertex of the polygon formed
	 * together with the information lowerLeftLongitude, upperRightLatitude and
	 * upperRightLongitude.<br>
	 * 
	 * - lowerLeftLongitude: is a geographic coordinate plus the lower left defined
	 * in the sphere measured in degrees, from 0 to 180 towards east or west, from
	 * the Greenwich Meridian indicating the vertex of the polygon formed together
	 * with the information lowerLeftLatitude, upperRightLatitude and
	 * upperRightLongitude.<br>
	 * 
	 * - upperRightLatitude: is a geographic coordinate plus the upper right defined
	 * in the sphere which is the angle between the plane of the equator and the
	 * normal to the reference surface indicating the vertex of the polygon formed
	 * together with the information lowerLeftLatitude, lowerLeftLongitude and
	 * upperRightLongitude.<br>
	 * 
	 * - upperRightLongitude: is a geographic coordinate plus the upper right
	 * defined in the sphere measured in degrees, from 0 to 180 towards east or
	 * west, from the Greenwich Meridian indicating the vertex of the polygon formed
	 * together with the information lowerLeftLatitude, lowerLeftLongitude and
	 * upperRightLatitude.<br>
	 * 
	 * - initDate: It is the starting date (according to the Gregorian calendar) of
	 * the interval in which the satellite data collection date must belong. If it
	 * belongs, a SAPS task will be created to process the satellite data.<br>
	 * 
	 * - endDate: It is the end date (according to the Gregorian calendar) of the
	 * interval in which the satellite data collection date must belong. If this
	 * belongs, a SAPS task will be created to process the satellite data.<br>
	 * 
	 * - inputdownloadingPhaseTag: is the version of the algorithm that will be used
	 * in the task's inputdownloading step.<br>
	 * 
	 * - preprocessingPhaseTag: is the version of the algorithm that will be used in
	 * the task's preprocessing step.<br>
	 * 
	 * - processingPhaseTag: is the version of the algorithm that will be used in
	 * the task's processing step.<br>
	 * 
	 * - priority: it is an integer in the range 0 to 31 that indicates how priority
	 * the task processing is.<br>
	 * 
	 * - userEmail: it is the email of the task owner (this information is obtained
	 * automatically by the authenticated user on the platform).<br>
	 * 
	 * Note: This function also seeks other information to form the SAPS task such
	 * as capturing the digests of the SAPS processing steps and extracting the
	 * satellite data dataset.
	 */
	public void addNewTasks(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude, Date initDate, Date endDate, String inputdownloadingPhaseTag,
			String preprocessingPhaseTag, String processingPhaseTag, int priority, String userEmail) {

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

					SapsImage task = addNewTaskInCatalog(taskId, dataset, region, cal.getTime(), priority, userEmail,
							inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag,
							digestPreprocessing, processingPhaseTag, digestProcessing);
					addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
				}
			}
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
	}

	/**
	 * 
	 * This function retrieves all information from all tasks registered in the
	 * Catalog using the communication mechanism using the retry approach.
	 */
	public List<SapsImage> getAllTasksInCatalog() {
		return CatalogUtils.getAllTasks(catalog, "get all tasks");
	}

	/**
	 * 
	 * This function retrieves the task information based on the past id registered
	 * in the Catalog using the communication mechanism that uses the retry
	 * approach.
	 */
	public SapsImage getTaskByIdInCatalog(String taskId) {
		return CatalogUtils.getTaskById(catalog, taskId, "gets task with id [" + taskId + "]");
	}

	/**
	 * This function retrieves a collection of tasks in a given state registered in
	 * the Catalog using the communication mechanism that uses the retry approach.
	 * 
	 * Note: The message parameter is a string that will be displayed in the SAPS
	 * log before attempting to communicate with the Catalog using the retry
	 * approach.
	 */
	public List<SapsImage> getTasksByStateInCatalog(ImageTaskState state) throws SQLException {
		return CatalogUtils.getTasks(catalog, state, CatalogConstants.UNLIMITED,
				"gets tasks with " + state.getValue() + " state");
	}

	/**
	 * This function seeks to retrieve data from successful tasks registered in the
	 * Catalog processed by SAPS by filtering for some information such as:
	 * 
	 * - lowerLeftLatitude: is a geographic coordinate plus the lower left defined
	 * in the sphere which is the angle between the plane of the equator and the
	 * normal to the reference surface indicating the vertex of the polygon formed
	 * together with the information lowerLeftLongitude, upperRightLatitude and
	 * upperRightLongitude.<br>
	 * 
	 * - lowerLeftLongitude: is a geographic coordinate plus the lower left defined
	 * in the sphere measured in degrees, from 0 to 180 towards east or west, from
	 * the Greenwich Meridian indicating the vertex of the polygon formed together
	 * with the information lowerLeftLatitude, upperRightLatitude and
	 * upperRightLongitude.<br>
	 * 
	 * - upperRightLatitude: is a geographic coordinate plus the upper right defined
	 * in the sphere which is the angle between the plane of the equator and the
	 * normal to the reference surface indicating the vertex of the polygon formed
	 * together with the information lowerLeftLatitude, lowerLeftLongitude and
	 * upperRightLongitude.<br>
	 *
	 * - upperRightLongitude: is a geographic coordinate plus the upper right
	 * defined in the sphere measured in degrees, from 0 to 180 towards east or
	 * west, from the Greenwich Meridian indicating the vertex of the polygon formed
	 * together with the information lowerLeftLatitude, lowerLeftLongitude and
	 * upperRightLatitude.<br>
	 * 
	 * - initDate: It is the start date (according to the Gregorian calendar) of the
	 * search interval to which the satellite data collection date for the
	 * successful task should belong.<br>
	 * 
	 * - endDate: It is the end date (according to the Gregorian calendar) of the
	 * search interval to which the satellite data collection date of the successful
	 * task must belong..<br>
	 * 
	 * - inputdownloadingPhaseTag: is the version of the algorithm that was used in
	 * the task's inputdownloading step, in which you want to recover.<br>
	 * 
	 * - preprocessingPhaseTag: is the version of the algorithm that was used in the
	 * task's preprocessing step, in which you want to recover.<br>
	 * 
	 * - processingPhaseTag: is the version of the algorithm that was used in the
	 * task's processing step, in which you want to recover.<br>
	 * 
	 * This function returns a collection of successful tasks that match past
	 * information.
	 */
	public List<SapsImage> searchProcessedTasksInCatalog(String lowerLeftLatitude, String lowerLeftLongitude,
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
=======
    private final Catalog catalog;

    private static final Logger LOGGER = Logger.getLogger(SubmissionDispatcher.class);

    public SubmissionDispatcher(Catalog catalog) {
        this.catalog = catalog;
    }

    public SubmissionDispatcher(Properties properties) throws SQLException {
        this.catalog = new JDBCCatalog(properties);
    }

    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * This function adds new user.
     *
     * @param userEmail  user email
     * @param userName   user name
     * @param userPass   user password
     * @param userState  user state
     * @param userNotify user notify
     * @param adminRole  administrator role
     * @param message    information message
     */
    private void addNewUserInCatalog(String userEmail, String userName, String userPass, boolean userState,
                                     boolean userNotify, boolean adminRole, String message) {
        CatalogUtils.addNewUser(catalog, userEmail, userName, userPass, userState, userNotify, adminRole,
                "add new user [" + userEmail + "]");
    }

    /**
     * This function adds new user in Catalog component.
     *
     * @param userEmail  user email
     * @param userName   user name
     * @param userPass   user password
     * @param userState  user state
     * @param userNotify user notify
     * @param adminRole  administrator role
     */
    public void addUser(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
                        boolean adminRole) {
        addNewUserInCatalog(userEmail, userName, userPass, userState, userNotify, adminRole,
                "add new user [" + userEmail + "]");
    }

    /**
     * This function gets user information.
     *
     * @param userEmail user email
     * @param message   information message
     */
    private SapsUser getUserInCatalog(String userEmail, String message) {
        return CatalogUtils.getUser(catalog, userEmail, message);
    }

    /**
     * This function gets user information in Catalog component.
     *
     * @param userEmail user email
     */
    public SapsUser getUser(String userEmail) {
        return getUserInCatalog(userEmail, "get user [" + userEmail + "] information");
    }

    /**
     * This function calling get_wrs (Python script) passing latitude and longitude
     * as paramater.
     *
     * @param latitude  latitude (point coordinate)
     * @param longitude longitude (point coordinate)
     * @return scene path/row from latitude and longitude coordinates
     * @throws Exception
     */
    private static String getRegionIds(String latitude, String longitude) throws Exception {

        LOGGER.debug("Calling get_wrs.py and passing (" + latitude + ", " + longitude + ") as parameter");
        Process builder = new ProcessBuilder("python", "./scripts/get_wrs.py", latitude, longitude).start();

        LOGGER.debug("Waiting for the process for execute command [" + builder.toString() + "] ...");
        builder.waitFor();
        LOGGER.debug("Process ended.");

        String result = null;

        if (builder.exitValue() != 0)
            throw new Exception("Process output exit code: " + builder.exitValue());

        BufferedReader reader = new BufferedReader(new InputStreamReader(builder.getInputStream()));
        StringBuilder builderS = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builderS.append(line);
            builderS.append(System.getProperty("line.separator"));
        }

        result = builderS.toString();

        LOGGER.debug("Process output (regions ID's): \n" + result);

        return result;
    }

    /**
     * This function returns regions set from area.
     *
     * @param lowerLeftLatitude   lower left latitude (coordinate)
     * @param lowerLeftLongitude  lower left longitude (coordinate)
     * @param upperRightLatitude  upper right latitude (coordinate)
     * @param upperRightLongitude upper right longitude (coordinate)
     * @return string set (regions set)
     */
    private Set<String> regionsFromArea(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
                                        String upperRightLongitude) {

        String regionLowerLeft, regionUpperRight;
        Set<String> regionsFound = new HashSet<String>();

        try {
            regionLowerLeft = getRegionIds(lowerLeftLatitude, lowerLeftLongitude).trim();
            regionUpperRight = getRegionIds(upperRightLatitude, upperRightLongitude).trim();

            int pathRegionLL = Integer.parseInt(regionLowerLeft.substring(0, 3));
            int rowRegionLL = Integer.parseInt(regionLowerLeft.substring(4, 6));

            int pathRegionUR = Integer.parseInt(regionUpperRight.substring(0, 3));
            int rowRegionUR = Integer.parseInt(regionUpperRight.substring(4, 6));

            LOGGER.info("pathRegionLL: " + pathRegionLL + "\n" + "rowRegionLL: " + rowRegionLL + "\n" + "pathRegionUR: "
                    + pathRegionUR + "\n" + "rowRegionUR: " + rowRegionUR + "\n");

            for (int i = pathRegionLL; i >= pathRegionUR; i--) {
                for (int j = rowRegionLL; j >= rowRegionUR; j--)
                    regionsFound.add(formatPathRow(i, j));
            }

            LOGGER.info("Regions found: " + regionsFound.toString());

        } catch (Exception e) {
            LOGGER.error("Error while calling the ConvertToWRS script", e);
        }

        return regionsFound;
    }

    /**
     * Get path and row and create format PPPRRR, where PPP is path of scene RRR is
     * row of scene
     */
    private String formatPathRow(int path, int row) {
        String pathAux = Integer.toString(path);
        String rowAux = Integer.toString(row);
        if (rowAux.length() == 1)
            rowAux = "00" + rowAux;
        else if (rowAux.length() == 2)
            rowAux = "0" + rowAux;

        return pathAux + rowAux;
    }

    /**
     * This function adds new task in Catalog.
     *
     * @param taskId                   task id
     * @param dataset                  task dataset
     * @param region                   task region
     * @param date                     task region
     * @param priority                 task priority
     * @param userEmail                user email that is creating task
     * @param inputdownloadingPhaseTag inputdownloading phase tag
     * @param preprocessingPhaseTag    preprocessing phase tag
     * @param processingPhaseTag       processing phase tag
     * @param digestProcessing
     * @param digestPreprocessing
     * @return new saps image
     */
    private SapsImage addNewTaskInCatalog(String taskId, String dataset, String region, Date date, int priority,
                                          String userEmail, String inputdownloadingPhaseTag, String digestInputdownloading,
                                          String preprocessingPhaseTag, String digestPreprocessing, String processingPhaseTag,
                                          String digestProcessing) {
        return CatalogUtils.addNewTask(catalog, taskId, dataset, region, date, priority, userEmail,
                inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag, digestPreprocessing,
                processingPhaseTag, digestProcessing, "add new task [" + taskId + "]");
    }

    /**
     * This function add new tuple in time stamp table and updates task time stamp.
     *
     * @param task    task to be update
     * @param message information message
     */
    private void addTimestampTaskInCatalog(SapsImage task, String message) {
        CatalogUtils.addTimestampTask(catalog, task, message);
    }

    /**
     * This function add new tasks in Catalog.
     *
     * @param lowerLeftLatitude        lower left latitude (coordinate)
     * @param lowerLeftLongitude       lower left longitude (coordinate)
     * @param upperRightLatitude       upper right latitude (coordinate)
     * @param upperRightLongitude      upper right longitude (coordinate)
     * @param initDate                 initial date
     * @param endDate                  end date
     * @param inputdownloadingPhaseTag inputdownloading phase tag
     * @param preprocessingPhaseTag    preprocessing phase tag
     * @param processingPhaseTag       processing phase tag
     * @param priority                 priority of new tasks
     * @param email                    user email
     */
    public void addNewTasks(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
                            String upperRightLongitude, Date initDate, Date endDate, String inputdownloadingPhaseTag,
                            String preprocessingPhaseTag, String processingPhaseTag, int priority, String email) {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(initDate);
        GregorianCalendar endCal = new GregorianCalendar();
        endCal.setTime(endDate);
        endCal.add(Calendar.DAY_OF_YEAR, 1);

        ExecutionScriptTag imageDockerInputdownloading = getExecutionScriptTag(ExecutionScriptTagUtil.INPUT_DOWNLOADER,
                inputdownloadingPhaseTag);
        ExecutionScriptTag imageDockerPreprocessing = getExecutionScriptTag(ExecutionScriptTagUtil.PRE_PROCESSING,
                preprocessingPhaseTag);
        ExecutionScriptTag imageDockerProcessing = getExecutionScriptTag(ExecutionScriptTagUtil.PROCESSING,
                processingPhaseTag);

        String digestInputdownloading = getDigest(imageDockerInputdownloading);
        String digestPreprocessing = getDigest(imageDockerPreprocessing);
        String digestProcessing = getDigest(imageDockerProcessing);

        Set<String> regions = regionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
                upperRightLongitude);

        while (cal.before(endCal)) {
            int startingYear = cal.get(Calendar.YEAR);
            List<String> datasets = DatasetUtil.getSatsInOperationByYear(startingYear);

            for (String dataset : datasets) {
                LOGGER.debug("Adding new tasks with dataset " + dataset);

                for (String region : regions) {
                    String taskId = UUID.randomUUID().toString();

                    SapsImage task = addNewTaskInCatalog(taskId, dataset, region, cal.getTime(), priority, email,
                            inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag,
                            digestPreprocessing, processingPhaseTag, digestProcessing);
                    addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    /**
     * This function gets script tag based in repository.
     *
     * @param repository task repository
     * @param tag        task tag
     * @return task information (tag, repository, type and name)
     */
    private ExecutionScriptTag getExecutionScriptTag(String repository, String tag) {
        try {
            return ExecutionScriptTagUtil.getExecutionScriptTag(tag, repository);
        } catch (SapsException e) {
            LOGGER.error("Error while trying get tag and repository Docker.", e);
            return null;
        }
    }

    /**
     * This function gets immutable identifier based in repository and tag
     *
     * @param imageDockerInfo image docker information
     * @return immutable identifier that match with repository and tag passed
     */
    private String getDigest(ExecutionScriptTag imageDockerInfo) {

        String dockerRepository = imageDockerInfo.getDockerRepository();
        String dockerTag = imageDockerInfo.getDockerTag();

        String result = null;

        try {
            Process builder = new ProcessBuilder("bash", "./scripts/get_digest.sh", dockerRepository, dockerTag)
                    .start();

            LOGGER.debug("Waiting for the process for execute command: " + builder.toString());
            builder.waitFor();

            if (builder.exitValue() != 0)
                throw new Exception("Process output exit code: " + builder.exitValue());

            BufferedReader reader = new BufferedReader(new InputStreamReader(builder.getInputStream()));

            result = reader.readLine();
        } catch (Exception e) {
            LOGGER.error("Error while trying get digest from Docker image [" + dockerRepository + "] with tag ["
                    + dockerTag + "].", e);
        }

        return result;
    }

    /**
     * This function get all tasks in Catalog.
     *
     * @return SAPS image list
     */
    private List<SapsImage> getAllTasksInCatalog() {
        return CatalogUtils.getAllTasks(catalog, "get all tasks");
    }

    /**
     * This function get all tasks.
     *
     * @return SAPS image list
     */
    public List<SapsImage> getAllTasks() {
        return getAllTasksInCatalog();
    }

    /**
     * This function gets task by id.
     *
     * @param taskId task id to be searched
     * @return SAPS image with id
     */
    private SapsImage getTaskByIdInCatalog(String taskId) {
        return CatalogUtils.getTaskById(catalog, taskId, "gets task with id [" + taskId + "]");
    }

    /**
     * This function gets task by id.
     *
     * @param taskId task id to be searched
     * @return SAPS image with id
     * @throws SQLException
     */
    public SapsImage getTaskById(String taskId) {
        return getTaskByIdInCatalog(taskId);
    }

    /**
     * This function gets tasks in specific state in Catalog.
     *
     * @param state   specific state for get tasks
     * @param message information message
     * @return tasks in specific state
     */
    private List<SapsImage> getTasksInCatalog(ImageTaskState state, String message) {
        return CatalogUtils.getTasks(catalog, state, message);
    }

    /**
     * This function gets tasks in specific state.
     *
     * @param state specific state for get tasks
     * @return tasks in specific state
     */
    public List<SapsImage> getTasksInState(ImageTaskState state) throws SQLException {
        return getTasksInCatalog(state, "gets tasks with " + state.getValue() + " state");
    }

    /**
     * This function gets all processed tasks in Catalog.
     *
     * @param region                   task region
     * @param initDate                 initial date
     * @param endDate                  end date
     * @param inputdownloadingPhaseTag inputdownloading phase tag
     * @param preprocessingPhaseTag    preprocessing phase tag
     * @param processingPhaseTag       processing phase tag
     * @return processed tasks list
     */
    private List<SapsImage> getProcessedTasksInCatalog(String region, Date initDate, Date endDate,
                                                       String inputdownloadingPhaseTag, String preprocessingPhaseTag, String processingPhaseTag) {
        return CatalogUtils.getProcessedTasks(catalog, region, initDate, endDate, inputdownloadingPhaseTag,
                preprocessingPhaseTag, processingPhaseTag,
                "gets all processed tasks with region [" + region + "], inputdownloading tag ["
                        + inputdownloadingPhaseTag + "], preprocessing tag [" + preprocessingPhaseTag
                        + "], processing tag [" + processingPhaseTag + "] beetwen " + initDate + " and " + endDate);
    }

    /**
     * This function search all processed tasks from area (between latitude and
     * longitude coordinates) between initial date and end date with
     * inputdownloading, preprocessing and processing tags.
     *
     * @param lowerLeftLatitude        lower left latitude (coordinate)
     * @param lowerLeftLongitude       lower left longitude (coordinate)
     * @param upperRightLatitude       upper right latitude (coordinate)
     * @param upperRightLongitude      upper right longitude (coordinate)
     * @param initDate                 initial date
     * @param endDate                  end date
     * @param inputdownloadingPhaseTag inputdownloading phase tag
     * @param preprocessingPhaseTag    preprocessing phase tag
     * @param processingPhaseTag       processing phase tag
     * @return processed tasks list following description
     */
    public List<SapsImage> searchProcessedTasks(String lowerLeftLatitude, String lowerLeftLongitude,
                                                String upperRightLatitude, String upperRightLongitude, Date initDate, Date endDate,
                                                String inputdownloadingPhaseTag, String preprocessingPhaseTag, String processingPhaseTag) {

        List<SapsImage> filteredTasks = new ArrayList<>();
        Set<String> regions = regionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
                upperRightLongitude);

        for (String region : regions) {
            List<SapsImage> tasksInCurrentRegion = getProcessedTasksInCatalog(region, initDate, endDate,
                    inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag);
            filteredTasks.addAll(tasksInCurrentRegion);
        }
        return filteredTasks;
    }
>>>>>>> 17fde90a51a179812a4c46c8786a6478a0c9ef17
}