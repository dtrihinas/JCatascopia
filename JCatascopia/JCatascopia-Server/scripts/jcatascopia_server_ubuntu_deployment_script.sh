#!/bin/bash

JC_SERVER_VERSION=0.0.1-SNAPSHOT

TOMCAT_VERSION=7.0.55
TOMCAT_DIR=/usr/share

CASSANDRA_VERSION=2.2.3

eval "sed -i 's/127.0.0.1.*localhost.*/127.0.0.1 localhost $HOSTNAME/g' /etc/hosts"

#do an OS repo update...
apt-get update

#download and install java...
apt-get install -y openjdk-7-jre-headless

#download, install and configure cassandra (the Monitoring DB)...
apt-get install -y curl
curl -L http://downloads.datastax.com/community/dsc-cassandra-$CASSANDRA_VERSION-bin.tar.gz | tar xz
mv dsc-cassandra-$CASSANDRA_VERSION/ /var/lib/cassandra
sleep 10s

#install JCatascopia-Server...
tar xvfz JCatascopia-Server-$JC_SERVER_VERSION.tar.gz
cd JCatascopia-Server-*
./installer.sh
mv -f JCatascopia-Server-Cassandra /etc/init.d/JCatascopia-Server
cd ..

#download, install and parameterize tomcat...
if [ ! -d apache-tomcat-$TOMCAT_VERSION ]; then
  wget http://archive.apache.org/dist/tomcat/tomcat-7/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
  tar xvfz apache-tomcat-$TOMCAT_VERSION.tar.gz -C $TOMCAT_DIR/
  mv $TOMCAT_DIR/apache-tomcat-$TOMCAT_VERSION $TOMCAT_DIR/tomcat/
fi

#download and install JCatascopia-Web...
cp JCatascopia-Web.war $TOMCAT_DIR/tomcat/webapps/