package org.fogbowcloud.scheduler.restlet.resource;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.fogbowcloud.sebal.SebalTasks;
import org.fogbowcloud.sebal.bootstrap.DBBootstrap;
import org.opensaml.util.resource.ResourceException;
import org.restlet.data.MediaType;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.amazonaws.util.json.JSONObject;

public class TaskResource  extends ServerResource {
	private static final Logger LOGGER = Logger.getLogger(TaskResource.class);

	List<String> validVariables = new ArrayList<String>();
	
	@Get
	public Representation fetch() throws Exception{
		LOGGER.info("Getting tasks...");
		fillValidVariables();
		String taskId = (String) getRequest().getAttributes().get("taskId");
		LOGGER.debug("TaskId is " + taskId);
		String varName = (String) getRequest().getAttributes().get("varName");
		LOGGER.debug("varName is " + varName);

		if (taskId == null) {
			throw new ResourceException("TaskId was not specified.");
		}

		SebalScheduleApplication application = (SebalScheduleApplication) getApplication();
		Task task = application.getTaskById(taskId);
		LOGGER.debug("TaskId " + taskId + " is of task " + task);
		if (task == null) {
			throw new ResourceException("TaskId " + taskId + " is not a completed task.");
		}
		
		Properties properties = application.getProperties();
		String resultstDir = properties.getProperty("scheduler_results_dir") == null ? "/mnt/sebal-results/results"
				: properties.getProperty("scheduler_results_dir");
		
		String imageResultFolder = resultstDir + "/"  + task.getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		String fileNamePrefix = getFileNamePrefix(task);

		if (varName != null) {
			return getImagemAsRepresentation(imageResultFolder, fileNamePrefix, varName);
		}
		
		JSONObject jsonTask = new JSONObject();		
		Map<String, String> existingFiles = filelist(imageResultFolder, fileNamePrefix);
		LOGGER.debug("existing files are " + existingFiles);
		
		if (existingFiles.size() != validVariables.size()) {
			LOGGER.debug("Results are not generated yet.");
			try {
				render(task, properties);
			} catch (Exception e) {
				LOGGER.error("Error while rendering.", e);
				throw new ResourceException("It was not possible rendering results of task "
						+ taskId + ".");
			}
		}
		
		jsonTask.put("variables", validVariables);
		return new StringRepresentation(jsonTask.toString(), MediaType.TEXT_PLAIN);
	}

	private void render(Task task, Properties properties) throws IOException, InterruptedException, ResourceException {		
		LOGGER.debug("Rendering results to task " + task);
		
		String libraryPath = properties.getProperty("scheduler_library_path") == null ? "/usr/local/lib/"
				: properties.getProperty("scheduler_library_path");

		String sebalClassPath = properties.getProperty("scheduler_sebal_classpath") == null ? "~/SEBAL/target/SEBAL-0.0.1-SNAPSHOT.jar:~/SEBAL/target/lib/*"
				: properties.getProperty("scheduler_sebal_classpath");

		String imagesDir = properties.getProperty("scheduler_images_dir") == null ? "/mnt/sebal-images/images"
				: properties.getProperty("scheduler_images_dir");
		
		//untaring image for getting MTL file	
		String imageName = task.getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		String untarCommand = "mkdir -p /tmp/" + imageName + "; cp " + imagesDir + "/" + imageName
				+ "/" + imageName + ".tar.gz /tmp/" + imageName + "; cd /tmp/" + imageName
				+ "; tar -xvzf " + imageName + ".tar.gz"; 
		
		LOGGER.debug("Untar command: " + untarCommand);
		ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", untarCommand);
		Process pr = builder.start();
		int exitValue = pr.waitFor();

		LOGGER.debug("Local process [cmdLine=" + untarCommand + "] output was: \n" + getOutout(pr));
		if (exitValue != 0) {
			LOGGER.debug("Local process [cmdLine=" + untarCommand + "] error output was: \n"
					+ getErrOutput(pr));
			throw new ResourceException("It was not possible run command " + untarCommand);
		}
		
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
				+ " " + resultstDir + " " + range + " " + n + " " + i + " " + boundingboxFilePath + " bmp";

		// java -Xss4m -Djava.library.path=/usr/local/lib/ -cp
		// target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/*
		// org.fogbowcloud.sebal.render.RenderHelper
		// ../images/$IMAGE_NAME/$IMAGE_NAME"_MTL.txt" ../results 0 0 8000 8000
		// 1 1 boundingbox_vertices_niels tiff

		LOGGER.debug("Render command: " + command);
		builder = new ProcessBuilder("/bin/bash", "-c", command);
		pr = builder.start();
		exitValue = pr.waitFor();

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
					+ DBBootstrap.getImageRegionFromName(task.getMetadata(SebalTasks.METADATA_IMAGE_NAME)));
			File boundingboxFile = new File(
					properties.getProperty("sebal_local_boundingbox_dir") + "/boundingbox_"
							+ DBBootstrap.getImageRegionFromName(task.getMetadata(SebalTasks.METADATA_IMAGE_NAME)));
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
	
	private String getFileNamePrefix(Task task) {
		return task.getMetadata(SebalTasks.METADATA_LEFT_X) + "."
				+ task.getMetadata(SebalTasks.METADATA_RIGHT_X) + "."
				+ task.getMetadata(SebalTasks.METADATA_UPPER_Y) + "."
				+ task.getMetadata(SebalTasks.METADATA_LOWER_Y) + "_"
				+ task.getMetadata(SebalTasks.METADATA_NUMBER_OF_PARTITIONS) + "_"
				+ task.getMetadata(SebalTasks.METADATA_PARTITION_INDEX) + "_new_";
	}

	private void fillValidVariables() {
		validVariables.add("ndvi");
		validVariables.add("evi");
		validVariables.add("iaf");
		validVariables.add("ts");
		validVariables.add("alpha");
		validVariables.add("rn");
		validVariables.add("g");
	}

	public Map<String, String> filelist(String parentFolder, String prefix) {
		Map<String, String> varNameToFile = new HashMap<String, String>();
		File parentFolderFile = new File(parentFolder);

		LOGGER.debug("image result dir is " + parentFolderFile.getAbsolutePath());
		if (parentFolderFile.exists() && parentFolderFile.isDirectory()) {
			for (String varName : validVariables) {
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

	private Representation getImagemAsRepresentation(String resultFolder, String fileNamePrefix, String varName) throws ResourceException{
		File file = new File(resultFolder, fileNamePrefix + varName + ".bmp");

		LOGGER.debug("Image result file path" + file.getAbsolutePath());
		if (file.exists()) {			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			FileInputStream fis = null;
			byte[] buf = new byte[1024];
			try {
				fis = new FileInputStream(file);
				for (int readNum; (readNum = fis.read(buf)) != -1;) {
					// Writes to this byte array output stream
					bos.write(buf, 0, readNum);
				}
				
				byte[] data = bos.toByteArray();
				
				ObjectRepresentation<byte[]> or = new ObjectRepresentation<byte[]>(data,
						MediaType.IMAGE_BMP) {
					@Override
					public void write(OutputStream os) throws IOException {
						super.write(os);
						os.write(this.getObject());
					}
				};
				return or; 
			} catch (IOException ex) {
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
					bos.close();
				} catch (IOException e) {
					
				}
			}		
		} else {
			LOGGER.error("Image result file path" + file.getAbsolutePath() + " does not exist.");
		}
		throw new ResourceException("It was not possible download the file for variable " + varName);
	}
}
