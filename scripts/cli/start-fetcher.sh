#!/bin/bash
#
# Start fetcher application

if [[ $# -ne 4 ]]; then
  echo "Usage:" $0 "db-instance-ip db-port ftp-server-ip ftp-server-port"
  exit 1
fi

#RECEIVED PARAM
DB_INSTANCE_IP=$1
DB_PORT=$2
FTP_SERVER_IP=$3
FTP_SERVER_PORT=$4

#GLOBAL CONSTANTS
SANDBOX_DIR=/home/fogbow/sebal-engine
CONF_DIR=$SANDBOX_DIR/config
CONF_FILE_PATH=$CONF_DIR/sebal.conf
OUT_LOG_PATH=$SANDBOX_DIR/log
LIBRARY_PATH=/usr/local/lib

function main() {
  echo "Starting fetcher app"
  sudo java -Djava.library.path=$LIBRARY_PATH -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.engine.sebal.fetcher.FetcherMain $CONF_FILE_PATH $DB_INSTANCE_IP $DB_PORT $FTP_SERVER_IP $FTP_SERVER_PORT > $OUT_LOG_PATH/fetcher-app.out 2> $OUT_LOG_PATH/fetcher-app.err &
}

main
