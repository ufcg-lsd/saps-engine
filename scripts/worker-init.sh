#!/bin/bash

BIN_INIT_SCRIPT="bin/init.sh"
PROCESS_OUTPUT=

# This function downloads all projects and dependencies
function prepareDependencies {
  #installing git
  sudo apt-get update

  # TODO: install in image
  echo -e "Y\n" | sudo apt-get install nfs-common

  # TODO: install in image
  echo -e "Y\n" | sudo apt-get install git

  cd ${SANDBOX}

  if [ ${PINPOINTED_SEBAL_TAG} = "NE" ]
  then
    git clone ${SEBAL_URL}	
  else
    git clone --branch ${PINPOINTED_SEBAL_TAG} ${SEBAL_URL}
  fi

  #when https://github.com/xpto/foo-baa.git we have foo-baa which is the root dir of the repo
  repositoryName=`echo ${SEBAL_URL} | rev | cut -d "/" -f1 | cut -d"." -f2 | rev`

  bash -x $repositoryName/$BIN_INIT_SCRIPT
}

# This function mounts exports dir from NFS server
function mountExportsDir {
  if grep -qs "${SEBAL_MOUNT_POINT}" /proc/mounts;
  then
    echo "Directory ${SEBAL_MOUNT_POINT} already mounted."
  else
    echo "Directory ${SEBAL_MOUNT_POINT} not mounted yet...proceeding to mount"
    sudo mount -t nfs -o proto=tcp,port=${NFS_SERVER_PORT} ${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT}
  fi
}

function checkProcessOutput {
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    finally
  fi
}

# This function ends the script
function finally {
  echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
  exit $PROCESS_OUTPUT
}

prepareDependencies
checkProcessOutput
mountExportsDir
checkProcessOutput
finally
