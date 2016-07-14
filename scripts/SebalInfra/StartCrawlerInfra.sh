#!/bin/bash
PRIVATE_KEY_FILE=$1
STORAGE_SIZE=$2

LOG4J=target/classe/log4j.properties
if [ -f $LOG4J ]; then
CONF_LOG=-Dlog4j.configuration=file:$LOG4J
else
CONF_LOG=
fi

#GLOBAL CONSTANTS
APP_FILE=sebal-scheduler-project.tar.gz
APP_FILE_LOCAL_DIR=baseapp
REMOTE_BASE_DIR=tmp
SANDBOX_DIR=sebal-scheduler
VOMS_CERT_FOLDER=tmp
VOMS_CERT_FILE=x509up_u1210
REMOTE_VOMS_CERT_FOLDER=/home/fogbow/Dev/keys/cert/

#Execution INFO
CRAWLER_EXECUTION_INFO=crawler-info/crawler-exec.info
if [ ! -e "$CRAWLER_EXECUTION_INFO" ]; then
	echo "Creating execution info file"
	touch $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_INSTANCE_ID=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_USER_NAME=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_INSTANCE_IP=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_INSTANCE_PORT=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_EXTRA_PORT=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_STORAGE_ID=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_STORAGE_ATTACHMENT_ID=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_STORAGE_PORT=" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_STORAGE_FORMATED=NO" >> $CRAWLER_EXECUTION_INFO
	echo "CRAWLER_NFS_PORT=" >> $CRAWLER_EXECUTION_INFO
fi

source $CRAWLER_EXECUTION_INFO

STORAGE_COMMAND="FORMAT";

if [ "$CRAWLER_STORAGE_FORMATED" = "YES" ]; then
	STORAGE_COMMAND="RETRIEVE";
fi

echo "Storage command: $STORAGE_COMMAND";

########### CRAWLER INFRASTRUCTURE ##############

#Starting infrastructure for Crawler.
#Verify the rigths paths for crawler.conf and crawlerSpec
CONFIG_FILE=target/classes/sebal.conf 
SPEC_FILE=target/classes/crawlerSpec
echo "Requesting Instance for Crawler".
VM_CRAWLER_INFO="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain compute $CONFIG_FILE $SPEC_FILE 2>&1)";
echo $VM_CRAWLER_INFO
if [[ ! $VM_CRAWLER_INFO == *"INSTANCE_ID"* ]]; then
	echo $VM_CRAWLER_INFO
	echo "There is no resource available for deploy Crawler App."
	exit
fi
echo "Instance sucessfully requested".

#PREPARING VARIABLES FOR SSH/SCP
#Sample return USER_NAME=fogbow;SSH_HOST=192.168.0.1;SSH_HOST=9000;SSH_HOST=
INSTANCE_ID=$(echo $VM_CRAWLER_INFO | cut -d";" -f1 | cut -d"=" -f2)
USER_NAME=$(echo $VM_CRAWLER_INFO | cut -d";" -f2 | cut -d"=" -f2)
INSTANCE_IP=$(echo $VM_CRAWLER_INFO | cut -d";" -f3 | cut -d"=" -f2)
INSTANCE_PORT=$(echo $VM_CRAWLER_INFO | cut -d";" -f4 | cut -d"=" -f2)
EXTRA_PORT=$(echo $VM_CRAWLER_INFO | cut -d";" -f5 | cut -d"=" -f2)

echo $INSTANCE_ID;
echo $USER_NAME;
echo $INSTANCE_IP;
echo $INSTANCE_PORT;
echo $EXTRA_PORT;
echo $CRAWLER_STORAGE_PORT;

#Request Storage for Crawler

#Verify if storage info exists. If true, the storage for Crawler already exists and dont need to create new one.
echo "Previous Storage ID: $CRAWLER_STORAGE_ID";
if [ -n "$CRAWLER_STORAGE_ID" ]; then

	echo "Storage found. ID: $CRAWLER_STORAGE_ID"

  	STORAGE_STATUS="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain test-storage $CRAWLER_STORAGE_ID $CONFIG_FILE $SPEC_FILE 2>&1)";
  	STORAGE_STATUS=$(echo $STORAGE_STATUS | cut -d";" -f1 | cut -d"=" -f2)
  	echo "Storage status: $STORAGE_STATUS";
  	if [ ! $STORAGE_STATUS = "active" ]; then
  		CRAWLER_STORAGE_ID="";
  	fi
fi

if [ -z "$CRAWLER_STORAGE_ID" ]; then

echo "Creating new Storage with size $STORAGE_SIZE"

STORAGE_COMMAND="FORMAT";

sed -i "/CRAWLER_STORAGE_FORMATED=/ s/=.*/=NO/" $CRAWLER_EXECUTION_INFO

#This tow variables indicates if a new storage is been used. For new storage, the disk must be formated and the database must be created.
STORAGE_INFO="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain storage $STORAGE_SIZE $CONFIG_FILE $SPEC_FILE 2>&1)";
echo $STORAGE_INFO

CRAWLER_STORAGE_ID=$(echo $STORAGE_INFO | cut -d";" -f1 | cut -d"=" -f2)

