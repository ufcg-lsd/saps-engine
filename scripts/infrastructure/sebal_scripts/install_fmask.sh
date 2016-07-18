#!/bin/bash

COMMAND=$1

apt-get install unzip

mkdir Fmask
cd Fmask/

wget http://ftp-earth.bu.edu/public/zhuzhe/Fmask_Linux_3.2v/Fmask_pkg.zip
unzip Fmask_pkg.zip
unzip MCRInstaller.zip
./install -mode silent -agreeToLicense yes
