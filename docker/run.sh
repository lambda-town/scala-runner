#!/usr/bin/env sh

nohup ./startBloop.sh &
seed bloop > /dev/null
bloop run runner