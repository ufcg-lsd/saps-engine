#!/bin/bash
images_dir_path=$1
error_images_dir_path=$2
desired_error=$3

images_file_path=/home/ubuntu/images_list.txt
images_errors_file_path=/home/ubuntu/images_errors.txt

ls $images_dir_path > $images_file_path
touch $images_errors_file_path

while read image_name
do
  if [ -d $error_images_dir_path/$image_name ]
  then
    for file in $error_images_dir_path/$image_name/*
    do
      if [[ $file == "temp-worker-run-*" ]]
      then
        echo "Image $image_name error:"
        tail -n 20 $file >> $images_errors_file_path
      fi
    done
  fi
done <$images_file_path

rm $images_file_path
