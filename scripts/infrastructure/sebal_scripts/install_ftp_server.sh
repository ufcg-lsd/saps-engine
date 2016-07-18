#!/bin/bash

COMMAND=$1
VERSION=9.3
CONFIG_FILE=/etc/vsftpd.conf

apt-get update
echo "Y\n" | apt-get install vsftpd

service vsftpd stop

sed -i "/listen =/ s/=.*/=YES/" $CONFIG_FILE
sed -i "/anonymous_enable =/ s/=.*/=YES/" $CONFIG_FILE
sed -i "/local_enable =/ s/=.*/=YES/" $CONFIG_FILE

service vsftpd restart
