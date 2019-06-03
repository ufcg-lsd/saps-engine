package org.fogbowcloud.saps.engine.core.model;

import java.io.File;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dto.CommandRequestDTO;
import org.fogbowcloud.saps.engine.core.task.TaskImpl;

public class SapsTask {

	public static final String METADATA_TASK_ID = "task_id";
	public static final String METADATA_EXPORT_PATH = "volume_export_path";
	protected static final String METADATA_REPOS_USER = "repository_user";
	protected static final String METADATA_NFS_SERVER_IP = "nfs_server_ip";
	protected static final String METADATA_NFS_SERVER_PORT = "nfs_server_port";
	protected static final String METADATA_MOUNT_POINT = "mount_point";
	protected static final String METADATA_WORKER_CONTAINER_REPOSITORY = "worker_container_repository";
	protected static final String METADATA_WORKER_CONTAINER_TAG = "worker_container_tag";
	protected static final String METADATA_MAX_TASK_EXECUTION_TIME = "max_task_execution_time";

	protected static final String WORKER_SANDBOX = "worker_sandbox";
	protected static final String WORKER_REMOTE_USER = "worker_remote_user";
	protected static final String WORKER_MOUNT_POINT = "worker_mount_point";
	protected static final String WORKER_EXPORT_PATH = "saps_export_path";
	protected static final String WORKER_TASK_TIMEOUT = "worker_task_timeout";
	protected static final String SAPS_WORKER_RUN_SCRIPT_PATH = "saps_worker_run_script_path";
	protected static final String MAX_RESOURCE_CONN_RETRIES = "max_resource_conn_retries";

	public static final String METADATA_WORKER_OPERATING_SYSTEM = "worker_operating_system";
	public static final String METADATA_WORKER_KERNEL_VERSION = "worker_kernel_version";

	private static final Logger LOGGER = Logger.getLogger(SapsTask.class);

	public static void createTask(TaskImpl taskImpl, ImageTask imageTask){
		// info shared folder beetweeen host (with NFS) and container
		// ...

		String imageFolder = imageTask.getTaskId();

		String inputPath = "/nfs/" + imageFolder + File.separator + "data" + File.separator + "input";
		String preprocessingPath = "/nfs/" + imageFolder + File.separator + "data" + File.separator + "preprocessing";;
		String metadataPath = "/nfs/" + imageFolder + File.separator + "metadata";
		String outputPath = "/nfs/" + imageFolder + File.separator + "data" + File.separator + "output";
		String logPath = "/nfs/" + imageFolder + File.separator + "/output.log";

		// Remove folders
		String removeFolders = String.format("rm -rf %s %s %s", metadataPath, outputPath, outputPath);
		taskImpl.addCommand(new CommandRequestDTO(removeFolders, CommandRequestDTO.Type.REMOTE));

		// Create folders
		String createFolders = String.format("mkdir -p %s %s %s", metadataPath, preprocessingPath, outputPath);
		taskImpl.addCommand(new CommandRequestDTO(createFolders, CommandRequestDTO.Type.REMOTE));

		// Run command
		String runCommand = String.format("bash /home/ubuntu/run.sh %s %s %s %s &> %s", inputPath, outputPath, preprocessingPath, metadataPath, logPath);
		taskImpl.addCommand(new CommandRequestDTO(runCommand, CommandRequestDTO.Type.REMOTE));
	}

