#!/bin/sh
if [ $# -lt 1 ]
then
  echo "Usage: release.sh [version-number]"
  exit
fi
jar cf shared-test-case-$1.jar java/
jar cf shared-test-case-gwt-$1.jar gwt/
zip -r shared-test-case-objc-$1.zip objc/
