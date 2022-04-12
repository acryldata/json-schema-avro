#!/bin/bash

#
# This will build everything that is needed and push to Maven central.
#

./gradlew --no-build-cache clean test publish

