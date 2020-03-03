package org.fogbowcloud.saps.engine.utils;

public class SapsPropertiesConstants {

	// Image database constants
	public static final String IMAGE_DATASTORE_IP = "datastore_ip";
	public static final String IMAGE_DATASTORE_PORT = "datastore_port";

	// Submission constants
	public static final String DATASET_LT5_TYPE = "landsat_5";
	public static final String DATASET_LE7_TYPE = "landsat_7";
	public static final String DATASET_LC8_TYPE = "landsat_8";

	// Swift constants
	public static final String SWIFT_CONTAINER_NAME = "swift_container_name";
	public static final String PERMANENT_STORAGE_TASKS_DIR = "permanent_storage_tasks_dir";
	public static final String PERMANENT_STORAGE_DEBUG_TASKS_DIR = "permanent_storage_debug_tasks_dir";
	public static final String SWIFT_USER_ID = "swift_user_id";
	public static final String SWIFT_PASSWORD = "swift_password";
	public static final String SWIFT_PROJECT_ID = "swift_project_id";
	public static final String SWIFT_AUTH_URL = "swift_auth_url";
	public static final String SWIFT_OBJECT_STORE_HOST = "swift_object_store_host";
	public static final String SWIFT_OBJECT_STORE_PATH = "swift_object_store_path";
	public static final String SWIFT_OBJECT_STORE_CONTAINER = "swift_object_store_container";
	public static final String SWIFT_OBJECT_STORE_KEY = "swift_object_store_key";

	// Restlet constants
	public static final String SUBMISSION_REST_SERVER_PORT = "submission_rest_server_port";

	// Specification constants
	public static final String IMAGE_WORKER = "image_worker";

	// KeystoneV3 constants
	public static final String FOGBOW_KEYSTONEV3_UPDATE_PERIOD = "fogbow.keystonev3.swift.token.update.period";
	public static final String FOGBOW_KEYSTONEV3_PROJECT_ID = "fogbow.keystonev3.project.id";
	public static final String FOGBOW_KEYSTONEV3_USER_ID = "fogbow.keystonev3.user.id";
	public static final String FOGBOW_KEYSTONEV3_PASSWORD = "fogbow.keystonev3.password";
	public static final String FOGBOW_KEYSTONEV3_AUTH_URL = "fogbow.keystonev3.auth.url";
	public static final String FOGBOW_KEYSTONEV3_SWIFT_URL = "fogbow.keystonev3.swift.url";

	// Monitors constants
	public static final String SAPS_EXECUTION_PERIOD_SUBMISSOR = "saps_execution_period_submissor";
	public static final String SAPS_EXECUTION_PERIOD_CHECKER = "saps_execution_period_checker";
	public static final String SAPS_EXECUTION_PERIOD_ARCHIVER = "saps_execution_period_archiver";
	public static final String SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR = "saps_execution_period_garbage_collector";

	// Execution mode constants
	public static final String SAPS_DEBUG_MODE = "saps_debug_mode";
	public static final String SAPS_PERMANENT_STORAGE_TYPE = "saps_permanent_storage_type";

	// Properties file constants
	public static final String SAPS_TEMP_STORAGE_PATH = "saps_temp_storage_path";
	public static final String FOGBOW_CLI_PATH = "fogbow_cli_path";

	public static final String NO_REPLY_EMAIL = "noreply_email";
	public static final String NO_REPLY_PASS = "noreply_password";

	/*
	 *  Arrebol batch jobs execution system configs
	 */
	public static final String ARREBOL_BASE_URL = "arrebol_base_url";

	public static final String NFS_PERMANENT_STORAGE_PATH = "nfs_permanent_storage_path";
}