	/*public static TaskImpl createSapsTask(TaskImpl taskImpl, Properties properties, String federationMember,
			String nfsServerIP, String nfsServerPort, String workerContainerRepository, String workerContainerTag) {
		LOGGER.debug("Creating Saps task " + taskImpl.getId() + " for Blowout");

		settingCommonTaskMetadata(properties, taskImpl);

		// setting image R execution properties
		taskImpl.putMetadata(METADATA_TASK_ID, taskImpl.getId());
		taskImpl.putMetadata(METADATA_WORKER_CONTAINER_REPOSITORY, workerContainerRepository);
		taskImpl.putMetadata(METADATA_WORKER_CONTAINER_TAG, workerContainerTag);
		taskImpl.putMetadata(METADATA_EXPORT_PATH, properties.getProperty(WORKER_EXPORT_PATH));
		taskImpl.putMetadata(METADATA_MAX_TASK_EXECUTION_TIME,
				properties.getProperty(METADATA_MAX_TASK_EXECUTION_TIME));

		taskImpl.putMetadata(METADATA_MOUNT_POINT, properties.getProperty(WORKER_MOUNT_POINT));

		taskImpl.putMetadata(METADATA_NFS_SERVER_IP, nfsServerIP);
		taskImpl.putMetadata(METADATA_NFS_SERVER_PORT, nfsServerPort);
		taskImpl.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH,
				taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX) + "/exit_" + taskImpl.getId());

		taskImpl.putMetadata(METADATA_WORKER_OPERATING_SYSTEM,
				properties.getProperty(SapsPropertiesConstants.WORKER_OPERATING_SYSTEM));
		taskImpl.putMetadata(METADATA_WORKER_KERNEL_VERSION,
				properties.getProperty(SapsPropertiesConstants.WORKER_KERNEL_VERSION));

		// cleaning environment
		String cleanEnvironment = "sudo rm -rf " + properties.getProperty(WORKER_SANDBOX);
		taskImpl.addCommand(new CommandRequestDTO(cleanEnvironment, CommandRequestDTO.Type.REMOTE));

		// creating sandbox
		String mkdirCommand = "mkdir -p " + taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX);
		taskImpl.addCommand(new CommandRequestDTO(mkdirCommand, CommandRequestDTO.Type.REMOTE));

		// creating run worker script for this task
		File localRunScriptFile = createScriptFile(properties, taskImpl);
		String remoteRunScriptPath = taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX) + File.separator
				+ localRunScriptFile.getName();

		// adding commands
		String scpUploadCommand = createSCPUploadCommand(localRunScriptFile.getAbsolutePath(), remoteRunScriptPath);
		LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
		taskImpl.addCommand(new CommandRequestDTO(scpUploadCommand, CommandRequestDTO.Type.LOCAL));

		// adding remote commands
		String remoteChmodRunScriptCommand = createChmodScriptCommand(remoteRunScriptPath);
		taskImpl.addCommand(new CommandRequestDTO(remoteChmodRunScriptCommand, CommandRequestDTO.Type.REMOTE));

		String remoteExecScriptCommand = createRemoteScriptExecCommand(remoteRunScriptPath, taskImpl);
		LOGGER.debug("remoteExecCommand=" + remoteExecScriptCommand);
		taskImpl.addCommand(new CommandRequestDTO(remoteExecScriptCommand, CommandRequestDTO.Type.REMOTE));

		return taskImpl;
	}

	protected static String createSCPUploadCommand(String localFilePath, String remoteFilePath) {
		return "scp -i $PRIVATE_KEY_FILE -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $SSH_PORT "
				+ localFilePath + " $SSH_USER@$HOST:" + remoteFilePath;
	}

	private static void settingCommonTaskMetadata(Properties properties, Task task) {
		// task property
		task.putMetadata(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES,
				properties.getProperty(MAX_RESOURCE_CONN_RETRIES));

		// sdexs properties
		task.putMetadata(TaskImpl.METADATA_SANDBOX, properties.getProperty(WORKER_SANDBOX) + "/" + task.getId());
		task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER, properties.getProperty(WORKER_SANDBOX) + "/output");
		task.putMetadata(TaskImpl.METADATA_TASK_TIMEOUT, properties.getProperty(WORKER_TASK_TIMEOUT));

		// repository properties
		task.putMetadata(METADATA_REPOS_USER, properties.getProperty(WORKER_REMOTE_USER));
		task.putMetadata(METADATA_MOUNT_POINT, properties.getProperty(WORKER_MOUNT_POINT));
	}

	private static String createChmodScriptCommand(String remoteScript) {
		return "\"chmod +x " + remoteScript + "\"";
	}

	private static String createRemoteScriptExecCommand(String remoteScript, TaskImpl taskImpl) {

		Path pathToRemoteScript = Paths.get(remoteScript);
		String execScriptCommand = null;
		String runOutName = pathToRemoteScript.getFileName().toString() + "." + "out";
		String runErrName = pathToRemoteScript.getFileName().toString() + "." + "err";

		execScriptCommand = "\"nohup " + remoteScript + " >> " + taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
				+ File.separator + runOutName + " 2>> " + taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
				+ File.separator + runErrName + "\"";

		return execScriptCommand;
	}

	protected static File createScriptFile(Properties props, TaskImpl task) {
		File tempFile = null;
		FileOutputStream fos = null;
		FileInputStream fis = null;
		try {
			tempFile = File.createTempFile("temp-worker-run-", ".sh");
			fis = new FileInputStream(props.getProperty(SAPS_WORKER_RUN_SCRIPT_PATH));

			String origExec = IOUtils.toString(fis);
			fos = new FileOutputStream(tempFile);
			IOUtils.write(replaceVariables(props, task, origExec), fos);
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

	public static String replaceVariables(Properties props, TaskImpl task, String command) {
		command = command.replaceAll(Pattern.quote("${TASK_ID}"), task.getMetadata(METADATA_TASK_ID));
		command = command.replaceAll(Pattern.quote("${SANDBOX}"), task.getMetadata(TaskImpl.METADATA_SANDBOX));
		command = command.replaceAll(Pattern.quote("${EXPORT_PATH}"), task.getMetadata(METADATA_EXPORT_PATH));
		command = command.replaceAll(Pattern.quote("${SAPS_MOUNT_POINT}"), task.getMetadata(METADATA_MOUNT_POINT));
		command = command.replaceAll(Pattern.quote("${NFS_SERVER_IP}"), task.getMetadata(METADATA_NFS_SERVER_IP));
		command = command.replaceAll(Pattern.quote("${NFS_SERVER_PORT}"), task.getMetadata(METADATA_NFS_SERVER_PORT));
		command = command.replaceAll(Pattern.quote("${WORKER_CONTAINER_REPOSITORY}"),
				task.getMetadata(METADATA_WORKER_CONTAINER_REPOSITORY));
		command = command.replaceAll(Pattern.quote("${WORKER_CONTAINER_TAG}"),
				task.getMetadata(METADATA_WORKER_CONTAINER_TAG));
		command = command.replaceAll(Pattern.quote("${REMOTE_COMMAND_EXIT_PATH}"),
				task.getMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH));

		LOGGER.debug("CommandRequestDTO that will be executed: " + command);
		return command;
	}*/
}
