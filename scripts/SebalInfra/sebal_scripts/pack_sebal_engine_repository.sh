#!/bin/bash

LOCAL_REPOSITORY_PATH=$1

echo "Cloning sebal-engine remote repository into $LOCAL_REPOSITORY_PATH"

cd $LOCAL_REPOSITORY_PATH
git clone https://github.com/fogbow/sebal-engine.git
cd sebal-engine

SEBAL_ENGINE_VERSION=$(git rev-parse HEAD)
echo "$SEBAL_ENGINE_VERSION" > sebal-engine.version.$SEBAL_ENGINE_VERSION
