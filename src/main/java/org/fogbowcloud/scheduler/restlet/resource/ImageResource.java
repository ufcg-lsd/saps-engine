package org.fogbowcloud.scheduler.restlet.resource;

import java.util.List;

import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.fogbowcloud.sebal.ImageData;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;


public class ImageResource extends ServerResource {
	
	
	@Get
	public Representation getEvents() throws Exception{
		
		
		List<ImageData> images = ((SebalScheduleApplication) getApplication()).getAllImages();
		Gson gson = new Gson();
		String jsonEvents = gson.toJson(images);
		StringRepresentation sr = new StringRepresentation(jsonEvents, MediaType.TEXT_PLAIN);
		return sr;
	}

}
