package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Command;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;


public class SebalTasks {

	public static final String JOIN_F1_RESULTS_PHASE = "join-f1-results";
	public static final String UPLOAD_PHASE = "upload";
	public static final String F1_PHASE = "f1";
	public static final String METADATA_PHASE = "phase";
	public static final String METADATA_IMAGE_NAME = "image_name";
	private static final String METADATA_NUMBER_OF_PARTITIONS = "number_of_partitions";
	private static final String METADATA_PARTITION_INDEX = "partition_index";
	private static final String METADATA_SEBAL_LOCAL_SCRIPTS_DIR = "local_scripts_dir";
	private static final String METADATA_ADDITIONAL_LIBRARY_PATH = "additonal_library_path";

	private static final Logger LOGGER = Logger.getLogger(SebalTasks.class);
	private static final String METADATA_LEFT_X = "left_x";
	private static final String METADATA_UPPER_Y = "upper_y";
	private static final String METADATA_RIGHT_X = "right_x";
	private static final String METADATA_LOWER_Y = "lower_y";
	private static final String METADATA_REMOTE_BOUNDINGBOX_PATH = "remote_boundingbox_path";
	private static final String METADATA_IMAGES_MOUNT_POINT = "images_mount_point";
	private static final String METADATA_RESULTS_MOUNT_POINT = "results_mount_point";
	private static final String METADATA_SEBAL_URL = "sebal_url";
	private static final String METADATA_REPOS_USER = "repository_user";
	private static final String METADATA_IMAGE_REPOSITORY = "image_repository";
	private static final String METADATA_RESULT_REPOSITORY = "result_repository";
	private static final String METADATA_REMOTE_REPOS_PRIVATE_KEY_PATH = "remote_repos_private_key_path";

	public static List<Task> createF1Tasks(Properties properties, String imageName, Specification spec) {
		LOGGER.debug("Creating F1 tasks for image " + imageName);

		String numberOfPartitions = properties.getProperty("sebal_number_of_partitions");
		List<Task> f1Tasks = new ArrayList<Task>();

		for (int partitionIndex = 1; partitionIndex <= Integer.parseInt(numberOfPartitions); partitionIndex++) {
			
			TaskImpl f1Task = new TaskImpl(UUID.randomUUID().toString(), spec);
						
			settingCommonTaskMetadata(properties, f1Task);
			
			// setting image F1 execution properties
			f1Task.putMetadata(METADATA_PHASE, F1_PHASE);
			f1Task.putMetadata(METADATA_SEBAL_URL, properties.getProperty("sebal_url"));
			f1Task.putMetadata(METADATA_IMAGE_NAME, imageName);
			f1Task.putMetadata(METADATA_NUMBER_OF_PARTITIONS,
					properties.getProperty("sebal_number_of_partitions"));
			f1Task.putMetadata(METADATA_PARTITION_INDEX, String.valueOf(partitionIndex));
			f1Task.putMetadata(METADATA_LEFT_X, properties.getProperty("sebal_image_interval_left_x"));
			f1Task.putMetadata(METADATA_UPPER_Y,
					properties.getProperty("sebal_image_interval_upper_y"));
			f1Task.putMetadata(METADATA_RIGHT_X,
					properties.getProperty("sebal_image_interval_right_x"));
			f1Task.putMetadata(METADATA_LOWER_Y,
					properties.getProperty("sebal_image_interval_lower_y"));
			f1Task.putMetadata(METADATA_SEBAL_LOCAL_SCRIPTS_DIR,
					properties.getProperty("sebal_local_scripts_dir"));
			
			//treating repository user private key
			if (properties.getProperty("sebal_repository_user_private_key") != null) {
				File privateKeyFile = new File(properties.getProperty("sebal_repository_user_private_key"));
				String remotePrivateKeyPath = f1Task.getMetadata(TaskImpl.METADATA_SANDBOX) + "/"
						+ privateKeyFile.getName();
				
				f1Task.putMetadata(METADATA_REMOTE_REPOS_PRIVATE_KEY_PATH, remotePrivateKeyPath);
//				f1Task.addCommand(new StageCommand(Command.Type.STAGE_IN, privateKeyFile
//						.getAbsolutePath(), remotePrivateKeyPath));
				//TODO Prologue command
			}
		
			// treating boundingbox 
			if (properties.getProperty("sebal_local_boundingbox_dir") != null) {
				LOGGER.debug("Region of image is "
						+ DBBootstrap.getImageRegionFromName(imageName));
				File boundingboxFile = new File(
						properties.getProperty("sebal_local_boundingbox_dir") + "/boundingbox_"
								+ DBBootstrap.getImageRegionFromName(imageName));
				LOGGER.debug("The boundingbox file for this image should be "
						+ boundingboxFile.getAbsolutePath());
				if (boundingboxFile.exists()) {
					String remoteBoundingboxPath = f1Task.getMetadata(TaskImpl.METADATA_SANDBOX)
							+ "/boundingbox_vertices";

					f1Task.putMetadata(METADATA_REMOTE_BOUNDINGBOX_PATH, remoteBoundingboxPath);
//					f1Task.addCommand(new StageCommand(Command.Type.STAGE_IN, boundingboxFile
//							.getAbsolutePath(), remoteBoundingboxPath));
					
					//TODO PRologue command
					
				} else {
					LOGGER.warn("There is no boundingbox file specified for image " + imageName);
				}
			}
			
			// creating f1 script for this image
			File localScriptFile = createScriptFile(properties, f1Task);
			String remoteScriptPath = f1Task.getMetadata(TaskImpl.METADATA_SANDBOX) + "/"
					+ localScriptFile.getName();
			
			// adding commands
//			f1Task.addCommand(new StageCommand(Command.Type.STAGE_IN, localScriptFile
//					.getAbsolutePath(), remoteScriptPath));
			//TODO Prologue command 
			
			//f1Task.addCommand(getTaskExecuteCommand(f1Task, remoteScriptPath));
			//TODO remote command
			
			String outputFileName = imageName + "_" + f1Task.getMetadata(METADATA_PARTITION_INDEX)
					+ "_" + f1Task.getMetadata(METADATA_NUMBER_OF_PARTITIONS);
			
//			f1Task.addCommand(new StageCommand(Command.Type.STAGE_OUT, f1Task
//					.getMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER) + "/" + outputFileName,
//					f1Task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER) + "/"
//							+ outputFileName));
			//TODO Epilogue command 
			
			f1Tasks.add(f1Task);
		}
		return f1Tasks;
	}

