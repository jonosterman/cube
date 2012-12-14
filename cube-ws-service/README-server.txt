###############################################
## generate CA 
###############################################
mkdir ~/cube-pki
cd ~/cube-pki
mkdir root-ca
mkdir root-ca/ca.db.certs
touch root-ca/ca.db.index
echo "1000" > root-ca/ca.db.serial
echo "123456
123456" > password.txt

openssl genrsa -des3 -out root-ca/root-ca.key -passout file:password.txt 2048
## self-sign it 
echo "US
SomeState
SomeCity
SomeOrg
SomeUnit
Test-RootCA
" | openssl req -new -x509 -days 3650 -key root-ca/root-ca.key -out root-ca/root-ca.crt -extensions v3_ca -passin file:password.txt
## check it
openssl x509 -in root-ca/root-ca.crt -text

###############################################
## client certificates :
## - cube-XX_auth	: (digitalSignature) (client auth,Microsoft Enrollment Infrastucture: smartcardlogon)
## - cube-XX_enciph	: (keyEncipherment, dataEncipherment) (email protection)
## - cube-XX_sign	: (digitalSignature, nonRepudiation) (email protection)
###############################################

for((i=0;i<5;i++)); do
## private/public keys
openssl genrsa -des3 -out client${i}-auth.key -passout file:password.txt 2048
openssl genrsa -des3 -out client${i}-enciph.key -passout file:password.txt 2048
openssl genrsa -des3 -out client${i}-sign.key -passout file:password.txt 2048

## certificate AUTH
echo "US
SomeState
SomeCity
SomeOrg
SomeUnit
client${i}


" | openssl req -key client${i}-auth.key -new -out client${i}-auth.req -passin file:password.txt
echo '[auth_sect]
keyUsage=digitalSignature
extendedKeyUsage=clientAuth,msSmartcardLogin
' > client-auth.cnf
openssl x509 -req -in client${i}-auth.req \
 -CA root-ca/root-ca.crt -CAkey root-ca/root-ca.key -CAserial root-ca/ca.db.serial \
 -extfile client-auth.cnf -extensions auth_sect -out client${i}-auth.pem -passin file:password.txt

## certificate ENCIPH
echo "US
SomeState
SomeCity
SomeOrg
SomeUnit
client${i}


" | openssl req -key client${i}-enciph.key -new -out client${i}-enciph.req -passin file:password.txt
echo '[enciph_sect]
keyUsage=keyEncipherment, dataEncipherment
extendedKeyUsage=emailProtection
' > client-enciph.cnf
openssl x509 -req -in client${i}-enciph.req \
 -CA root-ca/root-ca.crt -CAkey root-ca/root-ca.key -CAserial root-ca/ca.db.serial \
 -extfile client-enciph.cnf -extensions enciph_sect -out client${i}-enciph.pem  -passin file:password.txt

## certificate SIGN
echo "US
SomeState
SomeCity
SomeOrg
SomeUnit
client${i}


" | openssl req -key client${i}-sign.key -new -out client${i}-sign.req -passin file:password.txt
echo '[sign_sect]
keyUsage=digitalSignature, nonRepudiation
extendedKeyUsage=emailProtection
' > client-sign.cnf
openssl x509 -req -in client${i}-sign.req \
 -CA root-ca/root-ca.crt -CAkey root-ca/root-ca.key -CAserial root-ca/ca.db.serial \
 -extfile client-sign.cnf -extensions sign_sect -out client${i}-sign.pem -passin file:password.txt

## create PKCS12 files
openssl pkcs12 -export \
 -inkey client${i}-auth.key  \
 -in client${i}-auth.pem   \
 -name client${i}-auth \
 -out client${i}-auth.p12 \
 -passin file:password.txt \
 -passout file:password.txt
openssl pkcs12 -export \
 -inkey client${i}-enciph.key  \
 -in client${i}-enciph.pem   \
 -name client${i}-enciph \
 -out client${i}-enciph.p12 \
 -passin file:password.txt \
 -passout file:password.txt
openssl pkcs12 -export \
 -inkey client${i}-sign.key  \
 -in client${i}-sign.pem   \
 -name client${i}-sign \
 -out client${i}-sign.p12 \
 -passin file:password.txt \
 -passout file:password.txt

