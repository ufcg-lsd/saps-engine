# SAPS
* Requisitos: Ubuntu 16.04 ou 18.04


# Componentes SAPS
1. [Common](#common)
1. [Catalog](#catalog)
1. [Archiver](#archiver)
1. [Dispatcher](#dispatcher)
1. [Scheduler](#scheduler)
1. [Dashboard](#dashboard)
1. [Arrebol](#arrebol)
    1. [Clean Option](#clean-option)
    1. [Container Option](#container-option)
1. [Workers](#workers)
    1. [Raw Option](#raw-option)
    1. [Ansible Option](#ansible-option)
1. [Testes NOP](#testes-nop)
1. [Atachando volume ao nfs](#atachando-volume)
1. [Crontab](#crontab)
1. [Logrotate](#logrotate)

-------------------------------------------------------------------
## [Common](https://github.com/ufcg-lsd/saps-common)
* Esse repositório é necessário para:
    1. [Catalog](#catalog)
    2. [Archiver](#archiver)
    3. [Dispatcher](#dispatcher)
    4. [Scheduler](#scheduler)

### Instalação:
1. Instale o JDK, Maven, Git
    ```
    sudo apt-get update
    sudo apt-get -y install openjdk-8-jdk
    sudo apt-get -y install maven
    sudo apt-get -y install git
    ```
2. Clone e instale as dependencias
    ```
    git clone https://github.com/ufcg-lsd/saps-common ~/saps-common
    cd ~/saps-common
    sudo mvn install 
    ```

-------------------------------------------------------------------
## [Catalog](https://github.com/ufcg-lsd/saps-catalog)
### Variaveis a serem definidas:
* $catalog_user=catalog_user
* $catalog_passwd=catalog_passwd
* $catalog_db_name=catalog_db_name
* $installed_version= Verifique a sua versão do PostgreSQL 

### Instalação:
1. Configure o [saps-common](#common)
1. Clone e instale as dependencias
    ```
    git clone https://github.com/ufcg-lsd/saps-catalog ~/saps-catalog
    cd ~/saps-catalog
    sudo mvn install 
    ```
1. Instale o postgres 
    ``` 
    sudo apt-get install -y postgresql
    ```
1. Instale o pip e o pandas
    ``` 
    sudo apt install python3-pip
    pip3 install pandas
    pip3 install tqdm
    ```
1. Configure o Catalog
    ``` 
    sudo su postgres
    export catalog_user=catalog_user
    export catalog_passwd=catalog_passwd
    export catalog_db_name=catalog_db_name
    psql -c "CREATE USER $catalog_user WITH PASSWORD '$catalog_passwd';"
    psql -c "CREATE DATABASE $catalog_db_name OWNER $catalog_user;"
    psql -c "GRANT ALL PRIVILEGES ON DATABASE $catalog_db_name TO $catalog_user;"
    exit
    ```
1. Adicione a senha como exigencia para acesso ao PostgreSQL 
    * Esse passo irá fazer com que qualquer usuário cadastrado no postgresql precise de uma senha para acessar o banco de dados
    ```
    sudo su
    export installed_version=`ls /etc/postgresql`
    sed -i 's/peer/md5/g' /etc/postgresql/$installed_version/main/pg_hba.conf
    echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/$installed_version/main/pg_hba.conf
    sed -i "$ a\listen_addresses = '*'" /etc/postgresql/$installed_version/main/postgresql.conf
    service postgresql restart
    exit
    ```
1. Teste o acesso do Catalog
    ```
    psql -h <catalog_ip_address> -p 5432 $catalog_db_name $catalog_user
    ```
    * Exemplo:
        ```
        psql -h localhost -p 5432 catalog_db_name catalog_user
        ```

1. Configure o [Dispatcher](#dispatcher)
    
    Para rodar o script do landsat é necessário que o dispatcher esteja rodando.


### Configuração:
* Execute o script **/scripts/fetch_landsat_data.sh** (ele demora um pouco)
```
cd ~/saps-catalog/scripts
sudo bash fetch_landsat_data.sh
```

-------------------------------------------------------------------
## [Archiver](https://github.com/ufcg-lsd/saps-archiver)
### Variaveis a serem definidas:
* $nfs_server_folder_path=/nfs

### Instalação:
1. Configure o [saps-common](#common)
2. Instale as dependencias do [saps-catalog](#catalog)
    ```
    git clone https://github.com/ufcg-lsd/saps-catalog ~/temp/saps-catalog
    cd ~/temp/saps-catalog
    sudo mvn install 
    cd -
    sudo rm -rf ~/temp/saps-catalog
    sudo rm -d ~/temp/
    ```
3. Clone e instale as dependencias
    ```
    git clone https://github.com/ufcg-lsd/saps-archiver ~/saps-archiver
    cd ~/saps-archiver
    sudo mvn install 
    ```
4. Crie e configure uma pasta compartilhada para armazenar os arquivos de cada etapa de processamento
* **OBS:** Caso o espaço para armazenamento seja pequeno, você pode [atachar um volume ao nfs](#atachando-volume)
* NFS (Opção 1)
    * Configurando
    ```
    sudo su
    apt-get install -y nfs-kernel-server
    export nfs_server_folder_path=/nfs
    mkdir -p $nfs_server_folder_path
    echo $nfs_server_folder_path '*(rw,insecure,no_subtree_check,async,no_root_squash)' >> /etc/exports 
    exportfs -arvf
    service nfs-kernel-server enable
    service nfs-kernel-server restart
    exit
    ```

    * Testando
    ```
    showmount -e localhost
    ```

* SWIFT (Opção 2)
    ```
    TODO
    ```

5. Configure apache (necessário para acesso aos dados por email) 
* (Seria legal migrar isso pra nginx)
* Instale o apache
  ```
  sudo apt-get install -y apache2
  ```

* Modifique o arquivo sites-available/default-ssl.conf
  ```
  sudo vim /etc/apache2/sites-available/default-ssl.conf
  ```
  * Mude o DocumentRoot para o diretorio do nfs (default = /nfs)
    ```
    DocumentRoot $nfs_server_folder_path 
    # Exemplo: DocumentRoot /nfs
    ```

* Modifique o arquivo sites-available/000-default.conf
  ```
  sudo vim /etc/apache2/sites-available/000-default.conf
  ```
  * Mude o DocumentRoot e adicione as linhas em sequencia
    ```
    DocumentRoot $nfs_server_folder_path 
    # Exemplo: DocumentRoot /nfs
            Options +Indexes
            <Directory $nfs_server_folder_path>
            # Exemplo: <Directory /nfs>
                    Options Indexes FollowSymLinks
                    AllowOverride None
                    Require all granted
            </Directory>
    ```

* Modifique o arquivo sites-available/000-default.conf
  ```
  sudo vim /etc/apache2/apache2.conf
  ```
  * Mude o FilesMatch 
    ```
    <FilesMatch ".+\.(txt|TXT|nc|NC|tif|TIF|tiff|TIFF|csv|CSV|log|LOG|metadata)$">
            ForceType application/octet-stream
            Header set Content-Disposition attachment
    </FilesMatch>
    ```

* Após configurar os arquivos, execute:
  ```
  sudo a2enmod headers
  sudo service apache2 restart
  ```

### Configuração:
Configure o arquivo /config/archiver.conf de acordo com os outros componentes
* Exemplo (nfs): [archiver.conf](./confs/archiver/clean/archiver.conf) 

### Execução:
* Executando archiver
    ```
    bash bin/start-service
    ```

* Parando archiver
    ```
    bash bin/stop-service
    ```

------------------------------------------------------------------
## [Dispatcher](https://github.com/ufcg-lsd/saps-dispatcher)
### Instalação:
1. Configure o [saps-common](#common)

2.  Instale as dependencias do [saps-archiver](#archiver)
    ```
    git clone https://github.com/ufcg-lsd/saps-archiver ~/temp/saps-archiver
    cd ~/temp/saps-archiver
    sudo mvn install 
    cd -
    sudo rm -rf ~/temp/saps-archiver
    sudo rm -d ~/temp/
    ```
3. Instale as dependencias do [saps-catalog](#catalog)
    ```
    git clone https://github.com/ufcg-lsd/saps-catalog ~/temp/saps-catalog
    cd ~/temp/saps-catalog
    sudo mvn install 
    cd -
    sudo rm -rf ~/temp/saps-catalog
    sudo rm -d ~/temp/
    ```
4. Clone e instale as dependencias
    ```
    git clone https://github.com/ufcg-lsd/saps-dispatcher ~/saps-dispatcher
    cd ~/saps-dispatcher
    sudo mvn install 
    ```
5. Instale as dependências do script python (get_wrs.py)
    ```
    sudo apt-get install -y python-gdal
    sudo apt-get install -y python-shapely
    sudo apt-get -y install curl jq sed
    ```

### Configuração:
Configure o arquivo **/config/dispatcher.conf** de acordo com os outros componentes
* Exemplo (nfs): [dispatcher.conf](./confs/dispatcher/clean/dispatcher.conf) 

### Execução:
* Executando dispatcher
    ```
    bash bin/start-service
    ```

* Parando dispatcher
    ```
    bash bin/stop-service
    ```

-------------------------------------------------------------------
## [Scheduler](https://github.com/ufcg-lsd/saps-scheduler)
### Instalação:
1. Configure o [saps-common](#common)
2. Instale as dependencias do [saps-catalog](#catalog)
    ```
    git clone https://github.com/ufcg-lsd/saps-catalog ~/temp/saps-catalog
    cd ~/temp/saps-catalog
    sudo mvn install 
    cd -
    sudo rm -rf ~/temp/saps-catalog
    sudo rm -d ~/temp/
    ```
3. Clone e instale as dependencias
    ```
    git clone https://github.com/ufcg-lsd/saps-scheduler ~/saps-scheduler
    cd ~/saps-scheduler
    sudo mvn install 
    ```
### Configuração:
Configure o arquivo **/config/scheduler.conf** de acordo com os outros componentes
* Exemplo (nfs): [scheduler.conf](./confs/scheduler/clean/scheduler.conf) 


### Execução:

* Antes da execução é necessário instalar e configurar o [Arrebol](#arrebol)

* Executando scheduler
    ```
    bash bin/start-service
    ```

* Parando scheduler
    ```
    bash bin/stop-service
    ```

-------------------------------------------------------------------
## [Dashboard](https://github.com/ufcg-lsd/saps-dashboard)

## Configuração

Para rodar o projeto corretamente, siga os passos abaixo:

### 1. Alterando a Porta do Dashboard

Caso seja necessário, você pode alterar a porta na qual o Dashboard irá rodar (default = 3000). Abaixo o exemplo de como definir a porta como 8081:

1. Navegue até o arquivo `saps-dashboard/package.json`.
2. Localize a parte de scripts e altere a variável `dev` para:

```json
"dev": "next dev -p 8081"
```

### 2. Gerando o Token para o Mapa

1. Acesse [Mapbox](https://www.mapbox.com).
2. Crie uma conta ou faça login.
3. Gere um novo token para consumir a API.
4. Copie esse token e cole no campo `<Seu Token Aqui>` no arquivo `.env.local`.

### 3. Configurando Variáveis de Ambiente

Crie um arquivo chamado `.env.local` na raiz do projeto e adicione as seguintes linhas:

```
NEXT_PUBLIC_API_URL=<IP:Porta do Dispatcher>
NEXT_PUBLIC_MAP_API_KEY=<Seu Token Aqui>
```

### 4. Baixando dependência necessária

instale o AXIOS para que seja possivel realizar as requisições

```
npm install axios
```

## Executando o Projeto

Depois de concluir as etapas de configuração, você pode rodar o projeto com os comandos abaixo:

### Usando npm:
```bash
npm run dev
``````


-------------------------------------------------------------------
## [Arrebol](https://github.com/ufcg-lsd/arrebol) 
### ***Clean Option***
### Variaveis a serem definidas:
* $arrebol_db_passwd=@rrebol
* $arrebol_db_name=arrebol
* $arrebol_db_user=arrebol_db_user

### Observação:
   Para evitar conflitos na configuração do Arrebol, é importante que o banco de dados do ***Arrebol*** e do ***Catalog*** sejam armazenados em instâncias diferentes. Caso seja inviável armazená-los em instâncias separadas, uma alternativa é criar um único banco de dados para ambos, garantindo que os dados de cada sistema estejam claramente separados. Entretanto, essa opção requer cuidado para evitar conflitos no acesso aos dados.

### Instalação:
1. Instale o JDK, Maven e Git
    ```
    sudo apt-get update
    sudo apt-get -y install openjdk-8-jdk
    sudo apt-get -y install maven
    sudo apt-get -y install git
    sudo apt-get install -y postgresql
    ```
1. Clone e instale as dependencias
    ```
    git clone -b develop https://github.com/cilasmarques/arrebol ~/arrebol
    cd ~/arrebol
    sudo mvn install
    ```
1. Configure o BD do arrebol
    ``` 
    sudo su postgres
    export arrebol_db_user=arrebol_db_user
    export arrebol_db_passwd=@rrebol
    export arrebol_db_name=arrebol
    psql -c "CREATE USER $arrebol_db_user WITH PASSWORD '$arrebol_db_passwd';"
    psql -c "CREATE DATABASE $arrebol_db_name OWNER $arrebol_db_user;"
    psql -c "ALTER USER $arrebol_db_user PASSWORD '$arrebol_db_passwd';"
    exit
    ```

1. Teste o acesso do bd do arrebol
    ```
    psql -h <arrebol_ip_address> -p 5432 $arrebol_db_name arrebol_db_user
    ```
    * Exemplo:
        ```
        psql -h localhost -p 5432 arrebol arrebol_db_user
        ```


### Configuração:
Configure os arquivos **src/main/resources/application.properties** e **src/main/resources/arrebol.json** de acordo com os outros componentes
* Exemplo: [application.properties](./confs/arrebol/clean/application.properties) 
* Exemplo: [arrebol.json](./confs/arrebol/clean/arrebol.json) 

### Antes de executar, configure os workers do arrebol 
* Essa configuração deve ser feita na **mesma máquina que executará o arrebol**.
* Para configurar o worker, siga esses [passos](#workers)

### Execução:
* Executando arrebol
    ```
    sudo bash bin/start-service.sh
    ```

* Parando arrebol
    ```
    sudo bash bin/stop-service.sh
    ```

### Configuração das tabelas do arrebol_db
1. Após a execução do arrebol, são criadas as tabelas no bd, com isso é preciso adicionar as seguintes constraints
    ```
    psql -h localhost -p 5432 arrebol arrebol_db_user
    ALTER TABLE task_spec_commands DROP CONSTRAINT fk7j4vqu34tq49sh0hltl02wtlv;
    ALTER TABLE task_spec_commands ADD CONSTRAINT commands_id_fk FOREIGN KEY (commands_id) REFERENCES command(id) ON DELETE CASCADE;

    ALTER TABLE task_spec_commands DROP CONSTRAINT fk9y8pgyqjodor03p8983w1mwnq;
    ALTER TABLE task_spec_commands ADD CONSTRAINT task_spec_id_fk FOREIGN KEY (task_spec_id) REFERENCES task_spec(id) ON DELETE CASCADE;

    ALTER TABLE task_spec_requirements DROP CONSTRAINT fkrxke07njv364ypn1i8b2p6grm;
    ALTER TABLE task_spec_requirements ADD CONSTRAINT task_spec_id_fk FOREIGN KEY (task_spec_id) REFERENCES task_spec(id) ON DELETE CASCADE;

    ALTER TABLE task DROP CONSTRAINT fk303yjlm5m2en8gknk80nkd27p; 
    ALTER TABLE task ADD CONSTRAINT task_spec_id_fk FOREIGN KEY (task_spec_id) REFERENCES task_spec(id) ON DELETE CASCADE;
    ```

### Checagem
* Requisição
    ```
    curl http://<arrebol_ip>:8080/queues/default
    ```
* Resposta esperada
    ```
    {"id":"default","name":"Default Queue","waiting_jobs":0,"worker_pools":1,"pools_size":5}
    ```

-------------------------------------------------------------------
### ***Container Option***
### Instalação:
1. Clone o repositório
    ```
    git clone -b develop https://github.com/cilasmarques/arrebol ~/arrebol
    ```
2. Instale as dependencias do docker
    ```
    cd arrebol/deploy
    sudo bash setup.sh
    ```

### Configuração:
Configure os arquivos da pasta **deploy/config/** de acordo com os outros componentes
* Exemplo: [postgres.env](./confs/arrebol/container/postgres.env) 
* Exemplo: [pgadmin.env](./confs/arrebol/container/pgadmin.env) 
* Exemplo: [application.properties](./confs/arrebol/container/application.properties) 
* Exemplo: [arrebol.json](./confs/arrebol/container/arrebol.json) 
* Exemplo: [init.sql](./confs/arrebol/container/init.sql) 

### Antes de executar, configure os workers do arrebol 
* Essa configuração deve ser feita na **mesma máquina que executará** o arrebol**.
* Para configurar o worker, siga esses [passos](#workers)

### Execução:
* Executando arrebol
    ```
    sudo bash deploy.sh start
    ```

* Parando arrebol
    ```
    sudo bash deploy.sh stop
    ```

### Checagem
* Requisição
    ```
    curl http://<arrebol_ip>:8080/queues/default
    ```
* Resposta esperada
    ```
    {"id":"default","name":"Default Queue","waiting_jobs":0,"worker_pools":1,"pools_size":5}
    ```

-------------------------------------------------------------------
## Workers
### ***Raw Option***
### Configuração:
1. Instale Docker
    ```
    sudo apt-get update
    sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    sudo apt-key fingerprint 0EBFCD88
    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io
    ```

### Configurando worker para arrebol
* Modifique o arquivo /lib/systemd/system/docker.service
  ```
  sudo vim /lib/systemd/system/docker.service
  ```
  * Mude o ExecStart e substitua pelas seguintes linhas 
    ```
      ExecStart=/usr/bin/dockerd -H fd:// -H=tcp://0.0.0.0:5555 --containerd=/run/containerd/containerd.sock
    ```

* Reinicie o deamon e o docker
  ```
    sudo systemctl daemon-reload
    sudo service docker restart
  ```

* Faça pull imagens dockers
  ```
    sudo docker pull fogbow/inputdownloader:googleapis
    sudo docker pull fogbow/preprocessor:default
    sudo docker pull fogbow/worker:ufcg-sebal

    sudo docker pull fogbow/inputdownloader:usgsapis
    sudo docker pull fogbow/preprocessor:legacy
    sudo docker pull fogbow/worker:sebkc-sebal
    sudo docker pull fogbow/worker:sebkc-tseb
  ```

### Checagem
* Requisição
    ```
    curl http://<worker_ip>:5555/version
    ```
* Resposta esperada
    ```
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
    ```

-------------------------------------------------------------------
### ***Ansible Option***
### Configuração:
Configure os arquivos da pasta **/worker/deploy/hosts.conf ** de acordo com os outros componentes
* Exemplo:
    ```
    # worker_ip[anything]
    worker_ip_1=10.11.19.104

    remote_user=ubuntu

    # The NFS Server Address
    nfs_server=10.11.19.80

    # The NFS Server directory to mount
    nfs_server_dir=/nfs

    # Required (if not specified, ansible will use the host ssh keys)
    ansible_ssh_private_key_file=/home/ubuntu/keys/saps22
    ```

### Instalação:
```
cd ~/arrebol/worker/deploy/
sudo bash install.sh
```

### Checagem
* Requisição
    ```
    curl http://<worker_ip>:5555/version
    ```
* Resposta esperada
    ```
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
    ```

-------------------------------------------------------------------
## Testes NOP
* Para testar o sistema, você pode utilizar os testes NOP
* Existem varias variações do teste e você pode escolher qual executar pelo arquivo:  **saps-quality-assurance/start-systemtest**

### Adicione as tags dos testes NOP nas configurações dos seguintes componentes
* [Tags dos testes nop](./confs/NOPTests/nopTestTags.json)

1. [Dashboard](#-dashboard)
    * Arquivo: [dashboardApp.js](https://github.com/ufcg-lsd/saps-dashboard/blob/develop/public/dashboardApp.js) (linha 10 ~ 49)
1. [Dispatcher](#dispatcher)
    * Arquivo: [execution_script_tags.json](https://github.com/ufcg-lsd/saps-dispatcher/blob/develop/resources/execution_script_tags.json)
1. [Scheduler](#scheduler)
    * Arquivo: [execution_script_tags.json](https://github.com/ufcg-lsd/saps-scheduler/blob/develop/resources/execution_script_tags.json)


Após adicionados, execute novamente o build no Scheduler e no Dispatcher.

```
sudo mvn install
```

### Clone o repositório saps-quality-assurance
```
git clone https://github.com/ufcg-lsd/saps-quality-assurance ~/saps-quality-assurance
cd ~/saps-quality-assurance
```

### Execute os testes
* Comando: 
    ```
    sudo bash start-systemtest <admin_email> <admin_password> <dispatcher_ip_addrres> <submission_rest_server_port>
    ```
    * Exemplo: 
        ```
        sudo bash start-systemtest dispatcher_admin_email dispatcher_admin_password 127.0.0.1:8091
        ```

------------------------------------------------------------------
## Atachando volume
* Atachar um volume é útil para expandir o espaço de armazenamento de uma pasta (tipo um hd externo)

### Volume menor que 2TB
1. Crie uma patição no volume
    * Comando: ```fdisk <volume>```
    * Exemplo: ```fdisk /dev/sdb```
1. Verifique se a partição foi feita
    * Comando: ```lsblk```
1. Defina um tipo de formatação para a partição
    * Comando: ```mkfs --type <formato> <particao>```
    * Exemplo: ``` mkfs --type ext4 /dev/sdb1```
1. Monte a partição em um diretorio: 
    * Comando: ```mount <particao> <diretorio>```
    * Exemplo: ```mount /dev/sdb1 /nfs```

### Volume maior que 2TB
1. Crie uma patição no volume
    * Comando: ```parted <volume>```
    * Exemplo: ```parted /dev/sdb```
1. Crie uma label
    * Comando: ```mklabel gpt```
1. Defina a unidade de armazenamento
    * Comando: ```unit TB```
1. Informa o tamanho do armazenamento
    * Comando: ```mkpart primary <init>TB <limit>TB```
    * Exemplo: ```mkpart primary 0.00TB 2.00TB```
1. Verifique se a partição foi feita e salve
    * Comando: ```print```
    * Comando: ```quit```
1. Defina um tipo de formatação para a partição
    * Comando: ```sudo mkfs.ext4 <volume>```
    * Comando: ```sudo mkfs.ext4 /dev/sdb1```
1. Monte a partição em um diretorio: 
    * Comando: ```mount <particao> <diretorio>```
    * Exemplo: ```mount /dev/sdb1 /nfs```

-------------------------------------------------------------------
## [Crontab]
* catalog -> crontab do script de sumarização
  ```
  0 0 1,15 * * sudo bash /home/ubuntu/saps-catalog/scripts/fetch_landsat_data.sh
  0 0 * * * bash /home/ubuntu/saps-catalog/scripts/build_tasks_overview.sh
  ```

* archiver -> crontab do script de contagem-dirs-arquivados
  ```
  * * */1 * * bash /home/ubuntu/saps-archiver/scripts/build_archiver_overview.sh
  ```

* dispatcher -> crontab do script de acessos + scripts de sumarização_manel
  ```
  59 23 * * * sudo bash /home/ubuntu/saps-dispatcher/scripts/login_counter.sh
  0 0 * * * sudo /bin/bash ~/saps-dispatcher/stats/stats_archived.sh > ~/saps-dispatcher/scripts/summary.csv 
  0 0 * * * sudo /bin/bash ~/saps-dispatcher/stats/logins_accumulator.sh >> ~/saps-dispatcher/scripts/summary.csv
  0 0 * * * sudo python3 ~/saps-dispatcher/stats/stats_tasks_raw_data.py
  ```

* arrebol -> crontab do script de limpeza do banco de dados
  ```
  0 0 * * *   sudo bash /home/ubuntu/arrebol/bin/db_cleaner.sh
  ```

* workers -> crontab dos containers não finalizados
  ```
  0 0 * * *  sudo docker ps -aq | sudo xargs docker stop | sudo xargs docker rm
  ```

