#!/bin/bash

VERSION=9.3

sed -i 's/peer/md5/g' /etc/postgresql/$VERSION/main/pg_hba.conf

service postgresql restart
