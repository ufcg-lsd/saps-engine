#!/bin/bash

LOCAL_REPOSITORY_PATH=$1

echo "Cloning blowout repository into $LOCAL_REPOSITORY_PATH"

cd $LOCAL_REPOSITORY_PATH
git clone https://github.com/fogbow/blowout.git

cd blowout
# FIXME remember to change this later
# version used in blowout will be master
git checkout develop
mvn -e install -Dmaven.test.skip=true

BLOWOUT_VERSION=$(git rev-parse HEAD)
echo "$BLOWOUT_VERSION" > blowout.version.$BLOWOUT_VERSION

cd ..
tar -cvzf blowout-pkg.tar.gz blowout
rm -r blowout
