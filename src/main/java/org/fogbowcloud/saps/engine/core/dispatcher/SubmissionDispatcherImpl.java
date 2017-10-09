package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.notifier.Ward;

public class SubmissionDispatcherImpl implements SubmissionDispatcher {

	private final JDBCImageDataStore imageStore;
	private Properties properties;

	private static final Logger LOGGER = Logger.getLogger(SubmissionDispatcherImpl.class);

	public SubmissionDispatcherImpl(JDBCImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	public SubmissionDispatcherImpl(Properties properties) throws SQLException {
		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(properties);
	}

	@Override
	public void addUserInDB(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException {
		try {
			imageStore.addUser(userEmail, userName, userPass, userState, userNotify, adminRole);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in Catalogue", e);
			throw new SQLException(e);
		}
	}

	@Override
	public void updateUserState(String userEmail, boolean userState) throws SQLException {
		try {
			imageStore.updateUserState(userEmail, userState);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in Catalogue", e);
			throw new SQLException(e);
		}
	}

	@Override
	public SapsUser getUser(String userEmail) {
		try {
			return imageStore.getUser(userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while trying to get Sebal User with email: " + userEmail + ".", e);
		}
		return null;
	}

	@Override
	public void addTaskNotificationIntoDB(String submissionId, String taskId, String userEmail)
			throws SQLException {
		try {
			imageStore.addUserNotification(submissionId, taskId, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while adding task " + taskId + " notification for user " + userEmail
					+ " in Catalogue", e);
		}
	}

	@Override
	public void removeUserNotification(String submissionId, String taskId, String userEmail)
			throws SQLException {
		try {
			imageStore.removeNotification(submissionId, taskId, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while removing task " + taskId + " notification for user "
					+ userEmail + " from Catalogue", e);
		}
	}

	@Override
	public boolean isUserNotifiable(String userEmail) throws SQLException {
		try {
			return imageStore.isUserNotifiable(userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while verifying user notify", e);
		}

		return false;
	}

	@Override
	public void setTasksToPurge(String day, boolean force) throws SQLException, ParseException {
		List<ImageTask> tasksToPurge = force ? imageStore.getAllTasks()
				: imageStore.getIn(ImageTaskState.ARCHIVED);

		for (ImageTask imageTask : tasksToPurge) {
			long date = 0;
			try {
				date = parseStringToDate(day).getTime();
			} catch (ParseException e) {
				LOGGER.error("Error while parsing string to date", e);
			}
			if (isBeforeDay(date, imageTask.getUpdateTime())) {
				imageTask.setImageStatus(ImageTask.PURGED);

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

	@Override
	public void listTasksInDB() throws SQLException, ParseException {
		List<ImageTask> allImageTask = imageStore.getAllTasks();
		for (int i = 0; i < allImageTask.size(); i++) {
			System.out.println(allImageTask.get(i).toString());
		}
	}

	@Override
	public void listCorruptedImages() throws ParseException {
		List<ImageTask> allImageTask;
		try {
			allImageTask = imageStore.getIn(ImageTaskState.CORRUPTED);
			for (int i = 0; i < allImageTask.size(); i++) {
				System.out.println(allImageTask.get(i).toString());
			}
		} catch (SQLException e) {
			LOGGER.error("Error while gettin tasks in " + ImageTaskState.CORRUPTED
					+ " state from Catalogue", e);
		}
	}

	@Override
	public List<Task> fillDB(String firstYear, String lastYear, List<String> regions,
			String dataSet, String downloaderContainerRepository, String downloaderContainerTag,
			String preProcessorContainerRepository, String preProcessorContainerTag,
			String workerContainerRepository, String workerContainerTag)
			throws IOException, ParseException {
		LOGGER.debug("Regions: " + regions);
		List<Task> createdTasks = new ArrayList<Task>();

		int priority = 0;
		for (String region : regions) {
			createdTasks.addAll(submitImagesForYears(dataSet, firstYear, lastYear, region,
					downloaderContainerRepository, downloaderContainerTag,
					preProcessorContainerRepository, preProcessorContainerTag,
					workerContainerRepository, workerContainerTag, priority));
			priority++;
		}
		return createdTasks;
	}

	private List<Task> submitImagesForYears(String dataSet, String firstDate, String lastDate,
			String region, String downloaderContainerRepository, String downloaderContainerTag,
			String preProcessorContainerRepository, String preProcessorContainerTag,
			String workerContainerRepository, String workerContainerTag, int priority)
			throws ParseException {
		DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = dateFormater.parse(firstDate);
		Date endDate = dateFormater.parse(lastDate);
		List<Task> createdTasks = new ArrayList<Task>();
		
		for (int year = Integer.parseInt(getYearFromCalendar(startDate)); year <= Integer
				.parseInt(getYearFromCalendar(endDate)); year++) {
			GregorianCalendar cal = new GregorianCalendar();
			int total = 365;
			cal.set(Calendar.YEAR, year);
			if (cal.isLeapYear(year)) {
				total++;
			}

			for (int d = 1; d <= total; d++) {
				cal.set(Calendar.DAY_OF_YEAR, d);
				Date date = cal.getTime();
				try {
					String taskId = UUID.randomUUID().toString();

					getImageStore().addImageTask(taskId, dataSet, region, dateFormater.format(date),
							"None", priority, downloaderContainerRepository, downloaderContainerTag,
							preProcessorContainerRepository, preProcessorContainerTag,
							workerContainerRepository, workerContainerTag);
					getImageStore().addStateStamp(taskId, ImageTaskState.CREATED,
							getImageStore().getTask(taskId).getUpdateTime());

					Task task = new Task(UUID.randomUUID().toString());
					task.setImageTask(getImageStore().getTask(taskId));
					createdTasks.add(task);
				} catch (SQLException e) {
					LOGGER.error("Error while adding image to database", e);
				}
			}
		}


		return createdTasks;
	}
	
	/**
	 * Constructs a Calendar object, and then obtains the year
	 * by using the Calendar.get(...) method for the year.
	 */
	public static String getYearFromCalendar(Date dte) throws IllegalArgumentException
	{
	    String year = "";

	    if (dte == null) {
	        throw new IllegalArgumentException("Null date!");
	    }

	    // get a Calendar
	    Calendar cal = Calendar.getInstance();

	    // set the Calendar to the specific date; the reason why
	    // Calendar is deprecated is this mutability
	    cal.setTime(dte);

	    // get the year using the .get method, and convert to a String
	    year = String.valueOf(cal.get(Calendar.YEAR));

	    return year;
	}

	public List<ImageTask> getTaskListInDB() throws SQLException, ParseException {
		return imageStore.getAllTasks();
	}

	@Override
	public List<Ward> getUsersToNotify() throws SQLException {
		List<Ward> wards = imageStore.getUsersToNotify();
		return wards;
	}

	public ImageTask getTaskInDB(String taskId) throws SQLException {
		List<ImageTask> allTasks = imageStore.getAllTasks();

		for (ImageTask imageTask : allTasks) {
			if (imageTask.getTaskId().equals(taskId)) {
				return imageTask;
			}
		}

		return null;
	}

	public JDBCImageDataStore getImageStore() {
		return imageStore;
	}

	public Properties getProperties() {
		return properties;
	}
}