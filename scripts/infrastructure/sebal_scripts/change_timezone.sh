#!/bin/bash

echo "America/Recife" > /etc/timezone    
dpkg-reconfigure -f noninteractive tzdata
