#!/bin/bash
desired_state=$1
desired_date=$2

scheduler_db_user_name=sebal
scheduler_db_password=S3B4L
scheduler_db_name=sebal

scheduler_db_ip=10.11.4.214
scheduler_db_port=5432

sudo su postgres -c "echo -e \"$scheduler_db_password\n\" | psql -h $scheduler_db_ip -p $scheduler_db_port -d $scheduler_db_name -U $scheduler_db_user_name -c \"SELECT image_name, state, utime, federation_member FROM nasa_images WHERE state = '$desired_state';\"" > $desired_state-images.txt

cat $desired_state-images.txt | grep $desired_date
sudo rm $desired_state-images.txt
