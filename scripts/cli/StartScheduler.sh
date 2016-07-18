#!/bin/bash
DIRNAME=`dirname $0`
LOG4J=log4j.properties
cd $DIRNAME/..
if [ -f $LOG4J ]; then
CONF_LOG=-Dlog4j.configuration=file:$LOG4J
else
CONF_LOG=
fi

PRIVATE_KEY_FILE=$1

SANDBOX_DIR=sebal-engine
VM_SCHEDULER_INFO=$SANDBOX_DIR/scripts/infrastructure/scheduler-info

source scheduler.properties;

#VARIABLES
USER_NAME=$(echo $VM_SCHEDULER_INFO | cut -d";" -f2 | cut -d"=" -f2)
VM_IP=$(echo $VM_SCHEDULER_INFO | cut -d";" -f3 | cut -d"=" -f2)
VM_PORT=$(echo $VM_SCHEDULER_INFO | cut -d";" -f4 | cut -d"=" -f2)
EXTRA_PORT=$(echo $VM_SCHEDULER_INFO | cut -d";" -f5 | cut -d"=" -f2)

#SSH TO VM
REMOTE_COMMAND;

#Commands
SCP_UPLOAD_COMMAND="scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $VM_PORT -i $PRIVATE_KEY_FILE $LOCAL_FILE_PATH $ENV_SSH_USER@$VM_IP//$REMOTE_FILE_PATH";
SSH_COMMAND="ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $VM_PORT -i $PRIVATE_KEY_FILE $ENV_SSH_USER@$VM_IP $REMOTE_COMMAND";
UNTAR_FILE_COMMAND="tar -vzxf $REMOTE_FILE_PATH";

########### SCHEDULER EXECUTION ##############

#Starting infrastructure for Scheduler.
#Verify the rigths paths for scheduler.conf and schedulerSpec 
VM_SCHEDULER_INFO="$(java $CONF_LOG org.fogbowcloud.infrastructure.InfrastructureMain compute scheduler.conf schedulerSpec 2>&1)";
if [ -z "\$VM_SCHEDULER_INFO" ]; then
	echo "There is no resource available for deploy Scheduler App."
	exit
fi

#PREPARING VARIABLES FOR SSH/SCP
#Sample return USER_NAME=fogbow;SSH_HOST=192.168.0.1;SSH_HOST=9000;SSH_HOST=
INSTANCE_ID=$RETURN | cut -d";"  -f1 | cut -d"=" -f2
USER_NAME=$RETURN | cut -d";"  -f2 | cut -d"=" -f2
VM_IP=$RETURN | cut -d";"  -f3 | cut -d"=" -f2
VM_PORT=$RETURN | cut -d";"  -f4 | cut -d"=" -f2
EXTRA_PORT=$RETURN | cut -d";"  -f5 | cut -d"=" -f2


PRIVATE_KEY_FILE=

#Request Storage for Sheduler DB
SCHEDULER_DB_INFO=sheduler-db/sheduler-db-info
DB_STORAGE_ID=
DB_STORAGE_ATTACHMENT_ID=
STORAGE_COMMAND=MOUNT
DATABASE_COMMAND=RETRIEVE
#Verify if db info exists. If true, the storage for Sheduler DB already exists and dont need to create new one.
if [ -e "$SCHEDULER_DB_INFO" ]
then
	source $SCHEDULER_DB_INFO
  	DB_STORAGE_ID=$SCHEDULER_DB_STORAGE_ID;
  	STORAGE_STATUS="$(java $CONF_LOG org.fogbowcloud.infrastructure.InfrastructureMain test-storage $DB_STORAGE_ID 2>&1)";
  	if[$STORAGE_STATUS != "active"]
  	then
  		DB_STORAGE_ID=
  	fi
fi
if [ -z "$DB_STORAGE_ID" ]; then
	
	#This tow variables indicates if a new storage is been used. For new storage, the disk must be formated and the database must be created.
	STORAGE_COMMAND=FORMAT
	DATABASE_COMMAND=CREATE
	
	STORAGE_INFO="$(java $CONF_LOG org.fogbowcloud.infrastructure.InfrastructureMain storage 10 2>&1)";
	rm $SCHEDULER_DB_INFO
	DB_STORAGE_ID=$STORAGE_INFO | cut -d";"  -f1 | cut -d"=" -f2
	echo "SCHEDULER_DB_STORAGE_ID=$DB_STORAGE_ID" >> $SCHEDULER_DB_INFO
	echo "SCHEDULER_DB_ORDER_ID=" >> $SCHEDULER_DB_INFO
	echo "SCHEDULER_DB_VM_ATTACHED_ID=" >> $SCHEDULER_DB_INFO
fi
#Attache the storage to the VM.
DB_STORAGE_ATTACHMENT_ID ="$(java $CONF_LOG org.fogbowcloud.infrastructure.InfrastructureMain $INSTANCE_ID $DB_STORAGE_ID 2>&1)";

#Update  DB INFO FILE with the new attachment id.
sed -i "s/\(SCHEDULER_DB_ORDER_ID=\).*\$/\1${INSTANCE_ID}/" $SCHEDULER_DB_INFO
sed -i "s/\(SCHEDULER_DB_VM_ATTACHED_ID=\).*\$/\1${DB_STORAGE_ATTACHMENT_ID}/" $SCHEDULER_DB_INFO

