package org.fogbowcloud.saps.engine.core.dispatcher.restlet;

import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.SubmissionDispatcher;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.ImageResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.MainResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.ProcessedImagesResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.RegionResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.UserResource;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;
import org.restlet.service.CorsService;

public class DatabaseApplication extends Application {
	private static final String DB_WEB_STATIC_ROOT = "./dbWebHtml/static";

	public static final Logger LOGGER = Logger.getLogger(DatabaseApplication.class);

	private Properties properties;
	private SubmissionDispatcher submissionDispatcher;
	private Component restletComponent;

	public DatabaseApplication(Properties properties) throws Exception {
		if (!checkProperties(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Database Application.");

		this.properties = properties;
		this.submissionDispatcher = new SubmissionDispatcher(properties);

		CorsService cors = new CorsService();
		cors.setAllowedOrigins(new HashSet<>(Collections.singletonList("*")));
		cors.setAllowedCredentials(true);
		getServices().add(cors);
	}

	/**
	 * This function gets Scheduler properties
	 * 
	 * @return properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * This function sets Scheduler properties
	 * 
	 * @param properties new properties
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * This function gets submission dispatcher
	 * 
	 * @return submission dispatcher
	 */
	public SubmissionDispatcher getSubmissionDispatcher() {
		return submissionDispatcher;
	}

	/**
	 * This function sets submission dispatcher
	 * 
	 * @param submissionDispatcher new submission dispatcher
	 */
	public void setSubmissionDispatcher(SubmissionDispatcher submissionDispatcher) {
		this.submissionDispatcher = submissionDispatcher;
	}

	/**
	 * This function gets restlet component
	 * 
	 * @return restlet component
	 */
	public Component getRestletComponent() {
		return restletComponent;
	}

	/**
	 * This function sets restlet component
	 * 
	 * @param restletComponent new restlet component
	 */
	public void setRestletComponent(Component restletComponent) {
		this.restletComponent = restletComponent;
	}

	/**
	 * This function checks if the essential properties have been set.
	 * 
	 * @param properties saps properties to be check
	 * @return boolean representation, true (case all properties been set) or false
	 *         (otherwise)
	 */
	private boolean checkProperties(Properties properties) {
		if (!properties.containsKey(SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_OBJECT_STORE_HOST)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_OBJECT_STORE_HOST + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_OBJECT_STORE_CONTAINER)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_OBJECT_STORE_CONTAINER + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_FOLDER_PREFIX + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_AUTH_URL)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_AUTH_URL + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_PROJECT_ID)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_PROJECT_ID + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_USER_ID)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_USER_ID + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_PASSWORD)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_PASSWORD + " was not set");
			return false;
		}

		LOGGER.debug("All properties are set");
		return true;
	}

	/**
	 * Start Dispatcher component
	 * 
	 * @throws Exception
	 */
	public void startServer() throws Exception {
		Integer restServerPort = Integer
				.valueOf((String) properties.get(SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT));

		LOGGER.info("Starting service on port [ " + restServerPort + "]");

		ConnectorService corsService = new ConnectorService();
		this.getServices().add(corsService);

		this.restletComponent = new Component();
		this.restletComponent.getServers().add(Protocol.HTTP, restServerPort);
		this.restletComponent.getClients().add(Protocol.FILE);
		this.restletComponent.getDefaultHost().attach(this);

		this.restletComponent.start();
	}

	public void stopServer() throws Exception {
		this.restletComponent.stop();
	}

	@Override
	/**
	 * This function define application routes
	 */
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/", MainResource.class);
		router.attach("/ui/{requestPath}", MainResource.class);
		router.attach("/static",
				new Directory(getContext(), "file:///" + new File(DB_WEB_STATIC_ROOT).getAbsolutePath()));
		router.attach("/users", UserResource.class);
		router.attach("/processings", ImageResource.class);
		router.attach("/images/{imgName}", ImageResource.class);
		router.attach("/regions/details", RegionResource.class);
		router.attach("/regions/search", RegionResource.class);
		router.attach("/email", ProcessedImagesResource.class);

		return router;
	}

	/**
	 * This function gets tasks list in Catalog.
	 * 
	 * @return tasks list
	 * @throws SQLException
	 * @throws ParseException
	 */
	public List<SapsImage> getTasks() throws SQLException, ParseException {
		return submissionDispatcher.getAllTasks();
	}

	/**
	 * This function gets tasks with specific state in Catalog.
	 * 
	 * @param state task state to be searched
	 * @return tasks list with specific state
	 * @throws SQLException
	 */
	public List<SapsImage> getTasksInState(ImageTaskState state) throws SQLException {
		return this.submissionDispatcher.getTasksInState(state);
	}

	/**
	 * This function get saps image with specific id in Catalog.
	 * 
	 * @param taskId task id to be searched
	 * @return saps image with specific id
	 * @throws SQLException
	 */
	public SapsImage getTask(String taskId) {
		return submissionDispatcher.getTaskById(taskId);
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
			String preprocessingPhaseTag, String processingPhaseTag, String priority, String email) {
		submissionDispatcher.addNewTasks(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude,
				initDate, endDate, inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag,
				Integer.parseInt(priority), email);
	}

	/**
	 * This function add new user in Catalog.
	 * 
	 * @param userEmail  user email
	 * @param userName   user name
	 * @param userPass   user password
	 * @param userState  user state
	 * @param userNotify user notify
	 * @param adminRole  administrator role
	 * @throws SQLException
	 */
	public void createUser(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
			boolean adminRole) throws SQLException {
		submissionDispatcher.addUserInCatalog(userEmail, userName, userPass, userState, userNotify, adminRole);
	}

	public void addUserNotify(String submissionId, String taskId, String userEmail) throws SQLException {
		submissionDispatcher.addTaskNotificationIntoDB(submissionId, taskId, userEmail);
	}

	public boolean isUserNotifiable(String userEmail) throws SQLException {
		return submissionDispatcher.isUserNotifiable(userEmail);
	}

	/**
	 * This function gets user information in Catalog.
	 * 
	 * @param userEmail user email
	 * @return saps user
	 */
	public SapsUser getUser(String userEmail) {
		return submissionDispatcher.getUser(userEmail);
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
		return submissionDispatcher.searchProcessedTasks(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude, initDate, endDate, inputdownloadingPhaseTag, preprocessingPhaseTag,
				processingPhaseTag);
	}
}
