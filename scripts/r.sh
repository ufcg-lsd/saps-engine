#!/bin/bash

# Global variables
CONF_FILE=sebal.conf
BOUNDING_BOX_PATH=example/boundingbox_vertices
IMAGES_DIR_NAME=images
RESULTS_DIR_NAME=results
OUTPUT_IMAGE_DIR=${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}
LOG4J_FILE_PATH=/var/log/sebal/sebal.log

# User's responsability
R_EXEC_DIR=${SANDBOX}/SEBAL/workspace/R/
R_ALGORITHM_VERSION=AlgoritmoFinal-v2_01072016.R
SEBAL_SNAPSHOT_M2_PATH=/home/esdras/.m2/repository/org/fogbowcloud/SEBAL/0.0.1-SNAPSHOT/

SEBAL_DIR_PATH=
LOG4J_PATH=

# This function downloads all projects and dependencies
function prepareDependencies {
  #installing git
  sudo apt-get update
  echo -e "Y\n" | sudo apt-get install git

  cd ${SANDBOX}

  if [ ${PINPOINTED_SEBAL_TAG} = "NE" ]
  then
    git clone ${SEBAL_URL}
    PROCESS_OUTPUT=$?
	
    if [ $PROCESS_OUTPUT -ne 0 ]
    then
      echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
    fi
  else
    git clone --branch ${PINPOINTED_SEBAL_TAG} ${SEBAL_URL}
    PROCESS_OUTPUT=$?

    if [ $PROCESS_OUTPUT -ne 0 ]
    then
      echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
    fi
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

  #echo "Generating app snapshot"
  #mvn -e install -Dmaven.test.skip=true

  sudo java -Dlog4j.configuration=file:$LOG4J_PATH -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.PreProcessMain ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/ ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt" ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/ 0 0 9000 9000 1 1 $SEBAL_DIR_PATH/$BOUNDING_BOX_PATH $SEBAL_DIR_PATH/$CONF_FILE ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask"
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
  sudo Rscript $R_EXEC_DIR/$R_ALGORITHM_VERSION $R_EXEC_DIR
  #R CMD BATCH "--args WD='$R_EXEC_DIR'" $R_EXEC_DIR/$R_ALGORITHM_VERSION
  echo "Process finished!"

  echo "Renaming dados file"
  mv dados.csv dados"-${IMAGE_NAME}".csv
  sudo mv dados"-${IMAGE_NAME}".csv $OUTPUT_IMAGE_DIR

  cd ${SANDBOX}/SEBAL

  # check if a SEBAL version was not pinpointed
  if [ "${PINPOINTED_SEBAL_VERSION}" -eq "NE" ]
  then
    SEBAL_VERSION=$(git rev-parse HEAD)
  else
    SEBAL_VERSION=${PINPOINTED_SEBAL_VERSION}
  fi

  sudo sh -c "echo \"$SEBAL_VERSION\" > $OUTPUT_IMAGE_DIR/SEBAL.version.$SEBAL_VERSION"
  #sudo echo "$SEBAL_VERSION" > $OUTPUT_IMAGE_DIR/SEBAL.version.$SEBAL_VERSION
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
