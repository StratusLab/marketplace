#!/bin/sh
#=============================================================================
#
#  File      : stratuslab_download_and_convert_images_to_qcow2.sh
#  Date      : Jan 13th, 2016
#  Author    : Oleg Lodygensky
#
#  Change log:
#    Jan 13th, 2016; first version
#
#
# Copyright 2016  CNRS
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#
#=============================================================================

DOWNLOADER="wget"
DOWNLOADEROPTS="--output-document="
type $DOWNLOADER > /dev/null 2>&1
if [ $? -ne 0 ] ; then
  DOWNLOADER="curl"
  DOWNLOADEROPTS="--output "
  type $DOWNLOADER > /dev/null 2>&1
  if [ $? -ne 0 ] ; then
    echo "Please install wget or curl"
    exit 1
  fi
fi

currentDir=`pwd`

for uriDir in `find . -type d` ; do
  [ "$uriDir" == "." ] && continue
  [ "$uriDir" == ".." ] && continue
  #echo $uriDir
  cd $uriDir
  URIPATH=`cat uri.txt`
  URIFILENAME=${URIPATH##http:*/}
  #echo path=$URIPATH
  #echo filename=$URIFILENAME
  echo $DOWNLOADER `cat uri.txt` $DOWNLOADEROPTS$URIFILENAME
  cd $currentDir
done