#openssl pkcs12 -info -in client${i}.p12

## create jks with all three certificates
keytool -srcstorepass 123456 -storepass 123456 -importkeystore -srckeystore client${i}-auth.p12 -srcstoretype PKCS12 -deststoretype JKS -destkeystore client${i}.jks
keytool -srcstorepass 123456 -storepass 123456 -importkeystore -srckeystore client${i}-enciph.p12 -srcstoretype PKCS12 -deststoretype JKS -destkeystore client${i}.jks
keytool -srcstorepass 123456 -storepass 123456 -importkeystore -srckeystore client${i}-sign.p12 -srcstoretype PKCS12 -deststoretype JKS -destkeystore client${i}.jks


done

###############################################
## Server Certificates
###############################################
openssl genrsa -des3 -out server.key -passout file:password.txt 2048
#(need a password: 123456)
## Then we can create our server certificate signing request (csr)
echo "US
SomeState
SomeCity
SomeOrg
SomeUnit
server.cube.com


" | openssl req -new -key server.key -out server.csr -passin file:password.txt
#(need CN: server.cube.com or what you will use for url to access your dev webservice: HAVE TO MATCH!!)
#(no password needed)
## sign CSR (first server)
openssl x509 -req -days 1000 -in server.csr \
 -CA root-ca/root-ca.crt -CAkey root-ca/root-ca.key -CAserial root-ca/ca.db.serial \
 -out server.crt  -passin file:password.txt

## create pkcs12 file for server with both public and private keys
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -passin file:password.txt -passout file:password.txt

## We need now to transform the pkcs12 to a keystore file.
keytool -srcstorepass 123456 -storepass 123456 -importkeystore -srckeystore server.p12 -srcstoretype PKCS12 -deststoretype JKS -destkeystore server.jks

#check with keytool -v -list -keystore server.jks

## trusted store
keytool -storepass 123456 -genkey -alias dummy -keyalg RSA -keystore truststore.jks
#(no password)
keytool -delete -alias dummy -keystore truststore.jks
keytool -import -v -trustcacerts -alias my_ca -file root-ca/root-ca.crt -keystore truststore.jks
keytool -v -list -keystore truststore.jks


##################################################
## Update maven config
##################################################
vi ~/.m2/settings.xml 
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository/>
  <interactiveMode/>
  <usePluginRegistry/>
  <offline/>
  <pluginGroups/>
  <servers>
   <server>
    <id>tomcat-localhost</id>
    <username>tomcat</username>
    <password>tomcat</password>
   </server>
  </servers>
  <mirrors/>
  <proxies/>
  <profiles/>
  <activeProfiles/>
</settings>

##################################################
## Setup Tomcat7
##################################################
apt-get install tomcat7 tomcat7-admin

sudo vi /etc/tomcat7/tomcat-users.xml
  <role rolename="manager-gui"/>
  <role rolename="manager"/>
  <user username="tomcat" password="tomcat" roles="tomcat,manager,manager-gui"/>

sudo cp /etc/tomcat7/server.xml /etc/tomcat7/server.xml_ori
sudo vi /etc/tomcat7/server.xml
<Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
               maxThreads="150" scheme="https" secure="true"
               clientAuth="true" sslProtocol="TLS" 
               keystoreFile="/etc/tomcat7/certs/server.jks" keystorePass="123456"
               SSLVerifyClient="require" 
               truststoreFile="/etc/tomcat7/certs/truststore.jks" truststorePass="123456"
/>

sudo mkdir /etc/tomcat7/certs
sudo cp server.jks /etc/tomcat7/certs
sudo cp truststore.jks /etc/tomcat7/certs
sudo service tomcat7 restart

##################################################
## Test with browser
##################################################

## import root-ca.crt as new authority in firefox certificate manager
## -> website
## -> email
## -> dev

## import client0-auth.p12 as new 'your certificates' in firefox certificate manager
## password : 123456

## update /etc/hosts with hostname in cert
sudo vi /etc/hosts
127.0.0.1       localhost server.cube.com

## browse
https://server.cube.com:8443/manager/html





