package org.fogbowcloud.saps.engine.utils;

public class SapsPropertiesConstants {

	// Image database constants
	public static final String IMAGE_DATASTORE_IP = "datastore_ip";
	public static final String IMAGE_DATASTORE_PORT = "datastore_port";

	// Submission constants
	public static final String DATASET_LT5_TYPE = "landsat_5";
	public static final String DATASET_LE7_TYPE = "landsat_7";
	public static final String DATASET_LC8_TYPE = "landsat_8";


	public final class Openstack {

        public static final String PROJECT_ID = "openstack_project_id";
        public static final String USER_ID = "openstack_user_id";
        public static final String USER_PASSWORD = "openstack_user_password";

	    public final class IdentityService {
            public static final String API_URL = "openstack_identity_service_api_url";
        }

	    public final class ObjectStoreService {
            public static final String API_URL = "openstack_object_store_service_api_url";
            public static final String CONTAINER_NAME = "openstack_object_store_service_container_name";
            public static final String KEY = "openstack_object_store_service_key";
        }
    }

	// Swift constants
	public static final String PERMANENT_STORAGE_TASKS_DIR = "permanent_storage_tasks_dir";
	public static final String PERMANENT_STORAGE_DEBUG_TASKS_DIR = "permanent_storage_debug_tasks_dir";

	// Restlet constants
	public static final String SUBMISSION_REST_SERVER_PORT = "submission_rest_server_port";

	// Specification constants
	public static final String IMAGE_WORKER = "image_worker";

	// Monitors constants
	public static final String SAPS_EXECUTION_PERIOD_SUBMISSOR = "saps_execution_period_submissor";
	public static final String SAPS_EXECUTION_PERIOD_CHECKER = "saps_execution_period_checker";
	public static final String SAPS_EXECUTION_PERIOD_ARCHIVER = "saps_execution_period_archiver";
	public static final String SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR = "saps_execution_period_garbage_collector";

	// Execution mode constants
	public static final String SAPS_DEBUG_MODE = "saps_debug_mode";
	public static final String SAPS_PERMANENT_STORAGE_TYPE = "saps_permanent_storage_type";
	public static final String PERMANENT_STORAGE_BASE_URL = "permanent_storage_base_url";

	// Properties file constants
	public static final String SAPS_TEMP_STORAGE_PATH = "saps_temp_storage_path";

	public static final String NO_REPLY_EMAIL = "noreply_email";
	public static final String NO_REPLY_PASS = "noreply_password";

	/*
	 *  Arrebol batch jobs execution system configs
	 */
	public static final String ARREBOL_BASE_URL = "arrebol_base_url";

	public static final String NFS_PERMANENT_STORAGE_PATH = "nfs_permanent_storage_path";
}
