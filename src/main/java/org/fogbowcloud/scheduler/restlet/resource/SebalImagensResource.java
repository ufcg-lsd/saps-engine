package org.fogbowcloud.scheduler.restlet.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageState;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opensaml.util.resource.ResourceException;
import org.restlet.data.MediaType;
import org.restlet.engine.header.Header;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class SebalImagensResource extends ServerResource {
	
	
	private static final Logger LOGGER = Logger.getLogger(SebalImagensResource.class);
	private SebalScheduleApplication application;
	
	
	List<String> validVariables = new ArrayList<String>();
	
	/**
	 * This method returns tow types of Representation. First one is a JSON representation of an image with informations about this image.
	 * Second one is a byte of array that is the content of one of seven variations of the image. What will be returned is defined by the
	 * request parameters.
	 * 
	 * JSON Representation of image:
	 * 
	 * Atributes:
	 * {name:"",
	 *  status:"",
	 *  variables:[]
	 * }
	 * 
	 * Each image have seven variations of him, that is: 'ndvi', 'evi', 'iaf', 'ts', 'alpha', 'rn' and 'g'
	 * 
	 * @return JSON representation of an Image (if is a list option) OR a byte array with the image content if is a request for download.
	 * @throws Exception
	 */
	@Get
	public Representation fetch() throws Exception {
		LOGGER.info("Getting tasks...");
		fillValidVariables();
		
		String varName = (String) getRequest().getAttributes().get("varName");
		String imageName = (String) getRequest().getAttributes().get("imageName");
		String filter = (String) getRequest().getAttributes().get("filter");
		String state = getQuery().getValues("state");
		String name = getQuery().getValues("name");
		String periodInitFilter = getQuery().getValues("periodInit");
		String periodEndFilter = getQuery().getValues("periodEnd");
		
		application = (SebalScheduleApplication) getApplication();
		
		fixHeaders();
		
		if(filter != null){
			JSONArray jsonImagesArray = new JSONArray();
			if(filter.equals("image_state")){
				for (String stateValue : ImageState.getAllValues()) {
					jsonImagesArray.put(stateValue);
				}
			}
			return new StringRepresentation(jsonImagesArray.toString(), MediaType.TEXT_PLAIN);
		}
		
		if(varName==null || varName.isEmpty()){
			//TODO list all fetched images
			
			long periodInit = 0;
			long periodEnd = 0;
			
			if(periodInitFilter!=null){
				periodInit = Long.parseLong(periodInitFilter);
			}
			if(periodEndFilter!=null){
				periodEnd = Long.parseLong(periodEndFilter);
			}
			
			List<ImageData> allImages = application.getImagesByFilters(ImageState.getStateFromStr(state), name, 
					periodInit, periodEnd);
			
			JSONArray jsonImagesArray = new JSONArray();
			for (ImageData imageData : allImages) {
				JSONObject imageJson = new JSONObject();
				imageJson.put("name", imageData.getName());
				imageJson.put("status", imageData.getState());
				imageJson.put("time", new Date());
				if(ImageState.FETCHED.equals(imageData.getState())){
					imageJson.put("variables", validVariables);
				}
				jsonImagesArray.put(imageJson);
			}
			
			return new StringRepresentation(jsonImagesArray.toString(), MediaType.TEXT_PLAIN);
			
		}else{
			return getImagemAsRepresentation(imageName, varName);
		}
		
		
	}
	
	private Representation getImagemAsRepresentation(String fileNamePrefix,	String varName) throws ResourceException {
		try {

			ObjectRepresentation<byte[]> or = new ObjectRepresentation<byte[]>(application.getImageFromSwift(fileNamePrefix, varName),
					MediaType.IMAGE_BMP) {
				@Override
				public void write(OutputStream os) throws IOException {
					super.write(os);
					os.write(this.getObject());
				}
			};
			return or;
		} catch (Exception ex) {
			throw new ResourceException("It was not possible download the file for variable " + varName);
		}
	}
	
	private void fixHeaders() {
		Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get("org.restlet.http.headers");
		if (responseHeaders == null) {
		    responseHeaders = new Series(Header.class);
		    responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
		    responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		    responseHeaders.add("Access-Control-Allow-Origin", "*");
		    getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
		}
	}
	
	private void fillValidVariables() {
		validVariables.add("NDVI");
		validVariables.add("EVI");
		validVariables.add("LAI");
		validVariables.add("TS");
		validVariables.add("alb");
		validVariables.add("Rn");
		validVariables.add("G");
	}
}
