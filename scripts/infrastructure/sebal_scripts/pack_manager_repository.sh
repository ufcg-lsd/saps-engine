#!/bin/bash

LOCAL_REPOSITORY_PATH=$1

echo "Cloning manager repository into $LOCAL_REPOSITORY_PATH"

cd $LOCAL_REPOSITORY_PATH
git clone https://github.com/fogbow/fogbow-manager.git

cd fogbow-manager
git checkout develop
mvn -e install -Dmaven.test.skip=true

cd ..

tar -cvzf manager-pkg.tar.gz fogbow-manager
rm -r fogbow-manager
