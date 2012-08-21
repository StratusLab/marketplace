#!/bin/bash

CONFIG=/etc/stratuslab/marketplace-sync.cfg

MASTER=marketplace.stratuslab.eu
SLAVE=http://localhost:8080
DATADIR=/var/lib/stratuslab
MASTER_DIR=$DATADIR

if [ -f $CONFIG ]; then
    . $CONFIG
fi

/usr/bin/rsync -avz $USER@$MASTER:$MASTER_DIR/* $DATADIR | grep .xml | awk -F ".xml" '{print $1}' | sort > $DATADIR/.sync

curl -X POST -sS $SLAVE/sync

echo ""
