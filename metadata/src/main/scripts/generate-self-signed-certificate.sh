#!/bin/bash

cat > openssl.cfg <<EOF
[ req ]
distinguished_name     = req_distinguished_name
x509_extensions        = v3_ca
prompt                 = no
input_password         = XYZXYZ
output_password        = XYZXYZ

dirstring_type = nobmp

[ req_distinguished_name ]
C = EU
O = StratusLab Project
OU = Testing Department
CN = Jane Tester

[ v3_ca ]
basicConstraints = CA:false
nsCertType=client, email, objsign
keyUsage=critical, digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment, keyAgreement
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always
subjectAltName=email:jane.tester@example.org

EOF

# Generate initial private key.
openssl genrsa -passout pass:XYZXYZ -des3 -out test-key.pem 2048

# Create a certificate signing request.
openssl req -new -key test-key.pem -out test.csr -config openssl.cfg

# Create (self-)signed certificate. 
openssl x509 -req -days 365 -in test.csr -signkey test-key.pem \
             -out test-cert.pem -extfile openssl.cfg -extensions v3_ca \
             -passin pass:XYZXYZ

# Convert to PKCS12 format. 
openssl pkcs12 -export -in test-cert.pem -inkey test-key.pem -out test.p12 \
               -passin pass:XYZXYZ -passout pass:XYZXYZ

# Remove intermediate files.
rm -f openssl.cfg test-key.pem test.csr test-cert.pem
