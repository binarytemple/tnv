#!/bin/sh

BASE_DIR=java_binary
VERSION=`grep "VERSION = "  ../src/net/sourceforge/tnv/TNV.java | cut -d'"' -f2`
DEST=tnv-$VERSION
MAC_DEST=mac_universal_binary/tnv-$VERSION/tnv_osx-$VERSION

rm $BASE_DIR/tnv_java_$VERSION.zip
cd $BASE_DIR
mkdir -p $DEST
cp -r ../TNV/* $DEST
cp -r ../readmes/* $DEST
find $DEST -name CVS -type d -exec rm -rf {} \;
zip -qr tnv_java_$VERSION.zip $DEST
rm -rf $DEST
echo "Created tnv_java_$VERSION.zip"

/sw/bin/ncftpput upload.sourceforge.net /incoming tnv_java_$VERSION.zip
echo "Uploaded tnv_java_$VERSION.zip to upload.sourceforge.net"

cd ..
mkdir -p $MAC_DEST
cp -r readmes/* $MAC_DEST
find $MAC_DEST -name CVS -type d -exec rm -rf {} \;
cd mac_universal_binary
echo 'Created mac directories'
echo 'To create disk image, run:'
echo "  hdiutil create -srcfolder ./tnv-$VERSION tnv_mac_$VERSION.dmg "
echo 'To upload, run:'
echo "  /sw/bin/ncftpput upload.sourceforge.net /incoming tnv_mac_$VERSION.dmg"
