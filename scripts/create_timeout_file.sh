#!/bin/bash
SCRIPT_OUTPUT_PATH=$1

TIMEOUT_MESSAGE=$(cat $SCRIPT_OUTPUT_PATH | grep "NUMBER OF TIMEOUTS")

echo "Creating file for timeouts"
echo "$TIMEOUT_MESSAGE" >> number_of_timeouts.txt