	private static void settingCommonTaskMetadata(Properties properties, Task task) {
		// sdexs properties
		task.putMetadata(TaskImpl.METADATA_SANDBOX, properties.getProperty("sebal_sandbox"));
		task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER,
				properties.getProperty("sebal_sandbox") + "/output");
		task.putMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER,
				properties.getProperty("sebal_local_output_dir"));

		// repository properties
		task.putMetadata(METADATA_REPOS_USER, properties.getProperty("sebal_remote_user"));
		task.putMetadata(METADATA_IMAGE_REPOSITORY,
				properties.getProperty("sebal_image_repository"));
		task.putMetadata(METADATA_RESULT_REPOSITORY,
				properties.getProperty("sebal_result_repository"));
		task.putMetadata(METADATA_IMAGES_MOUNT_POINT,
				properties.getProperty("sebal_images_mount_point"));
		task.putMetadata(METADATA_RESULTS_MOUNT_POINT,
				properties.getProperty("sebal_results_mount_point"));
	}

//	private static Command getTaskExecuteCommand(TaskImpl task, String remoteScript) {
//		String command = "chmod +x " + remoteScript + "; nohup " + remoteScript
//				+ " > /dev/null 2>&1 &";
//		return new ExecuteCommand(command);
//	}

	private static File createScriptFile(Properties props, TaskImpl task) {
		File tempFile = null;
		FileOutputStream fos = null;
		FileInputStream fis = null;
		try {
			tempFile = File.createTempFile("temp-sebal-", ".sh");
			fis = new FileInputStream(task.getMetadata(METADATA_SEBAL_LOCAL_SCRIPTS_DIR) + "/"
					+ task.getMetadata(METADATA_PHASE) + ".sh");
			String origExec = IOUtils.toString(fis);
			fos = new FileOutputStream(tempFile);
			IOUtils.write(replaceVariables(props, task, origExec), fos);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Throwable t) {
				// Do nothing, best effort
			}
		}
		return tempFile;
	}

	public static String replaceVariables(Properties props, TaskImpl task, String command) {
		command = command.replaceAll(Pattern.quote("${IMAGE_NAME}"),
				task.getMetadata(METADATA_IMAGE_NAME));
		command = command.replaceAll(Pattern.quote("${OUTPUT_FOLDER}"),
				task.getMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER));
		command = command.replaceAll(Pattern.quote("${SANDBOX}"),
				task.getMetadata(TaskImpl.METADATA_SANDBOX));
		command = command.replaceAll(Pattern.quote("${SEBAL_URL}"),
				task.getMetadata(METADATA_SEBAL_URL));

		// repositories properties
		command = command.replaceAll(Pattern.quote("${REMOTE_USER}"),
				task.getMetadata(METADATA_REPOS_USER));
		command = command.replaceAll(Pattern.quote("${USER_PRIVATE_KEY}"),
				task.getMetadata(METADATA_REMOTE_REPOS_PRIVATE_KEY_PATH));
		command = command.replaceAll(Pattern.quote("${IMAGES_MOUNT_POINT}"),
				task.getMetadata(METADATA_IMAGES_MOUNT_POINT));
		command = command.replaceAll(Pattern.quote("${RESULTS_MOUNT_POINT}"),
				task.getMetadata(METADATA_RESULTS_MOUNT_POINT));
		command = command.replaceAll(Pattern.quote("${SEBAL_IMAGE_REPOSITORY}"),
				task.getMetadata(METADATA_IMAGE_REPOSITORY));
		command = command.replaceAll(Pattern.quote("${SEBAL_RESULT_REPOSITORY}"),
				task.getMetadata(METADATA_RESULT_REPOSITORY));

		// execution properties
		if (task.getMetadata(METADATA_ADDITIONAL_LIBRARY_PATH) != null) {
			command = command.replaceAll(Pattern.quote("${ADDITIONAL_LIBRARY_PATH}"),
					":" + task.getMetadata(METADATA_ADDITIONAL_LIBRARY_PATH));
		}

		command = command.replaceAll(Pattern.quote("${NUMBER_OF_PARTITIONS}"),
				task.getMetadata(METADATA_NUMBER_OF_PARTITIONS));
		command = command.replaceAll(Pattern.quote("${PARTITION_INDEX}"),
				task.getMetadata(METADATA_PARTITION_INDEX));
		command = command.replaceAll(Pattern.quote("${LEFT_X}"), task.getMetadata(METADATA_LEFT_X));
		command = command.replaceAll(Pattern.quote("${UPPER_Y}"),
				task.getMetadata(METADATA_UPPER_Y));
		command = command.replaceAll(Pattern.quote("${RIGHT_X}"),
				task.getMetadata(METADATA_RIGHT_X));
		command = command.replaceAll(Pattern.quote("${LOWER_Y}"),
				task.getMetadata(METADATA_LOWER_Y));

		if (task.getMetadata(METADATA_REMOTE_BOUNDINGBOX_PATH) != null) {
			command = command.replaceAll(Pattern.quote("${BOUNDING_BOX_PATH}"),
					task.getMetadata(METADATA_REMOTE_BOUNDINGBOX_PATH));
		} else {
			command = command.replaceAll(Pattern.quote("${BOUNDING_BOX_PATH}"), "");
		}
		LOGGER.debug("Command that will be executed: " + command);
		return command;
	}

