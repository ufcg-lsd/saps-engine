#!/bin/bash

COMMAND=$1
CONFIG_FILE=/etc/exports

apt-get update
echo "Y\n" | apt-get install nfs-kernel-server

service nfs-kernel-server stop

echo "/local/exports *(rw,insecure,no_subtree_check,async,no_root_squash)" >> $CONFIG_FILE

exportfs -a
service nfs-server-kernel restart
