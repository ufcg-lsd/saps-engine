#!/bin/sh
#initialize script with sudo
apt-get update
apt-get install nfs-kernel-server
echo "/local/exports *(rw,insecure,no_subtree_check,async)" | tee --append /etc/exports
exportfs -a
service nfs-kernel-server restart
