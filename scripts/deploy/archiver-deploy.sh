#!/bin/bash

# General constants
CONTAINER_ID=

# Container constants
CONTAINER_REPOSITORY=fogbow/archiver

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

function runDockerContainer {
  echo -e "Running docker container\n"

  docker pull $CONTAINER_REPOSITORY
  docker run -td $CONTAINER_REPOSITORY

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
runDockerContainer
checkProcessOutput
