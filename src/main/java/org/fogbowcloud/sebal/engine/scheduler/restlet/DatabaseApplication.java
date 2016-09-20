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
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.bootstrap.DBUtilsImpl;
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
	
	public void addImage(String firstYear, String lastYear, String region,
			String sebalVersion, String sebalTag) throws SQLException,
			NumberFormatException, IOException {
		List<String> regions = new ArrayList<String>();
		regions.add(region);

		// TODO: see the consequences of these casts
		dbUtilsImpl.fillDB(Integer.valueOf(firstYear),
				Integer.valueOf(lastYear), regions, sebalVersion, sebalTag);
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
}