echo "New storage created: "$CRAWLER_STORAGE_ID;

fi

#Attach the storage to the VM.
echo "Attaching $CRAWLER_STORAGE_ID to $INSTANCE_ID";
CRAWLER_STORAGE_ATTACHMENT_INFO="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain attachment $INSTANCE_ID $CRAWLER_STORAGE_ID $CONFIG_FILE $SPEC_FILE 2>&1)";

if [[ ! $CRAWLER_STORAGE_ATTACHMENT_INFO == *"ATTACHMENT_ID"* ]]; then
	echo $CRAWLER_STORAGE_ATTACHMENT_INFO
	echo "Erro while attaching $CRAWLER_STORAGE_ID to $INSTANCE_ID."
	exit
fi

echo $CRAWLER_STORAGE_ATTACHMENT_INFO;

CRAWLER_STORAGE_ATTACHMENT_ID=$(echo $CRAWLER_STORAGE_ATTACHMENT_INFO | cut -d";" -f1 | cut -d"=" -f2)

echo "Attach ID: $CRAWLER_STORAGE_ATTACHMENT_ID";

#Update Storage INFO FILE with the new attachment id.
sed -i "/CRAWLER_STORAGE_ATTACHMENT_ID=/ s/=.*/=$CRAWLER_STORAGE_ATTACHMENT_ID/" $CRAWLER_EXECUTION_INFO

#Coping scripts to mount disk.
LOCAL_FILE_PATH="sebal_scripts"
REMOTE_FILE_PATH="$REMOTE_BASE_DIR";

echo "UPLOAD SCRIPTS..."
echo "scp -r -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH/ $USER_NAME@$INSTANCE_IP:/$REMOTE_FILE_PATH"
scp -r -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH/ $USER_NAME@$INSTANCE_IP:/$REMOTE_FILE_PATH

echo "ALTER PERMISSION FOR SCRIPTS..."
REMOTE_COMMAND="sudo chmod -R 777 /$REMOTE_FILE_PATH/$LOCAL_FILE_PATH";
echo "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND

#Preparing storage
echo "PREPARING STORAGE..."
REMOTE_COMMAND="sudo sh /$REMOTE_FILE_PATH/$LOCAL_FILE_PATH/mount_partition.sh $STORAGE_COMMAND";
echo  "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND
sed -i "/CRAWLER_STORAGE_FORMATED=/ s/=.*/=YES/" $CRAWLER_EXECUTION_INFO

#Installing git
echo "INSTALLING GIT..."
REMOTE_COMMAND="sudo sh /$REMOTE_FILE_PATH/$LOCAL_FILE_PATH/install_git.sh";
echo "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND

# see if the name of the project will change
#Coping SEBAL-Scheduler App file to Crawler VM
echo "UPLOAD CRAWLER PACKAGE..."
LOCAL_FILE_PATH="$APP_FILE_LOCAL_DIR/$APP_FILE"
REMOTE_FILE_PATH="$APP_FILE"
echo "scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH"
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH

#UNTAR APP FILE
echo "EXTRACTING SEBAL-Scheduler..."
REMOTE_COMMAND="tar -vzxf $REMOTE_FILE_PATH";
echo "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND

#PREPARING CERT FILE SCP COMMAND
echo "UPLOAD CERTIFICATES..."
LOCAL_FILE_PATH="/$VOMS_CERT_FOLDER/$VOMS_CERT_FILE";
FILE_PATH="/$REMOTE_VOMS_CERT_FOLDER/$VOMS_CERT_FILE";
#Coping certificate file to Crawler VM
echo "scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH"
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH

# see what have to change to use this
IFS=',' read -r -a extraPortsArray <<< $EXTRA_PORT
arrayLength=${#extraPortsArray[@]};
for (( i=0; i<${arrayLength}; i++ ));
do
 actualPort=${extraPortsArray[$i]};
 if [[ $actualPort == *"nfs"* ]]; then
  CRAWLER_NFS_PORT=$(echo $actualPort | cut -d":" -f3 | tr -d })
 fi
done

#Putting informations on Crawler execution info.
sed -i "/CRAWLER_INSTANCE_ID=/ s/=.*/=$INSTANCE_ID/" $CRAWLER_EXECUTION_INFO
sed -i "/CRAWLER_USER_NAME=/ s/=.*/=$USER_NAME/" $CRAWLER_EXECUTION_INFO
sed -i "/CRAWLER_INSTANCE_IP=/ s/=.*/=$INSTANCE_IP/" $CRAWLER_EXECUTION_INFO
sed -i "/CRAWLER_INSTANCE_PORT=/ s/=.*/=$INSTANCE_PORT/" $CRAWLER_EXECUTION_INFO
sed -i "/CRAWLER_EXTRA_PORT=/ s/=.*/=$EXTRA_PORT/" $CRAWLER_EXECUTION_INFO
sed -i "/CRAWLER_NFS_PORT=/ s/=.*/=$CRAWLER_NFS_PORT/" $CRAWLER_EXECUTION_INFO
