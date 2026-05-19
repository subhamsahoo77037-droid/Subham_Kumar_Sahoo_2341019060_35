#!/bin/bash
DERBY_JAR=/usr/share/java/derby.jar
mkdir -p out
find src -name "*.java" > sources.txt
javac -cp "$DERBY_JAR" -d out @sources.txt && echo "Compilation successful!"
