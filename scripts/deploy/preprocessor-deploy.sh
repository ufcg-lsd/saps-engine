#!/bin/bash
INPUT_DOWNLOADER_IP=$1
INPUT_DOWNLOADER_PORT=$2
INPUT_DOWNLOADER_EXPORT_PATH=$3

# General constants
CONTAINER_ID=
LOCAL_MOUNT_DIR=/local/exports

# Container constants
CONTAINER_REPOSITORY=fogbow/preprocessor-deploy

function installNTPClient {
  echo -e "Installing and configuring NTP Client\n"

  bash -c ‘echo "America/Recife" > /etc/timezone’
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

function installAndMountNFSClient {
  echo -e "Installing NFS Client\n"
  apt-get update
  apt-get install -y nfs-common

  echo -e "Mounting NFS Client\n"
  mkdir -p $LOCAL_MOUNT_DIR
  mount -t nfs -o proto=tcp,port=$INPUT_DOWNLOADER_PORT $INPUT_DOWNLOADER_IP:$INPUT_DOWNLOADER_EXPORT_PATH $LOCAL_MOUNT_DIR
}

function runDockerContainer {
  echo -e "Running docker container\n"

  docker pull $CONTAINER_REPOSITORY
  docker run -td -v $LOCAL_MOUNT_DIR:$LOCAL_MOUNT_DIR $CONTAINER_REPOSITORY

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

installNTPClient
checkProcessOutput
installDocker
checkProcessOutput
installAndMountNFSClient
checkProcessOutput
runDockerContainer
checkProcessOutput
