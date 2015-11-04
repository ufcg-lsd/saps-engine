package org.fogbowcloud.scheduler.restlet.resource;

import java.util.List;
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
		
		String taskId = (String) getRequest().getAttributes().get("taskId");
		
		if (taskId != null){
		Task task = ((SebalScheduleApplication) getApplication()).getTaskById(taskId);
		JSONObject jsonTask = new JSONObject();
		jsonTask.put("taskId", task.getId());
		jsonTask.put("resultingFile", task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER));
		StringRepresentation sr = new StringRepresentation(jsonTask.toString(), MediaType.TEXT_PLAIN);
		return sr;
		} else {
		List<Task> tasks = ((SebalScheduleApplication) getApplication()).getAllCompletedTasks();
		JSONArray jasonTasks = new JSONArray();
		for(Task e : tasks){
			JSONObject jsonTask = new JSONObject();
			jsonTask.put("taskId", e.getId());
			jsonTask.put("resultingFile", e.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER));
			jasonTasks.put(jsonTask);
		}

		StringRepresentation sr = new StringRepresentation(jasonTasks.toString(), MediaType.TEXT_PLAIN);
		return sr;
		}
	}
	
	
}
