#!/bin/bash

# Global variables
# Saps Side
INPUTS_DIR_NAME=data/input
PREPROCESSING_DIR_NAME=data/preprocessing
OUTPUT_DIR_NAME=data/output
LOGS_DIR=data/logs
METADATA_DIR=metadata
SAPS_TMP=/mnt

# User Side
BIN_RUN_SCRIPT="bin/run.sh"
PROCESS_OUTPUT=

# This function mounts exports dir from NFS server
function mountExportsDir {
  if [ ! -d ${SAPS_MOUNT_POINT} ]
  then
    sudo mkdir -p ${SAPS_MOUNT_POINT}
  fi

  if grep -qs "${SAPS_MOUNT_POINT}" /proc/mounts;
  then
    echo "Directory ${SAPS_MOUNT_POINT} already mounted."
  else
    if (( $(ps -ef | grep -v grep | grep nfsiod | wc -l) > 0 ))
    then
      echo "NFS Client is not installed...Procceeding to install."
      sudo apt-get update
      sudo apt-get install nfs-common
    fi

    echo "Directory ${SAPS_MOUNT_POINT} not mounted yet...proceeding to mount"
    sudo mount -t nfs -o proto=tcp,port=${NFS_SERVER_PORT} ${NFS_SERVER_IP}:${EXPORT_PATH} ${SAPS_MOUNT_POINT}
  fi
}

# This function install Docker if it's not installed
function installDocker {
  sudo docker -v
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    echo "Docker is not installed...Starting installation process."
    sudo apt-get -y update
    sudo apt-get -y install linux-image-extra-$(uname -r) linux-image-extra-virtual
    sudo apt-get -y update
    sudo apt-get -y install apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    sudo apt-key fingerprint 0EBFCD88
    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    sudo apt-get -y update
    sudo apt-get -y install docker-ce

    docker_version=$(apt-cache madison docker-ce | awk '{print $3}' | sed -n 1p)
    sudo apt-get -y install docker-ce=$docker_version
    sudo docker run hello-world
    checkProcessOutput
  else
    echo "Docker is already installed...Procceeding with execution."
  fi
}

# This function downloads container image and prepare container to execution
function prepareAndExecuteDockerContainer {
  cd ${SANDBOX}

  echo "Pulling docker image from ${WORKER_CONTAINER_REPOSITORY}:${WORKER_CONTAINER_TAG}"
  sudo docker pull ${WORKER_CONTAINER_REPOSITORY}:${WORKER_CONTAINER_TAG}

  if [ "$(sudo docker ps -aq -f status=exited -f name=${WORKER_CONTAINER_TAG})" ] || [ "$(sudo docker ps -q -f name=${WORKER_CONTAINER_TAG})" ]
  then
    # cleanup
    sudo docker rm ${WORKER_CONTAINER_TAG}
  fi

  sudo docker run -v ${SAPS_MOUNT_POINT}:${SAPS_MOUNT_POINT} -v $SAPS_TMP:$SAPS_TMP ${WORKER_CONTAINER_REPOSITORY}:${WORKER_CONTAINER_TAG} bash -x $BIN_RUN_SCRIPT ${SAPS_MOUNT_POINT}/${TASK_ID}/$INPUTS_DIR_NAME ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME ${SAPS_MOUNT_POINT}/${TASK_ID}/$PREPROCESSING_DIR_NAME ${SAPS_MOUNT_POINT}/${TASK_ID}/$METADATA_DIR
}

# This function creates output directory if not exists
function createOutputDir {
  echo "Create output directory ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME if it not exists"
  sudo mkdir -p ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME
}

# This function cleans previous, and probably failed, output from task output dir
function garbageCollect {
  DIRECTORY=${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME
  if [ -d "$DIRECTORY" ]; then
    shopt -s nullglob dotglob     # To include hidden files
    files=($DIRECTORY/*)
    if [ ${#files[@]} -gt 0 ];
    then
      echo "Directory contains garbage...cleanning it"
      sudo rm ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME/*
    fi
  fi
}

# This function creates output directory if not exists
function createOutputDir {
  echo "Create output directory ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME if it not exists"
  sudo mkdir -p ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME
}

function executeDockerContainer {
  cd ${SANDBOX}

  CONTAINER_ID=$(sudo docker ps | grep "${WORKER_CONTAINER_REPOSITORY}:${WORKER_CONTAINER_TAG}" | awk '{print $1}')

  sudo timeout 3h docker exec $CONTAINER_ID bash -x $BIN_RUN_SCRIPT ${SAPS_MOUNT_POINT}/${TASK_ID}/$INPUTS_DIR_NAME ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME ${SAPS_MOUNT_POINT}/${TASK_ID}/$PREPROCESSING_DIR_NAME
}

function removeDockerContainer {
  CONTAINER_ID=$(sudo docker ps -a | grep "${WORKER_CONTAINER_REPOSITORY}:${WORKER_CONTAINER_TAG}" | awk '{print $1}')

  echo "Removing docker container $CONTAINER_ID"
  sudo docker rm -f $CONTAINER_ID
}

# This function do a checksum of all output files in image dir
function checkSum {
  sudo find ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME -type f -iname "*.nc" | while read f
  do
    CHECK_SUM=$(echo | md5sum $f | cut -c1-32)
    sudo touch $f.$CHECK_SUM.md5
  done
}

function moveTempFiles {
  echo "Moving temporary out and err files"
  sudo mv ${SANDBOX}/*out ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME
  sudo mv ${SANDBOX}/*err ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    echo "Fail while transfering out and err files to ${SAPS_MOUNT_POINT}/${TASK_ID}/$OUTPUT_DIR_NAME"
  fi
}

function checkProcessOutput {
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    echo "PROCESS_OUTPUT = $PROCESS_OUTPUT"
    if [ ! -d "${SAPS_MOUNT_POINT}/${TASK_ID}/$LOGS_DIR" ]
    then
      sudo mkdir -p ${SAPS_MOUNT_POINT}/${TASK_ID}/$LOGS_DIR
    fi

    echo "Copying temporary out and err files to ${SAPS_MOUNT_POINT}/${TASK_ID}/$LOGS_DIR"
    sudo cp ${SANDBOX}/*out ${SAPS_MOUNT_POINT}/${TASK_ID}/$LOGS_DIR
    sudo cp ${SANDBOX}/*err ${SAPS_MOUNT_POINT}/${TASK_ID}/$LOGS_DIR
    removeDockerContainer
    finally
  fi
}

# This function ends the script
function finally {
  echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
  exit $PROCESS_OUTPUT
}

mountExportsDir
checkProcessOutput
installDocker
checkProcessOutput
garbageCollect
checkProcessOutput
createOutputDir
checkProcessOutput
prepareAndExecuteDockerContainer
checkProcessOutput
removeDockerContainer
checkProcessOutput
checkSum
checkProcessOutput
moveTempFiles
checkProcessOutput
finally
