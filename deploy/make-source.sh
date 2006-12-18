#!/bin/sh

BASE_DIR=source_code
VERSION=`grep "VERSION = "  ../src/net/sourceforge/tnv/TNV.java | cut -d'"' -f2`
DEST=tnv_src-$VERSION

rm $BASE_DIR/tnv_source_$VERSION.zip
cd $BASE_DIR
mkdir -p $DEST/source
cp -r ../../src/* $DEST/source
cp ../readmes/* $DEST
find $DEST -name CVS -type d -exec rm -rf {} \;
zip -qr tnv_source_$VERSION.zip $DEST
rm -rf $DEST
echo "Created tnv_source_$VERSION.zip"

/sw/bin/ncftpput upload.sourceforge.net /incoming tnv_source_$VERSION.zip
echo "Uploaded tnv_source_$VERSION.zip to upload.sourceforge.net"