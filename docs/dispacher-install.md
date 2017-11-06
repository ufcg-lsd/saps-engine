### Install and Configure Dashboard and Submission Dispatcher

#### Dashboard and Submission Dispatcher
Once raised, the VM must contain all the necessary dependencies. In order to do that, follow the steps below:

    # add-apt-repository -y ppa:openjdk-r/ppa
    # apt-get update
    # curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
    # apt-get install -y nodejs
    # apt-get -y install git
    # apt-get install openjdk-7-jdk -y
    # apt-get -y install maven

Once the needed dependencies are installed, download and build saps-dashboard, fogbow-manager, blowout and saps-engine:

    # git clone -b backend-integration https://github.com/fogbow/saps-dashboard.git
    # cd saps-dashboard
    # npm install
    # mv node_modules public/
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

Before starting the dispatcher service, the dashboard configuration file (example available [here](../examples/dispatcher.conf.example)) needs to be edited to customize the behavior of the scheduler component. We show some of the most important properties below:

The value of the property **submission_rest_server_port** in *dispatcher.conf* should be the same as the port specified on **urlSapsService** in the file *dashboardApp.js*, e.g.:

dispatcher.conf:

    # submission_rest_server_port = 8080

dashboardApp.js:

    # "urlSapsService": "http://localhost:8080/"


To run saps-dispatcher, execute the following commands:

    # cd saps-engine
    # bash scripts/start-dispatcher.sh

To run the saps-dashboard, change the

    # cd saps-dashboard
    # node app.js

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
