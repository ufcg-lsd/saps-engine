# Install and Configure Archiver

## Specifications
In order to install and configure Archiver, it is necessary to consider which type of scenario this component will be deployed. In our current implementation, we have:

  - **Virtual Machine (recommended specs)**
    - 1vCPU;
    - 2GB RAM;
    - 10GB Disk.

## Dependencies
To run Archiver in your environment, it will be needed to install Docker CE. Follow the steps below to install it.

  ```
1. apt-get remove docker docker-engine docker.io
2. apt-get update
3. apt-get install linux-image-extra-$(uname -r) linux-image-extra-virtual
4. apt-get update
5. apt-get install apt-transport-https ca-certificates curl software-properties-common
6. curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
7. apt-key fingerprint 0EBFCD88
8. add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
9. apt-get update
10. apt-get install docker-ce
11. apt-cache madison docker-ce
12. apt-get install docker-ce=<VERSION>
  ```

After installed, your environment is ready to pull Archiver’s Docker image.

  ```
  1. docker pull <docker_user>/<docker_repository>
  2. docker run -td -v <docker_user>/<docker_repository>
  3. container_id=$(docker ps | grep “<docker_user>/<docker_repository>" | awk '{print $1}')
  ```

## Configure Archiver Software
With all dependencies set, now it’s time to configure Archiver software before starting it. In order to do this, we explain below each configuration from conf file (example available here).

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

  # LDAP token update plugin
  infra_fogbow_token_update_plugin=

  # LDAP authorization URL
  fogbow.ldap.auth.url=

  # LDAP user name
  fogbow.ldap.username=

  # LDAP user password
  fogbow.ldap.password=

  # LDAP base
  fogbow.ldap.base=

  # LDAP encrypt type
  fogbow.ldap.encrypt.type=

  # LDAP private key
  fogbow.ldap.private.key=

  # LDAP public key
  fogbow.ldap.public.key=

  # Fogbow Infrastructure token public key file path
  infra_fogbow_token_public_key_filepath=

  # Fogbow Infrastructure manager base URL
  infra_fogbow_manager_base_url=
  ```

Once edited, it’s necessary to copy the edited configuration file to running container with

  ```
  docker cp archiver.conf <container_id>:/home/ubuntu/saps-engine/config
  ```

## Running Archiver Software
To run Archiver software, replace the following variables in saps-engine/bin/start-archiver (example available here).

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

  # Scheduler Catalogue ip
  scheduler_ip=

  # Scheduler Catalogue port
  scheduler_db_port=
  ```

After configured, it’s necessary to copy the edited start-archiver file to running container with

  ```
  docker cp start-archiver <container_id>:/home/ubuntu/saps-engine/bin
  ```

Finally, it is possible to run Archiver using

  ```
  docker exec <container_id> bash -c “cd /home/ubuntu/saps-engine && bash bin/start-archiver &”
  ```