#Coping scripts to mount disk.
SCRIPTS_PATH="sebal_scripts/"
REMOTE_FILE_PATH="$REMOTE_BASE_DIR"
SCP_UPLOAD_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to Upload Sebal Scripts App to remote VM. Verify user permissions!"
	exit
fi

REMOTE_COMMAND="sudo chmod -R 777 &REMOTE_FILE_PATH/$SCRIPTS_PATH";
SSH_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to modify Sebal Scripts on remote VM. Verify user permissions!"
	exit
fi

#Preparing storage
REMOTE_COMMAND="sudo ./&REMOTE_FILE_PATH/$SCRIPTS_PATH/mount_partition.sh $STORAGE_COMMAND";
SSH_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to mount Sebal Storage!"
	exit
fi

#Preparing sebal db
REMOTE_COMMAND="sudo ./&REMOTE_FILE_PATH/$SCRIPTS_PATH/install_postgres.sh $DATABASE_COMMAND";
SSH_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to mount Sebal Storage!"
	exit
fi

#Coping Scheduler App file to Scheduler VM
LOCAL_FILE_PATH="$APP_FILE_LOCAL_DIR/$APP_FILE"
REMOTE_FILE_PATH="$REMOTE_BASE_DIR$APP_FILE"
SCP_UPLOAD_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to Upload Scheduler App to remote VM. Verify user permissions!"
	exit
fi

#UNTAR APP FILE
REMOTE_COMMAND=$UNTAR_FILE_COMMAND;
SSH_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to extract Scheduler App on remote VM. Verify user permissions!"
	exit
fi

#PREPARING CERT FILE SCP COMMAND
LOCAL_FILE_PATH="$VOMS_CERT_FOLDER$VOMS_CERT_FILE";
REMOTE_FILE_PATH="$REMOTE_VOMS_CERT_FOLDER$VOMS_CERT_FILE";
#Coping certificate file to Scheduler VM
SCP_UPLOAD_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to Upload Voms Certificate to remote VM. Verify user permissions!"
	exit
fi

#STARTING SCHEDULER APP.
START_SCHEDULER_COMMAND="sudo java -cp $REMOTE_BASE_DIR$APP_BASE_DIR/target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* 
org.fogbowcloud.scheduler.SebalMain $REMOTE_BASE_DIR$APP_BASE_DIR/src/main/resources/sebal.conf \ 
$DATABASE_IP $DATABASE_PORT $NFS_SERVER_IP $NFS_SERVER_PORT";

REMOTE_COMMAND=$START_SCHEDULER_COMMAND;
SSH_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Scheduler App finished with error. Exit code: $EXIT_CODE"
	exit
fi

########### CRAWLER EXECUTION ##############


#Starting infrastructure for Crawler.
#Verify the rigths paths for scheduler.conf and crawlerSpec 
SSH_VM_CRAWLER="$(java $CONF_LOG org.fogbowcloud.infrastructure.InfrastructureMain scheduler.conf crawlerSpec 2>&1)";
if [ -z "\$SSH_VM_CRAWLER" ]; then
	echo "There is no resource available for deploy Crawler App."
	exit
fi
OLD_IFS=\$IFS              # save internal field separator
IFS=":"                    # set it to ':'
set -- \$SSH_VM_CRAWLER  # make the result positional parameters
IFS=\$OLD_IFS              # restore IFS
VM_CRAWLER_IP=\$1
VM_CRAWLER_PORT=\$2

#PREPARING APP FILE SCP COMMAND
VM_IP=$VM_CRAWLER_IP;
VM_PORT=$VM_CRAWLER_PORT;
PRIVATE_KEY_FILE=

#mount storage for this VM
mountStorage

#Coping Crawler App file to Crawler VM
LOCAL_FILE_PATH="$APP_FILE_LOCAL_DIR/$APP_FILE"
REMOTE_FILE_PATH="$REMOTE_BASE_DIR$APP_FILE"
SCP_UPLOAD_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to Upload Crawler App to remote VM. Verify user permissions!"
	exit
fi

#UNTAR APP FILE
REMOTE_COMMAND=$UNTAR_FILE_COMMAND;
SSH_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to extract Scheduler App on remote VM. Verify user permissions!"
	exit
fi

#PREPARING CERT FILE SCP COMMAND
LOCAL_FILE_PATH="$VOMS_CERT_FOLDER$VOMS_CERT_FILE";
REMOTE_FILE_PATH="$REMOTE_VOMS_CERT_FOLDER$VOMS_CERT_FILE";
#Coping certificate file to Scheduler VM
SCP_UPLOAD_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Error while trying to Upload Voms Certificate to remote VM. Verify user permissions!"
	exit
fi

#STARTING CRAWLER APP.
START_CRAWLER_COMMAND="sudo java -cp $REMOTE_BASE_DIR$APP_BASE_DIR/target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* 
org.fogbowcloud.sebal.crawler.CrawlerMain $REMOTE_BASE_DIR$APP_BASE_DIR/src/main/resources/sebal.conf \ 
$DATABASE_IP $DATABASE_PORT $NFS_SERVER_IP $NFS_SERVER_PORT";

REMOTE_COMMAND=$START_CRAWLER_COMMAND;
SSH_COMMAND;
if[EXIT_CODE!=0]; then
	echo "Crawler App finished with error. Exit code: $EXIT_CODE"
	exit
fi


