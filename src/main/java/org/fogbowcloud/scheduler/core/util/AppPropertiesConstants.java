package org.fogbowcloud.scheduler.core.util;


public class AppPropertiesConstants {


	// __________ INFRASTRUCTURE CONSTANTS __________ //
	public static final String INFRA_IS_STATIC = "infra_is_elastic";
	public static final String INFRA_PROVIDER_CLASS_NAME = "infra_provider_class_name";
	public static final String INFRA_ORDER_SERVICE_TIME = "infra_order_service_time";
	public static final String INFRA_RESOURCE_SERVICE_TIME = "infra_resource_service_time";
	public static final String INFRA_RESOURCE_CONNECTION_TIMEOUT = "infra_resource_connection_timeout";
	public static final String INFRA_RESOURCE_IDLE_LIFETIME = "infra_resource_idle_lifetime";
	public static final String INFRA_RESOURCE_REUSE_TIMES = "max_resource_reuse";

	public static final String INFRA_INITIAL_SPECS_FILE_PATH = "infra_initial_specs_file_path";
	public static final String INFRA_CRAWLER_SPECS_FILE_PATH = "infra_crawler_specs_file_path";
	public static final String INFRA_SCHEDULER_SPECS_FILE_PATH = "infra_scheduler_specs_file_path";
	public static final String INFRA_FETCHER_SPECS_FILE_PATH = "infra_fetcher_specs_file_path";
	public static final String INFRA_SPECS_BLOCK_CREATING = "infra_specs_block_creating";
	
	public static final String INFRA_INITIAL_SPECS_BLOCK_CREATING = "infra_initial_specs_block_creating";

	// __________ FOGBOW INFRASTRUCTURE CONSTANTS __________ //
	public static final String INFRA_FOGBOW_MANAGER_BASE_URL = "infra_fogbow_manager_base_url";
	public static final String INFRA_FOGBOW_TOKEN_PUBLIC_KEY_FILEPATH = "infra_fogbow_token_public_key_filepath";
	public static final String INFRA_FOGBOW_USERNAME = "fogbow_username";

	public static final String EXECUTION_MONITOR_PERIOD = "execution_monitor_period";

	public static final String REST_SERVER_PORT = "rest_server_port";


	//___________ DB CONSTANTS ______________//


	public static final String DB_MAP_NAME = "jobMap";

	public static final String DB_MAP_USERS = "users";
	
	public static final String DB_FILE_NAME = "legacyJobs.db";
	
	public static final String DB_FILE_USERS = "usersmap.db";
	
	
	//___________ APPLICATION HEADERS  ____//
	
	public static final String X_AUTH_NONCE = "X-auth-nonce";
	public static final String X_AUTH_USER = "X-auth-username";
	public static final String X_AUTH_HASH = "X-auth-hash";


}
