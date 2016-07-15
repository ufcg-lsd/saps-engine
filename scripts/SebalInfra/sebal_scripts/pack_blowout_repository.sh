#!/bin/bash

LOCAL_REPOSITORY_PATH=$1

echo "Cloning blowout remote repository into $LOCAL_REPOSITORY_PATH"

cd $LOCAL_REPOSITORY_PATH
git clone https://github.com/fogbow/blowout.git

cd blowout

BLOWOUT_VERSION=$(git rev-parse HEAD)
cd ../sebal-engine
echo "$BLOWOUT_VERSION" > blowout.version.$BLOWOUT_VERSION
