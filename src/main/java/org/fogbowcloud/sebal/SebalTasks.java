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
import org.fogbowcloud.sebal.bootstrap.DBBootstrap;


public class SebalTasks {

	public static final String F1_PHASE = "f1";
	public static final String C_PHASE = "c";
	public static final String F2_PHASE = "f2";
	public static final String R_SCRIPT_PHASE = "rscript";
	
	public static final String METADATA_PHASE = "phase";
	public static final String METADATA_IMAGE_NAME = "image_name";
	public static final String METADATA_NUMBER_OF_PARTITIONS = "number_of_partitions";
	public static final String METADATA_PARTITION_INDEX = "partition_index";
	private static final String METADATA_SEBAL_LOCAL_SCRIPTS_DIR = "local_scripts_dir";
	private static final String METADATA_ADDITIONAL_LIBRARY_PATH = "additonal_library_path";
	private static final String METADATA_NFS_SERVER_IP = "nfs_server_ip";
	private static final String METADATA_NFS_SERVER_PORT = "nfs_server_port";
	private static final String METADATA_VOLUME_EXPORT_PATH = "volume_export_path";

	private static final Logger LOGGER = Logger.getLogger(SebalTasks.class);
	public static final String METADATA_LEFT_X = "left_x";
	public static final String METADATA_UPPER_Y = "upper_y";
	public static final String METADATA_RIGHT_X = "right_x";
	public static final String METADATA_LOWER_Y = "lower_y";
	private static final String METADATA_REMOTE_BOUNDINGBOX_PATH = "remote_boundingbox_path";
	private static final String METADATA_IMAGES_LOCAL_PATH = "images_local_path";
	public static final String METADATA_RESULTS_LOCAL_PATH = "results_local_path";
	private static final String METADATA_SEBAL_URL = "sebal_url";
	private static final String METADATA_R_URL = "r_url";
	private static final String METADATA_REPOS_USER = "repository_user";
	private static final String METADATA_MOUNT_POINT = "mount_point";
	private static final String METADATA_REMOTE_REPOS_PRIVATE_KEY_PATH = "remote_repos_private_key_path";

	public static List<Task> createF1Tasks(Properties properties, String imageName, Specification spec, String location) {
		LOGGER.debug("Creating F1 tasks for image " + imageName);

		String numberOfPartitions = properties.getProperty("sebal_number_of_partitions");
		List<Task> f1Tasks = new ArrayList<Task>();

		for (int partitionIndex = 1; partitionIndex <= Integer.parseInt(numberOfPartitions); partitionIndex++) {
			//setting location on spec
//			String locationSpec = "Glue2CloudComputeManagerID==\"" + location + "\"";
//			if (spec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS) == null) {
//				spec.addRequitement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS,
//						locationSpec);
//			} else {
//				spec.addRequitement(
//						FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS,
//						spec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS)
//								+ " && " + locationSpec);
//			}

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
			f1Task.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH,
					f1Task.getMetadata(TaskImpl.METADATA_SANDBOX) + "/exit_" + f1Task.getId());
			
			// creating sandbox
			String mkdirCommand = "mkdir -p " + f1Task.getMetadata(TaskImpl.METADATA_SANDBOX);
			String mkdirRemotly = createCommandToRunRemotly(mkdirCommand);
			f1Task.addCommand(new Command(mkdirRemotly, Command.Type.PROLOGUE));
			
			//treating repository user private key
			if (properties.getProperty("sebal_repository_user_private_key") != null) {
				File privateKeyFile = new File(properties.getProperty("sebal_repository_user_private_key"));
				String remotePrivateKeyPath = f1Task.getMetadata(TaskImpl.METADATA_SANDBOX) + "/"
						+ privateKeyFile.getName();
				
				f1Task.putMetadata(METADATA_REMOTE_REPOS_PRIVATE_KEY_PATH, remotePrivateKeyPath);
				String scpUploadCommand = createSCPUploadCommand(privateKeyFile.getAbsolutePath(),
						remotePrivateKeyPath);
				LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
				f1Task.addCommand(new Command(scpUploadCommand, Command.Type.PROLOGUE));
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
					String scpUploadCommand = createSCPUploadCommand(
							boundingboxFile.getAbsolutePath(), remoteBoundingboxPath);
					LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
					f1Task.addCommand(new Command(scpUploadCommand, Command.Type.PROLOGUE));
				} else {
					LOGGER.warn("There is no boundingbox file specified for image " + imageName);
				}
			}
			
