#!/bin/bash

if [[ -z ${JAVA_HOME+x} ]]
then
  JAVA_PATH=$JAVA_HOME/bin/java
else
  JAVA_PATH=java
fi

$JAVA_PATH -jar take-break-app.jar