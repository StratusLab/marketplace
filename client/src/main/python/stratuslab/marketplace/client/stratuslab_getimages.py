# coding: utf8
#!/usr/bin/python
#=============================================================================
#
#  File      : stratuslab_images_uri.py
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

from __future__ import unicode_literals
import codecs
import json
import os
import sys
import urllib2

sys.stdout = codecs.getwriter('utf8')(sys.stdout)

debugging = 0

def debug(arg, arg1 = ""):
    if debugging == 1:
        print "DEBUG : ", arg


def usage():
    print "Usage : ", __file__, " [StratuslabMarketplaceEndpointUri]"
    sys.exit(1)


UTF8Writer = codecs.getwriter('utf8')
sys.stdout = UTF8Writer(sys.stdout)

homeDir = os.environ['HOME']
stratuslabMarketplaceEndpointUri = None
stratuslabConfigPath = os.path.join(homeDir, ".stratuslab", "stratuslab-user.cfg")
stratuslabMarketplaceVariableName = "marketplace_endpoint"
jsonMedataPath = "/metadata?media=json"

with open(stratuslabConfigPath, 'r') as entree:
    for ligne in entree:
        if ligne.find(stratuslabMarketplaceVariableName) != -1 :
            stratuslabMarketplaceEndpointUri = ligne.split('=')[1].strip()
      

if len(sys.argv) == 2:
    stratuslabMarketplaceEndpointUri = sys.argv[1]

stratuslabMarketplaceEndpointUri = stratuslabMarketplaceEndpointUri + jsonMedataPath

debug("Marketplace :" +  stratuslabMarketplaceEndpointUri) 

connection = urllib2.urlopen(stratuslabMarketplaceEndpointUri)

if connection == None:
    print "Connection error"
    sys.exit(2)

decoded = json.load(connection)
debug(decoded)

dataLength = len(decoded['aaData'])

print "{ \"images\" : ["

for data in decoded['aaData']:
    dataLength = dataLength - 1
    debug(data)
    appliance = { "osname": data[1], "osversion": data[2], "osext": data[3], "endorser": data[4], "objtype": data[5], "creationdate": data[6], "uuid": data[7], "uri": data[8], "comment": data[9] }

    debug(appliance)
    
    if appliance['uri'].find("pdisk") != -1 :
        debug ("Excluding ", appliance)
        continue
    
    if dataLength > 0 :
        print json.dumps(appliance), ","
    else:
        print json.dumps(appliance)

    os.mkdir(appliance['uuid'])
    uriFilePath = os.path.join(os.curdir, appliance['uuid'], "uri.txt")
    with open(uriFilePath, 'w') as sortie:
        sortie.write(appliance['uri'])

    jsonFilePath = os.path.join(os.curdir, appliance['uuid'], "appliance.json")
    with open(jsonFilePath, 'w') as sortie:
        json.dump(appliance, sortie)

print "] }"
