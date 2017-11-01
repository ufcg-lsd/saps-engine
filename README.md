# SAPS Engine
## What is SAPS Engine?
  SAPS Engine is a tool created to provide a dynamic use of SEBAL algorithm using computational resources obtained through a multi-cloud environment federated by the [Fogbow Middleware](http://www.fogbowcloud.org) (to install it, follow the instructions [here](http://www.fogbowcloud.org/the-big-picture.html)).
  
  SAPS Engine has seven main components:
  - **Submission Dispatcher**: Serves requests from a federation member, such as the creation and monitoring new work units, or the purge of processed data.
  - **Task Catalogue**: Stores information of LANDSAT image data and its execution.
  - **Input Downloader**: Search at **Task Catalogue** for LANDSAT tasks in "created" state. This component assumes a NFS Server and FTP Server role and stores image data into a repository provided by an interation with **Fogbow Middleware**.
  - **Pre Processor**: Search at **Task Catalogue** for LANDSAT tasks already downloaded by **Input Downloader** for pre processing treatment, if necessary, before it arrives to **Scheduler**.
  - **Scheduler**: Order resources as it detects whether or not are images with "preprocessed" state in **Task Catalogue**, then redirects scheduled tasks to **Worker Nodes**, which performs the task processing.
  - **Worker Node**: Receives a task from **Scheduler** and executes it. The execution consists in perform an image processing and store data in the NFS Server at the end of it.
  - **Archiver**: Search at **Task Catalogue** for tasks in "finished" (processed by a **Worker Node**) state and transfer all task data from FTP Server to a permanent storage. After that, **Input Downloader** is able to detect if the task was archived, so it can remove all task files from its own local repository.

## Install and Deploy
### Install Docker CE
SAPS componentes are deployed as Docker containers. Thus, before proper installing them, Docker needs to be installed in the virtual machines provisioned to run the SAPS service. 

To install Docker in a Debian based virtual machine follow the instructions provided [here](docs/container-install.md).

Once Docker is installed, the SAPS components can be deployed by pulling the container images available in the service’s repository. In the following, we show how this can be done, for each SAPS component, as well as the necessary customizations made for these components.

### SAPS Components Installation
* [Dispacher/Dashboard](docs/dispacher-install.md)
* [Input Downloader](docs/input-downloader-install.md)
* [Pre Processor](docs/preprocessor-install.md)
* [Scheduler](docs/scheduler-install.md)
* [Archiver](docs/archiver-install.md)
  
