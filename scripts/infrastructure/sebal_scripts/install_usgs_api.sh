#!/bin/bash
echo -e "Y\n" | apt-get install python-pip
wget https://bootstrap.pypa.io/get-pip.py
python get-pip.py
pip install usgs

rm get-pip.py
