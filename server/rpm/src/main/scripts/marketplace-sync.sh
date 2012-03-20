#!/bin/bash

CONFIG=/etc/stratuslab/marketplace-sync.cfg

MASTER=marketplace.stratuslab.eu
SLAVE=http://localhost:8080
DATADIR=/var/lib/stratuslab

if [ -f $CONFIG ]; then
    . $CONFIG
fi

/usr/bin/rsync -avz $USER@$MASTER:/var/lib/stratuslab/* $DATADIR | grep .xml |sort > $DATADIR/sync

curl $SLAVE/sync
