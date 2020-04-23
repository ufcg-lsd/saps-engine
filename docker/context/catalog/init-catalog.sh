#!/bin/bash
set -e

echo "Setting up catalog server..."

sed -i 's/peer/md5/g' ${PGDATA}/pg_hba.conf
echo "host all all 0.0.0.0/0 md5" >> ${PGDATA}/pg_hba.conf
sed -i "$ a\listen_addresses = '*'" ${PGDATA}/postgresql.conf