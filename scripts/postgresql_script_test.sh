#!/bin/bash
MAX_DAYS=$1

TIMESTAMPS_FILE_NAME="timestamps.txt"
CONSTANT=$((24 * 3600))
FINAL_EPOCH=

function convertTimestampToEpoch {
  local DATE=$1
  local TIME=$2

  FINAL_EPOCH=$(date -d "$DATE $TIME" +%s)
}

function main {
  COMMAND_OUTPUT="$(su postgres -c "echo -e \"S3B4L\n\" | psql -d sebal -U sebal -c \"SELECT utime FROM nasa_images WHERE state = 'fetched';\"" > $TIMESTAMPS_FILE_NAME)"

  CURRENT_EPOCH="$(date +%s)"

  sed -i '1d' $TIMESTAMPS_FILE_NAME
  sed -i '1d' $TIMESTAMPS_FILE_NAME
  sed -i '$d' $TIMESTAMPS_FILE_NAME
  sed -i '$d' $TIMESTAMPS_FILE_NAME

  IFS=$'\n'       # make newlines the only separator
  set -f
  for line in $(cat $TIMESTAMPS_FILE_NAME)
  do
    TIMESTAMP=$line
    echo "$TIMESTAMP" | awk '{split($0,a," "); print a[2]}'
    DATE=$(echo "$TIMESTAMP" | awk '{split($0,a," "); print a[1]}')
    TIME=$(echo "$TIMESTAMP" | awk '{split($0,a," "); print a[2]}')
    convertTimestampToEpoch $DATE $TIME
    
    EPOCH_DAYS_SINCE=$(($CURRENT_EPOCH - $FINAL_EPOCH))
    DAYS_SINCE=$(($EPOCH_DAYS_SINCE / $CONSTANT))
    if [ $DAYS_SINCE -le $MAX_DAYS ]
    then
      SELECT_OUTPUT="$(su postgres -c "echo -e \"S3B4L\n\" | psql -d sebal -U sebal -c \"SELECT image_name, state, utime, federation_member FROM nasa_images WHERE utime = '$TIMESTAMP';\"")"
      echo "$SELECT_OUTPUT" >> fetched-files.txt
    fi
  done < $TIMESTAMPS_FILE_NAME
}

main
