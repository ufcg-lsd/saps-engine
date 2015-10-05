#!/bin/sh
mkdir -p ${SANDBOX}
cd ${SANDBOX}
mkdir -p ${OUTPUT_FOLDER}

# untar image
cd ${IMAGES_DIR_PATH}/${IMAGE_NAME}
tar -zxvf ${IMAGE_NAME}.tar.gz
cd ${SANDBOX}

LIBRARY_PATH=/usr/local/lib/${ADDITIONAL_LIBRARY_PATH}

java -Djava.library.path=$LIBRARY_PATH -cp ${SEBAL_HOME}/target/SEBAL-0.0.1-SNAPSHOT.jar:${SEBAL_HOME}/target/lib/* org.fogbowcloud.sebal.DeployBulkMain ${IMAGES_DIR_PATH}/${IMAGE_NAME}/${IMAGE_NAME}_MTL.txt ${SEBAL_OUTPUT_DIR} ${LEFT_X} ${UPPER_Y} ${RIGHT_X} ${LOWER_Y} ${NUMBER_OF_PARTITIONS} ${PARTITION_INDEX} ${BOUNDING_BOX_PATH} > ${OUTPUT_FOLDER}/out 2> ${OUTPUT_FOLDER}/err

echo $? > ${OUTPUT_FOLDER}/exit

echo "uploaded" > ${OUTPUT_FOLDER}/${IMAGE_NAME}_uploaded
