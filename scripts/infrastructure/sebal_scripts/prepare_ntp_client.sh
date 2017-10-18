#!/bin/bash

apt-get update
apt install -y ntp

sed -i "/server 0.ubuntu.pool.ntp.org/d" /etc/ntp.conf
sed -i "/server 1.ubuntu.pool.ntp.org/d" /etc/ntp.conf
sed -i "/server 2.ubuntu.pool.ntp.org/d" /etc/ntp.conf
sed -i "/server 3.ubuntu.pool.ntp.org/d" /etc/ntp.conf

sed -i "/server ntp.ubuntu.com/d" /etc/ntp.conf

echo "server ntp.lsd.ufcg.edu.br" >> /etc/ntp.conf

service ntp restart
