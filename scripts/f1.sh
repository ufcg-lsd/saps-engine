#!/bin/sh
mkdir -p ${SANDBOX}
cd ${SANDBOX}
mkdir -p ${OUTPUT_FOLDER}

# download sebal
wget -nc ${SEBAL_URL}
tar -zxvf SEBAL.tar.gz

sudo apt-get install sshfs

# mounting image repository
mkdir -p ${IMAGES_MOUNT_POINT}
sshfs -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=${USER_PRIVATE_KEY} ${REMOTE_USER}@${SEBAL_IMAGE_REPOSITORY} ${IMAGES_MOUNT_POINT}

# mounting result repository
mkdir -p ${RESULTS_MOUNT_POINT}
sshfs -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=${USER_PRIVATE_KEY} ${REMOTE_USER}@${SEBAL_RESULT_REPOSITORY} ${RESULTS_MOUNT_POINT}

# untar image
cp ${IMAGES_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}.tar.gz .
tar -zxvf ${IMAGE_NAME}.tar.gz

LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}

cd SEBAL

java -Djava.library.path=$LIBRARY_PATH -cp target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.DeployBulkMain ../${IMAGE_NAME}_MTL.txt ${RESULTS_MOUNT_POINT}/results ${LEFT_X} ${UPPER_Y} ${RIGHT_X} ${LOWER_Y} F1 ${NUMBER_OF_PARTITIONS} ${PARTITION_INDEX} ${BOUNDING_BOX_PATH} sebal-deployment.conf ${IMAGES_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}_MTLFmask > ${OUTPUT_FOLDER}/out 2> ${OUTPUT_FOLDER}/err

PROCESS_OUTPUT=$?

echo "executed" > ${OUTPUT_FOLDER}/${IMAGE_NAME}_${PARTITION_INDEX}_${NUMBER_OF_PARTITIONS}

echo $PROCESS_OUTPUT > ${OUTPUT_FOLDER}/exit
