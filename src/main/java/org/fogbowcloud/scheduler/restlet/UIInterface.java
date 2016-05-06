package org.fogbowcloud.scheduler.restlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import org.json.JSONArray;

public class UIInterface extends ServerResource {

	private static final Logger LOGGER = Logger.getLogger(UIInterface.class);

	@Get
	public Representation stopJob() throws IOException {
		File htmlFile = new File("src/main/resources/treeExample.html");
		FileInputStream input = new FileInputStream(htmlFile);
		String htmlString = IOUtils.toString(input);

		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();

		JSONArray jArray = new JSONArray();

		for (JDFJob job : application.getAllJobs()) {
			JSONObject jobInfo = new JSONObject();
			try {
				jobInfo.put("id", job.getId());
				jobInfo.put("parent", "#");
				jobInfo.put("text", job.getName());
				jArray.put(jobInfo);
				int taskNumber = 1;
				fillTasks(jArray, job, taskNumber);
			} catch (JSONException e) {
				LOGGER.error(e.getMessage());
			}
		}
		LOGGER.debug("Info:" + jArray.toString());

		htmlString = htmlString.replace("[PlaceholderContent]", jArray.toString());

		return new StringRepresentation(htmlString, MediaType.TEXT_HTML);
	}

	private void fillTasks(JSONArray jArray, JDFJob job, int taskNumber) throws JSONException {
		for (Task task : job.getByState(TaskState.READY)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("parent", job.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("taskState", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
		for (Task task : job.getByState(TaskState.RUNNING)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("parent", job.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("taskState", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
		for (Task task : job.getByState(TaskState.FAILED)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("parent", job.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("taskState", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
		for (Task task : job.getByState(TaskState.COMPLETED)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("parent", job.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("taskState", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
	}


}
