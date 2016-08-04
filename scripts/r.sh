#!/bin/bash

# Global variables
CONF_FILE=sebal.conf
BOUNDING_BOX_PATH=example/boundingbox_vertices
IMAGES_DIR_NAME=images
RESULTS_DIR_NAME=results
OUTPUT_IMAGE_DIR=${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}
LOG4J_PATH=${SANDBOX}/SEBAL/log4j.properties
LOG4J_FILE_PATH=/var/log/sebal/sebal.log
R_EXEC_DIR=${SANDBOX}/SEBAL/workspace/R/
R_ALGORITHM_VERSION=AlgoritmoFinal-v2_01072016.R

# This function downloads all projects and dependencies
function prepareDependencies {
  #mkdir -p ${SANDBOX}
  #cd ${SANDBOX}
  #mkdir -p ${OUTPUT_FOLDER}

  #installing git
  sudo apt-get update
  echo -e "Y\n" | sudo apt-get install git

  cd $SANDBOX

  # cloning SEBAL project
  git clone ${SEBAL_URL}

  sudo mkdir -p $LOG4J_FILE_PATH

  # TODO: install in image
  echo -e "Y\n" | sudo apt-get install nfs-common
}

# This function mounts exports dir from NFS server
function mountExportsDir {
  sudo mount -t nfs -o proto=tcp,port=${NFS_SERVER_PORT} ${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT}
  #sudo echo "${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT} nfs proto=tcp,port=${NFS_SERVER_PORT}" >> /etc/fstab
  #sudo mount -a
}

# This function untare image and creates an output dir into mounted dir
function untarImageAndPrepareDirs {
  cd ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME

  echo "Image file name is "${IMAGE_NAME}

  # untar image
  echo "Untaring image ${IMAGE_NAME}"
  cd ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}
  tar -xvzf ${IMAGE_NAME}".tar.gz"

  echo "Creating image output directory"
  mkdir -p $OUTPUT_IMAGE_DIR
}

# This function calls a pre process java code to prepare a station file of a given image
function preProcessImage {
  cd ${SANDBOX}/SEBAL/

#  echo "Generating app snapshot"
#  mvn -e install -Dmaven.test.skip=true

  sudo java -Dlog4j.configuration=file:$LOG4J_PATH -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.PreProcessMain ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/ ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt" ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/ 0 0 9000 9000 1 1 $BOUNDING_BOX_PATH $CONF_FILE ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask"
  sudo chmod 777 ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv"
  echo -e "\n" >> ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv"
}

# This function prepare a dados.csv file and calls R script to begin image execution
function executeRScript {
  echo "Creating dados.csv for image ${IMAGE_NAME}"

  cd $R_EXEC_DIR

  echo "File images;MTL;File Station Weather;File Fmask;Path Output" > dados.csv
  echo "${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME};${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt";${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv";${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask";$OUTPUT_IMAGE_DIR" >> dados.csv
  echo "Executing R script..."
  Rscript $R_EXEC_DIR/$R_ALGORITHM_VERSION $R_EXEC_DIR
  echo "Process finished!"

  echo "Renaming dados file"
  mv dados.csv dados"-${IMAGE_NAME}".csv
  sudo cp dados"-${IMAGE_NAME}".csv $OUTPUT_IMAGE_DIR

  cd ${SANDBOX}/SEBAL
  SEBAL_VERSION=$(git rev-parse HEAD)
  echo "$SEBAL_VERSION" > $OUTPUT_IMAGE_DIR/SEBAL.version.$SEBAL_VERSION
}

# This function do a checksum of all output files in image dir
function checkSum {
  find ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME} -type f -iname "*.nc" | while read f
  do
    CHECK_SUM=$(echo | md5sum $f | cut -c1-32)
    touch $f.$CHECK_SUM.md5
  done
}

# This function ends the script
function finally {
  # see if this rm will be necessary
  #rm -r /tmp/Rtmp*
  PROCESS_OUTPUT=$?

  echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
}

prepareDependencies
mountExportsDir
untarImageAndPrepareDirs
preProcessImage
executeRScript
checkSum
finally
