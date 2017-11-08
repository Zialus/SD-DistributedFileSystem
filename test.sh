#!/bin/bash

set -x

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