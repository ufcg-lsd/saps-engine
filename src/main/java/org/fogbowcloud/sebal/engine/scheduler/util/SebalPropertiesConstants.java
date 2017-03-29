package org.fogbowcloud.sebal.engine.scheduler.util;

public class SebalPropertiesConstants {

	// Image database constants
	public static final String IMAGE_DATASTORE_IP = "datastore_ip";
	public static final String IMAGE_DATASTORE_PORT = "datastore_port";
	
	// Federation constants
	public static final String LSD_FEDERATION_MEMBER = "lsd.manager.naf.lsd.ufcg.edu.br";
	public static final String AZURE_FEDERATION_MEMBER = "azure.lsd.ufcg.edu.br";
	
	// USGS constants
	public static final String MAX_USGS_DOWNLOAD_LINK_REQUESTS = "max_usgs_download_link_requests";
	public static final String USGS_LOGIN_URL = "usgs_login_url";
	public static final String USGS_JSON_URL = "usgs_json_url";
	public static final String USGS_USERNAME = "usgs_username";
	public static final String USGS_PASSWORD = "usgs_password";
	public static final String USGS_API_KEY_PERIOD = "usgs_api_key_period";

	// Swift constants
	public static final String SWIFT_CONTAINER_NAME = "swift_container_name";
	public static final String SWIFT_INPUT_PSEUDO_FOLDER_PREFIX = "swift_input_pseud_folder_prefix";
	public static final String SWIFT_OUTPUT_PSEUDO_FOLDER_PREFIX = "swift_output_pseud_folder_prefix";
	public static final String SWIFT_USERNAME = "swift_username";
	public static final String SWIFT_PASSWORD = "swift_password";
	public static final String SWIFT_TENANT_ID = "swift_tenant_id";
	public static final String SWIFT_TENANT_NAME = "swift_tenant_name";
	public static final String SWIFT_AUTH_URL = "swift_auth_url";
	public static final String SWIFT_IMAGE_EXTENSION = "swift_image_extension";
	public static final String SWIFT_PSEUDO_FOLDER_PREFIX = "swift_pseudo_folder_prefix";
	
	// Restlet constants
	public static final String DB_REST_SERVER_PORT = "db_rest_server_port";
	
	// Infrastructure constants
	public static final String INFRA_SPECS_BLOCK_CREATING = "infra_specs_block_creating";
	public static final String INFRA_INITIAL_SPECS_FILE_PATH = "infra_initial_specs_file_path";
	public static final String INFRA_PROVIDER_CLASS_NAME = "infra_provider_class_name";
	public static final String INFRA_IS_STATIC = "infra_is_elastic";
	
	// KeystoneV3 constants
	public static final String FOGBOW_KEYSTONEV3_UPDATE_PERIOD = "fogbow.keystonev3.swift.token.update.period";
	public static final String FOGBOW_KEYSTONEV3_PROJECT_ID = "fogbow.keystonev3.project.id";
	public static final String FOGBOW_KEYSTONEV3_USER_ID = "fogbow.keystonev3.user.id";
	public static final String FOGBOW_KEYSTONEV3_PASSWORD = "fogbow.keystonev3.password";
	public static final String FOGBOW_KEYSTONEV3_AUTH_URL = "fogbow.keystonev3.auth.url";
	public static final String FOGBOW_KEYSTONEV3_SWIFT_URL = "fogbow.keystonev3.swift.url";
	
	// Monitors constants
	public static final String EXECUTION_MONITOR_PERIOD = "execution_monitor_period";
	public static final String SEBAL_EXECUTION_PERIOD = "sebal_execution_period";
	public static final String DEFAULT_FETCHER_PERIOD = "default_fetcher_period";
	public static final String DEFAULT_CRAWLER_PERIOD = "default_crawler_period";
	
	// FTP constants
	public static final String DEFAULT_FTP_SERVER_USER = "default_ftp_server_user";
	public static final String DEFAULT_FTP_SERVER_PORT = "default_ftp_server_port";
	public static final String AZURE_FTP_SERVER_USER = "azure_ftp_server_user";
	public static final String AZURE_FTP_SERVER_PORT = "azure_ftp_server_port";

	// Script path constants
	public static final String SEBAL_SFTP_SCRIPT_PATH = "sebal_sftp_script_path";
	public static final String FMASK_SCRIPT_PATH = "fmask_script_path";
	public static final String FMASK_TOOL_PATH = "fmask_tool_path";
	
	// Properties file constants
	public static final String FMASK_VERSION_FILE_PATH = "fmask_version_file_path";
	public static final String LOCAL_INPUT_OUTPUT_PATH = "local_input_output_path";
	public static final String SEBAL_EXPORT_PATH = "sebal_export_path";
	public static final String BLOWOUT_DIR_PATH = "blowout_dir_path";
	public static final String FOGBOW_CLI_PATH = "fogbow_cli_path";
}
