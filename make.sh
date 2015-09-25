#!/bin/bash
#
# Author : fcbruce <fcbruce8964@gmail.com>
#
# Time : Thu 24 Sep 2015 06:29:33 PM CST
#
#

JAR_PATH=vjudge_lib
BIN_PATH=bin
SRC_PATH=src

rm -f $SRC_PATH/sources
find $SRC_PATH -name *.java > $SRC_PATH/sources.list

rm -rf $BIN_PATH
mkdir $BIN_PATH

javac -d $BIN_PATH -classpath $JAR_PATH/commons-codec-1.6.jar:$JAR_PATH/commons-httpclient.jar:$JAR_PATH/commons-io-2.4.jar:$JAR_PATH/commons-io-2.4-javadoc.jar:$JAR_PATH/commons-io-2.4-sources.jar:$JAR_PATH/commons-io-2.4-tests.jar:$JAR_PATH/commons-io-2.4-test-sources.jar:$JAR_PATH/commons-logging-1.1.1.jar:$JAR_PATH/fluent-hc-4.2.5.jar:$JAR_PATH/httpclient-4.2.5.jar:$JAR_PATH/httpclient-cache-4.2.5.jar:$JAR_PATH/httpcore-4.2.4.jar:$JAR_PATH/httpmime-4.2.5.jar:$JAR_PATH/mysql-connector-java-5.1.26-bin.jar @$SRC_PATH/sources.list 

cp -r META-INF/ bin/
cd bin/
jar cvfm vjudge.jar META-INF/MANIFEST.MF *
mv vjudge.jar ../
