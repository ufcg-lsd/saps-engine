package org.fogbowcloud.saps.engine.core.job;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.task.Task;
import org.fogbowcloud.saps.engine.core.task.TaskImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * It add the job name, job name and sched path to the {@link Job} abstraction.
 */
public class SapsJob extends Job implements Serializable {
	private static final Logger LOGGER = Logger.getLogger(SapsJob.class);
	private static final long serialVersionUID = 7780896231796955706L;

	private static final String JSON_HEADER_JOB_ID = "jobId";
	private static final String JSON_HEADER_JOB_ID_ARREBOL = "jobIdArrebol";
	private static final String JSON_HEADER_NAME = "name";
	private static final String JSON_HEADER_UUID = "uuid";
	private static final String JSON_HEADER_STATE = "state";
	private static final String JSON_HEADER_OWNER = "owner";
	private static final String JSON_HEADER_TASKS = "tasks";

	private final String jobId;
	private final String owner;
	private final String userId;
	private String name;
	private JobState state;
	private String jobIdArrebol;
	
	public SapsJob(String jobId, String owner, List<Task> taskList, String userID) {
		super(taskList);
		this.name = "";
		this.jobId = jobId;
		this.owner = owner;
		this.userId = userID;
		this.state = JobState.SUBMITTED;
	}

	public SapsJob(String jobId, String owner, List<Task> taskList, String userID, String jobIdArrebol) {
		super(taskList);
		this.name = "";
		this.jobId = jobId;
		this.owner = owner;
		this.userId = userID;
		this.state = JobState.SUBMITTED;
		this.jobIdArrebol = jobIdArrebol;
	}

	public SapsJob(String owner, List<Task> taskList, String userID) {
		this(UUID.randomUUID().toString(), owner, taskList, userID);
	}

	@Override
	public void setState(JobState state) {
		this.state = state;
	}

	public String getJobId() {
		return jobId;
	}
	
	public String getJobIdArrebol() {
		return jobIdArrebol;
	}
	
	public void setJobIdArrebol(String jobIdArrebol) {
		this.jobIdArrebol = jobIdArrebol;
	}
	
	public String getId() {
		return jobId;
	}

	public String getName() {
		return this.name;
	}

	public String getOwner() {
		return this.owner;
	}

	public float completionPercentage() {
		List<Task> tasks = getTasks();
		if (tasks.size() == 0) return 100.0f;
		float completedTasks = 0.0f;
		for (Task task : tasks) {
			if (task.isFinished()) completedTasks++;
		}
		return (float) (100.0*completedTasks/tasks.size());
	}

	public Task getTaskById(String taskId) {
		return this.getTaskList().get(taskId);
	}

	public void setFriendlyName(String name) {
		this.name = name;
	}

	public JobState getState() {
		return this.state;
	}

	public void finishCreation() {
		this.state = JobState.CREATED;
	}

	public void failCreation() {
		this.state = JobState.FAILED;
	}

	
	public String getUserId() {
		return this.userId;
	}

	public JSONObject toJSON() {
		try {
			JSONObject job = new JSONObject();
			job.put(JSON_HEADER_JOB_ID, this.getId());
			job.put(JSON_HEADER_NAME, this.getName());
			job.put(JSON_HEADER_OWNER, this.getOwner());
			job.put(JSON_HEADER_UUID, this.getUserId());
			job.put(JSON_HEADER_STATE, this.getState().value());
			job.put(JSON_HEADER_JOB_ID_ARREBOL, this.jobIdArrebol);
			JSONArray tasks = new JSONArray();
			Map<String, Task> taskList = this.getTaskList();
			for (Entry<String, Task> entry : taskList.entrySet()) {
				tasks.put(entry.getValue().toJSON());
			}
			job.put(JSON_HEADER_TASKS, tasks);
			return job;
		} catch (JSONException e) {
			LOGGER.debug("Error while trying to create a JSONObject from JDFJob", e);
			return null;
		}
	}

	// TODO implement JSON_HEADER_JOB_ID_ARREBOL
	public static SapsJob fromJSON(JSONObject job){
        LOGGER.info("Reading Job from JSON");
        List<Task> tasks = new ArrayList<>();
		
		JSONArray tasksJSON = job.optJSONArray(JSON_HEADER_TASKS);
		for (int i = 0; i < tasksJSON.length(); i++) {
			JSONObject taskJSON = tasksJSON.optJSONObject(i);
			Task task = TaskImpl.fromJSON(taskJSON);
			tasks.add(task);
		}
		
		SapsJob sapsJob = new SapsJob(
				job.optString(JSON_HEADER_JOB_ID),
				job.optString(JSON_HEADER_OWNER),
				tasks,
				job.optString(JSON_HEADER_UUID),
						job.optString(JSON_HEADER_JOB_ID_ARREBOL)
		);
		sapsJob.setFriendlyName(job.optString(JSON_HEADER_NAME));
		try {
			sapsJob.state = JobState.create(job.optString(JSON_HEADER_STATE));
		} catch (Exception e) {
			LOGGER.debug("JSON had bad state", e);
		}
        LOGGER.debug("Job read from JSON is from owner: " + job.optString(JSON_HEADER_OWNER));
        return sapsJob;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SapsJob jdfJob = (SapsJob) o;
		return jobId.equals(jdfJob.jobId) &&
				owner.equals(jdfJob.owner);
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId, owner);
	}
}