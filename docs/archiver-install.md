# Install and Configure Archiver

TODO: describe Archiver purpose
  
## Dependencies

In an apt-based Linux distro, type the below commands to install SAPS Archiver dependencies.

```
sudo apt-get update
sudo apt-get install openjdk-8-jdk
sudo apt-get -y install maven
sudo apt-get -y install git
sudo apt install python-swiftclient
sudo apt-get install -y nfs-kernel-server
```

In addition to above Linux packages, the Archiver also depends on three codebases: 1) ```fogbow-mono-manager```; 2) ```fogbow-mono-cli```; and 3) the own ```saps-engine``` repository (which holds the Archive code). To fetch and compile the source code of these repositories, follow the below steps:

```
# fogbow-mono-manager repository
git clone https://github.com/fogbow/fogbow-mono-manager.git
cd fogbow-mono-manager
git checkout develop
mvn install -Dmaven.test.skip=true

# fogbow-mono-cli repository
git clone https://github.com/fogbow/fogbow-mono-cli.git
cd fogbow-mono-cli
git checkout develop
mvn install -Dmaven.test.skip=true

# saps-engine repository
git clone https://github.com/ufcg-lsd/saps-engine
cd saps-engine
git checkout develop
mvn install -Dmaven.test.skip=true
```

## Configure

The first part of SAPS Archiver configuration deals with the setup of the NFS server temporary storage. Before starting the NFS daemon, please choose an directory, ```$nfs_server_folder_path```, in your machine local file system. The NFS daemon will hold the exported files within this directory. Below commands setup the NFS server:

```
mkdir -p $nfs_server_folder_path
echo "$nfs_server_folder_path *(rw,insecure,no_subtree_check,async,no_root_squash)" >> /etc/exports
sudo service nfs-kernel-server restart
```

# ALERT: Below instructions are outdated


## Configure
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

## Run
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
