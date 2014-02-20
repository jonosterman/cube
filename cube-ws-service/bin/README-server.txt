###############################################
## generate PKI in ~/cube-pki  
###############################################

see README-test-PKI.txt

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

sudo mkdir -p /etc/tomcat7/certs
sudo cp server.jks /etc/tomcat7/certs
sudo cp truststore.jks /etc/tomcat7/certs
sudo service tomcat7 restart
cp truststore.jks /opt/cube/client/certificates/

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





