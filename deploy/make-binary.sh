#!/bin/sh

BASE_DIR=java_binary
VERSION=`grep "VERSION = "  ../src/net/sourceforge/tnv/TNV.java | cut -d'"' -f2`
DEST=tnv-$VERSION

rm $BASE_DIR/tnv_java_$VERSION.zip
cd $BASE_DIR
mkdir -p $DEST
cp -r ../TNV/* $DEST
cp -r ../readmes/* $DEST
find $DEST -name CVS -type d -exec rm -rf {} \;
zip -qr tnv_java_$VERSION.zip $DEST
rm -rf $DEST
echo "Created tnv_java_$VERSION.zip"
