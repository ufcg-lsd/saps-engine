# Install and Configure Scheduler

The SAPS Scheduler component is responsible for selecting tasks (to this end, it interacts with the SAPS Catalog component) to be processed by the Workers managed by the Arrebol Service.
  
## Dependencies

In an apt-based Linux distro, type the below commands to install the Scheduler dependencies.

```bash
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install maven
sudo apt-get -y install git
```

In addition to the installation of the above Linux packages, the Scheduler source code should be fetched from its repository and compiled. This could be done following the below steps:

```bash
# saps-engine repository
git clone https://github.com/ufcg-lsd/saps-engine
cd saps-engine
git checkout develop
mvn install -Dmaven.test.skip=true
```

## Configure

Edit the files:
- [Scheduler configuration file](/config/scheduler.conf) to allow its comunication with the SAPS Catalog and Arrebol Service. This configuration file also customizes the behaviour of the Scheduler, including the frequency that the Scheduler tries to select new task to be submitted.
- [SAPS Scripts](/resources/execution_script_tags.json) to allow mapping the versions of the scripts with their respective Docker information (tag and repository) to be used in processing. For example:
    
```json
{
  "processing":[
    {
      "name": "$name_of_version_1_of_the_processing_step",
      "docker_tag": "$docker_tag_of_version_1_of_the_processing_step",
      "docker_repository": "$docker_repository_of_version_1_of_the_processing_step"
    },
    {
      "name": "$name_of_version_2_of_the_processing_step",
      "docker_tag": "$docker_tag_of_version_2_of_the_processing_step",
      "docker_repository": "$docker_tag_of_version_2_of_the_processing_step"
    }
  ],
  "preprocessing":[
    {
      "name": "$name_of_version_1_of_the_preprocessing_step",
      "docker_tag": "$docker_tag_of_version_1_of_the_preprocessing_step",
      "docker_repository": "$docker_repository_of_version_1_of_the_preprocessing_step"
    }
  ],
  "inputdownloading":[
    {
      "name": "$name_of_version_1_of_the_inputdownloading_step",
      "docker_tag": "$docker_tag_of_version_1_of_the_inputdownloading_step",
      "docker_repository": "$docker_repository_of_version_1_of_the_inputdownloading_step"
    }
  ]
}
```

## Run

Once the configuration file is edited, the below commands are used to start and stop the Scheduler component.

```bash
# Start command
bash bin/start-scheduler
```

```bash
# Stop command
bash bin/stop-scheduler
```
