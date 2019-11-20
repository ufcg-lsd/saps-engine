package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.dispatcher.notifier.Ward;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.DatasetUtil;

public class SubmissionDispatcher {
	public static final int DEFAULT_PRIORITY = 0;
	public static final String DEFAULT_USER = "admin";
	private final JDBCImageDataStore imageStore;
	private Properties properties;

	private static final Logger LOGGER = Logger.getLogger(SubmissionDispatcher.class);

	public SubmissionDispatcher(JDBCImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	public SubmissionDispatcher(Properties properties) throws SQLException {
		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(properties);
	}

	public void addUserInDB(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
			boolean adminRole) throws SQLException {
		try {
			imageStore.addUser(userEmail, userName, userPass, userState, userNotify, adminRole);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in Catalogue", e);
			throw new SQLException(e);
		}
	}

	public void updateUserState(String userEmail, boolean userState) throws SQLException {
		try {
			imageStore.updateUserState(userEmail, userState);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in Catalogue", e);
			throw new SQLException(e);
		}
	}

	public SapsUser getUser(String userEmail) {
		try {
			return imageStore.getUser(userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while trying to get Sebal User with email: " + userEmail + ".", e);
		}
		return null;
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

	public void setTasksToPurge(String day, boolean force) throws SQLException, ParseException {
		List<SapsImage> tasksToPurge = force ? imageStore.getAllTasks() : imageStore.getIn(ImageTaskState.ARCHIVED);

		for (SapsImage imageTask : tasksToPurge) {
			long date = 0;
			try {
				date = parseStringToDate(day).getTime();
			} catch (ParseException e) {
				LOGGER.error("Error while parsing string to date", e);
			}
			if (isBeforeDay(date, imageTask.getUpdateTime())) {
				imageTask.setStatus(SapsImage.PURGED);

				imageStore.updateImageTask(imageTask);
				imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
			}
		}
	}

	protected Date parseStringToDate(String day) throws ParseException {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		java.util.Date date = format.parse(day);
		java.sql.Date sqlDate = new java.sql.Date(date.getTime());
		return sqlDate;
	}

	protected boolean isBeforeDay(long date, Timestamp imageTaskDay) {
		return (imageTaskDay.getTime() <= date);
	}

	// FIXME is it necessaty ? "System.out.println" ?
	public void listTasksInDB() throws SQLException, ParseException {
		List<SapsImage> allImageTask = imageStore.getAllTasks();
		for (int i = 0; i < allImageTask.size(); i++) {
			System.out.println(allImageTask.get(i).toString());
		}
	}

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

	public Set<String> getRegionsFromArea(String lowerLeftLatitude, String lowerLeftLongitude,
			String upperRightLatitude, String upperRightLongitude) {

		LOGGER.debug("Getting landsat ID of: \n" + "lower left latitude: " + lowerLeftLatitude + "\n"
				+ "lower left longitude: " + lowerLeftLongitude + "\n" + "upper right latitude: " + upperRightLatitude
				+ "\n" + "upper right longitude: " + upperRightLongitude);

		String[] middlePoint = getMiddlePoint(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude).split(" ");

		LOGGER.debug("Setting middle point(reference point to the LandSat region): + \n" + "latitude: " + middlePoint[0]
				+ "\n" + "longitude: " + middlePoint[1]);

		String regionIds = "";
		try {
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

	public List<Task> fillDB(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude, Date initDate, Date endDate, String inputGathering, String inputPreprocessing,
			String algorithmExecution, String priorityS, String email) {
		List<Task> createdTasks = new ArrayList<>();

		int priority = Integer.parseInt(priorityS);
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(initDate);
		GregorianCalendar endCal = new GregorianCalendar();
		endCal.setTime(endDate);
		endCal.add(Calendar.DAY_OF_YEAR, 1);

		Set<String> regions = getRegionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude);

		while (cal.before(endCal)) {
			try {
				int startingYear = cal.get(Calendar.YEAR);
				List<String> datasets = DatasetUtil.getSatsInOperationByYear(startingYear);

				for (String dataset : datasets) {

					LOGGER.debug("----------------------------------------> " + dataset);

					for (String region : regions) {
						String taskId = UUID.randomUUID().toString();

						SapsImage iTask = getImageStore().addImageTask(taskId, dataset, region, cal.getTime(), "None",
								priority, email, inputGathering, inputPreprocessing, algorithmExecution);

						Task task = new Task(UUID.randomUUID().toString());
						task.setImageTask(iTask);
						getImageStore().addStateStamp(taskId, ImageTaskState.CREATED,
								getImageStore().getTask(taskId).getUpdateTime());
						getImageStore().dispatchMetadataInfo(taskId);

						createdTasks.add(task);
					}
				}

			} catch (SQLException e) {
				LOGGER.error("Error while adding image to database", e);
			}
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		return createdTasks;
	}

	public List<SapsImage> getTaskListInDB() throws SQLException, ParseException {
		return imageStore.getAllTasks();
	}

	public List<Ward> getUsersToNotify() throws SQLException {
		List<Ward> wards = imageStore.getUsersToNotify();
		return wards;
	}

	public SapsImage getTaskInDB(String taskId) throws SQLException {
		List<SapsImage> allTasks = imageStore.getAllTasks();

		for (SapsImage imageTask : allTasks) {
			if (imageTask.getTaskId().equals(taskId)) {
				return imageTask;
			}
		}

		return null;
	}

	public List<SapsImage> getTasksInState(ImageTaskState imageState) throws SQLException {
		return this.imageStore.getIn(imageState);
	}

	public JDBCImageDataStore getImageStore() {
		return imageStore;
	}

	public Properties getProperties() {
		return properties;
	}

	public List<SapsImage> searchProcessedTasks(String lowerLeftLatitude, String lowerLeftLongitude,
			String upperRightLatitude, String upperRightLongitude, Date initDate, Date endDate,
			String inputPreprocessing, String inputGathering, String algorithmExecution) {

		List<SapsImage> filteredTasks = new ArrayList<>();
		Set<String> regions = new HashSet<>();
		regions.addAll(getRegionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude));

		for (String region : regions) {
			List<SapsImage> iTasks = null;
			try {
				iTasks = getImageStore().getProcessedImages(region, initDate, endDate, inputGathering,
						inputPreprocessing, algorithmExecution);
				filteredTasks.addAll(iTasks);
			} catch (SQLException e) {
				String builder = "Failed to load images with configuration:\n" + "\tRegion: " + region + "\n"
						+ "\tInterval: " + initDate + " - " + endDate + "\n" + "\tPreprocessing: " + inputPreprocessing
						+ "\n" + "\tGathering: " + inputGathering + "\n" + "\tAlgorithm: " + algorithmExecution + "\n";
				LOGGER.error(builder, e);
			}
		}
		return filteredTasks;
	}
}