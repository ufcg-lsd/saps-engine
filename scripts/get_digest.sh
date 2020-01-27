#!/bin/bash

IMAGE=$1
TAG=$2

CURL=`which curl`
JQ=`which jq`
SED=`which sed`

HOST_REGISTRY="https://hub.docker.com/v2"
PATH="repositories/$IMAGE/tags"

DIGEST=`$CURL --silent --header "Accept: application/json" $HOST_REGISTRY/$PATH | $JQ ".results[] | select(.name == \"$TAG\") | .images[].digest" | $SED -e 's/\"//g'`

echo $DIGEST
