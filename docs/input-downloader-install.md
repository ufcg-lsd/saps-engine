# Install and Configure Input Downloader

## Specifications
In order to install and configure Input Downloader, it is necessary to consider which type of scenario this component will be deployed. In our current implementation, we have:

Virtual Machine (recommended specs)<br/>
2vCPU;<br/>
2GB RAM;<br/>
20GB Disk;<br/>
100GB Volume.<br/>

## Dependencies
Once raised, VM must have installed NFS server in order to Workers mount its directory. It also needs to install Docker platform to run the Input Downloader Software. To install and configure it, follow the steps below:

	$ apt-get update<br/>
	$ apt-get install wget<br/>
	$ wget https://raw.githubusercontent.com/fogbow/saps-scripts/test-usage/deploy-scripts/input-downloader/installation.sh<br/>
	$ chmod 777 installation.sh<br/>
	$ ./installation.sh<br/>
	
##### Now, we need to install and configure PostgreSQL, follow the steps below:
	
	$ apt-get update<br/>
	$ apt-get install postgresql<br/>
	$ su postgres<br/>
	$ psql -c "CREATE USER <user_name> WITH PASSWORD '<user_password>';"<br/>
	$ psql -c "CREATE DATABASE <database_name> OWNER <user_name>;"<br/>
	$ psql -c "GRANT ALL PRIVILEGES ON DATABASE <database_name> TO <user_name>;"<br/>
	$ exit<br/>
	$ sed -i 's/peer/md5/g' /etc/postgresql/<installed_version>/main/pg_hba.conf<br/>
	Add “host    all             all             0.0.0.0/0               md5” into /etc/postgresql/<installed_version>/main/postgresql.conf<br/>
	Add “listen_addresses = '*'” into /etc/postgresql/<installed_version>/main/postgresql.conf<br/>
	$ service postgresql restart<br/>

After that, we have the Input Downloader container running. We must configure that component to make it work.

## Configure Input Downloader Software
With all dependencies set, now it’s time to configure Input Downloader software before starting it. In order to do this, we explain below each configuration from conf file.

#### Input Downloader database URL prefix (ex.: jdbc:postgresql://)
datastore_url_prefix=

#### Input Downloader database ip
datastore_ip=

#### Input Downloader database port
datastore_port=

#### Input Downloader database name
datastore_name=

#### Input Downloader database driver
datastore_driver=

#### Input Downloader database user name
datastore_username=

#### Input Downloader database user password
datastore_password=

#### Input Downloader default volume size
default_volume_size=

#### Input Downloader default downloader period
default_downloader_period=

#### NFS server export path
saps_export_path=

#### Path to store scenes
saps_container_linked_path=

#### Max number of tasks to download
max_tasks_to_download=

#### Max attempts trying to download a scene
max_download_attempts=

#### Script used to download a scene
container_script=

#### Max number of requests to USGS download link
max_usgs_download_link_requests=10

#### Simultaneous downloads allowed
max_simultaneous_download=1

#### USGS login URL
usgs_login_url=https://ers.cr.usgs.gov/login/

#### USGS API URL
usgs_json_url=https://earthexplorer.usgs.gov/inventory/json

#### USGS username
usgs_username=

#### USGS password
usgs_password=

#### Period to refresh USGS’ API key
usgs_api_key_period=


Once edited, it’s necessary to copy the edited configuration file to running container with

```docker cp downloader.conf <container_id>:/home/ubuntu/saps-engine/config```

## Running Input Downloader Software
To run Input Downloader software, replace the following variables in saps-engine/bin/start-input-downloader (example available here).


#### SAPS Engine directory path (Usually /home/ubuntu/saps-engine)
sebal_engine_dir_path=

#### The IP running the Input Downloader Software
crawler_ip=

#### SSH Port
crawler_ssh_port=

#### NFS Port
crawler_nfs_port=

#### The federation member
federation_member=

After configured, it’s necessary to copy the edited start-archiver file to running container with

```docker cp start-input-downloader <container_id>:/home/ubuntu/saps-engine/bin```


Finally, it is possible to run Input Downloader using

```docker exec <container_id> cd /home/ubuntu/saps-engine && bash bin/start-input-downloader &```

