#!/bin/bash
#
# Starts restlet application

SCHEDULER_INSTANCE_IP=$1
RESTLET_PORT=$2

SEBAL_ENGINE_PATH=/home/fogbow/sebal-engine
SEBAL_PROPERTIES_PATH=$SEBAL_ENGINE_PATH/config/sebal.properties
LOG4J_FILE_PATH=$SEBAL_ENGINE_PATH/config/log4j.properties
CONF_FILE_PATH=$SEBAL_ENGINE_PATH/config/sebal.conf

if [ "$SCHEDULER_INSTANCE_IP" = ""  ]
then
  echo "Invalid Scheduler IP...field is empty!"
  exit 1
fi

if [ "$RESTLET_PORT" = ""  ]
then
  echo "Invalid Restlet port...field is empty!"
  exit 1
fi

echo "http://$SCHEDULER_INSTANCE_IP:$RESTLET_PORT" > $SEBAL_PROPERTIES_PATH

java -Dlog4j.configuration=file:$LOG4J_FILE_PATH -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.engine.scheduler.restlet.DBRestMain $CONF_FILE_PATH &

PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Error while starting restlet server"
  exit $PROCESS_OUTPUT
fi
