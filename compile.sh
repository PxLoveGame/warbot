#!/bin/bash
set -e 
set -o pipefail
#/usr/lib/jvm/java-8-oracle/bin/
javac -d ./bin -cp ../warbot-3.3.3.jar src/TeamPoutine/*.java
cd bin
jar -cf ../../teams/TeamPoutine.jar ./*
cd ../..
# pkill -f "java -jar warbot-3.3.3.jar"
java -jar warbot-3.3.3.jar
