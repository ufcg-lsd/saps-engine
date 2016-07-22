#!/bin/bash

COMMAND=$1
VERSION=9.3
DATABASE_DIR=/local/exports/postgres
DATABASE_DIR_CHAR="'\/local\/exports\/postgres'"
CONFIG_FILE=/etc/postgresql/$VERSION/main/postgresql.conf

#apt-get update
#echo "Y\n" | apt-get install postgresql-$VERSION
if [ ! -d "$DATABASE_DIR" ]; then
  mkdir -p $DATABASE_DIR
fi
chown -R postgres:postgres $DATABASE_DIR

su postgres <<'EOF'
VERSION=9.3
DATABASE_DIR=/local/exports/postgres
/usr/lib/postgresql/$VERSION/bin/initdb -D $DATABASE_DIR
service postgresql stop
EOF
sed -i "/data_directory =/ s/=.*/=$DATABASE_DIR_CHAR/" $CONFIG_FILE
#sed -i 's/peer/md5/g' /etc/postgresql/$VERSION/main/pg_hba.conf
service postgresql start

if[ "$COMMAND" = "CREATE" ]; then
	su postgres <<'EOF'
	psql -c "CREATE USER sebal WITH PASSWORD 'S3B4L';"
	psql -c "CREATE DATABASE sebal OWNER sebal;"
	psql -c "GRANT ALL PRIVILEGES ON DATABASE sebal TO sebal;"
	EOF
fi