			// creating f1 script for this image
			File localScriptFile = createScriptFile(properties, f1Task);
			String remoteScriptPath = f1Task.getMetadata(TaskImpl.METADATA_SANDBOX) + "/"
					+ localScriptFile.getName();
			
			// adding command
			String scpUploadCommand = createSCPUploadCommand(
					localScriptFile.getAbsolutePath(), remoteScriptPath);
			LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
			f1Task.addCommand(new Command(scpUploadCommand, Command.Type.PROLOGUE));
			
			// adding remote command
			String remoteExecScriptCommand = createRemoteScriptExecCommand(remoteScriptPath);
			LOGGER.debug("remoteExecCommand=" + remoteExecScriptCommand);
			f1Task.addCommand(new Command(remoteExecScriptCommand , Command.Type.REMOTE));

			// adding epilogue command
			
			String copyCommand = "cp -R " + f1Task.getMetadata(TaskImpl.METADATA_SANDBOX)
					+ "/SEBAL/local_results/" + f1Task.getMetadata(METADATA_IMAGE_NAME) + " "
					+ f1Task.getMetadata(METADATA_RESULTS_LOCAL_PATH) + "/results";
			String remoteCopyCommand = createCommandToRunRemotly(copyCommand);
			f1Task.addCommand(new Command(remoteCopyCommand, Command.Type.EPILOGUE));
			
			String cleanEnvironment = "rm -r " + f1Task.getMetadata(TaskImpl.METADATA_SANDBOX);
			String remoteCleanEnv = createCommandToRunRemotly(cleanEnvironment);
			f1Task.addCommand(new Command(remoteCleanEnv, Command.Type.EPILOGUE));

			// stage out of output files
			String remoteOutFilePath = f1Task.getMetadata(METADATA_NUMBER_OF_PARTITIONS) + "_"
					+ f1Task.getMetadata(METADATA_PARTITION_INDEX) + "_out";
			
			String scpDownloadCommand = createSCPDownloadCommand(
					f1Task.getMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER) + "/"
							+ remoteOutFilePath,
					f1Task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER) + "/"
							+ f1Task.getMetadata(METADATA_IMAGE_NAME) + "_" + remoteOutFilePath);
			f1Task.addCommand(new Command(scpDownloadCommand, Command.Type.EPILOGUE));

			String remoteErrFilePath = f1Task.getMetadata(METADATA_NUMBER_OF_PARTITIONS) + "_"
					+ f1Task.getMetadata(METADATA_PARTITION_INDEX) + "_err";
			scpDownloadCommand = createSCPDownloadCommand(
					f1Task.getMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER) + "/"
							+ remoteErrFilePath,
					f1Task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER) + "/"
							+ f1Task.getMetadata(METADATA_IMAGE_NAME) + "_" + remoteErrFilePath);
			f1Task.addCommand(new Command(scpDownloadCommand, Command.Type.EPILOGUE));

