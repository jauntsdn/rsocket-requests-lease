#!/bin/sh

./gradlew rsocket-requests-lease:runServer -DADDRESS=$1 -DALLOWED_REQUESTS=$2