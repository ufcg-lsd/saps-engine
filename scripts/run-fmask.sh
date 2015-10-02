#!/bin/sh
mkdir -p /tmp/${IMAGE_NAME}
cd /tmp/${IMAGE_NAME}

cp ${IMAGES_MOUNT_POINT}/${IMAGE_NAME}/${IMAGE_NAME}.tar.gz .
tar -zxvf ${IMAGE_NAME}.tar.gz

${FMASK_TOOL}

PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -eq 0 ]
then

  mv ${IMAGE_NAME}_MTLFmask.hdr ${IMAGES_MOUNT_POINT}/${IMAGE_NAME}/
  mv ${IMAGE_NAME}_MTLFmask ${IMAGES_MOUNT_POINT}/${IMAGE_NAME}/

  echo "Fmask executed successfully"
else
  echo "Fmask did not execute successfully"
fi

cd
rm -r /tmp/${IMAGE_NAME}

exit $PROCESS_OUTPUT
