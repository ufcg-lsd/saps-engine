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
		String logPath = "/nfs/" + imageFolder + File.separator + "output.log";
		String errorPath = "/nfs/" + imageFolder + File.separator + "error.log";

		// Remove folders
		String removeFolders = String.format("rm -rf %s %s %s", metadataPath, outputPath, outputPath);
		taskImpl.addCommand(new CommandRequestDTO(removeFolders, CommandRequestDTO.Type.REMOTE));

		// Create folders
		String createFolders = String.format("mkdir -p %s %s %s", metadataPath, preprocessingPath, outputPath);
		taskImpl.addCommand(new CommandRequestDTO(createFolders, CommandRequestDTO.Type.REMOTE));

		// Run command
		String runCommand = String.format("bash /home/ubuntu/bin/run.sh %s %s %s %s > %s 2> %s", inputPath, outputPath, preprocessingPath, metadataPath, logPath, errorPath);
		taskImpl.addCommand(new CommandRequestDTO(runCommand, CommandRequestDTO.Type.REMOTE));
	}
}
