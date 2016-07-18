#!/bin/bash
LOG4J=target/classe/log4j.properties
if [ -f $LOG4J ]; then
CONF_LOG=-Dlog4j.configuration=file:$LOG4J
else
CONF_LOG=
fi

#PARAM
STORAGE_SIZE=$1
if [[ ! $STORAGE_SIZE =~ ^-?[0-9]+$ ]] ; then
	echo "Invalid parameter ( $STORAGE_SIZE ) for storage size. Must be valid number (GigaByte)."
fi

#GLOBAL CONSTANTS
APP_FILE=sebal-scheduler-project.tar.gz
APP_FILE_LOCAL_DIR=baseapp
REMOTE_BASE_DIR=tmp
SANDBOX_DIR=sebal-fetcher
VOMS_CERT_FOLDER=tmp
VOMS_CERT_FILE=x509up_u1210
REMOTE_VOMS_CERT_FOLDER=/home/fogbow/Dev/keys/cert/

#Execution INFO
FETCHER_EXECUTION_INFO=scheduler-info/fetcher-exec.info
if [ ! -e "$FETCHER_EXECUTION_INFO" ]; then
	echo "Creating execution info file"
	touch $FETCHER_EXECUTION_INFO
	echo "FETCHER_START_INFRASTRUCTURE_DATE=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_INSTANCE_ID=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_USER_NAME=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_INSTANCE_IP=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_INSTANCE_PORT=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_EXTRA_PORT=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_STORAGE_ATTACHMENT_ID=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_STORAGE_ID=" >> $FETCHER_EXECUTION_INFO
	echo "FETCHER_STORAGE_FORMATED=NO" >> $FETCHER_EXECUTION_INFO

fi
sed -i "/FETCHER_START_INFRASTRUCTURE_DATE=/ s/=.*/='$(date)'/" $FETCHER_EXECUTION_INFO

source $FETCHER_EXECUTION_INFO

STORAGE_COMMAND="FORMAT";

if [ "$STORAGE_FORMATED" = "YES" ]; then
	STORAGE_COMMAND="RETRIEVE";
fi


#VARIABLES
PRIVATE_KEY_FILE=/home/gustavorag/.ssh/id_rsa;

########### SCHEDULER INFRASTRUCTURE ##############

#Starting infrastructure for Scheduler.
#Verify the rigths paths for scheduler.conf and schedulerSpec 
CONFIG_FILE=target/classes/sebal.conf 
SPEC_FILE=target/classes/fetcherSpec
echo "Requesting Instance for Fetcher".
VM_FETCHER_INFO="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain compute $CONFIG_FILE $SPEC_FILE 2>&1)";
echo $VM_FETCHER_INFO
if [[ ! $VM_FETCHER_INFO == *"INSTANCE_ID"* ]]; then
	echo $VM_FETCHER_INFO
	echo "There is no resource available for deploy Fetcher App."
	exit
fi
echo "Instance sucessfully requested".

#PREPARING VARIABLES FOR SSH/SCP
#Sample return USER_NAME=fogbow;SSH_HOST=192.168.0.1;SSH_HOST=9000;SSH_HOST=
INSTANCE_ID=$(echo $VM_FETCHER_INFO | cut -d";" -f1 | cut -d"=" -f2)
USER_NAME=$(echo $VM_FETCHER_INFO | cut -d";" -f2 | cut -d"=" -f2)
INSTANCE_IP=$(echo $VM_FETCHER_INFO | cut -d";" -f3 | cut -d"=" -f2)
INSTANCE_PORT=$(echo $VM_FETCHER_INFO | cut -d";" -f4 | cut -d"=" -f2)
EXTRA_PORT=$(echo $VM_FETCHER_INFO | cut -d";" -f5 | cut -d"=" -f2)

echo $INSTANCE_ID;
echo $USER_NAME;
echo $INSTANCE_IP;
echo $INSTANCE_PORT;
echo $EXTRA_PORT;

#Request Storage for Sheduler DB

#Verify if db info exists. If true, the storage for Sheduler DB already exists and dont need to create new one.
echo "Previous Storage ID: $DB_STORAGE_ID";
if [ -n "$DB_STORAGE_ID" ]; then

	echo "Storage found. ID: $DB_STORAGE_ID"

  	STORAGE_STATUS="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain test-storage $DB_STORAGE_ID $CONFIG_FILE $SPEC_FILE 2>&1)";
  	STORAGE_STATUS=$(echo $STORAGE_STATUS | cut -d";" -f1 | cut -d"=" -f2)
  	echo "Storage status: $STORAGE_STATUS";
  	if [ ! $STORAGE_STATUS = "active" ]; then
  		DB_STORAGE_ID="";
  	fi
fi

