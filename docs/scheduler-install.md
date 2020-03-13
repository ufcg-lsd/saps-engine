# Install and Configure Scheduler

The SAPS Scheduler component is responsible for selecting tasks (interacting with the SAPS Catalog service) and scheduling them for processing which is divided into [three steps](https://github.com/ufcg-lsd/saps-scripts-template):

1. Inputdownloading step: downloads the input data for later processing.
2. Preprocessing step: pre-processes the data downloaded by the InputDownload phase
3. Processing step: processes the data on the previous steps (Inputdownloading and preprocessing)

For each step mentioned above, there are a set of versions of scripts that will accomplish their purposes in the data of the designated task, however there must be compatibility between the versions of the subsequent steps with their predecessors. Here are some versions of the UFCG SAPS scripts:
-  [UFCG SAPS scripts for Inputdownloading step](https://github.com/ufcg-lsd/saps-scripts-inputdownload)
-  [UFCG SAPS scripts for Preprocessing step](https://github.com/ufcg-lsd/saps-scripts-preprocessing)
-  [UFCG SAPS scripts for Processing step](https://github.com/ufcg-lsd/saps-scripts-processing)
  
## Dependencies

In an apt-based Linux distro, type the below commands to install the Scheduler dependencies.

```bash
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install maven
sudo apt-get -y install git
```

In addition to above Linux packages, the Scheduler also depends own ```saps-engine``` repository (which holds the Scheduler code). To fetch and compile the source code of saps-engine repository, follow the below steps:

```bash
# saps-engine repository
git clone https://github.com/ufcg-lsd/saps-engine
cd saps-engine
git checkout develop
mvn install -Dmaven.test.skip=true
```

## Configure

The Scheduler configuration (made via the [scheduler.conf](/config/scheduler.conf) file) customize this component to interact with other components, including the SAPS Catalog and Arrebol Service.

## Run
Once the configuration file is customized, below command are used to start and stop the Scheduler component.

```bash
# Start command
bash bin/start-scheduler
```

```bash
# Stop command
bash bin/stop-scheduler
```

## Test
To test whether the Scheduler component is running, follow the step below:

```bash
ps xau | grep java | grep Scheduler
```

Expected result:

```bash
root      [PID]  0.0  0.0  51416  3852 ?        S    Jan24   0:00 [sudo] java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -Dlog4j.configuration=file:$saps_engine_dir_path/config/log4j.properties -Djava.library.path=/usr/local/lib -cp $saps_engine_dir_path/target/saps-engine-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.saps.engine.core.scheduler.SchedulerMain $saps_engine_dir_path/config/scheduler.conf
```