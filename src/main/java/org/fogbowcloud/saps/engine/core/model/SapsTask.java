package org.fogbowcloud.saps.engine.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.model.Command;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;

public class SapsTask {
	
	private static final String INIT_TYPE = "init";
	private static final String RUN_TYPE = "run";
	private static final String SEBAL_INIT_SCRIPT_PATH = "sebal_worker_init_script_path";
	private static final String SEBAL_RUN_SCRIPT_PATH = "sebal_worker_run_script_path";
	
	public static final String R_SCRIPT_PHASE = "rscript";
	
	private static final String SEBAL_SANDBOX = "sebal_sandbox";
	private static final String SEBAL_RASTER_TMP_DIR = "sebal_raster_tmp_dir";
	private static final String SEBAL_MOUNT_POINT = "sebal_mount_point";
	private static final String SEBAL_TASK_TIMEOUT = "sebal_task_timeout";
	private static final String SEBAL_LOCAL_OUTPUT_DIR = "sebal_local_output_dir";
	private static final String SEBAL_IMAGES_LOCAL_PATH = "sebal_images_local_path";
	private static final String SEBAL_RESULTS_LOCAL_PATH = "sebal_results_local_path";
	private static final String SEBAL_LOCAL_SCRIPTS_DIR = "sebal_local_scripts_dir";

	private static final String SEBAL_REMOTE_USER = "sebal_remote_user";
	private static final String SEBAL_EXPORT_PATH = "sebal_export_path";
	private static final String MAX_RESOURCE_CONN_RETRIES = "max_resource_conn_retries";
	
	public static final String METADATA_IMAGE_NAME = "image_name";
	public static final String METADATA_IMAGE_NEW_COLLECTION_NAME = "image_new_collection_name";
	private static final String METADATA_NFS_SERVER_IP = "nfs_server_ip";
	private static final String METADATA_NFS_SERVER_PORT = "nfs_server_port";

	public static final String METADATA_TASK_START_EXECUTION_TIME = "task_execution_time";
	public static final String METADATA_MAX_TASK_EXECUTION_TIME = "max_task_execution_time";

	private static final String METADATA_SEBAL_VERSION = "sebal_url";
	private static final String METADATA_SEBAL_TAG = "sebal_version";

	private static final String METADATA_REPOS_USER = "repository_user";	

	private static final String METADATA_MOUNT_POINT = "mount_point";
	private static final String METADATA_VOLUME_EXPORT_PATH = "volume_export_path";
	private static final String METADATA_SEBAL_LOCAL_SCRIPTS_DIR = "local_scripts_dir";

	public static final String METADATA_PHASE = "phase";

	private static final Logger LOGGER = Logger.getLogger(SapsTask.class);
	private static final String METADATA_IMAGES_LOCAL_PATH = "images_local_path";
	public static final String METADATA_RESULTS_LOCAL_PATH = "results_local_path";
	
	public static TaskImpl createSebalTask(TaskImpl rTaskImpl,
			Properties properties, String imageName,
			String imageNewCollectionName, Specification spec,
			String federationMember, String nfsServerIP, String nfsServerPort,
			String sebalVersion, String sebalTag) {
		LOGGER.debug("Creating R task for image " + imageNewCollectionName);

		settingCommonTaskMetadata(properties, rTaskImpl);

		// setting image R execution properties
		rTaskImpl.putMetadata(METADATA_SEBAL_VERSION, sebalVersion);
		rTaskImpl.putMetadata(METADATA_SEBAL_TAG, sebalTag);
		rTaskImpl.putMetadata(METADATA_PHASE, R_SCRIPT_PHASE);
		rTaskImpl.putMetadata(METADATA_IMAGE_NAME, imageName);
		rTaskImpl.putMetadata(METADATA_IMAGE_NEW_COLLECTION_NAME, imageNewCollectionName);
		rTaskImpl.putMetadata(METADATA_VOLUME_EXPORT_PATH,
				properties.getProperty(SEBAL_EXPORT_PATH));
		rTaskImpl.putMetadata(METADATA_SEBAL_LOCAL_SCRIPTS_DIR,
				properties.getProperty(SEBAL_LOCAL_SCRIPTS_DIR));
		rTaskImpl.putMetadata(METADATA_MAX_TASK_EXECUTION_TIME,
				properties.getProperty(METADATA_MAX_TASK_EXECUTION_TIME));
		
		rTaskImpl.putMetadata(METADATA_MOUNT_POINT,
				properties.getProperty(SEBAL_MOUNT_POINT));
		
		rTaskImpl.putMetadata(METADATA_NFS_SERVER_IP, nfsServerIP);
		rTaskImpl.putMetadata(METADATA_NFS_SERVER_PORT, nfsServerPort);
		rTaskImpl.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH,
				rTaskImpl.getMetadata(TaskImpl.METADATA_SANDBOX) + "/exit_"
						+ rTaskImpl.getId());
		
