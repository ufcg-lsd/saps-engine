#!/bin/sh
DIRNAME=`dirname $0`
DB_ADDRESS=$1
DB_PORT=$2
DB_USER=$3
DB_PASSWORD=$4
FIRST_YEAR=$5
LAST_YEAR=$6
REGIONS_FILE_PATH=$7
SPECIFIC_REGION=$8

$LIBRARY_PATH=/usr/local/lib

echo "Getting region images in database..."

java -Xmx1G -Xss1G -Djava.library.path=$LIBRARY_PATH -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.sebal.bootstrap.DBBootstrapMain src/main/resources/sebal.conf $DB_ADDRESS $DB_PORT $DB_USER $DB_PASSWORD get $FIRST_YEAR $LAST_YEAR $REGIONS_FILE_PATH $SPECIFIC_REGION > $SPECIFIC_REGION-images.txt

echo "Operation finished."
echo "Results writed in " $$SPECIFIC_REGION-images.txt
