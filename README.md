# SEBAL Engine
## What is SEBAL Engine?
  SEBAL Engine is a tool created to provide a dynamic use of SEBAL algorithm using computational resources obtained through a multi-cloud environment federated by the [Fogbow Middleware](http://www.fogbowcloud.org).
  
  SEBAL Engine has five main components:
  - **Centralized Database**: Stores image data information obtained from [NASA repository](https://ers.cr.usgs.gov).
  - **Crawler**: Search at **Centralized Database** for images in "not downloaded" state. This component assumes a NFS Server and FTP Server role and stores image data into a repository provided by an interation with **Fogbow Middleware**.
  - **Scheduler**: Order resources as it detects whether or not are images with "downloaded" state in **Centralized Database**, then redirects SEBAL execution tasks to **Worker Nodes**, which performs the image processing.
  - **Worker Node**: Receives a task from **Scheduler** and executes it. The execution consists of perform a image processing and store data in the NFS Server.
  - **Fetcher**: Search at **Centralized Database** for images in "finished" (processed by a **Worker Node**) state and transfer all output data from the FTP Server to a Swift. After that, **Crawler** is able to detect if the image was fetched, so it can remove all results from repository.

## How to use it?
  This section will contain all info about how to use SEBAL Engine, such as infrastructure deployment, database filling and application launch.
## Configuring SEBAL Engine Deploy
  This section will contain all info about how to deploy SEBAL Engine infrastructure, providing configuration help step-by-step.
## Using SEBAL Engine CLI
  This section will contain all info about SEBAL Engine CLI and how to use it, providing configuration help step-by-step.