//			// stage out render out 
//			String remoteRenderOutFilePath = f1Task.getMetadata(METADATA_NUMBER_OF_PARTITIONS) + "_"
//					+ f1Task.getMetadata(METADATA_PARTITION_INDEX) + "_render_out";
//			
//			scpDownloadCommand = createSCPDownloadCommand(
//					f1Task.getMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER) + "/"
//							+ remoteRenderOutFilePath,
//					f1Task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER) + "/"
//							+ f1Task.getMetadata(METADATA_IMAGE_NAME) + "_" + remoteRenderOutFilePath);
//			f1Task.addCommand(new Command(scpDownloadCommand, Command.Type.EPILOGUE));
//
//			String remoteRenderErrFilePath = f1Task.getMetadata(METADATA_NUMBER_OF_PARTITIONS) + "_"
//					+ f1Task.getMetadata(METADATA_PARTITION_INDEX) + "_render_err";
//			scpDownloadCommand = createSCPDownloadCommand(
//					f1Task.getMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER) + "/"
//							+ remoteRenderErrFilePath,
//					f1Task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER) + "/"
//							+ f1Task.getMetadata(METADATA_IMAGE_NAME) + "_" + remoteRenderErrFilePath);
//			f1Task.addCommand(new Command(scpDownloadCommand, Command.Type.EPILOGUE));
			
			f1Tasks.add(f1Task);
		}
		return f1Tasks;
	}
	
	public static TaskImpl createRTask(TaskImpl rTaskImpl,
			Properties properties, String imageName, Specification spec,
			String location, String nfsServerIP, String nfsServerPort) {
		LOGGER.debug("Creating R task for image " + imageName);

		settingCommonTaskMetadata(properties, rTaskImpl);

		// setting image R execution properties
		rTaskImpl.putMetadata(METADATA_PHASE, R_SCRIPT_PHASE);
		rTaskImpl.putMetadata(METADATA_R_URL, properties.getProperty("r_url"));
		rTaskImpl.putMetadata(METADATA_IMAGE_NAME, imageName);
		rTaskImpl.putMetadata(METADATA_VOLUME_EXPORT_PATH,
				properties.getProperty("sebal_export_path"));
		rTaskImpl.putMetadata(METADATA_SEBAL_LOCAL_SCRIPTS_DIR,
				properties.getProperty("sebal_local_scripts_dir"));
		rTaskImpl.putMetadata(METADATA_MOUNT_POINT,
				properties.getProperty("sebal_mount_point"));
		rTaskImpl.putMetadata(METADATA_NFS_SERVER_IP, nfsServerIP);
		rTaskImpl.putMetadata(METADATA_NFS_SERVER_PORT, nfsServerPort);
		rTaskImpl.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH,
				rTaskImpl.getMetadata(TaskImpl.METADATA_SANDBOX) + "/exit_"
						+ rTaskImpl.getId());

		// creating sandbox
		String mkdirCommand = "mkdir -p "
				+ rTaskImpl.getMetadata(TaskImpl.METADATA_SANDBOX);
		String mkdirRemotly = createCommandToRunRemotly(mkdirCommand);
		rTaskImpl.addCommand(new Command(mkdirRemotly, Command.Type.PROLOGUE));

		// treating repository user private key
		if (properties.getProperty("sebal_repository_user_private_key") != null) {
			File privateKeyFile = new File(
					properties.getProperty("sebal_repository_user_private_key"));
			String remotePrivateKeyPath = rTaskImpl
					.getMetadata(TaskImpl.METADATA_SANDBOX)
					+ "/"
					+ privateKeyFile.getName();

			rTaskImpl.putMetadata(METADATA_REMOTE_REPOS_PRIVATE_KEY_PATH,
					remotePrivateKeyPath);
			String scpUploadCommand = createSCPUploadCommand(
					privateKeyFile.getAbsolutePath(), remotePrivateKeyPath);
			LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
			rTaskImpl.addCommand(new Command(scpUploadCommand,
					Command.Type.PROLOGUE));
		}

		// TODO: check if the following have to change to support new script

		// creating r script for this image
		File localScriptFile = createScriptFile(properties, rTaskImpl);
		String remoteScriptPath = rTaskImpl
				.getMetadata(TaskImpl.METADATA_SANDBOX)
				+ "/"
				+ localScriptFile.getName();

		// adding command
		String scpUploadCommand = createSCPUploadCommand(
				localScriptFile.getAbsolutePath(), remoteScriptPath);
		LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
		rTaskImpl.addCommand(new Command(scpUploadCommand,
				Command.Type.PROLOGUE));

		// adding remote command
		String remoteExecScriptCommand = createRemoteScriptExecCommand(remoteScriptPath);
		LOGGER.debug("remoteExecCommand=" + remoteExecScriptCommand);
		rTaskImpl.addCommand(new Command(remoteExecScriptCommand,
				Command.Type.REMOTE));
		
		// adding epilogue command
		
		// the following probably wont be used in current implementation
