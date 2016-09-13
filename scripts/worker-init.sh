#!/bin/bash

# This function downloads all projects and dependencies
function prepareDependencies {
  #installing git
  sudo apt-get update

  # TODO: install in image
  echo -e "Y\n" | sudo apt-get install git

  cd ${SANDBOX}

  if [ ${PINPOINTED_SEBAL_TAG} = "NE" ]
  then
    git clone ${SEBAL_URL}	
  else
    git clone --branch ${PINPOINTED_SEBAL_TAG} ${SEBAL_URL}
  fi

  # getting sebal snapshot from public_html
  cd ${SANDBOX}/SEBAL
  sudo tar -xvzf target.tar.gz
  rm target.tar.gz

  # putting snapshot into .m2
  SEBAL_DIR_PATH=$(pwd)
  sudo mkdir -p $SEBAL_SNAPSHOT_M2_PATH
  sudo cp $SEBAL_DIR_PATH/target/SEBAL-0.0.1-SNAPSHOT.jar $SEBAL_SNAPSHOT_M2_PATH

  sudo mkdir -p $LOG4J_FILE_PATH

  cd ${SANDBOX}

  # TODO: install in image
  echo -e "Y\n" | sudo apt-get install nfs-common
}

# This function mounts exports dir from NFS server
function mountExportsDir {
  sudo mount -t nfs -o proto=tcp,port=${NFS_SERVER_PORT} ${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT}
}

function verifyRScript {
  echo "Verifying dependencies for R script"
  bash -x ${VERIFICATION_SCRIPT}
}

# This function ends the script
function finally {
  PROCESS_OUTPUT=$?

  echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
}

prepareDependencies
mountExportsDir
verifyRScript
finally
