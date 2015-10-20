package org.fogbowcloud.scheduler.restlet.resource;

import java.util.Map;
import java.util.Map.Entry;

import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class TaskResource  extends ServerResource {

	@Get
	public Representation getEvents() throws Exception{
		
		String imageName = (String) getRequest().getAttributes().get("imgName");
		Map<Task, TaskState> tasks = ((SebalScheduleApplication) getApplication()).getAllTaskByImage(imageName);
		JSONArray jasonTasks = new JSONArray();
		for(Entry<Task, TaskState> e : tasks.entrySet()){
			JSONObject jsonTask = new JSONObject();
			jsonTask.put("taskId", e.getKey().getId());
			jsonTask.put("state", e.getValue().name());
			jsonTask.put("resourceId", e.getKey().getMetadata(TaskImpl.METADATA_RESOURCE_ID));
			jasonTasks.put(jsonTask);
		}

		StringRepresentation sr = new StringRepresentation(jasonTasks.toString(), MediaType.TEXT_PLAIN);
		return sr;
	}
	
	
}
