# Test SSL connection locally with self-signed certificate

1) Follow the steps below to generate a server certificate signed by an intermediate CA -
   Explanation of each step can be found here - https://jamielinux.com/docs/openssl-certificate-authority/introduction.html

## Root Certificate

### Prepare the directory

mkdir -p ~/root/ca
cd ~/root/ca
mkdir certs crl newcerts private
chmod 700 private
touch index.txt
echo 1000 > serial

### Prepare the configuration file

Copy the root CA configuration file from the https://jamielinux.com/docs/openssl-certificate-authority/appendix/root-configuration-file.html to ~/root/ca/openssl.cnf
Note: Edit the directory path in the configuration file accordingly: ex: `dir = /Users/johndoe/root/ca`


### Create the CA root key
cd ~/root/ca
openssl genrsa -aes256 -out private/ca.key.pem 4096
chmod 400 private/ca.key.pem

### Create the CA root certificate

cd ~/root/ca
openssl req -config openssl.cnf \
-key private/ca.key.pem \
-new -x509 -days 7300 -sha256 -extensions v3_ca \
-out certs/ca.cert.pem

```
Example: 
Country Name (2 letter code) [GB]:US
State or Province Name [England]:Washington
Locality Name []:Seattle
Organization Name [Alice Ltd]:OCI
Organizational Unit Name []:
Common Name []:OCI Self-Signed CA
Email Address []:
```

chmod 444 certs/ca.cert.pem

### Verify the root certificate

openssl x509 -noout -text -in certs/ca.cert.pem


## Intermediate Certificate

### Prepare the directory

mkdir ~/root/ca/intermediate
cd ~/root/ca/intermediate
mkdir certs crl csr newcerts private
chmod 700 private
touch index.txt
echo 1000 > serial

### Prepare the configuration file

Copy the intermediate CA configuration file from  https://jamielinux.com/docs/openssl-certificate-authority/appendix/intermediate-configuration-file.html to ~/root/ca/intermediate/openssl.cnf
Note: Edit the directory path in the configuration file accordingly. ex: `/Users/johndoe/root/ca/intermediate`

### Create the intermediate key

cd ~/root/ca
openssl genrsa -aes256 \
-out intermediate/private/intermediate.key.pem 4096
chmod 400 intermediate/private/intermediate.key.pem

### Create CSR for the intermediate certificate
cd ~/root/ca
openssl req -config intermediate/openssl.cnf -new -sha256 \
-key intermediate/private/intermediate.key.pem \
-out intermediate/csr/intermediate.csr.pem

```
Example:
Country Name (2 letter code) [GB]:US
State or Province Name [England]:Washington
Locality Name []:Seattle
Organization Name [Alice Ltd]:OCI
Organizational Unit Name []:
Common Name []:OCI Self-Signed Intermediate CA
Email Address []:
```

### Create the intermediate certificate

cd ~/root/ca
openssl ca -config openssl.cnf -extensions v3_intermediate_ca \
-days 3650 -notext -md sha256 \
-in intermediate/csr/intermediate.csr.pem \
-out intermediate/certs/intermediate.cert.pem
chmod 444 intermediate/certs/intermediate.cert.pem

### Create the certificate chain file

cat intermediate/certs/intermediate.cert.pem \
certs/ca.cert.pem > intermediate/certs/ca-chain.cert.pem
chmod 444 intermediate/certs/ca-chain.cert.pem

### Verify the intermediate certificate

openssl x509 -noout -text \
-in intermediate/certs/intermediate.cert.pem

### Verify the intermediate certificate against the root certificate

openssl verify -CAfile certs/ca.cert.pem \
intermediate/certs/intermediate.cert.pem


## Server Certificate

### Create a key

cd ~/root/ca
openssl genrsa -aes256 \
-out intermediate/private/example.oci.key.pem 2048
chmod 400 intermediate/private/example.oci.key.pem

### Create a CSR for server certificate

cd ~/root/ca
openssl req -config intermediate/openssl.cnf \
-key intermediate/private/example.oci.key.pem \
-new -sha256 -out intermediate/csr/example.oci.csr.pem

```
Example:
Country Name (2 letter code) [GB]:US
State or Province Name [England]:Washington
Locality Name []:Seattle
Organization Name [Alice Ltd]:OCI
Organizational Unit Name []:
Common Name []:example.oci
Email Address []:
```

### Create a server certificate

cd ~/root/ca
openssl ca -config intermediate/openssl.cnf \
-extensions server_cert -days 375 -notext -md sha256 \
-in intermediate/csr/example.oci.csr.pem \
-out intermediate/certs/example.oci.cert.pem
chmod 444 intermediate/certs/example.oci.cert.pem

### Verify the certificate

openssl x509 -noout -text \
-in intermediate/certs/example.oci.cert.pem

### Verify certificate has a valid chain of trust

openssl verify -CAfile intermediate/certs/ca-chain.cert.pem \
intermediate/certs/example.oci.cert.pem

### Remove password from certificate key file

cd ~/root/ca
openssl pkcs8 -topk8 -inform pem -in intermediate/private/example.oci.key.pem -outform pem -nocrypt -out intermediate/private/example.oci.key.pem.unenc


2) Deploy certificate to sever, by providing the right path of the server certificate (i.e. example.oci.cert.pem), the private key (i.e. example.oci.key.pem.unenc), and intermediate certificate
   generated above. Set the respective paths in your SSL configuration. See config/base.conf ssl section. 

3) Add root certificate to a keychain using Keychain Access on Mac. Read more on how to do this -
   https://support.apple.com/guide/keychain-access/add-certificates-to-a-keychain-kyca2431/mac

4) Import root certificate java keystore
   sudo keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -alias oci_self_signed_ca -file ca.cert.pem

5) Edit the /etc/hosts file on your machine to point the server CommonName (provided while generating the server certificate CSR), to your local interface IP address (usually the en0 interface IP on mac).
   E.g.:  127.0.0.1  example.oci

6) Test if the SSL connection works using one of the following commands.
    * curl -d "message to echo" https://example.oci:<server-port> -vvv
    * openssl s_client -connect example.oci:<server-port>