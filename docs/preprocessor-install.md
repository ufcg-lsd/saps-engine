# Install and Configure Preprocessor

## Docker repository
* User: fogbow
* Repository: preprocessor

### Dependencies
Configure your timezone and NTP client as shown below.
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
Install and configure NFS Client Installation in the Host.
```
sudo apt-get update
sudo apt-get install nfs-common
# Now create the NFS directory <nfs_directory> mount point as follows:
sudo mkdir -p /mnt/nfs/home
# Next we will mount the NFS shared content as shown below:
mount -t nfs <ip_nfs_server>:<path_nfs_server> /mnt/nfs/home/
# Crosscheck:
mount -t nfs
```
After installing the NSF client, the environment is ready to pull the image of the Preprocessor component, and start a container that runs this image:
  ```
  1. docker pull <docker_user>/<docker_repository>:<docker_repository_tag>
  2. docker run -td -v <nfs_directory>:<container_dir> <docker_user>/<docker_repository>:<docker_repository_tag>
  3. container_id=$(docker ps | grep  “<docker_user>/<docker_repository>:<docker_repository_tag>" | awk '{print $1}')
  ```
### Configure Preprocessor Software
The configuration file of the Preprocessor component must be edited to customize its behavior:
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

# No Required Configuration ####
# Preprocessor Execution interval (ms)
preprocessor_execution_period=
```
Again, the file must be copied to the container:
```
docker cp preprocessor.conf <container_id>:/home/ubuntu/saps-engine/config
```
The saps-engine/bin/start-preprocessor script must
also be configured:
```
saps_engine_log_properties_path =
saps_engine_target_path =
saps_engine_conf_path =
```
Then, the edited start-preprocessor script must be copied to the container:
```
docker cp start-preprocessor <container_id>:/home/ubuntu/saps-engine/bin
```
Finally, the Preprocessor is started using the following command:
```
docker exec -i <container_id> bash -c “cd /home/ubuntu/saps-engine && bash bin/start-preprocessor &”
```
