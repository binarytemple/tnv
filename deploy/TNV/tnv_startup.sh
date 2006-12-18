#!/bin/sh
# Startup script for Linux, Unix, and MacOS X
# Edit the path to the Java 1.5+ JRE and the memory sizes below

# Edit the path to the Java JRE if necessary
JAVA="java"

# Edit the initial (-Xms) and maximum (-Xmx) heap sizes as appropriate
MEMORY="-Xms512m -Xmx512m"


###########  SHOULD NOT NEED TO EDIT BELOW HERE ###########

# Use uname to find the correct platform's jpcap library
SYSTEM=`uname -s`

# If MacOS, use the Apple Look and Feel (menu on top) and choose ppc or intel
APPLE=""

# Check to see if the libraries are available and start tnv if they are
if [ -d ./lib/$SYSTEM ] ; then
	if [ "$SYSTEM" = "Darwin" ] ; then
	   	APPLE=" -Dapple.laf.useScreenMenuBar=true"
		SYSTEM=$SYSTEM/`uname -p`
	fi
	START_STRING="$JAVA -Djava.library.path=./lib/$SYSTEM $MEMORY $APPLE -jar tnv.jar"
	echo "Starting tnv on $SYSTEM using JRE $JAVA..."
	`$START_STRING`
else
   echo "Unable to find the correct jpcap library for $SYSTEM"
fi
