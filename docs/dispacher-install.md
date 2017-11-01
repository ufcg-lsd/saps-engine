### Install and Configure Dashboard and Submission Dispatcher

#### Specifications
In order to install and configure the Dashboard and the Submission Dispatcher, it is necessary to consider the scenario where these components are going to be deployed. In our current deployment, we have a virtual machine with the following specs:

- w vCPU;
- x GB RAM;
- y GB Disk;
- z GB Volume.

#### Dependencies
Once raised, the VM must contain all the necessary dependencies. In order to do that, follow the steps below:

    # add-apt-repository -y ppa:openjdk-r/ppa
    # apt-get update
    # curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
    # apt-get install -y nodejs
    # apt-get -y install git
    # apt-get install openjdk-7-jdk -y
    # apt-get -y install maven


    # git clone -b backend-integration https://github.com/fogbow/saps-dashboard.git
    # cd saps-dashboard
    # npm install
    # mv node_modules/public
    # cd ..

    # git clone -b develop https://github.com/fogbow/fogbow-manager.git
    # cd fogbow-manager/
    # mvn install -Dmaven.test.skip=true
    # cd ..

    # git clone -b sebal-experiment-resources-fix https://github.com/fogbow/blowout.git
    # cd blowout
    # mvn install -Dmaven.test.skip=true
    # cd ..

    # git clone -b frontend-integration https://github.com/fogbow/saps-engine.git
    # cd saps-engine
    # mvn install -Dmaven.test.skip=true
    # cd ..

    
#### Configure Dispatcher Software
With all dependencies set, now it’s time to configure Preprocessor software before starting it. In order to do this, we explain below each configuration from conf file example available [here](https://github.com/fogbow/saps-engine/blob/frontend-integration/examples/dispatcher.conf.example).

##### Image Datastore Configuration
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
```

##### Restlet Configuration
```
# Admin email
admin_email=

# Admin username
admin_user=

# Admin password
admin_password=

# Submission Restlet port
submission_rest_server_port=
```

##### Container Configuration
```
# Path NFS directory <nfs_directory>
saps_export_path=
```

##### USGS Configuration
```
# USGS login URL
usgs_login_url=

# USGS API URL
usgs_json_url=

# USGS username
usgs_username=

# USGS password
usgs_password=

# Period to refresh USGS’ API key
usgs_api_key_period=
```
