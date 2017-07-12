#!/bin/bash

FTP_SERVER_USER=$1
FTP_SERVER_IP=$2
FTP_SERVER_PORT=$3
REMOTE_RESULTS_DIR=$4
LOCAL_RESULTS_DIR=$5
IMAGE_NAME=$6
PRIVATE_KEY_PATH=/home/ubuntu/keys/fetcher_key_rsa

#if [ -f $PRIVATE_KEY_PATH ]
#then
#  echo "Missing fetcher private key"
#  exit 1
#fi

#TODO: add log
echo -e "get -r $REMOTE_RESULTS_DIR\nquit\n" | sftp -i $PRIVATE_KEY_PATH -o "StrictHostKeyChecking=no" -P $FTP_SERVER_PORT $FTP_SERVER_USER@$FTP_SERVER_IP
mv $IMAGE_NAME/* $LOCAL_RESULTS_DIR
rm -r $IMAGE_NAME
