#!/bin/bash

# General constants
DASHBOARD_REPOSITORY=https://github.com/fogbow/saps-dashboard.git
MANAGER_REPOSITORY=https://github.com/fogbow/fogbow-manager.git
BLOWOUT_REPOSITORY=https://github.com/fogbow/blowout.git
ENGINE_REPOSITORY=https://github.com/fogbow/saps-engine.git

DASHBOARD_VERSION=backend-integration
MANAGER_VERSION=develop
BLOWOUT_VERSION=sebal-experiment-queue-fix
ENGINE_VERSION=develop-pure

function installDependencies {
  echo -e "Installing dependencies\n"
  
  add-apt-repository -y ppa:openjdk-r/ppa
  apt-get update
  curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
  apt-get install -y nodejs
  apt-get -y install git
  apt-get install openjdk-7-jdk -y
  apt-get -y install maven
}

function installNTPClient {
  echo -e "Installing NTP Client\n"

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
  service postgresql restart
}

function prepareEnv {
  echo -e "Preparing environment\n"

  git clone -b $DASHBOARD_VERSION $DASHBOARD_REPOSITORY
  cd saps-dashboard
  npm install
  mv node_modules public/
  cd ..

  git clone -b $MANAGER_VERSION $MANAGER_REPOSITORY
  cd fogbow-manager/
  mvn install -Dmaven.test.skip=true
  cd ..

  git clone -b $BLOWOUT_VERSION $BLOWOUT_REPOSITORY
  cd blowout
  mvn install -Dmaven.test.skip=true
  cd ..

  git clone -b $ENGINE_VERSION $ENGINE_REPOSITORY
  cd saps-engine
  mvn install -Dmaven.test.skip=true
  cd ..
}

function checkProcessOutput {
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    exit $PROCESS_OUTPUT
  fi
}

installDependencies
checkProcessOutput
installNTPClient
checkProcessOutput
