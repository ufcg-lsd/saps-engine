#!/bin/bash

# Global variables
IMAGES_DIR_NAME=images
RESULTS_DIR_NAME=results
BIN_RUN_SCRIPT="bin/run.sh"
ERROR_LOGS_DIR=error_logs
PROCESS_OUTPUT=

function executeRunScript {
  #when https://github.com/xpto/foo-baa.git we have foo-baa which is the root dir of the repo
  repositoryName=`echo ${SEBAL_URL} | rev | cut -d "/" -f1 | cut -d"." -f2 | rev`

  cd ${SANDBOX}

  bash -x $repositoryName/$BIN_RUN_SCRIPT ${IMAGE_NAME} ${IMAGE_NEW_COLLECTION_NAME} ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/ ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/ ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME} ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NEW_COLLECTION_NAME}"_MTL.txt" ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NEW_COLLECTION_NAME}"_station.csv"
}

# This function do a checksum of all output files in image dir
function checkSum {
  sudo find ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME} -type f -iname "*.nc" | while read f
  do
    CHECK_SUM=$(echo | md5sum $f | cut -c1-32)
    sudo touch $f.$CHECK_SUM.md5
  done
}

function moveTempFiles {
  echo "Moving temporary out and err files"
  sudo mv ${SANDBOX}/*out ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
  sudo mv ${SANDBOX}/*err ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    echo "Fail while transfering out and err files to ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}"
  fi
}

function checkProcessOutput {
  PROCESS_OUTPUT=$?

  if [ $PROCESS_OUTPUT -ne 0 ]
  then
    echo "PROCESS_OUTPUT = $PROCESS_OUTPUT"
    if [ ! -d "${SEBAL_MOUNT_POINT}/$ERROR_LOGS_DIR/${IMAGE_NAME}" ]
    then
      sudo mkdir -p ${SEBAL_MOUNT_POINT}/$ERROR_LOGS_DIR/${IMAGE_NAME}
    fi

    echo "Copying temporary out and err files to ${SEBAL_MOUNT_POINT}/$ERROR_LOGS_DIR/${IMAGE_NAME}"
    sudo cp ${SANDBOX}/*out ${SEBAL_MOUNT_POINT}/$ERROR_LOGS_DIR/${IMAGE_NAME}
    sudo cp ${SANDBOX}/*err ${SEBAL_MOUNT_POINT}/$ERROR_LOGS_DIR/${IMAGE_NAME}
    finally
  fi
}

# This function ends the script
function finally {
  # see if this rm will be necessary
  #rm -r /tmp/Rtmp*
  echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
  exit $PROCESS_OUTPUT
}

executeRunScript
checkProcessOutput
checkSum
checkProcessOutput
moveTempFiles
checkProcessOutput
finally
