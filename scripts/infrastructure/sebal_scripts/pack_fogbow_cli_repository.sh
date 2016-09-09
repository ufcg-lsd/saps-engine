#!/bin/bash

LOCAL_REPOSITORY_PATH=$1

echo "Cloning fogbow-cli repository into $LOCAL_REPOSITORY_PATH"

cd $LOCAL_REPOSITORY_PATH
git clone https://github.com/fogbow/fogbow-cli.git

cd fogbow-manager
git checkout develop
mvn -e install -Dmaven.test.skip=true

cd ..

tar -cvzf fogbow-cli-pkg.tar.gz fogbow-cli
rm -r fogbow-cli
