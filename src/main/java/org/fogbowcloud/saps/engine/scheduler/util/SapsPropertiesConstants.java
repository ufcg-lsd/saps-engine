package org.fogbowcloud.saps.engine.scheduler.util;

public class SapsPropertiesConstants {

	// Image database constants
	public static final String IMAGE_DATASTORE_IP = "datastore_ip";
	public static final String IMAGE_DATASTORE_PORT = "datastore_port";

	// Federation constants
	public static final String LSD_FEDERATION_MEMBER = "lsd.manager.naf.lsd.ufcg.edu.br";
	public static final String AZURE_FEDERATION_MEMBER = "azure.lsd.ufcg.edu.br";

	// Submission constants
	public static final String DATASET_LT5_TYPE = "landsat_5";
	public static final String DATASET_LE7_TYPE = "landsat_7";
	public static final String DATASET_LC8_TYPE = "landsat_8";

	// JSON constants
	public static final String TILE_ID_JSON_KEY = "id";
	public static final String TILES_JSON_KEY = "tiles";
	public static final String USERNAME_JSON_KEY = "username";
	public static final String PASSWORD_JSON_KEY = "password";
	public static final String AUTH_TYPE_JSON_KEY = "authType";
	public static final String API_KEY_JSON_KEY = "apiKey";
	public static final String NODE_JSON_KEY = "node";
	public static final String DATASET_NAME_JSON_KEY = "datasetName";
	public static final String DISPLAY_ID_JSON_KEY = "displayId";
	public static final String ENTITY_ID_JSON_KEY = "entityId";
	public static final String LONGITUDE_JSON_KEY = "longitude";
	public static final String LATITUDE_JSON_KEY = "latitude";
	public static final String SORT_ORDER_JSON_KEY = "sortOrder";
	public static final String MAX_RESULTS_JSON_KEY = "maxResults";
	public static final String TEMPORAL_FILTER_JSON_KEY = "temporalFilter";
	public static final String SPATIAL_FILTER_JSON_KEY = "spatialFilter";
	public static final String START_DATE_JSON_KEY = "startDate";
	public static final String END_DATE_JSON_KEY = "endDate";
	public static final String DATE_FIELD_JSON_KEY = "dateField";
	public static final String UPPER_RIGHT_JSON_KEY = "upperRight";
	public static final String LOWER_LEFT_JSON_KEY = "lowerLeft";
	public static final String FILTER_TYPE_JSON_KEY = "filterType";
	public static final String DATA_JSON_KEY = "data";
	public static final String PRODUCTS_JSON_KEY = "products";
	public static final String ENTITY_IDS_JSON_KEY = "entityIds";
	public static final String EROS_JSON_VALUE = "EROS";
	public static final String ASC_JSON_VALUE = "ASC";
	public static final String SEARCH_DATE_JSON_VALUE = "search_date";
	public static final String MBR_JSON_VALUE = "mbr";
	public static final String RESULTS_JSON_KEY = "results";

	// Dataset constants
	public static final String LANDSAT_5_PREFIX = "LT5";
	public static final String LANDSAT_7_PREFIX = "LE7";
	public static final String LANDSAT_8_PREFIX = "LC8";
	public static final String LANDSAT_5_DATASET = "LANDSAT_TM_C1";
	public static final String LANDSAT_7_DATASET = "LANDSAT_ETM_C1";
	public static final String LANDSAT_8_DATASET = "LANDSAT_8_C1";

	// USGS constants
	public static final String MAX_USGS_DOWNLOAD_LINK_REQUESTS = "max_usgs_download_link_requests";
	public static final String USGS_LOGIN_URL = "usgs_login_url";
	public static final String USGS_JSON_URL = "usgs_json_url";
	public static final String USGS_USERNAME = "usgs_username";
	public static final String USGS_PASSWORD = "usgs_password";
	public static final String USGS_API_KEY_PERIOD = "usgs_api_key_period";
	public static final String CONTAINER_SCRIPT = "container_script";
	public static final String MAX_DOWNLOAD_ATTEMPTS = "max_download_attempts";

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
	public static final String SWIFT_STATIONS_PSEUDO_FOLDER_PREFIX = "swift_stations_pseudo_folder_prefix";

	// Restlet constants
	public static final String SUBMISSION_REST_SERVER_PORT = "submission_rest_server_port";

	// Specification constants
	public static final String SPEC_FOGBOW_REQUIREMENTS = "FogbowRequirements";
	public static final String SPEC_REQUEST_TYPE = "RequestType";
	public static final String SPEC_GLUE2_CLOUD_COMPUTE_MANAGER_ID = "Glue2CloudComputeManagerID";
	public static final String BLOWOUT_VERSION_PREFIX = "blowout.version.";

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
	public static final String SAPS_EXECUTION_PERIOD = "saps_execution_period";
	public static final String DEFAULT_ARCHIVER_PERIOD = "default_fetcher_period";
	public static final String DEFAULT_CRAWLER_PERIOD = "default_crawler_period";

	// FTP constants
	public static final String DEFAULT_FTP_SERVER_USER = "default_ftp_server_user";
	public static final String DEFAULT_FTP_SERVER_PORT = "default_ftp_server_port";
	public static final String AZURE_FTP_SERVER_USER = "azure_ftp_server_user";
	public static final String AZURE_FTP_SERVER_PORT = "azure_ftp_server_port";

	// NOAA constants
	public static final String NOAA_FTP_URL = "noaa_ftp_url";

	// Script path constants
	public static final String SEBAL_SFTP_SCRIPT_PATH = "sebal_sftp_script_path";
	public static final String FMASK_SCRIPT_PATH = "fmask_script_path";
	public static final String FMASK_TOOL_PATH = "fmask_tool_path";

	// Quantities constants
	public static final String MAX_NUMBER_OF_TASKS = "max_tasks_to_download";

	// Properties file constants
	public static final String FMASK_VERSION_FILE_PATH = "fmask_version_file_path";
	public static final String LOCAL_INPUT_OUTPUT_PATH = "local_input_output_path";
	public static final String SAPS_EXPORT_PATH = "saps_export_path";
	public static final String SAPS_CONTAINER_LINKED_PATH = "saps_container_linked_path";
	public static final String BLOWOUT_DIR_PATH = "blowout_dir_path";
	public static final String FOGBOW_CLI_PATH = "fogbow_cli_path";
	public static final String STATIONS_FILE_PATH = "stations_file_path";
	public static final String BASE_YEAR_DIR_PATH = "base_year_dir_path";
	public static final String POSSIBLE_STATIONS_FILE_PATH = "src/main/resources/possible_stations";
	public static final String TILES_COORDINATES_FILE_PATH = "src/main/resources/tiles_coordinates.json";
}