if [ -z "$DB_STORAGE_ID" ]; then
	
	echo "Creating new Storage with size $STORAGE_SIZE"

	#This tow variables indicates if a new storage is been used. For new storage, the disk must be formated and the database must be created.
	STORAGE_INFO="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain storage $STORAGE_SIZE $CONFIG_FILE $SPEC_FILE 2>&1)";
	echo $STORAGE_INFO

	DB_STORAGE_ID=$(echo $STORAGE_INFO | cut -d";" -f1 | cut -d"=" -f2)

	echo "New storage created: "$DB_STORAGE_ID;

fi
#Attache the storage to the VM.
echo "Attaching $DB_STORAGE_ID to $INSTANCE_ID";
DB_STORAGE_ATTACHMENT_INFO="$(java $CONF_LOG -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain attachment $INSTANCE_ID $DB_STORAGE_ID $CONFIG_FILE $SPEC_FILE 2>&1)";

if [[ ! $DB_STORAGE_ATTACHMENT_INFO == *"ATTACHMENT_ID"* ]]; then
	echo $DB_STORAGE_ATTACHMENT_INFO
	echo "Erro while attaching $DB_STORAGE_ID to $INSTANCE_ID."
	exit
fi

echo $DB_STORAGE_ATTACHMENT_INFO;

DB_STORAGE_ATTACHMENT_ID=$(echo $DB_STORAGE_ATTACHMENT_INFO | cut -d";" -f1 | cut -d"=" -f2)

echo "Attach ID: $DB_STORAGE_ATTACHMENT_ID";


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


#Preparing sebal db
echo "PREPARING DATABASE..."
REMOTE_COMMAND="sudo sh /$REMOTE_FILE_PATH/$LOCAL_FILE_PATH/install_postgres_client.sh";
echo "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND


#Installing JAVA
echo "INSTALLING JAVA..."
REMOTE_COMMAND="echo 'Y\n' | sudo apt-get install default-jre";
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND

REMOTE_COMMAND="echo 'Y\n' | sudo apt-get install default-jdk"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND

#Installing git
echo "INSTALLING GIT..."
REMOTE_COMMAND="sudo sh /$REMOTE_FILE_PATH/$LOCAL_FILE_PATH/install_git.sh";
echo "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND

#Coping Scheduler App file to Scheduler VM
echo "UPLOAD SCHEDULER PACKAGE..."
LOCAL_FILE_PATH="$APP_FILE_LOCAL_DIR/$APP_FILE"
REMOTE_FILE_PATH="$APP_FILE"
echo "scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH"
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH

#UNTAR APP FILE
echo "EXTRACTING SCHEDULER..."
REMOTE_COMMAND="tar -vzxf $REMOTE_FILE_PATH";
echo "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $INSTANCE_PORT -i $PRIVATE_KEY_FILE  $USER_NAME@$INSTANCE_IP $REMOTE_COMMAND

#PREPARING CERT FILE SCP COMMAND
echo "UPLOAD CERTIFICATES..."
LOCAL_FILE_PATH="/$VOMS_CERT_FOLDER/$VOMS_CERT_FILE";
FILE_PATH="/$REMOTE_VOMS_CERT_FOLDER/$VOMS_CERT_FILE";
#Coping certificate file to Scheduler VM
echo "scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH"
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $INSTANCE_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $USER_NAME@$INSTANCE_IP:$REMOTE_FILE_PATH

#WRITING EXECUTION FILE
echo "Writing execution file..."


#Putting informations on Scheduler execution info.
sed -i "/FETCHER_INSTANCE_ID=/ s/=.*/=$INSTANCE_ID/" $FETCHER_EXECUTION_INFO
sed -i "/FETCHER_USER_NAME=/ s/=.*/=$USER_NAME/" $FETCHER_EXECUTION_INFO
sed -i "/FETCHER_INSTANCE_IP=/ s/=.*/=$INSTANCE_IP/" $FETCHER_EXECUTION_INFO
sed -i "/FETCHER_INSTANCE_PORT=/ s/=.*/=$INSTANCE_PORT/" $FETCHER_EXECUTION_INFO
sed -i "/FETCHER_EXTRA_PORT=/ s/=.*/=$EXTRA_PORT/" $FETCHER_EXECUTION_INFO
#Update  DB INFO FILE with the new attachment id.
sed -i "/FETCHER_STORAGE_ATTACHMENT_ID=/ s/=.*/=$STORAGE_ATTACHMENT_ID/" $FETCHER_EXECUTION_INFO
sed -i "/FETCHER_STORAGE_ID=/ s/=.*/=$STORAGE_ID/" $FETCHER_EXECUTION_INFO
sed -i "/FETCHER_STORAGE_FORMATED=/ s/=.*/=YES/" $SCHEDULER_EXECUTION_INFO
