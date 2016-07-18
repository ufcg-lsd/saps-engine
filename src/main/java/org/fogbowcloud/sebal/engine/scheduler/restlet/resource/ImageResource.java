package org.fogbowcloud.sebal.engine.scheduler.restlet.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.blowout.scheduler.core.model.Task;
import org.fogbowcloud.blowout.scheduler.core.model.TaskImpl;
import org.fogbowcloud.sebal.engine.scheduler.restlet.SebalScheduleApplication;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;


public class ImageResource extends ServerResource {
	
	private static final Logger LOGGER = Logger.getLogger(ImageResource.class);

	@Get
	public Representation getEvents() throws Exception{
		LOGGER.info("Getting image...");
		String imageName = (String) getRequest().getAttributes().get("imgName");
		
		LOGGER.debug("ImageName is " + imageName);
		
		if (imageName != null) {
			Map<Task, TaskState> tasks = ((SebalScheduleApplication) getApplication()).getAllTaskByImage(imageName);
			LOGGER.debug("The image " + imageName + " has " + tasks.size() + " tasks.");
			JSONArray jasonTasks = new JSONArray();
			for(Entry<Task, TaskState> e : tasks.entrySet()){
				JSONObject jsonTask = new JSONObject();
				jsonTask.put("taskId", e.getKey().getId());
				jsonTask.put("state", e.getValue().name());
				jsonTask.put("resourceId", e.getKey().getMetadata(TaskImpl.METADATA_RESOURCE_ID));
				jasonTasks.put(jsonTask);
			}
			return new StringRepresentation(jasonTasks.toString(), MediaType.TEXT_PLAIN);
		}
		
		List<ImageData> images = ((SebalScheduleApplication) getApplication()).getAllImages();
		LOGGER.debug("The are " + images.size() + " images.");
		
		for(ImageData image : images){
			Map<Task, TaskState> tasks = ((SebalScheduleApplication) getApplication()).getAllTaskByImage(image.getName());
			Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();
			
			LOGGER.debug("The image " + imageName + " has " + tasks.size() + " tasks.");
			
			for(Entry<Task, TaskState> e : tasks.entrySet()){
				
				Integer increment = new Integer(1);
				
				if(tasksStatesCount.containsKey(e.getValue().name())){
					increment = tasksStatesCount.get(e.getValue().name())+1;
				}
				tasksStatesCount.put(e.getValue().name(), increment);
				
			}
			image.setTasksStatesCount(tasksStatesCount);
		}
		
		
		Gson gson = new Gson();
		String jsonEvents = gson.toJson(images);
		StringRepresentation sr = new StringRepresentation(jsonEvents, MediaType.TEXT_PLAIN);
		return sr;
	}

}
