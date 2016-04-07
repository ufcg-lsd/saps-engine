package org.fogbowcloud.scheduler.restlet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.scheduler.client.JDFTasks;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.restlet.resource.JobResource;
import org.fogbowcloud.scheduler.restlet.resource.TaskResource4JDF;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;

public class JDFSchedulerApplication extends Application {

	private Properties properties;
	private Scheduler scheduler;

	private Component c;

	public JDFSchedulerApplication(Scheduler scheduler, Properties properties){
		this.properties = properties;
		this.scheduler = scheduler;
	}


	public void startServer() throws Exception {

		ConnectorService corsService = new ConnectorService();         

		this.getServices().add(corsService);

		c = new Component();
		int port = Integer.parseInt(properties.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
		c.getServers().add(Protocol.HTTP, port);
		c.getDefaultHost().attach(this); 
		c.start();
	}

	public void stopServer() throws Exception{
		c.stop();
	}

	@Override
	public Restlet createInboundRoot() {

		Router router = new Router(getContext());
		router.attach("/sebal-scheduler/job", JobResource.class);
		router.attach("/sebal-scheduler/job/{jobpath}", JobResource.class);
		router.attach("/sebal-scheduler/job/{jobpath}/{schedPath}", JobResource.class);
		router.attach("/sebal-scheduler/job/{jobpath}/{schedPath}/{jobName}", JobResource.class);
		router.attach("/sebal-scheduler/task/{taskId}", TaskResource4JDF.class);
		router.attach("/sebal-scheduler/task/{taskId}/{varName}", TaskResource4JDF.class);

		return router;
	}

	public Properties getProperties() {
		return properties;
	}


	public JDFJob getJobById(String jobId) {
		return (JDFJob) this.scheduler.getJobById(jobId);
	}

	public String addJob(String jdfFilePath, String schedPath){
		JDFJob job = new JDFJob();
		
		job.setSchedPath(schedPath);
		
		List<Task> taskList = getTasksFromJDFFile(job.getId(), jdfFilePath, schedPath, properties);
				
		for (Task task : taskList) {
			job.addTask(task);
		}
		this.scheduler.addJob(job);
		return job.getId();
	}
	
	public String addJob(String jdfFilePath, String schedPath, String friendlyName) {
		JDFJob job = new JDFJob();
		
		job.setSchedPath(schedPath);
		
		List<Task> taskList = getTasksFromJDFFile(job.getId(), jdfFilePath, schedPath, properties);
				
		for (Task task : taskList) {
			job.addTask(task);
		}
		job.setName(friendlyName);
		this.scheduler.addJob(job);
		return job.getId();
	}

	public List<Task> getTasksFromJDFFile(String jobId, String jdfFilePath,String schedPath, Properties properties) {
		return JDFTasks.getTasksFromJDFFile(jobId, jdfFilePath,schedPath, properties);
	}


	public ArrayList<JDFJob> getAllJobs() {
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		for (Job job : this.scheduler.getJobs()){
			jobList.add((JDFJob) job);
		}
		return jobList;
	}

	public Task getTaskById(String taskId){
		for (Job job : getAllJobs()){
			JDFJob jdfJob = (JDFJob) job;
			Task task = jdfJob.getTaskById(taskId);
			if (task != null) {
				return task;
			}
		}
		return null;
	}


	public TaskState getTaskState(String taskId) {
		for (Job job : getAllJobs()){
			JDFJob jdfJob = (JDFJob) job;
			TaskState taskState = jdfJob.getTaskState(taskId);
			if (taskState != null) {
				return taskState;
			}
		}
		return null;
	}


	public String stopJob(String jobId) {
		scheduler.removeJob(jobId);
		return null;
	}


}
