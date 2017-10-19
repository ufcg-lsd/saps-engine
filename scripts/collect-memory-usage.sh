#!/bin/bash

while true; do
  free -m | head -n 2 | tail -n 1
  sleep 1
done
