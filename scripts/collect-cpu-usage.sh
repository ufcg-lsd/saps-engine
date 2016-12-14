#!/bin/bash


while true; do
    top -bn1 | grep "load"
    sleep 1
done