//	public static Task createJoinF1ResultsTask(Properties props, String imageName) {
//		TaskImpl joinResultsTask = new TaskImpl();
//		
//		// setting metadata
//		joinResultsTask.setId(UUID.randomUUID().toString());
//				
//		joinResultsTask.putMetadata(METADATA_PHASE, JOIN_F1_RESULTS_PHASE);
//		
//		settingCommonTaskMetadata(props, joinResultsTask);
//		
//		// creating join-f1-results script for this image
//		File localScriptFile = createScriptFile(props, joinResultsTask);
//		String remoteScriptPath = joinResultsTask.getMetadata(TaskImpl.METADATA_SANDBOX) + "/"
//				+ localScriptFile.getName();
//
//		// adding commands
//		joinResultsTask.addCommand(new StageCommand(Command.Type.STAGE_IN,
//				localScriptFile.getAbsolutePath(), remoteScriptPath));
//
//		joinResultsTask.addCommand(getTaskExecuteCommand(joinResultsTask, remoteScriptPath));
//
//		String outputFileName = imageName + "_results_joined";
//		
//		joinResultsTask.addCommand(new StageCommand(Command.Type.STAGE_OUT, joinResultsTask
//				.getMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER) + "/" + outputFileName, joinResultsTask
//				.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER) + "/" + outputFileName));
//		
//		return joinResultsTask;
//	}
}
