#!/bin/sh

./gradlew rsocket-requests-lease:runServer -DHOST=$1 -DPORT=$2 -DALLOWED_REQUESTS=$3