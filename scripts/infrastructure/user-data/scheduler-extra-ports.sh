#!/bin/bash
mkdir -p /tmp/keys
echo '/tmp/keys/scheduler_key_rsa' | ssh-keygen -t rsa -P ''
create-fogbow-tunnel postgres 5432 &
