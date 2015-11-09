#!/bin/sh
mkdir -p ${SANDBOX}
cd ${SANDBOX}
mkdir -p ${OUTPUT_FOLDER}

# download sebal
wget -nc ${SEBAL_URL}
tar -zxvf SEBAL.tar.gz

sudo apt-get update
sudo apt-get install sshfs

# mounting image repository
mkdir -p ${IMAGES_MOUNT_POINT}
sshfs -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=${USER_PRIVATE_KEY} ${REMOTE_USER}@${SEBAL_IMAGE_REPOSITORY} ${IMAGES_MOUNT_POINT}

# mounting result repository
mkdir -p ${RESULTS_MOUNT_POINT}
sshfs -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=${USER_PRIVATE_KEY} ${REMOTE_USER}@${SEBAL_RESULT_REPOSITORY} ${RESULTS_MOUNT_POINT}

# untar image
mkdir ${IMAGE_NAME}
cd ${IMAGE_NAME}
cp ${IMAGES_MOUNT_POINT}/images/${IMAGE_NAME}/* .
tar -zxvf ${IMAGE_NAME}.tar.gz

LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}

cd ../SEBAL

mkdir -p local_results
LOCAL_RESULTS=local_results

java -Xmx1G -Xss1G -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.DeployBulkMain ../${IMAGE_NAME}/${IMAGE_NAME}_MTL.txt $LOCAL_RESULTS ${LEFT_X} ${UPPER_Y} ${RIGHT_X} ${LOWER_Y} F1 ${NUMBER_OF_PARTITIONS} ${PARTITION_INDEX} ${BOUNDING_BOX_PATH} sebal-deployment.conf ../${IMAGE_NAME}/${IMAGE_NAME}_MTLFmask > ${OUTPUT_FOLDER}/${NUMBER_OF_PARTITIONS}_${PARTITION_INDEX}_out 2> ${OUTPUT_FOLDER}/${NUMBER_OF_PARTITIONS}_${PARTITION_INDEX}_err

PROCESS_OUTPUT=$?

java -Xss16m -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.render.RenderHelper ../${IMAGE_NAME}/${IMAGE_NAME}_MTL.txt $LOCAL_RESULTS ${LEFT_X} ${UPPER_Y} ${RIGHT_X} ${LOWER_Y} ${NUMBER_OF_PARTITIONS} ${PARTITION_INDEX} ${BOUNDING_BOX_PATH} bmp > ${OUTPUT_FOLDER}/${NUMBER_OF_PARTITIONS}_${PARTITION_INDEX}_render_out 2> ${OUTPUT_FOLDER}/${NUMBER_OF_PARTITIONS}_${PARTITION_INDEX}_render_err

echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
