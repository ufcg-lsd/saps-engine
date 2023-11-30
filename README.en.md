# SAPS

-   Requirements: Ubuntu 16.04 or 18.04

# SAPS components

1.  [Common](#common)
2.  [Catalog](#catalog)
3.  [Archive](#archiver)
4.  [Dispatcher](#dispatcher)
5.  [Scheduler](#scheduler)
6.  [Dashboard](#dashboard)
7.  [Arbol](#arrebol)
    1.  [Clean Option](#clean-option)
    2.  [Container Option](#container-option)
8.  [Workers](#workers)
    1.  [Raw Option](#raw-option)
    2.  [Ansible Option](#ansible-option)
9.  [NOP tests](#testes-nop)
10. [Attaching volume to nfs](#atachando-volume)
11. [Crontab](#crontab)
12. [Logrotate](#logrotate)

* * *

## [Common](https://github.com/ufcg-lsd/saps-common)

-   This repository is needed to:
    1.  [Catalog](#catalog)
    2.  [Archive](#archiver)
    3.  [Dispatcher](#dispatcher)
    4.  [Scheduler](#scheduler)

### Installation:

1.  Install a JDK, Maven, Git
        sudo apt-get update
        sudo apt-get -y install openjdk-8-jdk
        sudo apt-get -y install maven
        sudo apt-get -y install git
2.  Clone and install dependencies
        git clone https://github.com/ufcg-lsd/saps-common ~/saps-common
        cd ~/saps-common
        sudo mvn install

* * *

## [Catalog](https://github.com/ufcg-lsd/saps-catalog)

### Variables to be defined:

-   $catalog_user=catalog_user
-   $catalog passwd=catalog passwd
-   $catalog_db_name=catalog_db_name
-   $installed_version= Check your PostgreSQL version

### Installation:

1.  Configure the[saps-common](#common)

2.  Clone and install dependencies
        git clone https://github.com/ufcg-lsd/saps-catalog ~/saps-catalog
        cd ~/saps-catalog
        sudo mvn install

3.  Install postgres
        sudo apt-get install -y postgresql

4.  Install pip and pandas
        sudo apt install python3-pip
        pip3 install pandas
        pip3 install tqdm

5.  Configure o Catalog
        sudo su postgres
        export catalog_user=catalog_user
        export catalog_passwd=catalog_passwd
        export catalog_db_name=catalog_db_name
        psql -c "CREATE USER $catalog_user WITH PASSWORD '$catalog_passwd';"
        psql -c "CREATE DATABASE $catalog_db_name OWNER $catalog_user;"
        psql -c "GRANT ALL PRIVILEGES ON DATABASE $catalog_db_name TO $catalog_user;"
        exit

6.  Add password as a requirement for access to PostgreSQL
    -   This step will make any user registered with postgresql need a password to access the database
            sudo su
            export installed_version=`ls /etc/postgresql`
            sed -i 's/peer/md5/g' /etc/postgresql/$installed_version/main/pg_hba.conf
            echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/$installed_version/main/pg_hba.conf
            sed -i "$ a\listen_addresses = '*'" /etc/postgresql/$installed_version/main/postgresql.conf
            service postgresql restart
            exit

7.  Test Catalog access
        psql -h <catalog_ip_address> -p 5432 $catalog_db_name $catalog_user
    -   Example:
            psql -h localhost -p 5432 catalog_db_name catalog_user

8.  Configure the[Dispatcher](#dispatcher)
        To run the landsat script, the dispatcher must be running.

### Settings:

-   Execute o script**/scripts/fetch_landsat_data.sh**(it takes a while)
        cd ~/saps-catalog/scripts
        sudo bash fetch_landsat_data.sh

* * *

## [Archive](https://github.com/ufcg-lsd/saps-archiver)

### Variables to be defined:

-   $nfs_server_folder_path=/nfs

### Installation:

1.  Configure the[saps-common](#common)
2.  Install dependencies from[saps-catalog](#catalog)
        git clone https://github.com/ufcg-lsd/saps-catalog ~/temp/saps-catalog
        cd ~/temp/saps-catalog
        sudo mvn install 
        cd -
        sudo rm -rf ~/temp/saps-catalog
        sudo rm -d ~/temp/
3.  Clone and install dependencies
        git clone https://github.com/ufcg-lsd/saps-archiver ~/saps-archiver
        cd ~/saps-archiver
        sudo mvn install
4.  Create and configure a shared folder to store files from each processing step

-   **OBS:**If you have limited storage space, you can[attach a volume to nfs](#atachando-volume)

-   NFS (Option 1)

    -   Setting
            sudo su
            apt-get install -y nfs-kernel-server
            export nfs_server_folder_path=/nfs
            mkdir -p $nfs_server_folder_path
            echo $nfs_server_folder_path '*(rw,insecure,no_subtree_check,async,no_root_squash)' >> /etc/exports 
            exportfs -arvf
            service nfs-kernel-server enable
            service nfs-kernel-server restart
            exit
    -   Testing
            showmount -e localhost

-   SWIFT (Option 2)
        TODO

1.  Configure Apache (necessary to access data via email)

-   (It would be cool to migrate this to nginx)

-   Install Apache
        sudo apt-get install -y apache2

-   Modify the sites-available/default-ssl.conf file
        sudo vim /etc/apache2/sites-available/default-ssl.conf
    -   Change the DocumentRoot to the nfs directory (default = /nfs)
            DocumentRoot $nfs_server_folder_path 
            # Exemplo: DocumentRoot /nfs

-   Modify the sites-available/000-default.conf file
        sudo vim /etc/apache2/sites-available/000-default.conf
    -   Change the DocumentRoot and add the lines in sequence
            DocumentRoot $nfs_server_folder_path 
                  # Exemplo: DocumentRoot /nfs
                          Options +Indexes
                          <Directory $nfs_server_folder_path>
                          # Exemplo: <Directory /nfs>
                                  Options Indexes FollowSymLinks
                                  AllowOverride None
                                  Require all granted
                          </Directory>

-   Modify the sites-available/000-default.conf file
        sudo vim /etc/apache2/apache2.conf
    -   Change FilesMatch
                  <FilesMatch ".+\.(txt|TXT|nc|NC|tif|TIF|tiff|TIFF|csv|CSV|log|LOG|metadata)$">
                          ForceType application/octet-stream
                          Header set Content-Disposition attachment
                  </FilesMatch>

-   After configuring the files, run:
        sudo a2enmod headers
        sudo service apache2 restart

### Settings:

Configure the /config/archiver.conf file according to the other components

-   Example (nfs):[archiver.conf](./confs/archiver/clean/archiver.conf)

### Execution:

-   Running archiver
        bash bin/start-service

-   Stopping archiver
        bash bin/stop-service

* * *

## [Dispatcher](https://github.com/ufcg-lsd/saps-dispatcher)

### Installation:

1.  Configure the [saps-common](#common)

2.  Install the dependencies from[saps-archiver](#archiver)
        git clone https://github.com/ufcg-lsd/saps-archiver ~/temp/saps-archiver
        cd ~/temp/saps-archiver
        sudo mvn install 
        cd -
        sudo rm -rf ~/temp/saps-archiver
        sudo rm -d ~/temp/

3.  Install the dependencies[saps-catalog](#catalog)
        git clone https://github.com/ufcg-lsd/saps-catalog ~/temp/saps-catalog
        cd ~/temp/saps-catalog
        sudo mvn install 
        cd -
        sudo rm -rf ~/temp/saps-catalog
        sudo rm -d ~/temp/

4.  Clone and install dependencies
        git clone https://github.com/ufcg-lsd/saps-dispatcher ~/saps-dispatcher
        cd ~/saps-dispatcher
        sudo mvn install

5.  Install python script dependencies (get_wrs.py)
        sudo apt-get install -y python-gdal
        sudo apt-get install -y python-shapely
        sudo apt-get -y install curl jq sed

### Settings:

Configure the file**/config/dispatcher.conf**according to the other components

-   Example (nfs):[dispatcher.conf](./confs/dispatcher/clean/dispatcher.conf)

### Execution:

-   Running dispatcher
        bash bin/start-service

-   Stopping dispatcher
        bash bin/stop-service

* * *

## [Scheduler](https://github.com/ufcg-lsd/saps-scheduler)

### Installation:

1.  Configure the[saps-common](#common)
2.  Install the dependencies[saps-catalog](#catalog)
        git clone https://github.com/ufcg-lsd/saps-catalog ~/temp/saps-catalog
        cd ~/temp/saps-catalog
        sudo mvn install 
        cd -
        sudo rm -rf ~/temp/saps-catalog
        sudo rm -d ~/temp/
3.  Clone and install dependencies
        git clone https://github.com/ufcg-lsd/saps-scheduler ~/saps-scheduler
        cd ~/saps-scheduler
        sudo mvn install

### Settings:

Configure the file**/config/scheduler.conf**according to the other components

-   Example (nfs):[scheduler.conf](./confs/scheduler/clean/scheduler.conf)

### Execution:

-   Before execution it is necessary to install and configure the[Tree](#arrebol)

-   Running scheduler
        bash bin/start-service

-   Stopping scheduler
        bash bin/stop-service

* * *

## [Dashboard](https://github.com/ufcg-lsd/saps-dashboard)

## Settings

To run the project correctly, follow the steps below:

### 1. Changing the Dashboard Port

If necessary, you can change the port on which the Dashboard will run (default = 3000). Below is an example of how to set the port to 8081:

1.  Navigate to the file`saps-dashboard/package.json`.
2.  Locate the scripts part and change the variable`dev`for:

```json
"dev": "next dev -p 8081"
```

### 2. Generating the Token for the Map

1.  Access[Mapbox](https://www.mapbox.com).
2.  Create an account or log in.
3.  Generate a new token to consume the API.
4.  Copies they token and cole no field`<Seu Token Aqui>`in the file`.env.local`.

### 3. Configuring Environment Variables

Create a file called`.env.local`in the project root and add the following lines:

    NEXT_PUBLIC_API_URL=<IP:Porta do Dispatcher>
    NEXT_PUBLIC_MAP_API_KEY=<Seu Token Aqui>

### 4. Downloading required dependency

install AXIOS so that requests can be made

    npm install axios

## Running the Project

After completing the configuration steps, you can run the project with the commands below:

### Using npm:

```bash
npm run dev
```

* * *

## [Arbol](https://github.com/ufcg-lsd/arrebol)

### **_Clean Option_**

### Variables to be defined:

-   $arrebol_db_passwd=@rrebol
-   $arrebol_db_name=arrebol
-   $arrebol_db_user=arrebol_db_user

### Observation:

To avoid conflicts in the Arrebol configuration, it is important that the**Arbol**database and**Catalog**are stored in different instances. If it is unfeasible to store them in separate instances, an alternative is to create a single database for both, ensuring that the data from each system is clearly separated. However, this option requires care to avoid conflicts in data access.

### Installation:

1.  Install the JDK, Maven and Git
        sudo apt-get update
        sudo apt-get -y install openjdk-8-jdk
        sudo apt-get -y install maven
        sudo apt-get -y install git
        sudo apt-get install -y postgresql

2.  Clone and install dependencies
        git clone -b develop https://github.com/cilasmarques/arrebol ~/arrebol
        cd ~/arrebol
        sudo mvn install

3.  Configure the afterglow DB
        sudo su postgres
        export arrebol_db_user=arrebol_db_user
        export arrebol_db_passwd=@rrebol
        export arrebol_db_name=arrebol
        psql -c "CREATE USER $arrebol_db_user WITH PASSWORD '$arrebol_db_passwd';"
        psql -c "CREATE DATABASE $arrebol_db_name OWNER $arrebol_db_user;"
        psql -c "ALTER USER $arrebol_db_user PASSWORD '$arrebol_db_passwd';"
        exit

4.  Test access to the arrebol database
        psql -h <arrebol_ip_address> -p 5432 $arrebol_db_name arrebol_db_user
    -   Example:
            psql -h localhost -p 5432 arrebol arrebol_db_user

### Settings:

Configure the files**src/main/resources/application.properties**e**src/main/resources/arrebol.json**according to the other components

-   Example:[application.properties](./confs/arrebol/clean/application.properties)
-   Example:[arrebol.json](./confs/arrebol/clean/arrebol.json)

### Before running, configure the afterglow workers

-   This configuration must be done in**same machine that will perform the afterglow**.
-   To configure the worker, follow these[steps](#workers)

### Execution:

-   Performing arrebol
        sudo bash bin/start-service.sh

-   Stopping arrebol
        sudo bash bin/stop-service.sh

### Configuration of arrebol_db tables

1.  After executing the arrebol, the tables are created in the database, so you need to add the following constraints

        psql -h localhost -p 5432 arrebol arrebol_db_user
        ALTER TABLE task_spec_commands DROP CONSTRAINT fk7j4vqu34tq49sh0hltl02wtlv;
        ALTER TABLE task_spec_commands ADD CONSTRAINT commands_id_fk FOREIGN KEY (commands_id) REFERENCES command(id) ON DELETE CASCADE;

        ALTER TABLE task_spec_commands DROP CONSTRAINT fk9y8pgyqjodor03p8983w1mwnq;
        ALTER TABLE task_spec_commands ADD CONSTRAINT task_spec_id_fk FOREIGN KEY (task_spec_id) REFERENCES task_spec(id) ON DELETE CASCADE;

        ALTER TABLE task_spec_requirements DROP CONSTRAINT fkrxke07njv364ypn1i8b2p6grm;
        ALTER TABLE task_spec_requirements ADD CONSTRAINT task_spec_id_fk FOREIGN KEY (task_spec_id) REFERENCES task_spec(id) ON DELETE CASCADE;

        ALTER TABLE task DROP CONSTRAINT fk303yjlm5m2en8gknk80nkd27p; 
        ALTER TABLE task ADD CONSTRAINT task_spec_id_fk FOREIGN KEY (task_spec_id) REFERENCES task_spec(id) ON DELETE CASCADE;

### Check

-   Request
        curl http://<arrebol_ip>:8080/queues/default
-   Expected response
        {"id":"default","name":"Default Queue","waiting_jobs":0,"worker_pools":1,"pools_size":5}

* * *

### **_Container Option_**

### Installation:

1.  Clone the repository
        git clone -b develop https://github.com/cilasmarques/arrebol ~/arrebol
2.  Install docker dependencies
        cd arrebol/deploy
        sudo bash setup.sh

### Settings:

Configure folder files**deploy/config/**according to the other components

-   Example:[postgres.env](./confs/arrebol/container/postgres.env)
-   Example:[pgadmin.env](./confs/arrebol/container/pgadmin.env)
-   Example:[application.properties](./confs/arrebol/container/application.properties)
-   Example:[arrebol.json](./confs/arrebol/container/arrebol.json)
-   Example:[init.sql](./confs/arrebol/container/init.sql)

### Before running, configure the afterglow workers

-   This configuration must be done in**same machine that will run**the arrebol\*\*.
-   To configure the worker, follow these[steps](#workers)

### Execution:

-   Performing aarrebol
        sudo bash deploy.sh start

-   Stopping afterglow
        sudo bash deploy.sh stop

### Check

-   Request
        curl http://<arrebol_ip>:8080/queues/default

-   Expected response
        {"id":"default","name":"Default Queue","waiting_jobs":0,"worker_pools":1,"pools_size":5}

* * *

## Workers

### **Raw Option**

### Settings:

1.  Install Docker
        sudo apt-get update
        sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
        sudo apt-key fingerprint 0EBFCD88
        sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
        sudo apt-get update
        sudo apt-get install -y docker-ce docker-ce-cli containerd.io

### Configuring worker for afterglow

-   Modify the /lib/systemd/system/docker.service file
        sudo vim /lib/systemd/system/docker.service
    -   Change the ExecStart and replace with the following lines
            ExecStart=/usr/bin/dockerd -H fd:// -H=tcp://0.0.0.0:5555 --containerd=/run/containerd/containerd.sock

-   Restart the docker daemon
        sudo systemctl daemon-reload
        sudo service docker restart

-   Face pull images docker

        sudo docker pull fogbow/inputdownloader:googleapis
        sudo docker pull fogbow/preprocessor:default
        sudo docker pull fogbow/worker:ufcg-sebal

        sudo docker pull fogbow/inputdownloader:usgsapis
        sudo docker pull fogbow/preprocessor:legacy
        sudo docker pull fogbow/worker:sebkc-sebal
        sudo docker pull fogbow/worker:sebkc-tseb

### Check

-   Request
        curl http://<worker_ip>:5555/version
-   Expected response
              {
                "Platform": {
                  "Name": "Docker Engine - Community"
                },
                "Components": [
                  {
                    "Name": "Engine",
                    "Version": "19.03.7",
                    "Details": {
                      "ApiVersion": "1.40",
                      "Arch": "amd64",
                      "BuildTime": "2020-03-04T01:21:08.000000000+00:00",
                      "Experimental": "false",
                      "GitCommit": "7141c199a2",
                      "GoVersion": "go1.12.17",
                      "KernelVersion": "4.15.0-88-generic",
                      "MinAPIVersion": "1.12",
                      "Os": "linux"
                    }
                  },
                  {
                    "Name": "containerd",
                    "Version": "1.2.13",
                    "Details": {
                      "GitCommit": "7ad184331fa3e55e52b890ea95e65ba581ae3429"
                    }
                  },
                  {
                    "Name": "runc",
                    "Version": "1.0.0-rc10",
                    "Details": {
                      "GitCommit": "dc9208a3303feef5b3839f4323d9beb36df0a9dd"
                    }
                  },
                  {
                    "Name": "docker-init",
                    "Version": "0.18.0",
                    "Details": {
                      "GitCommit": "fec3683"
                    }
                  }
                ],
                "Version": "19.03.7",
                "ApiVersion": "1.40",
                "MinAPIVersion": "1.12",
                "GitCommit": "7141c199a2",
                "GoVersion": "go1.12.17",
                "Os": "linux",
                "Arch": "amd64",
                "KernelVersion": "4.15.0-88-generic",
                "BuildTime": "2020-03-04T01:21:08.000000000+00:00"
              }

* * *

### **Ansible Option**

### Settings:

Configure folder files**/worker/deploy/hosts.conf**according to the other components

-   Example:

        # worker_ip[anything]
        worker_ip_1=10.11.19.104

        remote_user=ubuntu

        # The NFS Server Address
        nfs_server=10.11.19.80

        # The NFS Server directory to mount
        nfs_server_dir=/nfs

        # Required (if not specified, ansible will use the host ssh keys)
        ansible_ssh_private_key_file=/home/ubuntu/keys/saps22

### Installation:

    ```
    cd ~/arrebol/worker/deploy/
    sudo bash install.sh
    ```

### Check

-   Request
        curl http://<worker_ip>:5555/version
-   Expected response
              {
                "Platform": {
                  "Name": "Docker Engine - Community"
                },
                "Components": [
                  {
                    "Name": "Engine",
                    "Version": "19.03.7",
                    "Details": {
                      "ApiVersion": "1.40",
                      "Arch": "amd64",
                      "BuildTime": "2020-03-04T01:21:08.000000000+00:00",
                      "Experimental": "false",
                      "GitCommit": "7141c199a2",
                      "GoVersion": "go1.12.17",
                      "KernelVersion": "4.15.0-88-generic",
                      "MinAPIVersion": "1.12",
                      "Os": "linux"
                    }
                  },
                  {
                    "Name": "containerd",
                    "Version": "1.2.13",
                    "Details": {
                      "GitCommit": "7ad184331fa3e55e52b890ea95e65ba581ae3429"
                    }
                  },
                  {
                    "Name": "runc",
                    "Version": "1.0.0-rc10",
                    "Details": {
                      "GitCommit": "dc9208a3303feef5b3839f4323d9beb36df0a9dd"
                    }
                  },
                  {
                    "Name": "docker-init",
                    "Version": "0.18.0",
                    "Details": {
                      "GitCommit": "fec3683"
                    }
                  }
                ],
                "Version": "19.03.7",
                "ApiVersion": "1.40",
                "MinAPIVersion": "1.12",
                "GitCommit": "7141c199a2",
                "GoVersion": "go1.12.17",
                "Os": "linux",
                "Arch": "amd64",
                "KernelVersion": "4.15.0-88-generic",
                "BuildTime": "2020-03-04T01:21:08.000000000+00:00"
              }

* * *

## NOP tests

-   To test the system, you can use the NOP tests
-   There are several variations of the test and you can choose which one to run using the file:**saps-quality-assurance/start-systemtest**

### Add NOP test tags to the configurations of the following components

-   [Tags two tests nop](./confs/NOPTests/nopTestTags.json)

1.  [Dashboard](#-dashboard)
    -   File:[dashboardApp.js](https://github.com/ufcg-lsd/saps-dashboard/blob/develop/public/dashboardApp.js)(line 10~49)
2.  [Dispatcher](#dispatcher)
    -   File:[execution_script_tags.json](https://github.com/ufcg-lsd/saps-dispatcher/blob/develop/resources/execution_script_tags.json)
3.  [Scheduler](#scheduler)
    -   File:[execution_script_tags.json](https://github.com/ufcg-lsd/saps-scheduler/blob/develop/resources/execution_script_tags.json)

Once added, run the build again in the Scheduler and Dispatcher.

    ```
    sudo mvn install
    ```

### Clone the saps-quality-assurance repository

    git clone https://github.com/ufcg-lsd/saps-quality-assurance ~/saps-quality-assurance
    cd ~/saps-quality-assurance

### Run the tests

-   Command:
        sudo bash start-systemtest <admin_email> <admin_password> <dispatcher_ip_addrres> <submission_rest_server_port>
    -   Example:
            sudo bash start-systemtest dispatcher_admin_email dispatcher_admin_password 127.0.0.1:8091

* * *

## He will turn down the volume

-   Attaching a volume is useful for expanding the storage space of a folder (like an external hard drive)

### Volume less than 2TB

1.  Create a partition on the volume
    -   Command:`fdisk <volume>`
    -   Example:`fdisk /dev/sdb`
2.  Check if the partition was made
    -   Command:`lsblk`
3.  Set a formatting type for the partition
    -   Command:`mkfs --type <formato> <particao>`
    -   Example:` mkfs --type ext4 /dev/sdb1`
4.  Mount the partition to a directory:
    -   Command:`mount <particao> <diretorio>`
    -   Example:`mount /dev/sdb1 /nfs`

### Volume greater than 2TB

1.  Create a partition on the volume
    -   Command:`parted <volume>`
    -   Example:`parted /dev/sdb`
2.  Create a label
    -   Command:`mklabel gpt`
3.  Set the storage unit
    -   Command:`unit TB`
4.  Informs the storage size
    -   Command:`mkpart primary <init>TB <limit>TB`
    -   Example:`mkpart primary 0.00TB 2.00TB`
5.  Check if the partition was made and save
    -   Command:`print`
    -   Command:`quit`
6.  Set a formatting type for the partition
    -   Command:`sudo mkfs.ext4 <volume>`
    -   Command:`sudo mkfs.ext4 /dev/sdb1`
7.  Mount the partition to a directory:
    -   Command:`mount <particao> <diretorio>`
    -   Example:`mount /dev/sdb1 /nfs`

* * *

## [Crontab]

-   catalog -> summary script crontab
        0 0 1,15 * * sudo bash /home/ubuntu/saps-catalog/scripts/fetch_landsat_data.sh
        0 0 * * * bash /home/ubuntu/saps-catalog/scripts/build_tasks_overview.sh

-   archiver -> archived-dirs-count script crontab
        * * */1 * * bash /home/ubuntu/saps-archiver/scripts/build_archiver_overview.sh

-   dispatcher -> access script crontab + manel summarization scripts
        59 23 * * * sudo bash /home/ubuntu/saps-dispatcher/scripts/login_counter.sh
        0 0 * * * sudo /bin/bash ~/saps-dispatcher/stats/stats_archived.sh > ~/saps-dispatcher/scripts/summary.csv 
        0 0 * * * sudo /bin/bash ~/saps-dispatcher/stats/logins_accumulator.sh >> ~/saps-dispatcher/scripts/summary.csv
        0 0 * * * sudo python3 ~/saps-dispatcher/stats/stats_tasks_raw_data.py

-   arrebol -> database cleanup script crontab
        0 0 * * *   sudo bash /home/ubuntu/arrebol/bin/db_cleaner.sh

-   workers -> crontab of unfinished containers
        0 0 * * *  sudo docker ps -aq | sudo xargs docker stop | sudo xargs docker rm
