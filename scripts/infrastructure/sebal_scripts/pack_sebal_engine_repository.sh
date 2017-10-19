#!/bin/bash

LOCAL_REPOSITORY_PATH=$1
SANDBOX_DIR=$2

echo "Cloning sebal-engine repository into $LOCAL_REPOSITORY_PATH"

cd $LOCAL_REPOSITORY_PATH
git clone https://github.com/fogbow/sebal-engine.git
cd sebal-engine
# FIXME remember to change this later
# version used will be master in final sebal-engine
git checkout develop
mvn -e install -Dmaven.test.skip=true

SEBAL_ENGINE_VERSION=$(git rev-parse HEAD)
echo "$SEBAL_ENGINE_VERSION" > sebal-engine.version.$SEBAL_ENGINE_VERSION

cd config
sudo rm sebal.conf

cp $SANDBOX_DIR/config/sebal.conf .

cd ../..

echo "Packing sebal-engine repository from $LOCAL_REPOSITORY_PATH"
tar -cvzf sebal-engine-pkg.tar.gz sebal-engine

rm -r sebal-engine
