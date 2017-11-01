# SAPS Engine
## What is SAPS Engine?
  SAPS Engine is a tool created to provide a dynamic use of SEBAL algorithm using computational resources obtained through a multi-cloud environment federated by the [Fogbow Middleware](http://www.fogbowcloud.org).
  
  SAPS Engine has six main components:
  - **Submission Dispatcher**: Serves requests from a federation member, such as the creation and monitoring new work units, or the purge of processed data.
  - **Task Catalogue**: Stores information of LANDSAT image data (obtained from [NASA repository](https://ers.cr.usgs.gov)) and its execution.
  - **Input Download**: Search at **Task Catalogue** for LANDSAT tasks in "created" state. This component assumes a NFS Server and FTP Server role and stores image data into a repository provided by an interation with **Fogbow Middleware**.
  - **Pre Processor**: Search at **Task Catalogue** for LANDSAT tasks already downloaded by **Input Downloader** for pre processing treatment, if necessary, before it arrives to **Scheduler**.
  - **Scheduler**: Order resources as it detects whether or not are images with "preprocessed" state in **Task Catalogue**, then redirects scheduled tasks to **Worker Nodes**, which performs the task processing.
  - **Worker Node**: Receives a task from **Scheduler** and executes it. The execution consists in perform an image processing and store data in the NFS Server at the end of it.
  - **Archiver**: Search at **Task Catalogue** for tasks in "finished" (processed by a **Worker Node**) state and transfer all task data from FTP Server to a permanent storage. After that, **Input Downloader** is able to detect if the task was archived, so it can remove all task files from its own local repository.

## How to use it?
### Submitting Tasks
  A submission, which consists in a pool of tasks, is created when **Task Catalogue** is called passing two points in the world map for image search, which are the upper right latitude, upper right longitude, lower left latitude and lower left longitude, a range of years for which the images were captured by the LANDSAT series and the chosen version for: **Input Downloader**, **Pre Processor** and **Worker Node**.
  
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
### Install Docker CE
SAPS componentes are deployed as Docker containers. Thus, before proper installing them, Docker needs to be installed in the virtual machines provisioned to run the SAPS service. 

To install Docker in a Debian based virtual machine follow the instructions provided [here](docs/container-install.md).

Once Docker is installed, the SAPS components can be deployed by pulling the container images available in the service’s repository. In the following, we show how this can be done, for each SAPS component, as well as the necessary customizations made for these components.

### SAPS Components Installation
* [Scheduler](docs/scheduler-install.md)
* [Archiver](docs/archiver-install.md)
* [Input Downloader](docs/input-downloader-install.md)
* [Dispacher/Dashboard](docs/dispacher-install.md)
* [Pre Processor](docs/preprocessor-install.md)
  
