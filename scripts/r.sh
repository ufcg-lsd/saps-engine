#!/bin/sh
mkdir -p ${SANDBOX}
cd ${SANDBOX}
mkdir -p ${OUTPUT_FOLDER}

#download SEBAL
wget -nc ${SEBAL_URL}
tar -zxvf SEBAL-project.tar.gz

#download R
wget -nc ${R_URL}
tar -zxvf R-project.tar.gz

sudo apt-get update
sudo apt-get install nfs-common

# mounting image repository
# for nfs:
mount -t nfs -o proto=tcp,port=${NFS_SERVER_PORT} ${REMOTE_USER}@${NFS_SERVER_IP}:${VOLUME_EXPORT_PATH} ${SEBAL_MOUNT_POINT}

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
OUTPUT_IMAGE_DIR=${SEBAL_MOUNT_POINT}/results/${IMAGE_NAME}
mkdir -p $OUTPUT_IMAGE_DIR

cd ${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}

java -Xmx1G -Xss1G -Djava.library.path=/usr/local/lib -cp ${SANDBOX}/SEBAL/target/SEBAL-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.PreProcessMain ${SEBAL_MOUNT_POINT}/images/ ${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt" ${SEBAL_MOUNT_POINT}/results 0 0 9000 9000 1 1 example/boundingbox_vertices sebal.conf ${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask"

echo "Creating dados.csv for image ${IMAGE_NAME}"

R_EXEC_DIR=${SANDBOX}/R/
cd $R_EXEC_DIR

# insert before the java code that do a pre process of the image to generate station.csv for image
echo "File images,MTL,File Station Weather,File Fmask,Path Output\n${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME},${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt",${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv",${SEBAL_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask",$OUTPUT_IMAGE_DIR" > dados.csv

echo "Executing R script..."
Rscript AlgoritmoFinal.R $R_EXEC_DIR

echo "Renaming dados file"
mv dados.csv dados"-$IMAGE_NAME".csv

#tar -cvzf $OUTPUT_IMAGE_DIR/${IMAGE_NAME}"_results.tar.gz" $OUTPUT_IMAGE_DIR/*

find ${SEBAL_MOUNT_POINT}/results/${IMAGE_NAME} -type f -iname "*.nc" | while read f
do
CHECK_SUM=$(echo | md5sum $f | cut -c1-32)
touch $f.$CHECK_SUM.md5
done

#rm -r /tmp/Rtmp*

PROCESS_OUTPUT=$?

echo $PROCESS_OUTPUT > ${REMOTE_COMMAND_EXIT_PATH}
