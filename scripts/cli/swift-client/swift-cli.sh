#!/bin/bash
DIRNAME=`dirname $0`
SWIFT_CONTAINER_NAME=$1
IMAGE_NAME=$2
OUTPUT_DIRECTORY=$3

## including the openstack environment variables
. $DIRNAME/fogbow-openrc.sh

## generating OS token
mytoken=`openstack token issue -f value -c id`

## storage OS url
storageUrl="https://cloud.lsd.ufcg.edu.br:8080/swift/v1"

if [[ -z $IMAGE_NAME || -z $SWIFT_CONTAINER_NAME ]]; then
	echo "Please, inform the CONTAINER_NAME and the IMAGE_NAME to download"
	echo "USAGE: bash swift-cli.sh CONTAINER_NAME IMAGE_NAME"
	exit 1
else
	for completeImageFilePath in `swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME | grep $IMAGE_NAME`; do
		echo "Downloading file $completeImageFilePath"
		xbase=${completeImageFilePath##*/}
		xpref=${xbase%.*}
		swift --os-auth-token $mytoken --os-storage-url "https://cloud.lsd.ufcg.edu.br:8080/swift/v1" download $SWIFT_CONTAINER_NAME $completeImageFilePath -o $OUTPUT_DIRECTORY/$xpref
	done
fi
