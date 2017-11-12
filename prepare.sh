#!/bin/bash

set -x

mkdir -p ~/DFS/A/1
mkdir -p ~/DFS/A/2
mkdir -p ~/DFS/B/1
mkdir -p ~/DFS/B/2
mkdir -p ~/DFS/C/1
mkdir -p ~/DFS/logs

gradle build
