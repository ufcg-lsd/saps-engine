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
  
    created: 
    downloading: image is being downloaded by crawler from nasa repository
    downloaded : image is downloaded by crawler
    preprocessing:
    ready:
    running: image is ready to be processed/is being processed by worker node
    finished: image successfully processed by worker node
    archiving: 
    archived: image successfully archived
    failed: image execution returned error
  
## Install and Deploy
* [Scheduler](docs/scheduler-install.md)
* [Archiver](docs/archiver-install.md)
* [Input Downloader](docs/input-downloader-install.md)
* [Dispacher/Dashboard](docs/dispacher-install.md)
* [Pre Processor](docs/preprocessor-install.md)
  
