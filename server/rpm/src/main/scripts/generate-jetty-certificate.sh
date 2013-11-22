#!/bin/bash

JETTY_HOME=`dirname $0`
JETTY_CERT=${JETTY_HOME}/jetty.jks

# If certificate exists, then do nothing.
if [ -f "${JETTY_CERT}" ]; then
  exit 0;
fi

# File is needed for OpenSSL.
RANDFILE=${JETTY_HOME}/.rnd
touch ${RANDFILE}
export RANDFILE

cd ${JETTY_HOME}

echo "Creating SSL certificate for Jetty..."

# Get the full hostname of the machine.
FULL_HOSTNAME=`hostname -f`

cat > openssl.cfg <<EOF
[ req ]
distinguished_name     = req_distinguished_name
x509_extensions        = v3_ca
prompt                 = no
input_password         = jettycred
output_password        = jettycred

dirstring_type = nobmp

[ req_distinguished_name ]
C = EU
CN = ${FULL_HOSTNAME}

[ v3_ca ]
basicConstraints = CA:false
nsCertType=server, client, email, objsign
keyUsage=critical, digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment, keyAgreement
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always

EOF

# Generate initial private key.
openssl genrsa -passout pass:jettycred -des3 -out test-key.pem 2048

# Create a certificate signing request.
openssl req -new -key test-key.pem -out test.csr -config openssl.cfg

# Create (self-)signed certificate. 
openssl x509 -req -days 365 -in test.csr -signkey test-key.pem \
             -out test-cert.pem -extfile openssl.cfg -extensions v3_ca \
             -passin pass:jettycred

# Convert to PKCS12 format. 
openssl pkcs12 -export -in test-cert.pem -inkey test-key.pem -out test.p12 \
               -passin pass:jettycred -passout pass:jettycred

# Import PKCS12 certificate/key into the java store.
keytool -importkeystore \
        -srckeystore test.p12 \
        -srcstoretype pkcs12 \
        -srcstorepass jettycred \
        -destkeystore ${JETTY_CERT} \
        -deststoretype jks \
        -deststorepass jettycred

# Remove intermediate files.
rm -f openssl.cfg test-key.pem test.csr test-cert.pem
