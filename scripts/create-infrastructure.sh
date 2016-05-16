#!/bin/sh
DIRNAME=`dirname $0`
SPEC_FILE_PATH=$1

# string 'true' or 'false'
WANT_STORAGE=$2

$LIBRARY_PATH=/usr/local/lib

$INFRA_TXT_FILE=$SPEC_FILE_PATH"_"infra.txt

echo "Creating infrastructure..."

#--------------------------- Initializing Scheduler Infrastructure ------------------------------#
# see if additional library will be necessary
java -Xmx1G -Xss1G -Djava.library.path=$LIBRARY_PATH -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain src/main/resources/sebal.conf $SPEC_FILE_PATH $WANT_STORAGE > $INFRA_TXT_FILE
SCHEDULER_INSTANCE_USER=$(sed '1!d' $INFRA_TXT_FILE)
SCHEDULER_INSTANCE_IP=$(sed '2!d' $INFRA_TXT_FILE)
SCHEDULER_INSTANCE_PORT=$(sed '3!d' $INFRA_TXT_FILE)
SCHEDULER_INSTANCE_EXTRA_PORT=$(sed '4!d' $INFRA_TXT_FILE)
#------------------------------------------------------------------------------------------------#

echo "Operation finished."
