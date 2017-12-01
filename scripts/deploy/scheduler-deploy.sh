#!/bin/bash

# General constants
CONTAINER_ID=
POSTGRESQL_VERSION=9.3
LOCAL_DATABASE_DIR=/local/exports

# Container constants
CONTAINER_DATABASE_DIR=/local/exports
CONTAINER_REPOSITORY=fogbow/scheduler

function installPostgreSQL {
  echo -e "Verifying if PostgreSQL is installed\n"

  service postgresql status
  if [ $? -ne 0 ]
  then
    echo -e "PostgreSQL not installed\nStarting installation...\n"

    apt-get update
    apt-get install -y postgresql
  fi

  # Register Database Information
  echo -e "Register Database Name: "
  read DB_NAME
  echo -e "Register Database User: "
  read USER
  echo -e "Register Database Password: "
  read -s PASSWORD
  echo -e "Confirm Password: "
  read -S PASSWORD_CONFIRM

  if [ "$PASSWORD" != "$PASSWORD_CONFIRM" ]
  then
    echo "Passwords don't match...Exiting with non-success status"
    exit 1
  fi

  su postgres -c "psql -c \"CREATE USER $USER WITH PASSWORD '$PASSWORD';\""
  su postgres -c "psql -c \"CREATE DATABASE $DB_NAME OWNER $USER;\""
  su postgres -c "psql -c \"GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $USER;\""

  sed -i 's/peer/md5/g' /etc/postgresql/$POSTGRESQL_VERSION/main/pg_hba.conf
  bash -c "echo \"host    all             all             0.0.0.0/0               md5\" >> /etc/postgresql/$POSTGRESQL_VERSION/main/pg_hba.conf"
  sudo sed -i "$ a\\listen_addresses = '*'" /etc/postgresql/$POSTGRESQL_VERSION/main/postgresql.conf

  service postgresql restart
}

function installNTPClient {
  echo -e "Installing NTP Client\n"

  bash -c 'echo "America/Recife" > /etc/timezone'
  dpkg-reconfigure -f noninteractive tzdata

  apt-get update
  apt install -y ntp
  sed -i "/server 0.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  sed -i "/server 1.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  sed -i "/server 2.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  sed -i "/server 3.ubuntu.pool.ntp.org/d" /etc/ntp.conf
  sed -i "/server ntp.ubuntu.com/d" /etc/ntp.conf
  bash -c ‘echo "server ntp.lsd.ufcg.edu.br" >> /etc/ntp.conf’
  service ntp restart
  service postgresql restart
}

function installDocker {
  docker -v
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    echo -e "Docker is not installed...Starting installation process\n"
    apt-get -y update
    apt-get -y install linux-image-extra-$(uname -r) linux-image-extra-virtual
    apt-get -y update
    apt-get -y install apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    apt-key fingerprint 0EBFCD88
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    apt-get -y update
    apt-get -y install docker-ce

    docker_version=$(apt-cache madison docker-ce | awk '{print $3}' | sed -n 1p)
    apt-get -y install docker-ce=$docker_version
  else
    echo -e "Docker is already installed...Procceeding with execution\n"
  fi
}

function runDockerContainer {
  echo -e "Running docker container\n"

  docker pull $CONTAINER_REPOSITORY
  docker run -td -v $LOCAL_DATABASE_DIR:$CONTAINER_DATABASE_DIR $CONTAINER_REPOSITORY
  
  CONTAINER_ID=$(docker ps | grep  "$CONTAINER_REPOSITORY" | awk '{print $1}')
  echo -e "Container ID: $CONTAINER_ID\n"
}

function checkProcessOutput {
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    exit $PROCESS_OUTPUT
  fi
}

installPostgreSQL
checkProcessOutput
installNTPClient
checkProcessOutput
installDocker
checkProcessOutput
runDockerContainer
checkProcessOutput
