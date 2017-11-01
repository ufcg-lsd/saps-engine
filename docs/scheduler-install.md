# Install and Configure Scheduler

## Dependencies
Before starting the Scheduler container, the Catalog database must be created, using the following commands

  ```
  1. apt-get update
  2. apt-get install postgresql
  3. su postgres
  4. psql -c "CREATE USER <user_name> WITH PASSWORD '<user_password>';"
  5. psql -c "CREATE DATABASE <database_name> OWNER <user_name>;"
  6. psql -c "GRANT ALL PRIVILEGES ON DATABASE <database_name> TO <user_name>;"
  7. exit
  8. sed -i 's/peer/md5/g' /etc/postgresql/<installed_version>/main/pg_hba.conf
  9. bash -c 'echo “host    all             all             0.0.0.0/0               md5” >> /etc/postgresql/<installed_version>/main/pg_hba.conf'
  10. Add “listen_addresses = '*'” into /etc/postgresql/<installed_version>/main/postgresql.conf
service postgresql restart
  ```

After that, configure your timezone and NTP client as shown below.

  ```
  1. bash -c ‘echo "America/Recife" > /etc/timezone’
  2. dpkg-reconfigure -f noninteractive tzdata
  3. apt-get update
  4. apt install -y ntp
  5. sed -i "/server 0.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  6. sed -i "/server 1.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  7. sed -i "/server 2.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  8. sed -i "/server 3.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  9. sed -i "/server ntp.ubuntu.com/d" /etc/ntp.conf
  10. bash -c ‘echo "server ntp.lsd.ufcg.edu.br" >> /etc/ntp.conf’
  11. service ntp restart
  12. service postgresql restart
  ```

Once the Catalogue is prepared, install Docker CE in order to deploy Scheduler component. To do this, follow the instructions provided [here](container-install.md).

After installed, your environment is ready to pull Scheduler’s Docker image.

  ```
  1. docker pull <docker_user>/<docker_repository>:<docker_repository_tag>
  2. docker run -td -v <local_database_dir>:<container_database_dir> <docker_user>/<docker_repository>:<docker_repository_tag>
  3. container_id=$(docker ps | grep  “<docker_user>/<docker_repository>:<docker_repository_tag>" | awk '{print $1}')
  ```

## Configure Scheduler Software
With all dependencies set, now it’s time to configure Scheduler software before starting it. In order to do this, we explain below each configuration from conf file (example available [here](../examples/scheduler.conf.example)).

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

  # Worker spec file
  infra_initial_specs_file_path=

  # Worker sandbox path
  worker_sandbox=

  # Worker temporary raster directory
  worker_raster_tmp_dir=

  # Worker run script path
  saps_worker_run_script_path=

  # Worker remote user
  worker_remote_user=

  # NFS server export path
  saps_export_path=

  # Worker mount point
  worker_mount_point=

  # Worker exit code file path
  remote_command_exit_path=

  # Blowout directory path
  blowout_dir_path=

  # Infrastructure order service time
  infra_order_service_time=

  # Infrastructure resource service time
  infra_resource_service_time=

  # Scheduler loop period
  scheduler_period=

  # SAPS loop period
  saps_execution_period=

  # Infrastructure specs block creating
  infra_specs_block_creating=

  # Execution monitor period
  execution_monitor_period=

  # Blowout Infrastructure manager implementation
  impl_infra_manager_class_name=

  # Blowout Scheduler implementation
  impl_scheduler_class_name=

  # Blowout Pool implementation
  impl_blowout_pool_class_name=

  # Infrastructure is elastic
  infra_is_elastic=

  # Infrastructure Provider implementation
  infra_provider_class_name=

  # Infrastructure resource connection timeout
  infra_resource_connection_timeout=

  # Infrastructure resource idle lifetime
  infra_resource_idle_lifetime=

  # Maximum resource reuse
  max_resource_reuse=

  # Maximum resource connection retries
  max_resource_connection_retry=

  # Infrastructure monitor period
  infra_monitor_period=

  # Local Blowout command interpreter
  local_command_interpreter=

  # Token update time
  token_update_time=

  # Token update time unit
  token_update_time_unit=

  # Blowout datastore URL
  blowout_datastore_url=
  ```

Scheduler must know from which Fogbow Manager the resources requests will be made. Currently, we have three plugins available to use in SAPS Engine: LDAPTokenUpdatePlugin, NAFTokenUpdatePlugin and KeystoneTokenUpdatePlugin. Depending on Manager implementation, the plugin chosen will change. To configure this, modify your scheduler.conf with

  ```
  # Fogbow infrastructure manager base URL
  infra_fogbow_manager_base_url=

  # Infrastructure authorization token plugin
  infra_auth_token_update_plugin=
  ```

To configure LDAP authentication:

  ```
  # LDAP Infrastructure user name
  fogbow.ldap.username=

  # LDAP Infrastructure user password
  fogbow.ldap.password=

  # LDAP Infrastructure authorization URL
  fogbow.ldap.auth.url=

  # LDAP Infrastructure base
  fogbow.ldap.base=

  # LDAP Infrastructure encrypt type
  auth_token_prop_ldap_encrypt_type=

  # LDAP Infrastructure private key
  fogbow.ldap.private.key=

  # LDAP Infrastructure public key
  fogbow.ldap.public.key=
  ```

To configure NAF authentication:

  ```
  # NAF Infrastructure private key
  auth_token_prop_naf_identity_private_key=

  # NAF Infrastructure public key
  auth_token_prop_naf_identity_private_key=

  # NAF Infrastructure user name
  auth_token_prop_naf_identity_token_username=

  # NAF Infrastructure user password
  auth_token_prop_naf_identity_token_password=
  ```

To configure Keystone authentication:

  ```
  # Keystone Infrastructure tenant name
  auth_token_prop_keystone_tenantname=

  # Keystone Infrastructure user password
  auth_token_prop_keystone_password=

  # Keystone Infrastructure user name
  auth_token_prop_keystone_username=

  # Keystone Infrastructure authentication URL
  auth_token_prop_keystone_auth_url=
  ```

Once edited, it’s necessary to copy the edited configuration file to running container with

docker cp scheduler.conf <container_id>:/home/ubuntu/saps-engine/config

## Running Scheduler Software
To run Scheduler software, replace the following variables in saps-engine/bin/start-scheduler (example available [here](../bin/start-scheduler)).

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

After configured, it’s necessary to copy the edited start-scheduler file to running container with

  ```
  docker cp start-scheduler <container_id>:/home/ubuntu/saps-engine/bin
  ```

Finally, it is possible to run Scheduler using

  ```
  docker exec -i <container_id> bash -c “cd /home/ubuntu/saps-engine && bash bin/start-scheduler &”
  ```
