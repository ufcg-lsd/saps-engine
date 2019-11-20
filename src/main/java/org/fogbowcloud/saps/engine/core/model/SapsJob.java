package org.fogbowcloud.saps.engine.core.model;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dto.CommandRequestDTO;
import org.fogbowcloud.saps.engine.core.dto.JobResponseDTO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * It add the job name, job name and sched path to the {@link Job} abstraction.
 */
public class SapsJob implements Serializable {
	private static final Logger LOGGER = Logger.getLogger(SapsJob.class);
	
	private static final long serialVersionUID = 7780896231796955706L;

	private static final String JSON_HEADER_NAME = "name";
	private static final String JSON_HEADER_TASKS = "tasks";
	
	private String name;
	private List<SapsTask> tasksList;
	
	public SapsJob(String name, List<SapsTask> tasks) {
		this.name = name;
		this.tasksList = tasks;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<SapsTask> getTasksList() {
		return tasksList;
	}

	public void setTasksList(List<SapsTask> tasksList) {
		this.tasksList = tasksList;
	}
	
	public void addTask(SapsTask task) {
		tasksList.add(task);
	}

	public JSONObject toJSON() {
		try {
			JSONObject job = new JSONObject();
			
			job.put(JSON_HEADER_NAME, getName());
			
			JSONArray taskList = new JSONArray();
			for (SapsTask task: getTasksList()) 
				taskList.put(task.toJSON());
			job.put(JSON_HEADER_TASKS, taskList);
			
			return job;
		} catch (JSONException e) {
			LOGGER.debug("Error while trying to create a JSONObject from JDFJob", e);
			return null;
		}
	}

	public static SapsJob fromJSON(JSONObject job){
        List<SapsTask> tasks = new ArrayList<SapsTask>();
		
        String name = job.optString(JSON_HEADER_NAME);
        
		JSONArray tasksJSON = job.optJSONArray(JSON_HEADER_TASKS);
		for (int i = 0; i < tasksJSON.length(); i++) {
			JSONObject taskJSON = tasksJSON.optJSONObject(i);
			SapsTask task = SapsTask.fromJSON(taskJSON);
			tasks.add(task);
		}
		
		SapsJob sapsJob = new SapsJob(name, tasks);
        return sapsJob;
	}

}