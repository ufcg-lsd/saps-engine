#!/bin/bash
FETCHER_PUBLIC_KEY="$1"

echo "$FETCHER_PUBLIC_KEY" >> /home/fogbow/.ssh/authorized_keys
