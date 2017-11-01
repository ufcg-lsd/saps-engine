# Install and Configure Preprocessor

## Specifications
In order to install and configure Preprocessor, it is necessary to consider which type of scenario this component will be deployed. In our current implementation, we have:

* Virtual Machine (recommended specs)
  * 2vCPU;
  * 4GB RAM;
  * 20GB Disk;

Dependencies
To run Preprocessor in your environment, it will be needed to install Docker CE. Follow the steps below to install it.
```
apt-get remove docker docker-engine docker.io
apt-get update
apt-get install linux-image-extra-$(uname -r) linux-image-extra-virtual
apt-get update
apt-get install apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
apt-key fingerprint 0EBFCD88
add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
apt-get update
apt-get install docker-ce
apt-cache madison docker-ce
apt-get install docker-ce=<VERSION>
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

# Container Configuration
# Path NFS directory <nfs_directory>
saps_export_path=

# Path in the container
# default : /tmp
saps_container_linked_path=


# Non Required Configuration
# Preprocessor Execution interval (ms)
# default : 60000 ms
preprocessor_execution_period=
```

Once edited, it’s necessary to copy the edited configuration file to running container with

```
docker cp preprocessor.conf <container_id>:/home/ubuntu/saps-engine/config
```

### Running Preprocessor Software
To run Archiver software, replace the following variables in saps-engine/bin/start-preprocessor example available [here](https://github.com/fogbow/saps-engine/blob/frontend-integration/bin/start-preprocessor). 

After configured, it’s necessary to copy the edited start-preprocessor file to running container with
```
docker cp start-preprocessor <container_id>:/home/ubuntu/saps-engine/bin
```
Finally, it is possible to run Preprocessor using
```
docker exec -i <container_id> bash -c “cd /home/ubuntu/saps-engine && bash bin/start-preprocessor &”
```