/*		String copyCommand = "cp -R "
				+ rTaskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
				+ "/SEBAL/local_results/"
				+ rTaskImpl.getMetadata(METADATA_IMAGE_NAME) + " "
				+ rTaskImpl.getMetadata(METADATA_RESULTS_LOCAL_PATH)
				+ "/results";
		String remoteCopyCommand = createCommandToRunRemotly(copyCommand);
		rTaskImpl.addCommand(new Command(remoteCopyCommand,
				Command.Type.EPILOGUE));*/
		
		// TODO: see if this will be used
		/*String getChecukSumNameCommand = createCommandToRunRemotly("cat " + imageName + "_checksum.md5");
		rTaskImpl.addCommand(new Command(getChecukSumNameCommand, Command.Type.EPILOGUE));*/
		
		String cleanEnvironment = "rm -r "
				+ rTaskImpl.getMetadata(TaskImpl.METADATA_SANDBOX);
		String remoteCleanEnv = createCommandToRunRemotly(cleanEnvironment);
		rTaskImpl.addCommand(new Command(remoteCleanEnv, Command.Type.EPILOGUE));

		String scpDownloadCommand = createSCPDownloadCommand(
				METADATA_RESULTS_LOCAL_PATH + "/"
						+ rTaskImpl.getMetadata(METADATA_IMAGE_NAME),
				METADATA_VOLUME_EXPORT_PATH + "/results/"
						+ rTaskImpl.getMetadata(METADATA_IMAGE_NAME) + "/"
						+ rTaskImpl.getMetadata(METADATA_IMAGE_NAME) + "_out");
		rTaskImpl.addCommand(new Command(scpDownloadCommand,
				Command.Type.EPILOGUE));

		scpDownloadCommand = createSCPDownloadCommand(
				METADATA_RESULTS_LOCAL_PATH + "/"
						+ rTaskImpl.getMetadata(METADATA_IMAGE_NAME),
				METADATA_VOLUME_EXPORT_PATH + "/results/"
						+ rTaskImpl.getMetadata(METADATA_IMAGE_NAME) + "/"
						+ rTaskImpl.getMetadata(METADATA_IMAGE_NAME) + "_err");
		rTaskImpl.addCommand(new Command(scpDownloadCommand,
				Command.Type.EPILOGUE));

		return rTaskImpl;
	}

	private static String createCommandToRunRemotly(String command) {		
		return "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $PRIVATE_KEY_FILE $SSH_USER@$HOST -p $SSH_PORT " + command;
	}
	
	private static String createSCPDownloadCommand(String remoteFilePath, String localFilePath) {
		return "scp -i $PRIVATE_KEY_FILE -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $SSH_PORT $SSH_USER@$HOST:"
				+ remoteFilePath + " " + localFilePath;
	}

	private static String createSCPUploadCommand(String localFilePath, String remoteFilePath) {
		return "scp -i $PRIVATE_KEY_FILE -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $SSH_PORT "
				+ localFilePath + " $SSH_USER@$HOST:" + remoteFilePath;
	}

	private static void settingCommonTaskMetadata(Properties properties, Task task) {
		// task property
		task.putMetadata(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES, properties.getProperty("max_resource_conn_retries"));
		
		// sdexs properties
		task.putMetadata(TaskImpl.METADATA_SANDBOX, properties.getProperty("sebal_sandbox") + "/" + task.getId());
		task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER,
				properties.getProperty("sebal_sandbox") + "/output");
		task.putMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER,
				properties.getProperty("sebal_local_output_dir"));
		task.putMetadata(TaskImpl.METADATA_TASK_TIMEOUT, properties.getProperty("sebal_task_timeout"));
		
		// repository properties
		task.putMetadata(METADATA_REPOS_USER, properties.getProperty("sebal_remote_user"));
		task.putMetadata(METADATA_MOUNT_POINT,
				properties.getProperty("sebal_mount_point"));
		task.putMetadata(METADATA_IMAGES_LOCAL_PATH,
				properties.getProperty("sebal_images_local_path"));
		task.putMetadata(METADATA_RESULTS_LOCAL_PATH,
				properties.getProperty("sebal_results_local_path"));
	}

	private static String createRemoteScriptExecCommand(String remoteScript) {
		String execScriptCommand = "\"chmod +x " + remoteScript + "; nohup " + remoteScript
				+ " > /dev/null 2>&1 &\"";
		return execScriptCommand;
	}

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
		command = command.replaceAll(Pattern.quote("${R_URL}"),
				task.getMetadata(METADATA_R_URL));

		// repositories properties
		command = command.replaceAll(Pattern.quote("${NFS_SERVER_IP}"),
				task.getMetadata(METADATA_NFS_SERVER_IP));
		command = command.replaceAll(Pattern.quote("${NFS_SERVER_PORT}"),
				task.getMetadata(METADATA_NFS_SERVER_PORT));
		command = command.replaceAll(Pattern.quote("${VOLUME_EXPORT_PATH}"),
				task.getMetadata(METADATA_VOLUME_EXPORT_PATH));
		command = command.replaceAll(Pattern.quote("${REMOTE_USER}"),
				task.getMetadata(METADATA_REPOS_USER));
		command = command.replaceAll(Pattern.quote("${USER_PRIVATE_KEY}"),
				task.getMetadata(METADATA_REMOTE_REPOS_PRIVATE_KEY_PATH));
		command = command.replaceAll(Pattern.quote("${IMAGES_LOCAL_PATH}"),
				task.getMetadata(METADATA_IMAGES_LOCAL_PATH));
		command = command.replaceAll(Pattern.quote("${RESULTS_LOCAL_PATH}"),
				task.getMetadata(METADATA_RESULTS_LOCAL_PATH));
		command = command.replaceAll(Pattern.quote("${SEBAL_MOUNT_POINT}"),
				task.getMetadata(METADATA_MOUNT_POINT));

		// execution properties
		if (task.getMetadata(METADATA_ADDITIONAL_LIBRARY_PATH) != null) {
			command = command.replaceAll(Pattern.quote("${ADDITIONAL_LIBRARY_PATH}"),
					":" + task.getMetadata(METADATA_ADDITIONAL_LIBRARY_PATH));
		} else {
			command = command.replaceAll(Pattern.quote("${ADDITIONAL_LIBRARY_PATH}"),
					"");
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

		command = command.replaceAll(Pattern.quote("${REMOTE_COMMAND_EXIT_PATH}"),
				task.getMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH));
		
		if (task.getMetadata(METADATA_REMOTE_BOUNDINGBOX_PATH) != null) {
			command = command.replaceAll(Pattern.quote("${BOUNDING_BOX_PATH}"),
					task.getMetadata(METADATA_REMOTE_BOUNDINGBOX_PATH));
		} else {
			command = command.replaceAll(Pattern.quote("${BOUNDING_BOX_PATH}"), "");
		}
		LOGGER.debug("Command that will be executed: " + command);
		return command;
	}

	public static List<Task> createCTasks(Properties properties, String name,
			Specification sebalSpec) {
		// TODO Auto-generated method stub
		return null;
	}

	public static List<Task> createF2Tasks(Properties properties, String name,
			Specification sebalSpec) {
		// TODO Auto-generated method stub
		return null;
	}
}
