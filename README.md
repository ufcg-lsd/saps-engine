# SEBAL Engine
## What is SEBAL Engine?
  SEBAL Engine is a tool created to provide a dynamic use of SEBAL algorithm using computational resources obtained through a multi-cloud environment federated by the [Fogbow Middleware](http://www.fogbowcloud.org).
  
  SEBAL Engine has six main components:
  - **Submission Service**: Serves requests from a federation member, such as the creation and monitoring new work units, or the purge of processed data.
  - **Task Catalog**: Stores information of LANDSAT image data (obtained from [NASA repository](https://ers.cr.usgs.gov)) and its execution.
  - **Crawler**: Search at **Task Catalog** for LANDSAT images in "not downloaded" state. This component assumes a NFS Server and FTP Server role and stores image data into a repository provided by an interation with **Fogbow Middleware**.
  - **Scheduler**: Order resources as it detects whether or not are images with "downloaded" state in **Task Catalog**, then redirects SEBAL execution tasks to **Worker Nodes**, which performs the image processing.
  - **Worker Node**: Receives a task from **Scheduler** and executes it. The execution consists of perform a image processing and store data in the NFS Server.
  - **Fetcher**: Search at **Task Catalog** for images in "finished" (processed by a **Worker Node**) state and transfer all output data from the FTP Server to a Swift. After that, **Crawler** is able to detect if the image was fetched, so it can remove all results from repository.

## How to use it?
  This section will contain all info about how to use SEBAL Engine, such as infrastructure deployment, database filling and application launch.
## Configuring SEBAL Engine
  This section will contain all info about how to deploy SEBAL Engine infrastructure, providing configuration help step-by-step.
## Infrastructure Deploy
### Configuring Deploy
  To configure **SEBAL Engine** deploy, it is necessary to generate a token that will be used to order resources from **Fogbow**. For that, the **fogbow-cli** project must be cloned from repository (https://github.com/fogbow/fogbow-cli.git) using command:
  
  ```
  git clone [fogbow-cli-url]
  ```
  
  After that, simply generate a token using the following command:
  
  ```
  bash bin/fogbow-cli token --create --type openstack -Dusername=[user-name] -Dpassword=[password] -DauthUrl=[auth-url] -DtenantName=[tenant-name]
  ```
  
  When token is generated, put it into a file and insert its path in **sebal-engine/config/sebal.conf**
  
  ```
  infra_fogbow_token_public_key_filepath=path-to-file 
  ```
  
### Deploying Task Catalog and Scheduler
  To deploy Task Catalog and Scheduler, run the command:
  
  ```
  bash scripts/infrastructure/deploy_scheduler [private-key-path] [storage-size]
  ```
  
  When ran, the above command will generate a file into **scheduler/scheduler-info/scheduler-exec-info** with all needed information about returned resource.
  
### Deploying Crawler
  To deploy Crawler, run the command:

  ```
  bash scripts/infrastructure/deploy_crawler [private-key-path] [storage-size]
  ```
  
  When ran, the above command will generate a file into **crawler/crawler-info/crawler-exec-info** with all needed information about returned resource.
  
### Deploying Fetcher
  To deploy Fetcher, run the command:

  ```
  bash scripts/infrastructure/deploy_fetcher [private-key-path]
  ```
  
  When ran, the above command will generate a file into **fetcher/fetcher-info/fetcher-exec-info** with all needed information about returned resource.
  
## Using SEBAL Engine CLI
### Using Catalog
  To add LANDSAT images from a list of regions with first and last year, run the **add** command:
  
  ```
  bash scripts/cli/catalog add [first-year] [last-year] [regions-file-path]
  ```
  
  To get LANDSAT images from a list of regions with first and last year, run the **get** command:
  
  ```
  bash scripts/cli/catalog get [first-year] [last-year] [regions-file-path]
  ```
  
  To list corrupted LANDSAT images from **Task Catalog**, run the **list-corrupted** command:
  
  ```
  bash scripts/cli/catalog list-corrupted
  ```
  
  To list all LANDSAT images from **Task Catalog**, run the **list** command:
  
  ```
  bash scripts/cli/catalog list
  ```
  
### Using Crawler
  To start Crawler application, run the Crawler CLI command:

  ```
  bash scripts/cli/crawler [task-catalog-ip] [task-catalog-port] [federation-member]
  ```
  
### Using Scheduler
  To start Scheduler application, run the Scheduler CLI command:
  
  ```
  bash scripts/cli/scheduler [task-catalog-ip] [task-catalog-port] [nfs-server-ip] [nfs-server-port]
  ```
  
### Using Fetcher
  To start Fetcher application, run the Fetcher CLI command:
  
  ```
  bash scripts/cli/crawler [task-catalog-ip] [task-catalog-port] [ftp-server-ip] [ftp-server-port]
  ```
