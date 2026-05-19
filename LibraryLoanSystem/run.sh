#!/bin/bash
# Run Library Loan Management System
DERBY_JAR=/usr/share/java/derby.jar
if [ ! -f "$DERBY_JAR" ]; then
    echo "Derby not found. Run: sudo apt install libderby-java"
    exit 1
fi
java -cp "out:$DERBY_JAR" com.library.ui.MainApp
