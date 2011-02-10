#!/bin/sh

JAR_FILE=target/metadata-0.0.1-SNAPSHOT-jar-with-dependencies.jar 

cat > example.xml <<EOF
<Envelope xmlns="urn:envelope">
    <dummy/>
</Envelope> 
EOF

java -cp $JAR_FILE eu.stratuslab.marketplace.metadata.SignMetadata example.xml example.xml.sign ~/Desktop/grid.p12 whoson1st
rc=$?
echo SIGNING: $rc
java -cp $JAR_FILE eu.stratuslab.marketplace.metadata.CheckXMLSignatures example.xml.sign
rc=$?
echo VALIDATION: $rc



