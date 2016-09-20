package org.fogbowcloud.sebal.engine.scheduler.restlet.resource;

import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.scheduler.restlet.DatabaseApplication;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.json.JSONArray;
import org.json.JSONException;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class DBImageResource extends ServerResource {

	private static final String PURGE_MESSAGE_OK = "Images purged from database";
	private static final String FIRST_YEAR = "firstYear";
	private static final String LAST_YEAR = "lastYear";
	private static final String REGION = "region";
	private static final String SEBAL_VERSION = "sebalVersion";
	private static final String SEBAL_TAG = "sebalTag";
	private static final String DAY = "day";
	private static final String FORCE = "force";

	private static final Logger LOGGER = Logger
			.getLogger(DBImageResource.class);

	private static final String ADD_IMAGES_MESSAGE_OK = "Images successfully added";

	@Get
	public Representation getImages() throws Exception {

		LOGGER.info("Getting image");
		String imageName = (String) getRequest().getAttributes().get("imgName");

		LOGGER.debug("ImageName is " + imageName);

		if (imageName != null) {
			ImageData imageData = ((DatabaseApplication) getApplication())
					.getImage(imageName);
			JSONArray image = new JSONArray();
			try {
				image.put(imageData.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image data "
						+ imageData, e);
			}

			return new StringRepresentation(image.toString(),
					MediaType.APPLICATION_JSON);
		}

		LOGGER.info("Getting all images");

		List<ImageData> listOfImages = ((DatabaseApplication) getApplication())
				.getImages();
		JSONArray images = new JSONArray();

		for (ImageData imageData : listOfImages) {
			try {
				images.put(imageData.toJSON());
			} catch (JSONException e) {
				LOGGER.error("Error while creating JSON from image data "
						+ imageData, e);
			}
		}

		return new StringRepresentation(images.toString(),
				MediaType.APPLICATION_JSON);
	}

	@Post
	public StringRepresentation insertImages(Representation entity)
			throws Exception {

		Form form = new Form(entity);

		String firstYear = form.getFirstValue(FIRST_YEAR);
		String lastYear = form.getFirstValue(LAST_YEAR);
		String region = form.getFirstValue(REGION);
		String sebalVersion = form.getFirstValue(SEBAL_VERSION);
		String sebalTag = form.getFirstValue(SEBAL_TAG);

		if (firstYear == null || lastYear == null || region == null
				|| sebalVersion == null || sebalTag == null) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}

		DatabaseApplication application = (DatabaseApplication) getApplication();

		try {
			application.addImage(firstYear, lastYear, region, sebalVersion,
					sebalTag);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, e);
		}

		return new StringRepresentation(ADD_IMAGES_MESSAGE_OK,
				MediaType.APPLICATION_JSON);
	}
	
	@Delete
	public StringRepresentation purgeImage(Representation entity) throws Exception {
		
		Form form = new Form(entity);		
		
		String day = form.getFirstValue(DAY);
		String force = form.getFirstValue(FORCE);
		
		DatabaseApplication application = (DatabaseApplication) getApplication();
		
		try {
			application.purgeImage(day, force);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, e);
		}
		
		return new StringRepresentation(PURGE_MESSAGE_OK,
				MediaType.APPLICATION_JSON);
	}
}
