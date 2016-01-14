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

import json
import sys


debugging = 0

def debug(arg):
    if debugging == 1:
        print "DEBUG : ", arg


def usage():
    print "Usage : ", __file__, " [jsonfileFromMarketplace]"
    sys.exit(1)

if len(sys.argv) < 2:
    usage()

jsonFile = open(sys.argv[1])
decoded = json.load(jsonFile)


for image in decoded['images']:
    print image['uri']

