#!/bin/sh

./gradlew rsocket-requests-lease:runProxy -DADDRESS=$1 -DSERVERS=$2