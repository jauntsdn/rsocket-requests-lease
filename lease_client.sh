#!/bin/sh

./gradlew rsocket-requests-lease:runClient -DHOST=$1 -DPORT=$2