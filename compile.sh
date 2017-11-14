#!/bin/bash
javac -d ./bin -cp ../warbot-3.3.3.jar src/pxl/*.java
cd bin
jar -cf ../../teams/pxl.jar ./*
cd ../..
pkill java
java -jar warbot-3.3.3.jar
