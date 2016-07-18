#!/bin/bash

SCHEDULER_EXECUTION_INFO=scheduler-info/scheduler-exec.info
CRAWLER_EXECUTION_INFO=scheduler-info/crawler-exec.info
FETCHER_EXECUTION_INFO=scheduler-info/fetcher-exec.info

source sebal.properties


#Chamar os scripts de InfraStrutura
sh ./StartCrawlerInfra.sh
sh ./StartFetcherInfra.sh
sh ./StartSchedulerInfra.sh

#Loading infrastructure informations.
source $SCHEDULER_EXECUTION_INFO
source $CRAWLER_EXECUTION_INFO
source $FETCHER_EXECUTION_INFO

#Verifying scheduler infrastructure
if [ -z "$SCHEDULER_INSTANCE_IP" ]; then
	echo "ERROR: Instance for shceduler app wasn't found."
	exit
fi
if [ -z "$SCHEDULER_INSTANCE_PORT" ]; then
	echo "ERROR: Instance for shceduler isn't accessible. Port not defined."
	exit
fi
if [ -z "$DB_STORAGE_PORT" ]; then
	echo "ERROR: Sebal data base isn't accessible. Port not defined."
	exit
fi

#Verifying fetcher infrastructure
if [ -z "$FETCHER_INSTANCE_IP" ]; then
	echo "ERROR: Instance for fetcher app wasn't found."
	exit
fi
if [ -z "$FETCHER_INSTANCE_PORT" ]; then
	echo "ERROR: Instance for fetcher isn't accessible. Port not defined."
	exit
fi

#Verifying crawler infrastructure
if [ -z "$CRAWLER_INSTANCE_IP" ]; then
	echo "ERROR: Instance for crawler app wasn't found."
	exit
fi
if [ -z "$CRAWLER_INSTANCE_PORT" ]; then
	echo "ERROR: Instance for crawler isn't accessible. Port not defined."
	exit
fi
if [ -z "$CRAWLER_NFS_PORT" ]; then
	echo "ERROR: Instance for nfs isn't accessible. Port not defined."
	exit
fi


#Parâmetros:
#Crawler: sebal.conf, DB_IP (SCHEDULER_INSTANCE_IP), DB_Port (DB_STORAGE_PORT) e Federation Member;
#SebalMain: (chama o Scheduler): sebal.conf, DB_IP, DB_Port, NFS_IP (CRAWLER_INSTANCE_IP) e NFS_Port (CRAWLER_NFS_PORT);
#Fetcher: sebal.conf, DB_IP (SCHEDULER_INSTANCE_IP), DB_Port (DB_STORAGE_PORT), FTP_IP (CRAWLER_INSTANCE_IP) e FTP_Port (CRAWLER_INSTANCE_PORT).

#STARTING CRAWLER APP.
START_CRAWLER_COMMAND="sudo java -cp $CRAWLER_BASE_DIR/target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* 
org.fogbowcloud.sebal.crawler.CrawlerMain $CRAWLER_BASE_DIR/src/main/resources/sebal.conf \ 
$SCHEDULER_INSTANCE_IP $DB_STORAGE_PORT $CRAWLER_INSTANCE_IP $CRAWLER_NFS_PORT";


REMOTE_COMMAND=$START_CRAWLER_COMMAND;
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $CRAWLER_INSTANCE_PORT -i $PRIVATE_KEY_FILE  $CRAWLER_USER_NAME@$CRAWLER_INSTANCE_IP $REMOTE_COMMAND


#STARTING SCHEDULER APP.
START_SCHEDULER_COMMAND="sudo java -cp $SCHEDULER_BASE_DIR/target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* 
org.fogbowcloud.scheduler.SebalMain $SCHEDULER_BASE_DIR/src/main/resources/sebal.conf \ 
$SCHEDULER_INSTANCE_IP $DB_STORAGE_PORT $CRAWLER_INSTANCE_IP $CRAWLER_NFS_PORT";

REMOTE_COMMAND=$START_SCHEDULER_COMMAND;
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $SCHEDULER_INSTANCE_PORT -i $PRIVATE_KEY_FILE  $SCHEDULER_USER_NAME@$SCHEDULER_INSTANCE_IP $REMOTE_COMMAND


#STARTING FETCHER APP.
START_FETCHER_COMMAND="sudo java -cp $FETCHER_BASE_DIR/target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* 
org.fogbowcloud.sebal.fetcher.FetcherMain $FETCHER_BASE_DIR/src/main/resources/sebal.conf \ 
$SCHEDULER_INSTANCE_IP $DB_STORAGE_PORT $CRAWLER_INSTANCE_IP $CRAWLER_INSTANCE_PORT";


REMOTE_COMMAND=$START_CRAWLER_COMMAND;
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p $FETCHER_INSTANCE_PORT -i $PRIVATE_KEY_FILE  $FETCHER_USER_NAME@$FETCHER_INSTANCE_IP $REMOTE_COMMAND
