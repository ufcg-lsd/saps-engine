# Install and Configure Preprocessor

## Specifications
In order to install and configure Preprocessor, it is necessary to consider which type of scenario this component will be deployed. In our current implementation, we have:

### Virtual Machine (recommended specs)
* 2vCPU;
* 4GB RAM;
* 20GB Disk;

Dependencies
```
* apt-get -y update
* apt-get -y install net-tools
* apt-get -y install iputils-ping
* apt-get install -y software-properties-common
* add-apt-repository ppa:openjdk-r/ppa
* apt-get -y update

* apt-get install -y build-essential
* apt-get install -y openjdk-7-jdk
* * apt-get install -y git
* apt-get install -y maven
```
```
* cd {workdir}
* git clone https://github.com/fogbow/fogbow-manager.git
* cd fogbow-manager
* mvn install -Dmaven.test.skip=true
```
```
* cd {workdir}
* git clone https://github.com/fogbow/blowout.git
* cd blowout
* git checkout sebal-experiment-resources-fix
* mvn install -Dmaven.test.skip=true
```
```
* cd {workdir}
* git clone https://github.com/fogbow/saps-engine.git
* cd saps-engine
* git checkout sebal-experiment-resources-fix
* mvn install -Dmaven.test.skip=true
```
Once raised, VM must have installed NFS client to access the NFS server directory of Input Downloader.

### NFS Client Installation 
Install packages:
```
sudo apt-get update
sudo apt-get install nfs-common
```
Now create the NFS directory <nfs_directory> mount point as follows:
```
sudo mkdir -p /mnt/nfs/home
```
Next we will mount the NFS shared content as shown below:
```
mount -t nfs <ip_nfs_server>:<path_nfs_server> /mnt/nfs/home/
```
Crosscheck:
```
* mount -t nfs
```

After installed, your environment is ready to pull Preprocessor’s Docker image.

```
docker pull <docker_user>/<docker_repository>:<docker_repository_tag>
docker run -td -v <local_database_dir>:<container_database_dir> <docker_user>/<docker_repository>:<docker_repository_tag>
container_id=$(docker ps | grep  “<docker_user>/<docker_repository>:<docker_repository_tag>" | awk '{print $1}')
```

### Configure Preprocessor Software
With all dependencies set, now it’s time to configure Preprocessor software before starting it. In order to do this, we explain below each configuration from conf file (example available here).

#### Image Datastore Configuration ####
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

# Container Configuration #####
# Path NFS directory <nfs_directory>
saps_export_path=

# Path in the container
# default : /tmp
saps_container_linked_path=

# Non Required Configuration ####
# Preprocessor Execution interval (ms)
preprocessor_execution_period=
```

Once edited, it’s necessary to copy the edited configuration file to running container with

```
docker cp preprocessor.conf <container_id>:/home/ubuntu/saps-engine/config
```

Running Preprocessor Software
To run Archiver software, replace the following variables in saps-engine/bin/start-preprocessor (example available here). 

After configured, it’s necessary to copy the edited start-preprocessor file to running container with
```
docker cp start-preprocessor <container_id>:/home/ubuntu/saps-engine/bin
```
Finally, it is possible to run Preprocessor using
```
docker exec -i <container_id> bash -c “cd /home/ubuntu/saps-engine && bash bin/start-preprocessor &”
```
