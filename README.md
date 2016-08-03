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
## Deploying Infrastructure
### Deploying Task Catalog and Scheduler
  To deploy Task Catalog and Scheduler, run the command:
  
  ```bash scripts/infrastructure/deploy_scheduler [private-key-path] [storage-size]```
  
### Deploying Crawler
  To deploy Crawler, run the command:

  ```bash scripts/infrastructure/deploy_crawler [private-key-path] [storage-size]```
  
### Deploying Fetcher
  To deploy Fetcher, run the command:

  ```bash scripts/infrastructure/deploy_fetcher [private-key-path]```
  
## Using SEBAL Engine CLI
### Using Catalog
  To add LANDSAT images from a list of regions with a first and last year, run the **add** command:
  
  ```bash scripts/cli/catalog add [first-year] [last-year] [regions-file-path]```

### Using Crawler
  To start Crawler application, run the Crawler CLI command:

  ```bash scripts/cli/crawler [task-catalog-ip] [task-catalog-port] [federation-member]```
  
### Using Scheduler
  To start Scheduler application, run the Scheduler CLI command:
  
  ```bash scripts/cli/scheduler [task-catalog-ip] [task-catalog-port] [nfs-server-ip] [nfs-server-port]```
  
### Using Fetcher
  To start Fetcher application, run the Fetcher CLI command:
  
  ```bash scripts/cli/crawler [task-catalog-ip] [task-catalog-port] [ftp-server-ip] [ftp-server-port]```
