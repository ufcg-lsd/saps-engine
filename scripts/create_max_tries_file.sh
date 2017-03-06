#!/bin/bash
SCRIPT_OUTPUT_PATH=$1

MAX_TRIES_MESSAGE=$(cat $SCRIPT_OUTPUT_PATH | grep "NUMBER OF MAX TRIES")

echo "$MAX_TRIES_MESSAGE" >> number_of_max_tries.txt