		// cleaning environment
		String cleanEnvironment = "sudo rm -rf "
				+ properties.getProperty(SEBAL_SANDBOX);
		rTaskImpl.addCommand(new Command(cleanEnvironment, Command.Type.REMOTE));
		
		String cleanRaster = "sudo rm -rf "
				+ properties.getProperty(SEBAL_RASTER_TMP_DIR);
		rTaskImpl.addCommand(new Command(cleanRaster, Command.Type.REMOTE));

		// creating sandbox
		String mkdirCommand = "mkdir -p "
				+ rTaskImpl.getMetadata(TaskImpl.METADATA_SANDBOX);
		rTaskImpl.addCommand(new Command(mkdirCommand, Command.Type.REMOTE));

		// creating init and run R script for this image
		File localInitScriptFile = createScriptFile(properties, rTaskImpl, INIT_TYPE);
		String remoteInitScriptPath = rTaskImpl
				.getMetadata(TaskImpl.METADATA_SANDBOX)
				+ File.separator
				+ localInitScriptFile.getName();
				
		File localRunScriptFile = createScriptFile(properties, rTaskImpl, RUN_TYPE);
		String remoteRunScriptPath = rTaskImpl
				.getMetadata(TaskImpl.METADATA_SANDBOX)
				+ File.separator
				+ localRunScriptFile.getName();

		// adding commands
		String scpUploadCommand = createSCPUploadCommand(
				localInitScriptFile.getAbsolutePath(), remoteInitScriptPath);
		LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
		rTaskImpl.addCommand(new Command(scpUploadCommand,
				Command.Type.LOCAL));

		scpUploadCommand = createSCPUploadCommand(
				localRunScriptFile.getAbsolutePath(), remoteRunScriptPath);
		LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
		rTaskImpl.addCommand(new Command(scpUploadCommand,
				Command.Type.LOCAL));		

		// adding remote commands
		String remoteChmodInitScriptCommand = createChmodScriptCommand(remoteInitScriptPath);
		rTaskImpl.addCommand(new Command(remoteChmodInitScriptCommand,
				Command.Type.REMOTE));
		
		String remoteChmodRunScriptCommand = createChmodScriptCommand(remoteRunScriptPath);
		rTaskImpl.addCommand(new Command(remoteChmodRunScriptCommand,
				Command.Type.REMOTE));
		
		String remoteExecScriptCommand = createRemoteScriptExecCommand(remoteInitScriptPath, INIT_TYPE, rTaskImpl);
		LOGGER.debug("remoteExecCommand=" + remoteExecScriptCommand);
		rTaskImpl.addCommand(new Command(remoteExecScriptCommand,
				Command.Type.REMOTE));
		
		remoteExecScriptCommand = createRemoteScriptExecCommand(remoteRunScriptPath, RUN_TYPE, rTaskImpl);
		LOGGER.debug("remoteExecCommand=" + remoteExecScriptCommand);
		rTaskImpl.addCommand(new Command(remoteExecScriptCommand,
				Command.Type.REMOTE));

