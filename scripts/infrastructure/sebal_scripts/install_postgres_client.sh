#!/bin/bash

COMMAND=$1
VERSION=9.3
CONFIG_FILE=/etc/postgresql/$VERSION/main/postgresql.conf

apt-get update
echo "Y\n" | apt-get install postgresql-$VERSION

service postgresql start
