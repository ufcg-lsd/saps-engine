#!/bin/bash

# Global variables
CONF_FILE=sebal.conf
BOUNDING_BOX_PATH=example/boundingbox_vertices
IMAGES_DIR_NAME=images
RESULTS_DIR_NAME=results
OUTPUT_IMAGE_DIR=${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}
R_EXEC_DIR=${SANDBOX}/R/
R_ALGORITHM_VERSION=AlgoritmoFinal-v2_01042016.R

# This function downloads all projects and dependencies
function prepareDependencies {
  mkdir -p ${SANDBOX}
  cd ${SANDBOX}
  mkdir -p ${OUTPUT_FOLDER}

  #download SEBAL
  wget -nc ${SEBAL_URL}
  tar -zxvf SEBAL-project.tar.gz

  #download R
  wget -nc ${R_URL}
  tar -zxvf R-project.tar.gz

  # TODO: install in image
  sudo apt-get update
  sudo apt-get install nfs-common
}

# This function mounts exports dir from NFS server
function mountExportsDir {
  # TODO: step 1 - write in fstab; step 2 - mount -a
  sudo mount -t nfs -o proto=tcp,port=${NFS_SERVER_PORT} ${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT}
  sudo echo "${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT} nfs proto=tcp,port=${NFS_SERVER_PORT}" >> /etc/fstab
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

  # see if the memory options will be necessary
  sudo java -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.PreProcessMain ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/ ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt" ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/ 0 0 9000 9000 1 1 $BOUNDING_BOX_PATH $CONF_FILE ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask"
}

# This function prepare a dados.csv file and calls R script to begin image execution
function executeRScript {
  echo "Creating dados.csv for image ${IMAGE_NAME}"

  cd $R_EXEC_DIR

  echo "File images;MTL;File Station Weather;File Fmask;Path Output" > dados.csv
  echo "${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME};${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt";${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv";${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask";$OUTPUT_IMAGE_DIR" >> dados.csv
  echo "Executing R script..."
  Rscript $R_ALGORITHM_VERSION $R_EXEC_DIR
  echo "Process finished!"

  echo "Renaming dados file"
  mv dados.csv dados"-${IMAGE_NAME}".csv
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
