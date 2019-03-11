#!/bin/bash

set -x

mkdir -p ~/DFS/A/1
mkdir -p ~/DFS/A/2
mkdir -p ~/DFS/B/1
mkdir -p ~/DFS/B/2
mkdir -p ~/DFS/C/1
mkdir -p ~/DFS/logs

java -jar build/libs/MetadataServer-1.0-SNAPSHOT.jar &> ~/DFS/logs/Meta.log &
sleep 1

java -jar build/libs/StorageServer-1.0-SNAPSHOT.jar ~/DFS/A / localhost &> ~/DFS/logs/Storage1.log &
java -jar build/libs/StorageServer-1.0-SNAPSHOT.jar ~/DFS/B /B localhost &> ~/DFS/logs/Storage2.log &
java -jar build/libs/StorageServer-1.0-SNAPSHOT.jar ~/DFS/C /C localhost &> ~/DFS/logs/Storage3.log &

sleep 1

java -jar build/libs/Client-1.0-SNAPSHOT.jar ./apps.conf localhost < ./some_commands

jobs

kill %4
kill %3
kill %2
sleep 1
kill %1

sleep 1

cat ~/DFS/logs/Meta.log
cat ~/DFS/logs/Storage1.log
cat ~/DFS/logs/Storage2.log
cat ~/DFS/logs/Storage3.log
