#!/bin/bash

docker run -d -p 8000:8000 -v=/var/run/docker.sock:/tmp/docker.sock gliderlabs/logspout 
sleep 3
curl "$(docker port $(docker ps -lq) 8000)/logs"
