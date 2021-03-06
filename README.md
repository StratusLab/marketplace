Marketplace
===========

[![Build Status](https://secure.travis-ci.org/StratusLab/marketplace.png)](https://secure.travis-ci.org/StratusLab/marketplace.png)

Contains the Marketplace, a registry of virtual appliances, that 
promotes sharing of those appliances and trust between the creators, 
users, and cloud administrators.

System Requirements
-------------------

The only requirement is JDK v1.7+. As such the Marketplace should install on most systems, once this dependency is met.

The metadata files and searchable metadata index, are stored locally as flat files. The Marketplace host should have adequate storage for these.

Installation
-------------

StratusLab runs its own YUM repository, so you must add it to your YUM configuration. Drop a file (named say stratuslab-releases.repo) in the /etc/yum.repos.d/ with the following content, modifying the OS as required:
```
[StratusLab-Releases]
name=StratusLab-Releases
baseurl=http://yum.stratuslab.eu/releases/centos-6.2-v2.0
gpgcheck=0
enabled=1
```
With this in place, you can now install the package:

```
yum install stratuslab-marketplace
```

This will install the Marketplace, along with a default configuration file /etc/stratuslab/marketplace.cfg. This file should be modified to configure the Marketplace as required.

An important step is to ensure that the two directories data.dir and pending.dir exist and are writeable by the user that will be running the Marketplace instance.

By default the Marketplace will use a Memory store as the backend. This type of store is volatile, and any uploaded metadata will not persist if the Marketplace is restarted. For this reason, for use in a production environment the Marketplace should be configured to use a Native store as the storage backend.

Starting the service
---------------------

The Marketplace can be started with the following command:

/etc/init.d/marketplace start

This will start the Jetty server. By default this will start on port 8081, meaning the Marketplace can be accessed on http://localhost:8081. The port can be changed by modifying the file /opt/stratuslab/marketplace/etc/jetty-stratuslab.xml.

Configuration reference
------------------------

The following describes the parameters in the Marketplace configuration file.
```
# Directory containing raw metadata data entries.
data.dir=/var/lib/stratuslab/marketplace

# Directory for pending (unconfirmed) entries.
pending.dir=/var/lib/stratuslab/pending

# Storage type for metadata database (memory or native)
store.type=native

# Flag to determine if endorser email address must be validated.
validate.email=false

# Flag to enable/disable Endorser reminder emails
endorser.reminder=false

# Email address for Marketplace administrator.
admin.email=admin@example.org

# Host for SMTP server for sending email notifications.
mail.host=smtp.example.org

Port on SMTP server (defaults to standard ports).
mail.port=465

# Username for SMTP server.
mail.user=no-reply@example.org

Password for SMTP server.
mail.password=xxxxxxx

# Use SSL for SMTP server (default is 'true').
mail.ssl=true

# Debug mail sending (default is 'false').
mail.debug=false

```

Replication settings
---------------------

The Marketplace can be configured to allow for replication. This is done through a central Git repository. 

```
replication.enabled=true|false
replication.type=public|private
```

With replication enabled the Marketplace will periodically pull metadata files from the configured Git repository in order to update its internal index. If the replication type is set to public, metadata files uploaded to the Marketplace will be pushed to the Git repository. With private replication the files will not be pushed, and will only be available locally.

The Git repository to use is set with the following property:

```
git.uri=https://github.com/repo/metadata.git
```

Authentication with the Git repository can be either by username and password or SSH key. For username/password set:
```
git.user=
git.password=
```

For SSH key authentication use:
```
git.key=
git.key.passphrase=
git.known.hosts=
```

Email verification
-------------------

It is possible to configure the Marketplace to require email verification of uploaded metadata. The Marketplace will send an email to the metadata endorser requiring them to verify their uploaded entry. Whilst awaiting verification the metadata will be stored in the pending.dir.

The relevant configuration parameters are:

```
validate.email=true
admin.email=admin@example.org
mail.host=smtp.example.org
mail.port=465
mail.user=no-reply@example.org
mail.password=xxxxxxx
mail.ssl=true
mail.debug=false
```

Changing the Marketplace Style
-------------------------------

The current release contains code to allow the page style to be changed easily. The instructions are the following:

1. Copy the current web archive (war) for the service to a temporary directory.
```
$ cp /opt/stratuslab/marketplace/webapps/marketplace-server-war-1.0.13-SNAPSHOT.war . 
```
2. Extract the contents of the archive. (You must have the java development tools installed.)
```
$ jar xf marketplace-server-war-1.0.13-SNAPSHOT.war 
```
3. Create a new directory hierarchy to hold the modified style files. You can choose any path here, but it is best to follow the usual java conventions and use an inverted domain name. This CANNOT be eu/stratuslab/style/css if you want your changes to be visible.
```
$ mkdir -p eu/egi/style/css 
```
4. Copy the current style files into your new directory hierarchy.
```
$ cp WEB-INF/classes/eu/stratuslab/style/css/* eu/egi/style/css/ 
```
5. Change the CSS and other files as you like. Note that some of the images are used by the javascript for the service, so it is better to replace images or add new ones rather than delete existing ones.

6. Create a new jar file with your modified style files and verify the correct hierarchy within the jar file.
```
$ jar cf egi-style.jar eu 
$ jar tf egi-style.jar 
```
7. Drop this jar file into the Marketplace server's extensions directory.
```
$ cp egi-style.jar /opt/stratuslab/marketplace/lib/ext/ 
```
8. Modify the server's configuration file by adding the style.path option. NOTE: The path must match the path in the jar file and MUST start and end with a slash!
```
style.path=/eu/egi/style/css/ 
```
9. Restart the service.
```
$ service marketplace restart 
```
At this point your new style should be active.

If necessary you can also perform the same operation for the service javascript. The configuration option is named js.path and the procedure is complete analogous to the one used for style.path.


Using the client
----------------
The client eases appliances update/migration.
This has been written to be able to test and use Stratuslab appliances (e.g. locally, with other cloud middlewares).
It is made of a python script that saves appliances metadata having an HTTP URI, each appliance on their own directory labelled by the appliance uuid.
It also contains a shell script as exemple.

* stratuslab_getimages.py
This downloads appliances metadata and dumps those that are subject to be downloaded (aka. images with an HTTP URI).
This script expects a valid stratuslab marketplace endpoint either read from $HOME/.stratuslab/stratuslab-ser.cfg or provided on the command line.
The appliance metadata are dump to stdout
(an 'images.json' is provided as an output example).
For each appliance, this scripts creates one directory labelled with the appliance uuid where it stores two files:
appliance.json contains the appliance metadata; uri.txt contains the appliance image URI only.

* stratuslab_download_and_convert_images_to_qcow2.sh
Having this URI, the user can download appliances image to test them locally, convert them to another format etc.
A shell script is provided as exemple; this read uri.txt from each found directory.

License
-------

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License.  You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.

Acknowledgements
----------------

This software originated in the StratusLab project that was co-funded
by the European Community’s Seventh Framework Programme (Capacities)
Grant Agreement INFSO-RI-261552 and that ran from June 2010 to May
2012.
