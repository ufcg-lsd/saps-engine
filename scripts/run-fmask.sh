#!/bin/bash

# We source the profile to load the matlab shared libraries path
ROOT_PROFILE=/root/.profile

mkdir -p /tmp/${IMAGE_NAME}
cd /tmp/${IMAGE_NAME}

cp ${IMAGES_MOUNT_POINT}/images/${IMAGE_NAME}/${IMAGE_NAME}.tar.gz .
tar -zxvf ${IMAGE_NAME}.tar.gz

echo -e "source $ROOT_PROFILE\n${FMASK_TOOL}" | sudo su

PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -eq 0 ]
then

  sudo mv /tmp/${IMAGE_NAME}/${IMAGE_NAME}_MTLFmask.hdr ${IMAGES_MOUNT_POINT}/images/${IMAGE_NAME}/
  sudo mv /tmp/${IMAGE_NAME}/${IMAGE_NAME}_MTLFmask ${IMAGES_MOUNT_POINT}/images/${IMAGE_NAME}/

  echo "Fmask executed successfully"
else
  echo "Fmask did not execute successfully"
fi

sudo rm -r /tmp/${IMAGE_NAME}

exit $PROCESS_OUTPUT
