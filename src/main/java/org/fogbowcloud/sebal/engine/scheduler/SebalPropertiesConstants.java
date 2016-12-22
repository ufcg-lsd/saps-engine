package org.fogbowcloud.sebal.engine.scheduler;

public class SebalPropertiesConstants {

	// Federation constants
	public static final String AZURE_FEDERATION_MEMBER = "azure.lsd.ufcg.edu.br";

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
	
	// KeystoneV3 constants
	public static final String FOGBOW_KEYSTONEV3_PROJECT_ID = "fogbow.keystonev3.project.id";
	public static final String FOGBOW_KEYSTONEV3_USER_ID = "fogbow.keystonev3.user.id";
	public static final String FOGBOW_KEYSTONEV3_PASSWORD = "fogbow.keystonev3.password";
	public static final String FOGBOW_KEYSTONEV3_AUTH_URL = "fogbow.keystonev3.auth.url";
	public static final String FOGBOW_KEYSTONEV3_SWIFT_URL = "fogbow.keystonev3.swift.url";
	public static final String FOGBOW_KEYSTONEV3_UPDATE_PERIOD = "fogbow.keystonev3.swift.token.update.period";
	
	// Monitors constants
	public static final String EXECUTION_MONITOR_PERIOD = "execution_monitor_period";
	public static final String SEBAL_EXECUTION_PERIOD = "sebal_execution_period";
	public static final String DEFAULT_FETCHER_PERIOD = "default_fetcher_period";
	
	// Properties file constants
	public static final String SEBAL_EXPORT_PATH = "sebal_export_path";
	public static final String FMASK_TOOL_PATH = "fmask_tool_path";
	public static final String FMASK_SCRIPT_PATH = "fmask_script_path";
	public static final String SEBAL_SFTP_SCRIPT_PATH = "sebal_sftp_script_path";
	public static final String BLOWOUT_DIR_PATH = "blowout_dir_path";
	public static final String LOCAL_INPUT_OUTPUT_PATH = "local_input_output_path";
	public static final String AZURE_FTP_SERVER_USER = "azure_ftp_server_user";
	public static final String DEFAULT_FTP_SERVER_USER = "default_ftp_server_user";
	public static final String FOGBOW_CLI_PATH = "fogbow_cli_path";
}
