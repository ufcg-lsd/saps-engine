#!/bin/bash
images_dir_path=$1
error_images_dir_path=$2
desired_error=$3

images_file_path=/home/ubuntu/images_list.txt

ls $images_dir_path > $images_file_path

while read image_name
do
  if [ -d $error_images_dir_path/$image_name ]
  then
    grep -R "$desired_error" *
    echo "Image $image_name presented the given error"
  fi
done <$images_file_path

rm $images_file_path
