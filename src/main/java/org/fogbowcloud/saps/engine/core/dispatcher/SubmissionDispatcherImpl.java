package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.repository.DefaultImageRepository;
import org.fogbowcloud.saps.engine.core.repository.USGSNasaRepository;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.notifier.Ward;
import org.json.JSONArray;
import org.json.JSONException;

public class SubmissionDispatcherImpl implements SubmissionDispatcher {

	private final JDBCImageDataStore imageStore;
	private DefaultImageRepository nasaRepository;
	private USGSNasaRepository usgsRepository;
	private Properties properties;

	private static final Logger LOGGER = Logger.getLogger(SubmissionDispatcherImpl.class);

	public SubmissionDispatcherImpl(Properties properties) throws SQLException {
		this.properties = properties;
		this.imageStore = new JDBCImageDataStore(this.properties);
		this.nasaRepository = new DefaultImageRepository(properties);
		this.usgsRepository = new USGSNasaRepository(properties);
		this.usgsRepository.handleAPIKeyUpdate(Executors.newScheduledThreadPool(1));
	}

	@Override
	public void addUserInDB(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException {
		try {
			imageStore.addUser(userEmail, userName, userPass, userState, userNotify, adminRole);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in DB", e);
			throw new SQLException(e);
		}
	}

	@Override
	public void updateUserState(String userEmail, boolean userState) throws SQLException {
		try {
			imageStore.updateUserState(userEmail, userState);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in DB", e);
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
	public void addTaskNotificationIntoDB(String submissionId, String taskId, String imageName,
			String userEmail) throws SQLException {
		try {
			imageStore.addUserNotification(submissionId, taskId, imageName, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while adding image " + imageName + " user " + userEmail
					+ " in notify DB", e);
		}
	}

	@Override
	public void removeUserNotification(String submissionId, String taskId, String imageName,
			String userEmail) throws SQLException {
		try {
			imageStore.removeNotification(submissionId, taskId, imageName, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while removing image " + imageName + " user " + userEmail
					+ " from notify DB", e);
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
		List<ImageTask> tasksToPurge = force ? imageStore.getAllTasks() : imageStore
				.getIn(ImageTaskState.ARCHIVED);

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
				imageTask.setUpdateTime(imageStore.getTask(imageTask.getName()).getUpdateTime());
			}
		}
	}

	protected Date parseStringToDate(String day) throws ParseException {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		java.util.Date date = format.parse(day);
		java.sql.Date sqlDate = new java.sql.Date(date.getTime());
		return sqlDate;
	}

	protected boolean isBeforeDay(long date, Timestamp imageDataDay) {
		return (imageDataDay.getTime() <= date);
	}

	@Override
	public void listImagesInDB() throws SQLException, ParseException {
		List<ImageTask> allImageData = imageStore.getAllTasks();
		for (int i = 0; i < allImageData.size(); i++) {
			System.out.println(allImageData.get(i).toString());
		}
	}

	@Override
	public void listCorruptedImages() throws ParseException {
		List<ImageTask> allImageData;
		try {
			allImageData = imageStore.getIn(ImageTaskState.CORRUPTED);
			for (int i = 0; i < allImageData.size(); i++) {
				System.out.println(allImageData.get(i).toString());
			}
		} catch (SQLException e) {
			LOGGER.error("Error while gettin images in " + ImageTaskState.CORRUPTED
					+ " state from DB", e);
		}
	}

	@Override
	public List<Task> fillDB(int firstYear, int lastYear, List<String> regions, String dataSet,
			String containerRepository, String containerTag) throws IOException {
		LOGGER.debug("Regions: " + regions);
		List<Task> createdTasks = new ArrayList<Task>();
		String parsedDataSet = parseDataset(dataSet);

		int priority = 0;
		for (String region : regions) {
			createdTasks = submitImagesForYears(parsedDataSet, firstYear, lastYear, region,
					containerRepository, containerTag, priority);
			priority++;
		}
		return createdTasks;
	}

	private String parseDataset(String dataSet) {
		if (dataSet.equals(SapsPropertiesConstants.DATASET_LT5_TYPE)) {
			return SapsPropertiesConstants.LANDSAT_5_DATASET;
		} else if (dataSet.equals(SapsPropertiesConstants.DATASET_LE7_TYPE)) {
			return SapsPropertiesConstants.LANDSAT_7_DATASET;
		} else if (dataSet.equals(SapsPropertiesConstants.DATASET_LC8_TYPE)) {
			return SapsPropertiesConstants.LANDSAT_8_DATASET;
		}

		return null;
	}

	private List<Task> submitImagesForYears(String dataSet, int firstYear, int lastYear,
			String region, String containerRepository, String containerTag, int priority) {
		List<Task> createdTasks = new ArrayList<Task>();
		JSONArray availableImagesJSON = getUSGSRepository().getAvailableImagesInRange(dataSet,
				firstYear, lastYear, region);

		if (availableImagesJSON != null) {
			try {
				for (int i = 0; i < availableImagesJSON.length(); i++) {
					String entityId = availableImagesJSON.getJSONObject(i).getString(
							SapsPropertiesConstants.ENTITY_ID_JSON_KEY);
					String displayId = availableImagesJSON.getJSONObject(i).getString(
							SapsPropertiesConstants.DISPLAY_ID_JSON_KEY);
					String taskId = UUID.randomUUID().toString();

					getImageStore().addImageTask(taskId, entityId, "None", priority, containerRepository,
							containerTag, displayId);
					getImageStore().addStateStamp(entityId, ImageTaskState.CREATED,
							getImageStore().getTask(entityId).getUpdateTime());

					Task task = new Task();
					task.setId(UUID.randomUUID().toString());
					task.setImageTask(getImageStore().getTask(taskId));
					createdTasks.add(task);
				}
			} catch (JSONException e) {
				LOGGER.error("Error while getting entityId and displayId from JSON response", e);
			} catch (SQLException e) {
				LOGGER.error("Error while adding image to database", e);
			}
		}

		return createdTasks;
	}

	public List<ImageTask> getImagesInDB() throws SQLException, ParseException {
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

	protected String createImageList(String region, int year, String dataSet) {
		StringBuilder imageList = new StringBuilder();
		for (int day = 1; day < 366; day++) {
			NumberFormat formatter = new DecimalFormat("000");
			String imageName = new String();

			if (dataSet.equals(SapsPropertiesConstants.DATASET_LT5_TYPE)) {
				imageName = "LT5" + region + year + formatter.format(day);
			} else if (dataSet.equals(SapsPropertiesConstants.DATASET_LE7_TYPE)) {
				imageName = "LE7" + region + year + formatter.format(day);
			} else if (dataSet.equals(SapsPropertiesConstants.DATASET_LC8_TYPE)) {
				imageName = "LC8" + region + year + formatter.format(day);
			}

			imageList.append(imageName + "\n");
		}
		return imageList.toString().trim();
	}

	public JDBCImageDataStore getImageStore() {
		return imageStore;
	}

	protected void setNasaRepository(DefaultImageRepository nasaRepository) {
		this.nasaRepository = nasaRepository;
	}

	protected DefaultImageRepository getNasaRepository() {
		return nasaRepository;
	}

	protected void setUSGSRepository(USGSNasaRepository usgsRepository) {
		this.usgsRepository = usgsRepository;
	}

	protected USGSNasaRepository getUSGSRepository() {
		return usgsRepository;
	}

	public static String getImageRegionFromName(String imageName) {
		return imageName.substring(3, 9);
	}

	public Properties getProperties() {
		return properties;
	}
}