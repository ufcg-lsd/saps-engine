# Install and Configure Archiver

## Docker Image Information
  - **User:** fogbow
  - **Repository:** archiver
  
## Dependencies
The Docker image of the Archiver component can be pulled, and a container running this image can be started, using the following commands:

  ```
  1. docker pull <docker_user>/<docker_repository>
  2. docker run -td -v <docker_user>/<docker_repository>
  3. container_id=$(docker ps | grep “<docker_user>/<docker_repository>" | awk '{print $1}')
  ```

## Configure Archiver Software
The Archiver component can also be customized through its configuration file (example available [here](../examples/archiver.conf.example)):

  ```
  # Catalogue database URL prefix (ex.: jdbc:postgresql://)
  datastore_url_prefix=

  # Catalogue database ip
  datastore_ip=

  # Catalogue database port
  datastore_port=

  # Catalogue database name
  datastore_name=

  # Catalogue database driver
  datastore_driver=

  # Catalogue database user name
  datastore_username=

  # Catalogue database user password
  datastore_password=

  # Archiver SFTP script path
  saps_sftp_script_path=

  # Default FTP server user
  default_ftp_server_user=

  # Default FTP server port
  default_ftp_server_port=

  # FTP server export path
  saps_export_path=

  # Local files path
  local_input_output_path=

  # SAPS execution period
  saps_execution_period=

  # Default Archiver loop period
  default_archiver_period=

  # Swift container name
  swift_container_name=

  # Swift input pseudo folder prefix
  swift_input_pseud_folder_prefix=

  # Swift output pseudo folder prefix
  swift_output_pseud_folder_prefix=

  # Swift user name
  swift_username=

  # Swift user password
  swift_password=

  # Swift tenant id
  swift_tenant_id=

  # Swift tenant name
  swift_tenant_name=

  # Swift authorization URL
  swift_auth_url=

  # Keystone V3 project id
  fogbow.keystonev3.project.id=

  # Keystone V3 user id
  fogbow.keystonev3.user.id=

  # Keystone V3 user password
  fogbow.keystonev3.password=

  # Keystone V3 authorization URL
  fogbow.keystonev3.auth.url=

  # Keystone V3 Swift authorization URL
  fogbow.keystonev3.swift.url=

  # Keystone V3 Swift token update period
  fogbow.keystonev3.swift.token.update.period=

  # Fogbow-cli directory path
  fogbow_cli_path=
  ```

Once edited, the configuration file needs to be copied to the container:

  ```
  docker cp archiver.conf <container_id>:/home/ubuntu/saps-engine/config
  ```

## Running Archiver Software
Before running the Archiver, the saps-engine/bin/start-archiver configuration file (example available [here](../bin/start-archiver)) also needs to be edited.

  ```
  # SAPS Engine directory path (Usually /home/ubuntu/saps-engine)
  saps_engine_dir_path=

  # Scheduler configuration file path
  saps_engine_conf_path=

  # Scheduler log file path
  saps_engine_log_properties_path=

  # Scheduler target file path (ex.: target/saps-engine-0.0.1-SNAPSHOT.jar:target/lib)
  saps_engine_target_path=

  # Local library path
  library_path=

  # Debug port
  debug_port=
  ```

Then, it needs to be copied to the container:

  ```
  docker cp start-archiver <container_id>:/home/ubuntu/saps-engine/bin
  ```

Finally, run the Archiver using:

  ```
  docker exec <container_id> bash -c “cd /home/ubuntu/saps-engine && bash bin/start-archiver &”
  ```
