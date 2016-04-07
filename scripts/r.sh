#!/bin/sh
mkdir -p ${SANDBOX}
cd ${SANDBOX}
mkdir -p ${OUTPUT_FOLDER}

#download R
wget -nc ${R_URL}
tar -zxvf R.tar.gz

sudo apt-get update
sudo apt-get install sshfs

# mounting image repository
mkdir -p ${IMAGES_MOUNT_POINT}
sshfs -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=${USER_PRIVATE_KEY} ${REMOTE_USER}@${IMAGE_SITE_IP}:${SEBAL_IMAGE_REPOSITORY} ${IMAGES_MOUNT_POINT}
# for nfs:
#mount ${REMOTE_USER}@${IMAGE_SITE_IP}:${SEBAL_IMAGE_REPOSITORY} ${IMAGES_MOUNT_POINT}

# mounting result repository
mkdir -p ${RESULTS_MOUNT_POINT}
sshfs -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=${USER_PRIVATE_KEY} ${REMOTE_USER}@${IMAGE_SITE_IP}:${SEBAL_RESULT_REPOSITORY} ${RESULTS_MOUNT_POINT}
# for nfs:
#mount ${REMOTE_USER}@${IMAGE_SITE_IP}:${SEBAL_RESULT_REPOSITORY} ${RESULTS_MOUNT_POINT}

# untar image
#mkdir ${IMAGE_NAME}
#cd ${IMAGE_NAME}
#cp ${IMAGES_MOUNT_POINT}/images/${IMAGE_NAME}/* .
#tar -zxvf ${IMAGE_NAME}.tar.gz

cd ${IMAGES_MOUNT_POINT}

ls > images.txt

IMAGE_NAMES_FILE=${IMAGES_MOUNT_POINT}/images.txt

LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}

cd ${SANDBOX}

mkdir -p ${RESULTS_MOUNT_POINT}
#LOCAL_RESULTS=${RESULTS_MOUNT_POINT}/local_results

cd ${IMAGES_MOUNT_POINT}/images/

# remember to verify if the following will be for a number of images or for one image

echo "Image names file is "$IMAGE_NAMES_FILE

for IMAGE_NAME in `cat $IMAGE_NAMES_FILE`

do
   # untar image
   echo "Untaring image $IMAGE_NAME"
   mkdir ${IMAGES_MOUNT_POINT}/$IMAGE_NAME
   cd ${IMAGES_MOUNT_POINT}/$IMAGE_NAME
   cp ${SEBAL_IMAGE_REPOSITORY}/images/$IMAGE_NAME".tar.gz" . 
   tar -xvzf $IMAGE_NAME".tar.gz"

   echo "Creating image output directory"
   OUTPUT_IMAGE_DIR=${RESULTS_MOUNT_POINT}/$IMAGE_NAME
   mkdir -p $OUTPUT_IMAGE_DIR

   echo "Creating dados.csv for image $IMAGE_NAME"

   R_EXEC_DIR=${SANDBOX}/R/
   cd $R_EXEC_DIR

   echo "File images,MTL,File Station Weather,File Fmask,Path Output\n/tmp/$IMAGE_NAME,/tmp/$IMAGE_NAME/$IMAGE_NAME"_MTL.txt",$IMAGES_DIR/$IMAGE_NAME/$IMAGE_NAME".station.csv",$IMAGES_DIR/$IMAGE_NAME/$IMAGE_NAME"_MTLFmask",$OUTPUT_IMAGE_DIR" > dados.csv

   echo "Executing R script..."
   Rscript AlgoritmoFinal.R $R_EXEC_DIR

   echo "Renaming dados file"
   mv dados.csv dados"-$IMAGE_NAME".csv

   rm -r ${IMAGES_MOUNT_POINT}/$IMAGE_NAME
   rm -r /tmp/Rtmp*
done

cp ${RESULTS_MOUNT_POINT}/* ${SEBAL_RESULT_REPOSITORY}

PROCESS_OUTPUT=$?

echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
