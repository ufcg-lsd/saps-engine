#!/bin/bash
DIRNAME=`dirname $0`
LOG4J=log4j.properties
cd $DIRNAME/..
if [ -f $LOG4J ]; then
CONF_LOG=-Dlog4j.configuration=file:$LOG4J
else
CONF_LOG=
fi

source scheduler.properties;

#VARIABLES
VM_IP;
VM_PORT;
PRIVATE_KEY_FILE;

#SSH TO VM
REMOTE_COMMAND;

#Commands
SCP_UPLOAD_COMMAND="scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $VM_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $ENV_SSH_USER@$VM_IP//$REMOTE_FILE_PATH";
SSH_COMMAND="ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $VM_PORT -i $PRIVATE_KEY_FILE $ENV_SSH_USER@$VM_IP $REMOTE_COMMAND";
UNTAR_FILE_COMMAND="tar -vzxf $REMOTE_FILE_PATH";
