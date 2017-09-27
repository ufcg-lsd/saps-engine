package org.fogbowcloud.saps.engine.scheduler.restlet;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.SubmissionDispatcherImpl;
import org.fogbowcloud.saps.engine.core.dispatcher.Task;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.scheduler.restlet.resource.ImageResource;
import org.fogbowcloud.saps.engine.scheduler.restlet.resource.MainResource;
import org.fogbowcloud.saps.engine.scheduler.restlet.resource.UserResource;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;

public class DatabaseApplication extends Application {
	private static final String DB_WEB_STATIC_ROOT = "./dbWebHtml/static";

	public static final Logger LOGGER = Logger.getLogger(DatabaseApplication.class);

	private SubmissionDispatcherImpl submissionDispatcher;
	private Component restletComponent;

	public DatabaseApplication(SubmissionDispatcherImpl submissionDispatcher) throws Exception {
		this.submissionDispatcher = submissionDispatcher;
	}

	public void startServer() throws Exception {
		Properties properties = this.submissionDispatcher.getProperties();
		if (!properties.containsKey(SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT)) {
			throw new IllegalArgumentException(SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT
					+ " is missing on properties.");
		}
		Integer restServerPort = Integer.valueOf(
				(String) properties.get(SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT));

		LOGGER.info("Starting service on port: " + restServerPort);

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
	public Restlet createInboundRoot() {
		// TODO: change endpoints for new SAPS dashboard
		Router router = new Router(getContext());
		router.attach("/", MainResource.class);
		router.attach("/ui/{requestPath}", MainResource.class);
		router.attach("/static", new Directory(getContext(),
				"file:///" + new File(DB_WEB_STATIC_ROOT).getAbsolutePath()));
		router.attach("/users", UserResource.class);
		router.attach("/users/{userEmail}", UserResource.class);
		router.attach("/user/register", UserResource.class);
		router.attach("/images", ImageResource.class);
		router.attach("/images/{imgName}", ImageResource.class);

		return router;
	}

	public List<ImageTask> getTasks() throws SQLException, ParseException {
		return submissionDispatcher.getTaskListInDB();
	}

	public ImageTask getTask(String taskId) throws SQLException {
		return submissionDispatcher.getTaskInDB(taskId);
	}

	/**
	 * 
	 * @param firstYear
	 * @param lastYear
	 * @param region
	 * @param workerContainerRepository
	 * @param workerContainerTag
	 * @return List<String> Image names list
	 * @throws SQLException
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public List<Task> addTasks(Date firstYear, Date lastYear, String region, String dataSet,
			String downloaderContainerRepository, String downloaderContainerTag,
			String preProcessorContainerRepository, String preProcessorContainerTag,
			String workerContainerRepository, String workerContainerTag)
			throws SQLException, NumberFormatException, IOException {
		List<String> regions = new ArrayList<String>();
		regions.add(region);

		return submissionDispatcher.fillDB(firstYear, lastYear, regions, dataSet,
				downloaderContainerRepository, downloaderContainerTag,
				preProcessorContainerRepository, preProcessorContainerTag,
				workerContainerRepository, workerContainerTag);
	}

	public void purgeImage(String day, String force) throws SQLException, ParseException {
		boolean forceValue;

		if (force.equals("yes")) {
			forceValue = true;
		} else {
			forceValue = false;
		}

		submissionDispatcher.setTasksToPurge(day, forceValue);
	}

	public void createUser(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException {
		submissionDispatcher.addUserInDB(userEmail, userName, userPass, userState, userNotify,
				adminRole);
	}

	public void updateUserState(String userEmail, boolean userState) throws SQLException {
		submissionDispatcher.updateUserState(userEmail, userState);
	}

	public void addUserNotify(String submissionId, String taskId, String userEmail) throws SQLException {
		submissionDispatcher.addTaskNotificationIntoDB(submissionId, taskId, userEmail);
	}

	public boolean isUserNotifiable(String userEmail) throws SQLException {
		return submissionDispatcher.isUserNotifiable(userEmail);
	}

	public SapsUser getUser(String userEmail) {
		return submissionDispatcher.getUser(userEmail);
	}

}
