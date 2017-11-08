#!/bin/bash

set -x

mkdir lib
cd lib
wget -nc "http://central.maven.org/maven2/org/jline/jline/3.5.1/jline-3.5.1.jar"
cd ..

mkdir -p ~/DFS/A/1
mkdir -p ~/DFS/A/2
mkdir -p ~/DFS/B/1
mkdir -p ~/DFS/B/2
mkdir -p ~/DFS/C/1
mkdir -p ~/DFS/logs
