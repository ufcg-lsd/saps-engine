package org.fogbowcloud.scheduler.restlet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.scheduler.client.JDFTasks;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.restlet.resource.JobResource;
import org.fogbowcloud.scheduler.restlet.resource.TaskResource;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.service.CorsService;

public class JDFSchedulerApplication extends Application {
	
	private Properties properties;
	private Scheduler scheduler;
	
	
	public JDFSchedulerApplication(Scheduler scheduler, Properties properties){
		this.properties = properties;
		this.scheduler = scheduler;
	}
	

	public void startServer() throws Exception {
		
		CorsService corsService = new CorsService();         
		corsService.setAllowedOrigins(new HashSet(Arrays.asList("*")));
		corsService.setAllowedCredentials(true);
		
		this.getServices().add(corsService);
		
		Component c = new Component();
		int port = Integer.parseInt(properties.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
		c.getServers().add(Protocol.HTTP, port);
		c.getDefaultHost().attach(this); 
		c.start();

	}

	@Override
	public Restlet createInboundRoot() {
		
		Router router = new Router(getContext());
		router.attach("/sebal-scheduler/job", JobResource.class);
		router.attach("/sebal-scheduler/job/{JDFPath}", JobResource.class);
		router.attach("/sebal-scheduler/job/{jobId}", JobResource.class);
		router.attach("/sebal-scheduler/task/{taskId}", TaskResource.class);
		router.attach("/sebal-scheduler/task/{taskId}/{varName}", TaskResource.class);
		
		return router;
	}
	
	public Properties getProperties() {
		return properties;
	}


	public JDFJob getJobById(String jobId) {
		return (JDFJob) this.scheduler.getJobById(jobId);
	}

	public void addJob(String jdfFilePath){
		JDFJob job = new JDFJob();

		List<Task> taskList = JDFTasks.getTasksFromJDFFile(job.getId(), jdfFilePath, properties);
		
		for (Task task : taskList) {
			job.addTask(task);
		}
		this.scheduler.addJob(job);
	}


	public ArrayList<Job> getAllJobs() {
		return this.scheduler.getJobs();
	}
	
}
