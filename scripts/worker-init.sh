#!/bin/bash

BIN_INIT_SCRIPT="bin/init.sh"
IMAGES_DIR_NAME=images
PROCESS_OUTPUT=
repositoryName=

# This function removes all garbage from tmp
function tmpGarbageCollect {
  count=`ls -1 /tmp/Rtmp* 2>/dev/null | wc -l`
  if [ $count != 0 ]
  then 
    sudo rm -r /tmp/Rtmp*
  fi
}

# This function downloads all projects and dependencies
function prepareDependencies {
  #installing git
  sudo apt-get update

  # TODO: install in image
  echo -e "Y\n" | sudo apt-get install nfs-common

  # TODO: install in image
  echo -e "Y\n" | sudo apt-get install git

  cd ${SANDBOX}

  #when https://github.com/xpto/foo-baa.git we have foo-baa which is the root dir of the repo
  repositoryName=`echo ${SEBAL_URL} | rev | cut -d "/" -f1 | cut -d"." -f2 | rev`

  if [ ${PINPOINTED_SEBAL_TAG} = "NE" ]
  then
    git clone ${SEBAL_URL}	
  else
    #git clone --branch ${PINPOINTED_SEBAL_TAG} ${SEBAL_URL}
    git clone ${SEBAL_URL}
    cd $repositoryName
    git checkout ${PINPOINTED_SEBAL_TAG}
    cd ..
  fi

  bash -x $repositoryName/$BIN_INIT_SCRIPT
}

# This function checks if there is a missing dependencies file created
# if it exists, it will be stored in image directory
function checkMissingDependenciesFile {
  if [ -f ${SANDBOX}/$repositoryName/missing_dependencies ];
  then
    echo "Transfering missing_dependencies file to ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME} directory"
    sudo mv ${SANDBOX}/$repositoryName/missing_dependencies ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}
  else
    echo "No missing dependencies"
    if [ -f ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/missing_dependencies ]
    then
      sudo rm ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/missing_dependencies
    fi
  fi
}

# This function mounts exports dir from NFS server
function mountExportsDir {
  if [ ! -d ${SEBAL_MOUNT_POINT} ]
  then
    sudo mkdir -p ${SEBAL_MOUNT_POINT}
  fi

  if grep -qs "${SEBAL_MOUNT_POINT}" /proc/mounts;
  then
    echo "Directory ${SEBAL_MOUNT_POINT} already mounted."
  else
    echo "Directory ${SEBAL_MOUNT_POINT} not mounted yet...proceeding to mount"
    sudo mount -t nfs -o proto=tcp,port=${NFS_SERVER_PORT} ${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT}
  fi
}

function garbageCollect {
  DIRECTORY=${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
  if [ -d "$DIRECTORY" ]; then
    shopt -s nullglob dotglob     # To include hidden files
    files=($DIRECTORY/*)
    if [ ${#files[@]} -gt 0 ];
    then
      echo "Directory contains garbage...cleanning it"
      sudo rm ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/*
    fi
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

tmpGarbageCollect
checkProcessOutput
prepareDependencies
checkProcessOutput
checkMissingDependenciesFile
checkProcessOutput
mountExportsDir
checkProcessOutput
garbageCollect
checkProcessOutput
finally
