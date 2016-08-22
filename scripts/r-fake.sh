#!/bin/bash

# Global variables
CONF_FILE=sebal.conf
BOUNDING_BOX_PATH=example/boundingbox_vertices
IMAGES_DIR_NAME=images
RESULTS_DIR_NAME=results
OUTPUT_IMAGE_DIR=${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}
LOG4J_FILE_PATH=/var/log/sebal/sebal.log
R_EXEC_DIR=${SANDBOX}/SEBAL/workspace/R/
R_ALGORITHM_VERSION=AlgoritmoFinal-v2_01072016.R
SEBAL_SNAPSHOT_M2_PATH=/home/esdras/.m2/repository/org/fogbowcloud/SEBAL/0.0.1-SNAPSHOT/
SEBAL_DIR_PATH=
LOG4J_PATH=

# This function downloads all projects and dependencies
function prepareDependencies {
  #mkdir -p ${SANDBOX}
  #cd ${SANDBOX}
  #mkdir -p ${OUTPUT_FOLDER}

  #installing git
  sudo apt-get update
  echo -e "Y\n" | sudo apt-get install git

  cd ${SANDBOX}

  # cloning SEBAL project
  git clone ${SEBAL_URL}

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
  sudo tar -xvzf ${IMAGE_NAME}".tar.gz"

  echo "Creating image output directory"
  sudo mkdir -p $OUTPUT_IMAGE_DIR
}

# This function calls a pre process java code to prepare a station file of a given image
function preProcessImage {
  cd ${SANDBOX}/SEBAL/
  SEBAL_DIR_PATH=$(pwd)
  LOG4J_PATH=$SEBAL_DIR_PATH/log4j.properties

  echo "Creating fake station.csv for ${IMAGE_NAME}" 
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv"
  sudo chmod 777 ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv"
  echo -e "\n" >> ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv"
}

# This function prepare a dados.csv file and calls R script to begin image execution
function executeRScript {
  echo "Creating dados.csv for image ${IMAGE_NAME}"

  cd $R_EXEC_DIR

  echo "File images;MTL;File Station Weather;File Fmask;Path Output" > dados.csv
  echo "${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME};${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt";${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv";${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask";$OUTPUT_IMAGE_DIR" >> dados.csv
  echo "Executing R script..."
  # Generating fake output files
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_alb.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_EF.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_ET24h.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_EVI.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_G.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_LAI.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_NDVI.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_Rn.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_TS.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_alb.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_EF.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_ET24h.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_EVI.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_G.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_LAI.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_NDVI.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_Rn.nc"
  sudo touch ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_new_TS.nc"
  echo "Process finished!"

  echo "Renaming dados file"
  mv dados.csv dados"-${IMAGE_NAME}".csv
  sudo mv dados"-${IMAGE_NAME}".csv $OUTPUT_IMAGE_DIR

  cd ${SANDBOX}/SEBAL
  SEBAL_VERSION=$(git rev-parse HEAD)
  sudo sh -c "echo \"$SEBAL_VERSION\" > $OUTPUT_IMAGE_DIR/SEBAL.version.$SEBAL_VERSION"
}

# This function do a checksum of all output files in image dir
function checkSum {
  sudo find ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME} -type f -iname "*.nc" | while read f
  do
    CHECK_SUM=$(echo | md5sum $f | cut -c1-32)
    sudo touch $f.$CHECK_SUM.md5
  done
}

# This function ends the script
function finally {
  echo "0" > ${REMOTE_COMMAND_EXIT_PATH}
}

prepareDependencies
mountExportsDir
untarImageAndPrepareDirs
preProcessImage
executeRScript
checkSum
finally
