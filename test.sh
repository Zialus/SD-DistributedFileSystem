#!/bin/bash

mkdir lib
cd lib
wget -nc "http://central.maven.org/maven2/org/jline/jline/3.5.1/jline-3.5.1.jar"
cd ..

set -x

mkdir -p ~/DFS/A/1
mkdir -p ~/DFS/A/2
mkdir -p ~/DFS/B/1
mkdir -p ~/DFS/B/2
mkdir -p ~/DFS/C/1
mkdir -p ~/DFS/logs

cd outd
rmiregistry&
sleep 1
java fcup.MetadataServer &> ~/DFS/logs/Meta.log &
sleep 1
java fcup.StorageServer ~/DFS/A / localhost &> ~/DFS/logs/Storage1.log &
sleep 1
java fcup.StorageServer ~/DFS/B /B localhost &> ~/DFS/logs/Storage2.log &
sleep 1
java fcup.StorageServer ~/DFS/C /C localhost &> ~/DFS/logs/Storage3.log &
sleep 1
java -cp .:../lib/jline-3.5.1.jar fcup.Client ../apps.conf localhost < ../some_commands