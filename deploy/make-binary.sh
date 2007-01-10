#!/bin/sh

BASE_DIR=java_binary
VERSION=`grep "VERSION = "  ../src/net/sourceforge/tnv/TNV.java | cut -d'"' -f2`
DEST=tnv-$VERSION

rm $BASE_DIR/tnv_java_$VERSION.zip
cd $BASE_DIR
mkdir -p $DEST
cp -r ../TNV/* $DEST
mkdir $DEST/docs
cp ../readmes/* $DEST/docs
find $DEST -name CVS -type d -exec rm -rf {} \;
zip -qr tnv_java_$VERSION.zip $DEST
rm -rf $DEST
echo "Created tnv_java_$VERSION.zip"

/sw/bin/ncftpput upload.sourceforge.net /incoming tnv_java_$VERSION.zip
echo "Uploaded tnv_java_$VERSION.zip to upload.sourceforge.net"

cd ..
mkdir -p mac_x86_binary/tnv_x86-$VERSION/tnv_osx_x86-$VERSION/docs
cp readmes/* mac_x86_binary/tnv_x86-$VERSION/tnv_osx_x86-$VERSION/docs
mkdir -p mac_ppc_binary/tnv_ppc-$VERSION/tnv_osx_ppc-$VERSION/docs
cp readmes/* mac_ppc_binary/tnv_ppc-$VERSION/tnv_osx_ppc-$VERSION/docs
echo 'Created mac directories'