		return rTaskImpl;
	}

	private static String createSCPUploadCommand(String localFilePath, String remoteFilePath) {
		return "scp -i $PRIVATE_KEY_FILE -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $SSH_PORT "
				+ localFilePath + " $SSH_USER@$HOST:" + remoteFilePath;
	}

	private static void settingCommonTaskMetadata(Properties properties, Task task) {
		// task property
		task.putMetadata(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES, properties.getProperty(MAX_RESOURCE_CONN_RETRIES));
		
		// sdexs properties
		task.putMetadata(TaskImpl.METADATA_SANDBOX, properties.getProperty(SEBAL_SANDBOX) + "/" + task.getId());
		task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER,
				properties.getProperty(SEBAL_SANDBOX) + "/output");
		task.putMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER,
				properties.getProperty(SEBAL_LOCAL_OUTPUT_DIR));
		task.putMetadata(TaskImpl.METADATA_TASK_TIMEOUT, properties.getProperty(SEBAL_TASK_TIMEOUT));
		
		// repository properties
		task.putMetadata(METADATA_REPOS_USER, properties.getProperty(SEBAL_REMOTE_USER));
		task.putMetadata(METADATA_MOUNT_POINT,
				properties.getProperty(SEBAL_MOUNT_POINT));
		task.putMetadata(METADATA_IMAGES_LOCAL_PATH,
				properties.getProperty(SEBAL_IMAGES_LOCAL_PATH));
		task.putMetadata(METADATA_RESULTS_LOCAL_PATH,
				properties.getProperty(SEBAL_RESULTS_LOCAL_PATH));
	}
	
	private static String createChmodScriptCommand(String remoteScript) {
		return "\"chmod +x " + remoteScript + "\"";
	}

	private static String createRemoteScriptExecCommand(String remoteScript, String scriptType, TaskImpl taskImpl) {
		
		Path pathToRemoteScript = Paths.get(remoteScript);
		String execScriptCommand = null;
		if(scriptType.equals(INIT_TYPE)) {
			String initOutName = pathToRemoteScript.getFileName().toString() + "." + "out";
			String initErrName = pathToRemoteScript.getFileName().toString() + "." + "err";
			
			execScriptCommand = "\"nohup " + remoteScript + " >> "
					+ taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
					+ File.separator + initOutName + " 2>> "
					+ taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
					+ File.separator + initErrName + "\"";
		} else {
			String runOutName = pathToRemoteScript.getFileName().toString() + "." + "out";
			String runErrName = pathToRemoteScript.getFileName().toString() + "." + "err";

			execScriptCommand = "\"nohup " + remoteScript + " >> "
					+ taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
					+ File.separator + runOutName + " 2>> "
					+ taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
					+ File.separator + runErrName + "\"";
		}
		
		return execScriptCommand;
	}

	private static File createScriptFile(Properties props, TaskImpl task, String scriptType) {
		File tempFile = null;
		FileOutputStream fos = null;
		FileInputStream fis = null;
		try {			
			if(scriptType.equals(INIT_TYPE)) {
				tempFile = File.createTempFile("temp-worker-init-", ".sh");
				fis = new FileInputStream(props.getProperty(SEBAL_INIT_SCRIPT_PATH));
			} else {
				tempFile = File.createTempFile("temp-worker-run-", ".sh");
				fis = new FileInputStream(props.getProperty(SEBAL_RUN_SCRIPT_PATH));
			}
			
			String origExec = IOUtils.toString(fis);
			fos = new FileOutputStream(tempFile);
			IOUtils.write(replaceVariables(props, task, origExec, scriptType), fos);
		} catch (IOException e) {
			LOGGER.error("Error while creating script " + tempFile.getName() + " file", e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Throwable t) {
				LOGGER.error(t);
				// Do nothing, best effort
			}
		}
		return tempFile;
	}

	public static String replaceVariables(Properties props, TaskImpl task, String command, String scriptType) {
		
		if(scriptType.equals(INIT_TYPE)) {
			command = command.replaceAll(Pattern.quote("${PINPOINTED_SEBAL_TAG}"),
					task.getMetadata(METADATA_SEBAL_TAG));			
			command = command.replaceAll(Pattern.quote("${NFS_SERVER_IP}"),
					task.getMetadata(METADATA_NFS_SERVER_IP));
			command = command.replaceAll(Pattern.quote("${NFS_SERVER_PORT}"),
					task.getMetadata(METADATA_NFS_SERVER_PORT));
			command = command.replaceAll(Pattern.quote("${VOLUME_EXPORT_PATH}"),
					task.getMetadata(METADATA_VOLUME_EXPORT_PATH));
		}
		
		// common variables for both scripts
		command = command.replaceAll(Pattern.quote("${IMAGE_NEW_COLLECTION_NAME}"),
				task.getMetadata(METADATA_IMAGE_NEW_COLLECTION_NAME));
		command = command.replaceAll(Pattern.quote("${SEBAL_URL}"),
				task.getMetadata(METADATA_SEBAL_VERSION));
		command = command.replaceAll(Pattern.quote("${SANDBOX}"),
				task.getMetadata(TaskImpl.METADATA_SANDBOX));
		command = command.replaceAll(Pattern.quote("${SEBAL_MOUNT_POINT}"),
				task.getMetadata(METADATA_MOUNT_POINT));
		command = command.replaceAll(Pattern.quote("${REMOTE_COMMAND_EXIT_PATH}"),
				task.getMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH));

		LOGGER.debug("Command that will be executed: " + command);
		return command;
	}
}
