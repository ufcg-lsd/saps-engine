#!/bin/sh
mkdir -p ${SANDBOX}
cd ${SANDBOX}
mkdir -p ${OUTPUT_FOLDER}

sudo apt-get install sshfs

# mounting result repository
mkdir -p ${RESULTS_MOUNT_POINT}
sshfs -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentityFile=${USER_PRIVATE_KEY} ${REMOTE_USER}@${SEBAL_RESULT_REPOSITORY} ${RESULTS_MOUNT_POINT}

ALL_RESULTS_FILE=${SEBAL_OUTPUT_DIR}/results/${IMAGE_NAME}/${LEFT_X}.${RIGHT_X}.${UPPER_Y}.${LOWER_Y}.pixels.csv

for file in ${RESULTS_MOUNT_POINT}/results/${IMAGE_NAME}/*.pixels.csv 
do
   cat $file >> $ALL_RESULTS_FILE
   echo "" >> $ALL_RESULTS_FILE
   rm $file
done

echo "joined" > ${OUTPUT_FOLDER}/${IMAGE_NAME}_results_joined

echo 0 > ${OUTPUT_FOLDER}/exit

