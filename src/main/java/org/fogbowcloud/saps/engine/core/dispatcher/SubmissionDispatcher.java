package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.dispatcher.notifier.Ward;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.DatasetUtil;
import org.fogbowcloud.saps.engine.utils.retry.CatalogUtils;

public class SubmissionDispatcher {
	public static final int DEFAULT_PRIORITY = 0;
	public static final String DEFAULT_USER = "admin";

	private ImageDataStore imageStore;

	private static final Logger LOGGER = Logger.getLogger(SubmissionDispatcher.class);

	public SubmissionDispatcher(ImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	public SubmissionDispatcher(Properties properties) throws SQLException {
		this.imageStore = new JDBCImageDataStore(properties);
	}

	/**
	 * This function gets image store
	 * 
	 * @return image store
	 */
	public ImageDataStore getImageStore() {
		return imageStore;
	}

	/**
	 * This function sets image store
	 * 
	 * @param imageStore new image store
	 */
	public void setImageStore(ImageDataStore imageStore) {
		this.imageStore = imageStore;
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
		CatalogUtils.addNewUser(imageStore, userEmail, userName, userPass, userState, userNotify, adminRole,
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
		return CatalogUtils.getUser(imageStore, userEmail, message);
	}

	/**
	 * This function gets user information in Catalog component.
	 * 
	 * @param userEmail user email
	 */
	public SapsUser getUser(String userEmail) {
		return getUserInCatalog(userEmail, "get user [" + userEmail + "] information");
	}

	public void addTaskNotificationIntoDB(String submissionId, String taskId, String userEmail) throws SQLException {
		try {
			imageStore.addUserNotification(submissionId, taskId, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while adding task " + taskId + " notification for user " + userEmail + " in Catalogue",
					e);
		}
	}

	public void removeUserNotification(String submissionId, String taskId, String userEmail) throws SQLException {
		try {
			imageStore.removeNotification(submissionId, taskId, userEmail);
		} catch (SQLException e) {
			LOGGER.error(
					"Error while removing task " + taskId + " notification for user " + userEmail + " from Catalogue",
					e);
		}
	}

	public boolean isUserNotifiable(String userEmail) throws SQLException {
		try {
			return imageStore.isUserNotifiable(userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while verifying user notify", e);
		}

		return false;
	}

	private static String getRegionIds(String latitude, String longitude) throws IOException, InterruptedException {

		LOGGER.debug("Calling get_wrs.py and passing (" + latitude + ", " + longitude + ") as parameter");
		Process processBuildScript = new ProcessBuilder("python", "./scripts/get_wrs.py", latitude, longitude).start();

		LOGGER.debug("Waiting for the process...");
		processBuildScript.waitFor();
		LOGGER.debug("Process ended.");

		BufferedReader reader = new BufferedReader(new InputStreamReader(processBuildScript.getInputStream()));
		StringBuilder builder = new StringBuilder();

		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}

		String result = builder.toString();

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

		LOGGER.debug("Getting landsat ID of: \n" + "lower left latitude: " + lowerLeftLatitude + "\n"
				+ "lower left longitude: " + lowerLeftLongitude + "\n" + "upper right latitude: " + upperRightLatitude
				+ "\n" + "upper right longitude: " + upperRightLongitude);

		String[] middlePoint = getMiddlePoint(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude).split(" ");

		LOGGER.debug("Setting middle point(reference point to the LandSat region): + \n" + "latitude: " + middlePoint[0]
				+ "\n" + "longitude: " + middlePoint[1]);

		String regionIds = "";
		try {
			// TODO add check in get_wrs script
			regionIds = getRegionIds(middlePoint[0], middlePoint[1]).trim();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while calling the ConvertToWRS script");
			e.printStackTrace();
		}
		Set<String> regionsFound = new HashSet<>(Arrays.asList(regionIds.split(" ")));

		LOGGER.debug("Returned regions as set: ");
		int regionsCount = 1;
		for (String s : regionsFound) {
			LOGGER.debug(regionsCount + "# " + s);
			regionsCount++;
		}

		return regionsFound;
	}

	/**
	 * This function returns middle point based in latitude and longitude
	 * coordinates.
	 * 
	 * @param lowerLeftLatitude   lower left latitude (coordinate)
	 * @param lowerLeftLongitude  lower left longitude (coordinate)
	 * @param upperRightLatitude  upper right latitude (coordinate)
	 * @param upperRightLongitude upper right longitude (coordinate)
	 * @return middle point
	 */
	private String getMiddlePoint(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude) {
		double lat1 = Double.parseDouble(lowerLeftLatitude);
		double lon1 = Double.parseDouble(lowerLeftLongitude);
		double lat2 = Double.parseDouble(upperRightLatitude);
		double lon2 = Double.parseDouble(upperRightLongitude);

		double dLon = Math.toRadians(lon2 - lon1);

		// convert to radians
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);
		lon1 = Math.toRadians(lon1);

		double Bx = Math.cos(lat2) * Math.cos(dLon);
		double By = Math.cos(lat2) * Math.sin(dLon);
		double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2),
				Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
		double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

		String result = Math.toDegrees(lat3) + " " + Math.toDegrees(lon3);
		return result;
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
	 * @return new saps image
	 */
	private SapsImage addNewTaskInCatalog(String taskId, String dataset, String region, Date date, int priority,
			String userEmail, String inputdownloadingPhaseTag, String preprocessingPhaseTag,
			String processingPhaseTag) {
		return CatalogUtils.addNewTask(imageStore, taskId, dataset, region, date, priority, userEmail,
				inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag, "add new task [" + taskId + "]");
	}

	/**
	 * This function add new tuple in time stamp table and updates task time stamp.
	 * 
	 * @param task    task to be update
	 * @param message information message
	 */
	private void addTimestampTaskInCatalog(SapsImage task, String message) {
		CatalogUtils.addTimestampTask(imageStore, task, message);
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
							inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag);
					addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
				}
			}
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
	}

	/**
	 * This function get all tasks in Catalog.
	 * 
	 * @param imageStore catalog component
	 * @return SAPS image list
	 */
	private List<SapsImage> getAllTasksInCatalog() {
		return CatalogUtils.getAllTasks(imageStore, "get all tasks");
	}

	/**
	 * This function get all tasks.
	 * 
	 * @return SAPS image list
	 */
	public List<SapsImage> getAllTasks() {
		return getAllTasksInCatalog();
	}

	// TODO
	public List<Ward> getUsersToNotify() throws SQLException {
		List<Ward> wards = imageStore.getUsersToNotify();
		return wards;
	}

	/**
	 * This function gets task by id.
	 * 
	 * @param taskId task id to be searched
	 * @return SAPS image with id
	 */
	private SapsImage getTaskByIdInCatalog(String taskId) {
		return CatalogUtils.getTaskById(imageStore, taskId, "gets task with id [" + taskId + "]");
	}

	/**
	 * This function gets task by id.
	 * 
	 * @param taskId task id to be searched
	 * @return SAPS image with id
	 * @throws SQLException
	 */
	public SapsImage getTaskById(String taskId) throws SQLException {
		return getTaskByIdInCatalog(taskId);
	}

	/**
	 * This function gets tasks in specific state in Catalog.
	 * 
	 * @param state   specific state for get tasks
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	private List<SapsImage> getTasksInCatalog(ImageTaskState state, int limit, String message) {
		return CatalogUtils.getTasks(imageStore, state, limit, message);
	}

	/**
	 * This function gets tasks in specific state.
	 * 
	 * @param state   specific state for get tasks
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	public List<SapsImage> getTasksInState(ImageTaskState state) throws SQLException {
		return getTasksInCatalog(state, ImageDataStore.UNLIMITED, "gets tasks with " + state.getValue() + " state");
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
		return CatalogUtils.getProcessedTasks(imageStore, region, initDate, endDate, inputdownloadingPhaseTag,
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
}