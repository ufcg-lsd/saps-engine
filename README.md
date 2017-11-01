# SEBAL Engine
## What is SEBAL Engine?
  SEBAL Engine is a tool created to provide a dynamic use of SEBAL algorithm using computational resources obtained through a multi-cloud environment federated by the [Fogbow Middleware](http://www.fogbowcloud.org).
  
  [here](https://github.com/fogbow/saps-engine/blob/master/scheduler-install.md)
   [here](scheduler-install.md)
  
  SEBAL Engine has six main components:
  - **Submission Service**: Serves requests from a federation member, such as the creation and monitoring new work units, or the purge of processed data.
  - **Task Catalog**: Stores information of LANDSAT image data (obtained from [NASA repository](https://ers.cr.usgs.gov)) and its execution.
  - **Crawler**: Search at **Task Catalog** for LANDSAT images in "not downloaded" state. This component assumes a NFS Server and FTP Server role and stores image data into a repository provided by an interation with **Fogbow Middleware**.
  - **Scheduler**: Order resources as it detects whether or not are images with "downloaded" state in **Task Catalog**, then redirects SEBAL execution tasks to **Worker Nodes**, which performs the image processing.
  - **Worker Node**: Receives a task from **Scheduler** and executes it. The execution consists of perform a image processing and store data in the NFS Server.
  - **Fetcher**: Search at **Task Catalog** for images in "finished" (processed by a **Worker Node**) state and transfer all output data from the FTP Server to a Swift. After that, **Crawler** is able to detect if the image was fetched, so it can remove all results from repository.

## How to use it?
### Submitting Tasks
  A pool of tasks is created when **Task Catalog** is called passing the range of years for which the images were captured by the satellite and a text file, containing the regions that will be processed.
  
  Task Field | Description
  ---- | --------------------
  Image Name: | Image name with landsat type, region and year as prefix
  Download Link: | Image download link from NASA repository
  Image State: | Image state in system execution
  Federation Member: | Federation member that deals with the image
  Priority: | Processing priority
  Station ID: | Image's nearest station ID
  Sebal Version: | Current SEBAL application version
  Sebal Engine Version: | Current SEBAL Engine application version
  Blowout Version: | Current blowout application version
  Creation Time: | Date of first interation with image in database
  Update Time: | Date of last interation with image in database
  Status: | Tells if image was purged from database or not
  Error: | Shows the error message in case of execution failure
  
  For that, SEBAL Engine relies on a centralized database that gets and stores informations about image data and process output so the components can make their decisions.

#### Image States
  While running SEBAL Engine application, each image might be in several different states. The image state will show in which phase exactly the execution is.
  
    not_downloaded: image was not downloaded by crawler yet
    downloading: image is being downloaded by crawler from nasa repository
    downloaded : image is downloaded by crawler
    running_r: image is ready to be processed/is being processed by worker node
    finished: image successfully processed by worker node
    fetching: image is being fetched into a swift
    fetched: image successfully fetched
    error: image execution returned error

## Configuring SEBAL Engine
### Getting all dependencies
  Before configure SEBAL Engine, is necessary to get all dependencies and projects to use the application.
  
  The first step is to get [fogbow-manager](https://github.com/fogbow/fogbow-manager.git), [fogbow-cli](https://github.com/fogbow/fogbow-cli.git), [blowout](https://github.com/fogbow/blowout.git) and [SEBAL Engine](https://github.com/fogbow/sebal-engine.git) repositories from **Git Hub** with the command:
  
  ```
  git clone [repository-url]
  ```
  
  The second step is to get all projects JAR to run application as expected. To achieve that, maven and maven2 must be installed in client's machine with commands:
  
  ```
  apt-get install maven
  ```
  
  ```
  apt-get install maven2
  ```
  
  After that, simply use the following command in each project directory:
  
  ```
  mvn -e install -Dmaven.test.skip=true
  ```
  
## Infrastructure Deploy
### Configuring deploy with keystone token
  To configure SEBAL Engine deploy, is necessary to generate a token that will be used to order resources from **Fogbow**. For that, simply generate a token using the following **fogbow-cli** command:
  
  ```
  bash fogbow-cli/bin/fogbow-cli token --create --type openstack -Dusername=[user-name] -Dpassword=[password] -DauthUrl=[auth-url] -DtenantName=[tenant-name]
  ```
  
  When token is generated, put it into a file and insert its path in **sebal-engine/config/sebal.conf**
  
  ```
  infra_fogbow_token_public_key_filepath=path-to-file 
  ```
  
### Deploying Task Catalog and Scheduler
  To deploy Task Catalog and Scheduler, run the command:
  
  ```
  bash sebal-engine/scripts/infrastructure/deploy_scheduler [private-key-path] [storage-size]
  ```
  
  When finished, the above command will generate a file into **scheduler/scheduler-info/scheduler-exec-info** with all needed information about returned resource.
  
### Deploying Crawler
  To deploy Crawler, run the command:

  ```
  bash sebal-engine/scripts/infrastructure/deploy_crawler [private-key-path] [storage-size]
  ```
  
  When finished, the above command will generate a file into **crawler/crawler-info/crawler-exec-info** with all needed information about returned resource.
  
### Deploying Fetcher
  To deploy Fetcher, run the command:

  ```
  bash sebal-engine/scripts/infrastructure/deploy_fetcher [private-key-path]
  ```
  
  When finished, the above command will generate a file into **fetcher/fetcher-info/fetcher-exec-info** with all needed information about returned resource.
  
## Using SEBAL Engine CLI
### Using Catalog
  To add LANDSAT images from a list of regions with first and last year, run the **add** command:
  
  ```
  bash sebal-engine/scripts/cli/catalog add [first-year] [last-year] [regions-file-path]
  ```
  
  To get LANDSAT images from a list of regions with first and last year, run the **get** command:
  
  ```
  bash sebal-engine/scripts/cli/catalog get [first-year] [last-year] [regions-file-path]
  ```
  
  To list corrupted LANDSAT images from **Task Catalog**, run the **list-corrupted** command:
  
  ```
  bash sebal-engine/scripts/cli/catalog list-corrupted
  ```
  
  To list all LANDSAT images from **Task Catalog**, run the **list** command:
  
  ```
  bash sebal-engine/scripts/cli/catalog list
  ```
  
### Using Crawler
  To start Crawler application, run the Crawler CLI command:

  ```
  bash sebal-engine/scripts/cli/crawler [task-catalog-ip] [task-catalog-port] [federation-member]
  ```
  
### Using Scheduler
  To start Scheduler application, run the Scheduler CLI command:
  
  ```
  bash sebal-engine/scripts/cli/scheduler [task-catalog-ip] [task-catalog-port] [nfs-server-ip] [nfs-server-port]
  ```
  
### Using Fetcher
  To start Fetcher application, run the Fetcher CLI command:
  
  ```
  bash sebal-engine/scripts/cli/crawler [task-catalog-ip] [task-catalog-port] [ftp-server-ip] [ftp-server-port]
  ```
