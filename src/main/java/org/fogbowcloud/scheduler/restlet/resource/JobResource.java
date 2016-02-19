package org.fogbowcloud.scheduler.restlet.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.restlet.JDFSchedulerApplication;
import org.fogbowcloud.sebal.SebalTasks;
import org.fogbowcloud.sebal.bootstrap.DBBootstrap;
import org.opensaml.util.resource.ResourceException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.amazonaws.util.json.JSONObject;

public class JobResource extends ServerResource {
	private static final Logger LOGGER = Logger.getLogger(TaskResource.class);

	Map<String, TaskState> jobTasks = new HashMap<String, TaskState>();
	
	Map<String, Integer> jobsNumberOfTasks = new HashMap<String, Integer>();

	@Get
	public Representation fetch() throws Exception {
		LOGGER.info("Getting Jobs...");
		String JobId = (String) getRequest().getAttributes().get("JobId");
		LOGGER.debug("JobId is " + JobId);
		String varName = (String) getRequest().getAttributes().get("varName");
		LOGGER.debug("varName is " + varName);
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		JSONObject jsonJob = new JSONObject();

		if (JobId == null) {
			for (Job job : application.getAllJobs()){
				jobsNumberOfTasks.put(job.getId(), job.getByState(TaskState.READY).size());
			}
			
			jsonJob.put("Jobs", jobsNumberOfTasks);
			return new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);
		}

		JDFJob job = application.getJobById(JobId);
		LOGGER.debug("TaskId " + JobId + " is of task " + job);
		if (job == null) {
			throw new ResourceException("TaskId " + JobId + " is not a completed task.");
		}

//		Properties properties = application.getProperties();

		

		for (Task task : job.getByState(TaskState.READY)){
			jobTasks.put(task.getId(), TaskState.READY);
		};
		for (Task task : job.getByState(TaskState.RUNNING)){
			jobTasks.put(task.getId(), TaskState.RUNNING);
		};
		for (Task task : job.getByState(TaskState.COMPLETED)){
			jobTasks.put(task.getId(), TaskState.COMPLETED);
		};
		for (Task task : job.getByState(TaskState.FAILED)){
			jobTasks.put(task.getId(), TaskState.FAILED);
		};
		
		
		jsonJob.put("Tasks", jobTasks);
		return new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);
	}

	@Post
	private void addJob() {
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		String JDFPath = (String) getRequest().getAttributes().get("JDFPath");
		application.addJob(JDFPath);
	}
	
	private void render(Task task, Properties properties) throws IOException, InterruptedException,
			ResourceException {
		LOGGER.debug("Rendering results to task " + task);

		String libraryPath = properties.getProperty("scheduler_library_path") == null ? "/usr/local/lib/"
				: properties.getProperty("scheduler_library_path");

		String sebalClassPath = properties.getProperty("scheduler_sebal_classpath") == null ? "~/SEBAL/target/SEBAL-0.0.1-SNAPSHOT.jar:~/SEBAL/target/lib/*"
				: properties.getProperty("scheduler_sebal_classpath");

		String imagesDir = properties.getProperty("scheduler_images_dir") == null ? "/mnt/sebal-images/images"
				: properties.getProperty("scheduler_images_dir");

		// untaring image for getting MTL file
		String imageName = task.getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		String mtlFilePath = "/tmp/" + imageName + "/" + imageName + "_MTL.txt";

		String resultstDir = properties.getProperty("scheduler_results_dir") == null ? "/mnt/sebal-results/results"
				: properties.getProperty("scheduler_results_dir");

		String range = task.getMetadata(SebalTasks.METADATA_LEFT_X) + " "
				+ task.getMetadata(SebalTasks.METADATA_UPPER_Y) + " "
				+ task.getMetadata(SebalTasks.METADATA_RIGHT_X) + " "
				+ task.getMetadata(SebalTasks.METADATA_LOWER_Y);

		String n = task.getMetadata(SebalTasks.METADATA_NUMBER_OF_PARTITIONS);
		String i = task.getMetadata(SebalTasks.METADATA_PARTITION_INDEX);
		String boundingboxFilePath = getBoundingBoxFilePath(task, properties);

		String command = "java -Xss16m -Djava.library.path=" + libraryPath + " -cp "
				+ sebalClassPath + " org.fogbowcloud.sebal.render.RenderHelper " + mtlFilePath
				+ " " + resultstDir + " " + range + " " + n + " " + i + " " + boundingboxFilePath
				+ " bmp";

		// java -Xss4m -Djava.library.path=/usr/local/lib/ -cp
		// target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/*
		// org.fogbowcloud.sebal.render.RenderHelper
		// ../images/$IMAGE_NAME/$IMAGE_NAME"_MTL.txt" ../results 0 0 8000 8000
		// 1 1 boundingbox_vertices_niels tiff

		LOGGER.debug("Render command: " + command);
		ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", command);
		Process pr = builder.start();
		int exitValue = pr.waitFor();

		LOGGER.debug("Local process [cmdLine=" + command + "] output was: \n" + getOutout(pr));
		if (exitValue != 0) {
			LOGGER.debug("Local process [cmdLine=" + command + "] error output was: \n"
					+ getErrOutput(pr));
			throw new ResourceException("It was not possible run command " + command);

		}
	}

	private String getBoundingBoxFilePath(Task task, Properties properties) {
		String boundingboxFilePath = "";

		if (properties.getProperty("sebal_local_boundingbox_dir") != null) {
			LOGGER.debug("Region of image is "
					+ DBBootstrap.getImageRegionFromName(task
							.getMetadata(SebalTasks.METADATA_IMAGE_NAME)));
			File boundingboxFile = new File(properties.getProperty("sebal_local_boundingbox_dir")
					+ "/boundingbox_"
					+ DBBootstrap.getImageRegionFromName(task
							.getMetadata(SebalTasks.METADATA_IMAGE_NAME)));
			LOGGER.debug("The boundingbox file for this image should be "
					+ boundingboxFile.getAbsolutePath());
			if (boundingboxFile.exists()) {
				boundingboxFilePath = boundingboxFile.getAbsolutePath();
			}
		}
		return boundingboxFilePath;
	}

	private String getOutout(Process pr) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		StringBuilder out = new StringBuilder();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			out.append(line);
		}
		return out.toString();
	}

	private String getErrOutput(Process pr) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		StringBuilder err = new StringBuilder();
		while (true) {
			String line = r.readLine();
			if (line == null) {
				break;
			}
			err.append(line);
		}
		return err.toString();
	}

	public Map<String, String> filelist(String parentFolder, String prefix) {
		Map<String, String> varNameToFile = new HashMap<String, String>();
		File parentFolderFile = new File(parentFolder);

		LOGGER.debug("image result dir is " + parentFolderFile.getAbsolutePath());
		if (parentFolderFile.exists() && parentFolderFile.isDirectory()) {
			for (String varName : jobTasks.keySet()) {
				for (File file : parentFolderFile.listFiles()) {
					if (file.isFile() && file.getName().equals(prefix + varName + ".bmp")) {
						varNameToFile.put(varName, file.getAbsolutePath());
						break;
					}
				}
			}
		} else {

		}
		return varNameToFile;
	}
}
