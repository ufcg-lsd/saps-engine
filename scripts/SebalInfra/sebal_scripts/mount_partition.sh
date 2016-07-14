#!/bin/sh
#lembrar de chamar o script como sudo

COMMAND=$1

mkdir -p /local/exports
if [ "$COMMAND" = "FORMAT" ]; then
	echo "n\np\n\n\n\nw" | fdisk /dev/vdb
	mkfs.ext4 /dev/vdb1
fi
mount /dev/vdb1 /local/exports
