package org.fogbowcloud.scheduler.restlet.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.restlet.JDFSchedulerApplication;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.amazonaws.util.json.JSONObject;

public class JobResource extends ServerResource {
	private static final String FRIENDLY = "friendly";

	private static final String SCHED_PATH = "schedpath";

	private static final String JDF_FILE_PATH = "jdffilepath";

	private static final Logger LOGGER = Logger.getLogger(JobResource.class);

	Map<String, String> jobTasks = new HashMap<String, String>();

	Map<String, String> jobsNumberOfTasks = new HashMap<String, String>();

	@Get
	public Representation fetch() throws Exception {
		LOGGER.info("Getting Jobs...");
		String jobId = (String) getRequest().getAttributes().get("jobpath");
		LOGGER.debug("JobId is " + jobId);
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		JSONObject jsonJob = new JSONObject();
		
		if (jobId == null) { 
			for (JDFJob job : application.getAllJobs()){
				if (job.getName() != null) {
					jobsNumberOfTasks.put(job.getId() ,  job.getName() +" "+ job.getByState(TaskState.READY).size());
				} else {
					jobsNumberOfTasks.put(job.getId(), String.valueOf(job.getByState(TaskState.READY).size()));
				}
			}

			LOGGER.debug("Got all jobs, there are " + jobsNumberOfTasks.size());

			jsonJob.put("Jobs", jobsNumberOfTasks);

			LOGGER.debug("My info Is: " + jsonJob.toString());

			StringRepresentation result = new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);

			return result;
		}

		JDFJob job = application.getJobById(jobId);
		if (job == null) {
			
			job = application.getJobByName(jobId);
			if (job == null) {
			throw new ResourceException(404);
			}
		}
		LOGGER.debug("JobID " + jobId + " is of job " + job);


		for (Task task : job.getByState(TaskState.READY)){
			LOGGER.debug("Task Id is:" + task.getId());
			jobTasks.put(task.getId(), TaskState.READY.toString());
		};
		for (Task task : job.getByState(TaskState.RUNNING)){
			jobTasks.put(task.getId(), TaskState.RUNNING.toString());
		};
		for (Task task : job.getByState(TaskState.COMPLETED)){
			jobTasks.put(task.getId(), TaskState.COMPLETED.toString());
		};
		for (Task task : job.getByState(TaskState.FAILED)){
			jobTasks.put(task.getId(), TaskState.FAILED.toString());
		};


		jsonJob.put("Tasks", jobTasks);
		return new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);
	}

	@Post
	public StringRepresentation addJob(Representation entity) throws IOException {
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		final Form form = new Form(entity);
	
		String JDFFilePath = form.getFirstValue(JDF_FILE_PATH);
		LOGGER.debug("all names: " +form.getNames());					
		String schedPath = form.getFirstValue(SCHED_PATH);
		
		String friendlyName = form.getFirstValue(FRIENDLY);
		
		if (application.getJobByName(friendlyName) != null) {
			throw new ResourceException(406, "Friendly name already in use" , "406", friendlyName);
		}
		
		
		LOGGER.debug("URL INFO" + JDFFilePath);

		String jobId = "job";

		if (friendlyName != null) {
			LOGGER.debug("Job friendly name is: "+ friendlyName);
			jobId = application.addJob(JDFFilePath, schedPath, friendlyName);
		} else {
			jobId = application.addJob(JDFFilePath, schedPath);
		}
		return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
	}

	@Delete
	public StringRepresentation stopJob() {
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();

		String JDFString = (String) getRequest().getAttributes().get("jobpath");

		LOGGER.debug("Got JDF File: " + JDFString);

		String jobId = application.stopJob(JDFString);
		
		if (jobId == null) {
			throw new ResourceException(404);
		}

		return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
	}
}
