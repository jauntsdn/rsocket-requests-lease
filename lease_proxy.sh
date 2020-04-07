#!/bin/sh

./gradlew rsocket-requests-lease:runProxy -DHOST=$1 -DPORT=$2 -DSERVERS=$3