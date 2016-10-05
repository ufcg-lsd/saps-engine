#!/bin/bash

HOST_NAME=$(cat /etc/hostname)

echo "Inserting hostname $HOST_NAME into /etc/hosts"
echo "127.0.0.1       $HOST_NAME" >> /etc/hosts
