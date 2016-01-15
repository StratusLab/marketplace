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

DEBUG=1
INFO=2
WARN=3
ERROR=4

LOGLEVEL=$INFO

debug() {
  if [ $LOGLEVEL -le $DEBUG ] ; then
    echo "[`date`] ($0) DEBUG: $*"
  fi
}

info() {
  if [ $LOGLEVEL -le $INFO ] ; then
    echo "[`date`] ($0) INFO: $*"
  fi
}

warn() {
  if [ $LOGLEVEL -le $WARN ] ; then
    echo "[`date`] ($0) WARN: $*"
  fi
}

error() {
  if [ $LOGLEVEL -le $ERROR ] ; then
    echo "[`date`] ($0) ERROR: $*"
  fi
}

fatal() {
  echo "[`date`] ($0) FATAL: $*"
  exit 1
}

message() {
  echo "[`date`] ($0) : $*"
}


usage () {
  cat << USAGEEOF

Usage: $0 [ --help | --download | --log [ 1 | 2 | 3 | 4  ] ]
   --help     : to see this help
   --download : to download and convert images
              : if not provided this scripts only shows what it could do
   --log      : to pecify the logger level
              : 1 - DEBUG
              : 2 - INFO
              : 3 - WARN
              : 4 - ERROR

 This script :
  * crosses first level subdirectories and looks for 'uri.txt' file
  * downloads the image from 'uri.txt'
  * converts downloaded image to QCOW2 format

USAGEEOF

  exit 0
}


DOWNLOAD=""

while [ $# -gt 0 ]
do

  case $1 in

  --help | -h )
    usage
    ;;

  --download )
    DOWNLOAD="1"
    ;;  

  --log )
    shift
    LOGLEVEL=$1
    ;;  

  esac

  shift

done


DOWNLOADER="wget"
DOWNLOADEROPTS="--output-document="
type $DOWNLOADER > /dev/null 2>&1
if [ $? -ne 0 ] ; then
  DOWNLOADER="curl"
  DOWNLOADEROPTS="--output "
  type $DOWNLOADER > /dev/null 2>&1
  if [ $? -ne 0 ] ; then
    fatal "Please install wget or curl"
  fi
fi

CONVERTER="qemu-img"
CONVERTEROPTS="convert -f raw -O qcow2"
type $DOWNLOADER > /dev/null 2>&1
if [ $? -ne 0 ] ; then
  warn "qemu-img not found. Will not be able to convert to QCOW2 format"
  CONVERTER=""
fi

GUNZIP="gunzip"
type $GUNZIP > /dev/null 2>&1
if [ $? -ne 0 ] ; then
  fatal "Please install gunzip"
fi


currentDir=`pwd`

if [ ! "$DOWNLOAD" ] ; then
  message "demo mode only (not downloading anything)"
fi

for uriDir in `find . -type d` ; do
  [ "$uriDir" == "." ] && continue
  [ "$uriDir" == ".." ] && continue
  debug "LOCALPATH $uriDir"
  cd $uriDir
  URIPATH=`cat uri.txt`


  echo $URIPATH | grep -E "^http://" > /dev/null 2>&1
  if [ $? -ne 0 ] ; then
    warn "unable to download $URIPATH"
    cd $currentDir
    continue
  fi

  URIFILENAME=${URIPATH##http:*/}
  echo $URIPATH | grep -E "^http://172\.|^http://10\.|^http://192\." > /dev/null 2>&1
  if [ $? -eq 0 ] ; then
    warn "unable to download $URIPATH"
    cd $currentDir
    continue
  fi

  URIFILENAME=${URIPATH##http:*/}
  debug URL=$URIPATH
  debug FILENAME=$URIFILENAME
  BASENAME=`echo $URIFILENAME | cut -d'.' -f 1`
  QCOW2FILENAME="${BASENAME}.qcow2"
  debug QCOW2FILENAME=$QCOW2FILENAME

  DOWNLOADERROR=1
  if [ "$DOWNLOAD" ] ; then
    info "(exec) $DOWNLOADER `cat uri.txt` $DOWNLOADEROPTS$URIFILENAME"
    $DOWNLOADER `cat uri.txt` $DOWNLOADEROPTS$URIFILENAME
    DOWNLOADERROR=$?
  else
    info "(could do) $DOWNLOADER `cat uri.txt` $DOWNLOADEROPTS$URIFILENAME"
  fi

  FILENAME=$URIFILENAME
  UNZIPERROR=1

  echo $URIFILENAME | grep -E ".gz$" >/dev/null 2>&1
  if [ $? -eq 0  ] ; then
    UNCOMPRESSEDFILENAME=`echo $URIFILENAME | sed "s/\.gz//"`
    debug "UNCOMPRESSEDFILENAME=$UNCOMPRESSEDFILENAME"
    if [ $DOWNLOADERROR -eq 0 ] ; then
      info "(exec) $GUNZIP $URIFILENAME"
      $GUNZIP $URIFILENAME
      UNZIPERROR=$?
    else
      info "(could do) $GUNZIP $URIFILENAME"
    fi
    FILENAME=$UNCOMPRESSEDFILENAME
  fi

  debug "FILENAME=$FILENAME"
  if [ "$CONVERTER" ] ; then
    if [ "$UNZIPERROR" -eq 0 ] ; then
      info "(exec) $CONVERTER $CONVERTEROPTS $FILENAME $QCOW2FILENAME"
      $CONVERTER $CONVERTEROPTS $FILENAME $QCOW2FILENAME
    else
      info "(could do) $CONVERTER $CONVERTEROPTS $FILENAME $QCOW2FILENAME"
    fi
  fi

  cd $currentDir

done

