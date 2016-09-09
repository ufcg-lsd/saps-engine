#!/bin/bash

echo "America/Bahia" > /etc/timezone    
dpkg-reconfigure -f noninteractive tzdata
