package org.fogbowcloud.sebal.engine.scheduler.restlet;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.sebal.engine.scheduler.restlet.resource.DBImageResource;
import org.fogbowcloud.sebal.engine.scheduler.restlet.resource.UserResource;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.bootstrap.DBUtilsImpl;
import org.fogbowcloud.sebal.engine.sebal.model.SebalUser;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;

public class DatabaseApplication extends Application {
	
	private DBUtilsImpl dbUtilsImpl;
	private Component restletComponent;
	
	public static final Logger LOGGER = Logger.getLogger(DatabaseApplication.class);
	
	public DatabaseApplication(DBUtilsImpl dbUtilsImpl) throws Exception {
		this.dbUtilsImpl = dbUtilsImpl;
	}
	
	public void startServer() throws Exception {
		Properties properties = this.dbUtilsImpl.getProperties();
		if (!properties.containsKey(AppPropertiesConstants.DB_REST_SERVER_PORT)) {
			throw new IllegalArgumentException(
					AppPropertiesConstants.DB_REST_SERVER_PORT
							+ " is missing on properties.");
		}
		Integer restServerPort = Integer.valueOf((String) properties
				.get(AppPropertiesConstants.DB_REST_SERVER_PORT));

		LOGGER.info("Starting service on port: " + restServerPort);

		ConnectorService corsService = new ConnectorService();
		this.getServices().add(corsService);

		this.restletComponent = new Component();
		this.restletComponent.getServers().add(Protocol.HTTP, restServerPort);
		this.restletComponent.getDefaultHost().attach(this);

		this.restletComponent.start();
	}
	
	public void stopServer() throws Exception {
		this.restletComponent.stop();
	}

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/users", UserResource.class);
		router.attach("/users/{userEmail}", UserResource.class);
		router.attach("/user/register", UserResource.class);
		router.attach("/images", DBImageResource.class);
		router.attach("/images/{imgName}", DBImageResource.class);

		return router;
	}
	
	public List<ImageData> getImages() throws SQLException, ParseException {
		return dbUtilsImpl.getImagesInDB();
	}
	
	public ImageData getImage(String imageName) throws SQLException {
		return dbUtilsImpl.getImageInDB(imageName);
	}
	
	/**
	 * 
	 * @param firstYear
	 * @param lastYear
	 * @param region
	 * @param sebalVersion
	 * @param sebalTag
	 * @return List<String> Image names list
	 * @throws SQLException
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public List<String> addImages(int firstYear, int lastYear, String region,
			String sebalVersion, String sebalTag) throws SQLException,
			NumberFormatException, IOException {
		List<String> regions = new ArrayList<String>();
		regions.add(region);

		return dbUtilsImpl.fillDB(firstYear, lastYear, regions, sebalVersion, sebalTag);
	}
	
	public void purgeImage(String day, String force) throws SQLException, ParseException {
		boolean forceValue;
		
		if(force.equals("yes")) {
			forceValue = true;
		} else {
			forceValue = false;
		}
		
		dbUtilsImpl.setImagesToPurge(day, forceValue);		
	}

	public void createUser(String userEmail, String userName, String userPass,
			boolean userState, boolean userNotify, boolean adminRole)
			throws SQLException {

		dbUtilsImpl.addUserInDB(userEmail, userName, userPass, userState,
				userNotify, adminRole);
	}
	
	public void updateUserState(String userEmail, boolean userState)
			throws SQLException {

		dbUtilsImpl.updateUserState(userEmail, userState);
	}
	
	public void addUserNotify(String imageName, String userEmail) throws SQLException {
		
		dbUtilsImpl.addUserInNotifyDB(imageName, userEmail);
	}
	
	public boolean isUserNotifiable(String userEmail) throws SQLException {
		
		return dbUtilsImpl.isUserNotifiable(userEmail);
	}

	public SebalUser getUser(String userEmail) {
		return dbUtilsImpl.getUser(userEmail);
	}
}
