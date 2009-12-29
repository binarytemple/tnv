#!/bin/sh
# Startup script for Linux, Unix, and MacOS X
# Edit the path to the Java 1.5+ JRE and the memory sizes below

# Edit the path to the Java JRE if necessary
JAVA="java"

# Options for JVM
# To force a 32 bit kernel, use -d32
JAVA_OPTIONS=""

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
		#check for 64 or 32 bit JVM
		VERSION_64=`java -version 2>&1 | tail -n 1 | grep 64-Bit`
		if [ "$VERSION_64" != "" ] ; then
			SYSTEM=$SYSTEM/64
		else
			SYSTEM=$SYSTEM/32
		fi
	fi
	START_STRING="$JAVA $JAVA_OPTIONS -Djava.library.path=./lib/$SYSTEM $MEMORY $APPLE -jar tnv.jar"
	echo "Starting tnv on $SYSTEM using JRE $JAVA..."
	echo $START_STRING
	`$START_STRING`
else
   echo "Unable to find the correct jpcap library for $SYSTEM"
fi
