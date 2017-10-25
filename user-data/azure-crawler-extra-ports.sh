#!/bin/bash

apt-get update
apt-get install nfs-kernel-server

mkdir -p /local/exports
echo "/local/exports *(rw,insecure,no_subtree_check,async,no_root_squash)" >> /etc/exports

service nfs-kernel-server restart

mkdir -p /var/log/sebal-execution
touch /var/log/sebal-execution/sebal-execution.log
chmod 777 /var/log/sebal-execution/sebal-execution.log

create-fogbow-tunnel nfs 2049 &
