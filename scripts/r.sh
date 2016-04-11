#!/bin/sh
mkdir -p ${SANDBOX}
cd ${SANDBOX}
mkdir -p ${OUTPUT_FOLDER}

#download R
wget -nc ${R_URL}
tar -zxvf R.tar.gz

sudo apt-get update
sudo apt-get install nfs-common

# mounting image repository
# for nfs:
mount -t nfs -o proto=ftp ${REMOTE_USER}@${IMAGE_REMOTE_REP_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT}

LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}

cd ${SEBAL_MOUNT_POINT}/images

echo "Image file name is "${IMAGE_NAME}

# untar image
echo "Untaring image ${IMAGE_NAME}"
mkdir ${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}
cd ${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}
cp ${SEBAL_IMAGE_REPOSITORY}/images/${IMAGE_NAME}".tar.gz" . 
tar -xvzf ${IMAGE_NAME}".tar.gz"

echo "Creating image output directory"
OUTPUT_IMAGE_DIR=${SEBAL_MOUNT_POINT}/${IMAGE_NAME}
mkdir -p $OUTPUT_IMAGE_DIR

echo "Creating dados.csv for image ${IMAGE_NAME}"

R_EXEC_DIR=${SANDBOX}/R/
cd $R_EXEC_DIR

# insert before the java code that do a pre process of the image to generate station.csv for image
echo "File images,MTL,File Station Weather,File Fmask,Path Output\n${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME},${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt",${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}".station.csv",${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask",$OUTPUT_IMAGE_DIR" > dados.csv

echo "Executing R script..."
Rscript AlgoritmoFinal.R $R_EXEC_DIR

echo "Renaming dados file"
mv dados.csv dados"-$IMAGE_NAME".csv

#rm -r /tmp/Rtmp*

PROCESS_OUTPUT=$?

echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
