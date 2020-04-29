#!/bin/bash

sudo docker stack deploy -c $(pwd)/deploy/docker-compose.yml saps