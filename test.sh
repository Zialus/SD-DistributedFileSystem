#!/bin/bash

set -x

CLASSPATH=./build/libs/MetadataServer-1.0-SNAPSHOT.jar rmiregistry&
sleep 1

java -jar build/libs/MetadataServer-1.0-SNAPSHOT.jar &> ~/DFS/logs/Meta.log &
sleep 1
java -jar build/libs/StorageServer-1.0-SNAPSHOT.jar ~/DFS/A / localhost &> ~/DFS/logs/Storage1.log &
sleep 1
java -jar build/libs/StorageServer-1.0-SNAPSHOT.jar ~/DFS/B /B localhost &> ~/DFS/logs/Storage2.log &
sleep 1
java -jar build/libs/StorageServer-1.0-SNAPSHOT.jar ~/DFS/C /C localhost &> ~/DFS/logs/Storage3.log &
sleep 1
java -jar build/libs/Client-1.0-SNAPSHOT.jar ./apps.conf localhost < ./some_commands
