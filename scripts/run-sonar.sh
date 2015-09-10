#!/bin/bash

JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Dmaven.test.failure.ignore=true
mvn sonar:sonar
